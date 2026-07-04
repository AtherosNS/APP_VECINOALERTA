package com.upn.app_vecinoalerta.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.upn.app_vecinoalerta.R
import com.upn.app_vecinoalerta.data.local.entities.UsuarioEntity
import com.upn.app_vecinoalerta.databinding.FragmentAprobacionUsuariosBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * RF-02: Panel del Admin para aprobar o rechazar cuentas PENDIENTES.
 */
@AndroidEntryPoint
class AprobacionUsuariosFragment : Fragment() {

    private var _binding: FragmentAprobacionUsuariosBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AdminViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAprobacionUsuariosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvPendientes.layoutManager = LinearLayoutManager(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.pendientes.collect { lista ->
                    binding.tvSinPendientes.visibility =
                        if (lista.isEmpty()) View.VISIBLE else View.GONE

                    binding.rvPendientes.adapter = UsuarioPendienteAdapter(
                        lista,
                        onAprobar  = { 
                            viewModel.aprobarUsuario(it.idUsuario)
                            Toast.makeText(requireContext(), "Usuario ${it.usuario} aprobado", Toast.LENGTH_SHORT).show()
                        },
                        onRechazar = { 
                            viewModel.desactivarUsuario(it.idUsuario)
                            Toast.makeText(requireContext(), "Usuario ${it.usuario} rechazado", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    private class UsuarioPendienteAdapter(
        private val list: List<UsuarioEntity>,
        private val onAprobar: (UsuarioEntity) -> Unit,
        private val onRechazar: (UsuarioEntity) -> Unit
    ) : RecyclerView.Adapter<UsuarioPendienteAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvNombre: TextView = view.findViewById(android.R.id.text1)
            val tvDetalle: TextView = view.findViewById(android.R.id.text2)
            val btnAprobar: Button = com.google.android.material.button.MaterialButton(view.context).apply {
                text = "Aprobar"
                setBackgroundColor(android.graphics.Color.parseColor("#10B981"))
                setTextColor(android.graphics.Color.WHITE)
                textSize = 12f
                minimumHeight = 0
                minimumWidth = 0
                setPadding(24, 12, 24, 12)
                (this as? com.google.android.material.button.MaterialButton)?.cornerRadius = 16
            }
            val btnRechazar: Button = com.google.android.material.button.MaterialButton(view.context).apply {
                text = "Rechazar"
                setBackgroundColor(android.graphics.Color.parseColor("#EF4444"))
                setTextColor(android.graphics.Color.WHITE)
                textSize = 12f
                minimumHeight = 0
                minimumWidth = 0
                setPadding(24, 12, 24, 12)
                (this as? com.google.android.material.button.MaterialButton)?.cornerRadius = 16
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val density = parent.context.resources.displayMetrics.density
            val margin12 = (12 * density).toInt()
            val padding16 = (16 * density).toInt()

            val layout = android.widget.LinearLayout(parent.context).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                layoutParams = androidx.recyclerview.widget.RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, margin12, 0, margin12)
                }
                setPadding(padding16, padding16, padding16, padding16)
                setBackgroundResource(R.drawable.bg_card_white)
            }

            val textLayout = android.widget.LinearLayout(parent.context).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            val t1 = TextView(parent.context).apply {
                id = android.R.id.text1
                setTextColor(android.graphics.Color.parseColor("#1F2937"))
                textSize = 16f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            val t2 = TextView(parent.context).apply {
                id = android.R.id.text2
                setTextColor(android.graphics.Color.parseColor("#6B7280"))
                textSize = 13f
                setLineSpacing(2f, 1f)
            }
            textLayout.addView(t1)
            textLayout.addView(t2)
            layout.addView(textLayout)

            val holder = ViewHolder(layout)
            
            val buttonLayout = android.widget.LinearLayout(parent.context).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.END
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = (12 * density).toInt()
                }
            }
            
            val btnParams = android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                (36 * density).toInt()
            ).apply {
                setMargins((8 * density).toInt(), 0, 0, 0)
            }
            holder.btnAprobar.layoutParams = btnParams
            holder.btnRechazar.layoutParams = btnParams
            
            buttonLayout.addView(holder.btnRechazar)
            buttonLayout.addView(holder.btnAprobar)
            layout.addView(buttonLayout)
            
            return holder
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.tvNombre.text = "${item.nombre} ${item.apellido}"
            holder.tvDetalle.text = "Rol: ${item.rol} | DNI: ${item.dni}\nCorreo: ${item.correo}"
            holder.btnAprobar.setOnClickListener { onAprobar(item) }
            holder.btnRechazar.setOnClickListener { onRechazar(item) }
        }

        override fun getItemCount() = list.size
    }
}