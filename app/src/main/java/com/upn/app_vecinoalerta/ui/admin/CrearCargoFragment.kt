package com.upn.app_vecinoalerta.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.upn.app_vecinoalerta.R
import com.upn.app_vecinoalerta.data.local.entities.InmuebleEntity
import com.upn.app_vecinoalerta.data.local.entities.UsuarioEntity
import com.upn.app_vecinoalerta.databinding.FragmentCrearCargoBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CrearCargoFragment : Fragment() {

    private var _binding: FragmentCrearCargoBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AdminViewModel by viewModels()

    private var inmuebles: List<InmuebleEntity> = emptyList()
    private var usuarios: List<UsuarioEntity> = emptyList()

    private var idInmuebleSeleccionado: Int? = null
    private var idUsuarioSeleccionado: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCrearCargoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sesion = requireActivity().getSharedPreferences("sesion", 0)
        val idAdmin = sesion.getInt("id_usuario", -1)

        binding.cbTodosInmuebles.setOnCheckedChangeListener { _, isChecked ->
            binding.spinnerInmuebles.isEnabled = !isChecked
            binding.spinnerUsuarios.isEnabled = !isChecked
            binding.spinnerInmuebles.alpha = if (isChecked) 0.5f else 1.0f
            binding.spinnerUsuarios.alpha = if (isChecked) 0.5f else 1.0f
        }

        cargarSpinners()
        configurarBoton(idAdmin)
    }

    private fun cargarSpinners() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observar inmuebles
                launch {
                    viewModel.todosInmuebles.collect { lista ->
                        inmuebles = lista
                        val direcciones = lista.map { it.direccion }
                        val adapter = ArrayAdapter(
                            requireContext(),
                            R.layout.spinner_item,
                            direcciones
                        ).also { it.setDropDownViewResource(R.layout.spinner_dropdown_item) }
                        binding.spinnerInmuebles.adapter = adapter
                        
                        binding.spinnerInmuebles.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                                idInmuebleSeleccionado = inmuebles.getOrNull(pos)?.idInmueble
                            }
                            override fun onNothingSelected(p0: AdapterView<*>?) {
                                idInmuebleSeleccionado = null
                            }
                        }
                    }
                }

                // Observar usuarios activos
                launch {
                    viewModel.usuariosActivos.collect { lista ->
                        usuarios = lista
                        // El morador es opcional para poder crear deudas heredadas (idUsuario = null)
                        val nombres = listOf("Ninguno (Deuda Heredada)") + lista.map { "${it.nombre} ${it.apellido} (${it.rol})" }
                        
                        val adapter = ArrayAdapter(
                            requireContext(),
                            R.layout.spinner_item,
                            nombres
                        ).also { it.setDropDownViewResource(R.layout.spinner_dropdown_item) }
                        binding.spinnerUsuarios.adapter = adapter

                        binding.spinnerUsuarios.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                                idUsuarioSeleccionado = if (pos == 0) {
                                    null
                                } else {
                                    usuarios.getOrNull(pos - 1)?.idUsuario
                                }
                            }
                            override fun onNothingSelected(p0: AdapterView<*>?) {
                                idUsuarioSeleccionado = null
                            }
                        }
                    }
                }
            }
        }
    }

    private fun configurarBoton(idAdmin: Int) {
        binding.btnCrearCargo.setOnClickListener {
            val concepto = binding.etConcepto.text.toString().trim()
            val montoStr = binding.etMonto.text.toString().trim()
            val mesStr = binding.etMes.text.toString().trim()
            val anioStr = binding.etAnio.text.toString().trim()

            val esMasivo = binding.cbTodosInmuebles.isChecked

            if (!esMasivo && idInmuebleSeleccionado == null) {
                Toast.makeText(requireContext(), "Selecciona un inmueble", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (concepto.isEmpty() || montoStr.isEmpty() || mesStr.isEmpty() || anioStr.isEmpty()) {
                Toast.makeText(requireContext(), "Completa todos los campos obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val monto = montoStr.toDoubleOrNull()
            val mes = mesStr.toIntOrNull()
            val anio = anioStr.toIntOrNull()

            if (monto == null || monto <= 0.0) {
                Toast.makeText(requireContext(), "Monto inválido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (mes == null || mes !in 1..12) {
                Toast.makeText(requireContext(), "Mes debe ser entre 1 y 12", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (anio == null || anio < 2020) {
                Toast.makeText(requireContext(), "Año inválido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (esMasivo) {
                viewModel.crearCargoTodosInmuebles(
                    concepto = concepto,
                    monto = monto,
                    mes = mes,
                    anio = anio,
                    idAdmin = idAdmin
                )
                Toast.makeText(requireContext(), "Cargos financieros masivos generados con éxito", Toast.LENGTH_LONG).show()
            } else {
                viewModel.crearCargo(
                    idInmueble = idInmuebleSeleccionado!!,
                    idUsuario = idUsuarioSeleccionado,
                    concepto = concepto,
                    monto = monto,
                    mes = mes,
                    anio = anio,
                    idAdmin = idAdmin
                )
                Toast.makeText(requireContext(), "Cargo financiero generado con éxito", Toast.LENGTH_LONG).show()
            }
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
