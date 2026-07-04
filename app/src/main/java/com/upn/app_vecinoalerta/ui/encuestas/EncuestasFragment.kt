package com.upn.app_vecinoalerta.ui.encuestas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.navigation.fragment.findNavController
import com.upn.app_vecinoalerta.R
import com.upn.app_vecinoalerta.data.local.entities.EncuestaEntity
import com.upn.app_vecinoalerta.databinding.FragmentEncuestasBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EncuestasFragment : Fragment() {

    private var _binding: FragmentEncuestasBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EncuestaViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEncuestasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvEncuestas.layoutManager = LinearLayoutManager(requireContext())

        val rol = com.upn.app_vecinoalerta.utils.SecurePrefs.getString(requireContext(), "rol", "") ?: ""

        if (rol == "ADMINISTRADOR") {
            binding.fabNuevaEncuesta.visibility = View.VISIBLE
            binding.fabNuevaEncuesta.setOnClickListener {
                mostrarDialogoNuevaEncuesta()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.encuestasActivas.collect { lista ->
                    binding.tvSinEncuestas.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
                    binding.rvEncuestas.adapter = EncuestasAdapter(
                        list = lista,
                        observeResults = { id -> viewModel.observarResultados(id) },
                        scope = viewLifecycleOwner.lifecycleScope
                    ) { encuesta ->
                        val args = Bundle().apply {
                            putInt("id_encuesta", encuesta.idEncuesta)
                            putString("pregunta", encuesta.pregunta)
                        }
                        findNavController().navigate(R.id.dest_votar, args)
                    }
                }
            }
        }
    }

    private fun mostrarDialogoNuevaEncuesta() {
        val context = requireContext()
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_nueva_encuesta, null)
        val etPregunta = dialogView.findViewById<android.widget.EditText>(R.id.etPregunta)
        val layoutOpciones = dialogView.findViewById<android.widget.LinearLayout>(R.id.layoutOpciones)
        val btnAgregarOpcion = dialogView.findViewById<android.widget.Button>(R.id.btnAgregarOpcion)

        fun agregarCampoOpcion(placeholder: String) {
            val etOpcion = android.widget.EditText(context).apply {
                hint = placeholder
                setHintTextColor(android.graphics.Color.parseColor("#9CA3AF"))
                setTextColor(android.graphics.Color.parseColor("#1F2937"))
                setPadding(20, 20, 20, 20)
                setBackgroundResource(R.drawable.bg_input_field)
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.setMargins(0, 0, 0, 12) }
            }
            layoutOpciones.addView(etOpcion)
        }

        agregarCampoOpcion("Opción 1")
        agregarCampoOpcion("Opción 2")

        btnAgregarOpcion.setOnClickListener {
            val numOpciones = layoutOpciones.childCount
            agregarCampoOpcion("Opción ${numOpciones + 1}")
        }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(context)
            .setView(dialogView)
            .setPositiveButton("Crear", null)
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.show()

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val pregunta = etPregunta.text.toString().trim()
            val opcionesList = mutableListOf<String>()
            for (i in 0 until layoutOpciones.childCount) {
                val et = layoutOpciones.getChildAt(i) as? android.widget.EditText
                val texto = et?.text?.toString()?.trim() ?: ""
                if (texto.isNotEmpty()) {
                    opcionesList.add(texto)
                }
            }

            if (pregunta.isEmpty()) {
                android.widget.Toast.makeText(context, "Ingresa la pregunta", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (opcionesList.size < 2) {
                android.widget.Toast.makeText(context, "Debes ingresar al menos 2 opciones", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val idAdmin = com.upn.app_vecinoalerta.utils.SecurePrefs.getInt(requireContext(), "id_usuario", -1)

            viewModel.crearEncuesta(pregunta, opcionesList, idAdmin)
            android.widget.Toast.makeText(context, "Encuesta creada con éxito", android.widget.Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class EncuestasAdapter(
        private val list: List<EncuestaEntity>,
        private val observeResults: (Int) -> kotlinx.coroutines.flow.Flow<List<com.upn.app_vecinoalerta.data.local.dao.ResultadoOpcion>>,
        private val scope: kotlinx.coroutines.CoroutineScope,
        private val onClick: (EncuestaEntity) -> Unit
    ) : RecyclerView.Adapter<EncuestasAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvPregunta: TextView = view.findViewById(android.R.id.text1)
            val tvSubtitulo: TextView = view.findViewById(android.R.id.text2)
            var job: kotlinx.coroutines.Job? = null
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val density = parent.context.resources.displayMetrics.density
            val margin12 = (12 * density).toInt()
            val padding24 = (24 * density).toInt()
            val padding20 = (20 * density).toInt()
            val accentWidth = (4 * density).toInt()
            val accentEndMargin = (12 * density).toInt()

            // Outer horizontal wrapper – holds the green accent bar + content card
            val outerWrapper = android.widget.LinearLayout(parent.context).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                layoutParams = androidx.recyclerview.widget.RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, margin12, 0, margin12)
                }
                setBackgroundResource(R.drawable.bg_card_white)
                clipToPadding = false
            }

            // Green left accent bar (4 dp wide)
            val accentBar = View(parent.context).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    accentWidth,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ).apply {
                    setMargins(0, 0, accentEndMargin, 0)
                }
                setBackgroundColor(android.graphics.Color.parseColor("#16A34A"))
            }

            // Inner vertical layout – content (question + subtitle)
            val layout = android.widget.LinearLayout(parent.context).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                )
                setPadding(0, padding20, padding24, padding20)
            }

            val t1 = TextView(parent.context).apply {
                id = android.R.id.text1
                setTextColor(android.graphics.Color.parseColor("#1F2937"))
                textSize = 16f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            val t2 = TextView(parent.context).apply {
                id = android.R.id.text2
                setTextColor(android.graphics.Color.parseColor("#9CA3AF"))
                textSize = 13f
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = (4 * density).toInt()
                }
            }
            layout.addView(t1)
            layout.addView(t2)

            outerWrapper.addView(accentBar)
            outerWrapper.addView(layout)
            return ViewHolder(outerWrapper)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.tvPregunta.text = item.pregunta
            holder.itemView.setOnClickListener { onClick(item) }

            holder.job?.cancel()
            holder.job = scope.launch {
                observeResults(item.idEncuesta).collect { resultados ->
                    val totalVotos = resultados.sumOf { it.total_votos }
                    if (totalVotos == 0) {
                        holder.tvSubtitulo.text = "Sin votos registrados todavía"
                        holder.tvSubtitulo.setTextColor(android.graphics.Color.parseColor("#9CA3AF"))
                    } else {
                        val ganadora = resultados.maxByOrNull { it.total_votos }
                        val pct = if (ganadora != null) (ganadora.total_votos.toDouble() / totalVotos) * 100 else 0.0
                        if (ganadora != null) {
                            holder.tvSubtitulo.text = "Votos: $totalVotos • Líder: ${ganadora.texto_opcion} (${String.format(java.util.Locale.US, "%.0f", pct)}%)"
                            holder.tvSubtitulo.setTextColor(android.graphics.Color.parseColor("#16A34A"))
                        } else {
                            holder.tvSubtitulo.text = "Votos: $totalVotos"
                            holder.tvSubtitulo.setTextColor(android.graphics.Color.parseColor("#9CA3AF"))
                        }
                    }
                }
            }
        }

        override fun getItemCount() = list.size
    }
}
