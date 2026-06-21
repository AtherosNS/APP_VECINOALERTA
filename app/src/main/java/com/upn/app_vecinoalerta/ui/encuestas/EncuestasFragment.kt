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

        val sesion = requireActivity().getSharedPreferences("sesion", 0)
        val rol = sesion.getString("rol", "") ?: ""

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
                    binding.rvEncuestas.adapter = EncuestasAdapter(lista) { encuesta ->
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
        val linearLayout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 32, 48, 32)
            setBackgroundColor(android.graphics.Color.parseColor("#1A1F2C"))
        }

        val title = android.widget.TextView(context).apply {
            text = "Nueva Encuesta / Votación"
            setTextColor(android.graphics.Color.WHITE)
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 24)
        }
        linearLayout.addView(title)

        val etPregunta = android.widget.EditText(context).apply {
            hint = "Pregunta o tema a votar"
            setHintTextColor(android.graphics.Color.parseColor("#94A3B8"))
            setTextColor(android.graphics.Color.WHITE)
            setPadding(24, 24, 24, 24)
            setBackgroundColor(android.graphics.Color.parseColor("#242B3D"))
        }
        linearLayout.addView(etPregunta)

        val space = android.widget.Space(context).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(1, 24)
        }
        linearLayout.addView(space)

        val labelOpciones = android.widget.TextView(context).apply {
            text = "Opciones de respuesta:"
            setTextColor(android.graphics.Color.parseColor("#94A3B8"))
            textSize = 14f
            setPadding(0, 0, 0, 12)
        }
        linearLayout.addView(labelOpciones)

        val layoutOpciones = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
        }
        linearLayout.addView(layoutOpciones)

        fun agregarCampoOpcion(placeholder: String) {
            val etOpcion = android.widget.EditText(context).apply {
                hint = placeholder
                setHintTextColor(android.graphics.Color.parseColor("#64748B"))
                setTextColor(android.graphics.Color.WHITE)
                setPadding(20, 20, 20, 20)
                setBackgroundColor(android.graphics.Color.parseColor("#242B3D"))
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.setMargins(0, 0, 0, 16) }
            }
            layoutOpciones.addView(etOpcion)
        }

        agregarCampoOpcion("Opción 1")
        agregarCampoOpcion("Opción 2")

        val btnAgregarOpcion = android.widget.Button(context).apply {
            text = "+ Agregar Opción"
            setTextColor(android.graphics.Color.parseColor("#6366F1"))
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            textSize = 14f
            setPadding(16, 16, 16, 16)
        }
        btnAgregarOpcion.setOnClickListener {
            val numOpciones = layoutOpciones.childCount
            agregarCampoOpcion("Opción ${numOpciones + 1}")
        }
        linearLayout.addView(btnAgregarOpcion)

        val space2 = android.widget.Space(context).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(1, 24)
        }
        linearLayout.addView(space2)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(context, R.style.Theme_VecinoAlerta)
            .setView(linearLayout)
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

            val sesion = requireActivity().getSharedPreferences("sesion", 0)
            val idAdmin = sesion.getInt("id_usuario", -1)

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
        private val onClick: (EncuestaEntity) -> Unit
    ) : RecyclerView.Adapter<EncuestasAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvPregunta: TextView = view.findViewById(android.R.id.text1)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val layout = android.widget.LinearLayout(parent.context).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setPadding(32, 20, 32, 20)
            }
            val t1 = TextView(parent.context).apply {
                id = android.R.id.text1
                setTextColor(android.graphics.Color.WHITE)
                textSize = 16f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            layout.addView(t1)
            return ViewHolder(layout)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.tvPregunta.text = item.pregunta
            holder.itemView.setOnClickListener { onClick(item) }
        }

        override fun getItemCount() = list.size
    }
}
