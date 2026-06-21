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
            val btnAprobar: Button = Button(view.context).apply {
                text = "Aprobar"
                setBackgroundColor(android.graphics.Color.parseColor("#10B981"))
                setTextColor(android.graphics.Color.WHITE)
            }
            val btnRechazar: Button = Button(view.context).apply {
                text = "Rechazar"
                setBackgroundColor(android.graphics.Color.parseColor("#EF4444"))
                setTextColor(android.graphics.Color.WHITE)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val layout = android.widget.LinearLayout(parent.context).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setPaddingRelative(32, 16, 32, 16)
            }
            val textLayout = android.widget.LinearLayout(parent.context).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1.0f
                )
            }
            val t1 = TextView(parent.context).apply {
                id = android.R.id.text1
                setTextColor(android.graphics.Color.WHITE)
                textSize = 16f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            val t2 = TextView(parent.context).apply {
                id = android.R.id.text2
                setTextColor(android.graphics.Color.GRAY)
                textSize = 14f
            }
            textLayout.addView(t1)
            textLayout.addView(t2)
            layout.addView(textLayout)

            val holder = ViewHolder(layout)
            
            val buttonLayout = android.widget.LinearLayout(parent.context).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            
            val btnParams = android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(8, 0, 0, 0)
            }
            holder.btnAprobar.layoutParams = btnParams
            holder.btnRechazar.layoutParams = btnParams
            
            buttonLayout.addView(holder.btnAprobar)
            buttonLayout.addView(holder.btnRechazar)
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