package com.upn.app_vecinoalerta.ui.mural

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.google.android.material.button.MaterialButton
import com.upn.app_vecinoalerta.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NuevoAvisoDialog : DialogFragment() {

    private val viewModel: MuralViewModel by viewModels({ requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_nuevo_aviso, container, false)

        val etTitulo = view.findViewById<EditText>(R.id.etTitulo)
        val etContenido = view.findViewById<EditText>(R.id.etContenido)
        val btnPublicar = view.findViewById<MaterialButton>(R.id.btnPublicar)

        btnPublicar.setOnClickListener {
            val titulo = etTitulo.text.toString().trim()
            val contenido = etContenido.text.toString().trim()
            val idAdmin = com.upn.app_vecinoalerta.utils.SecurePrefs.getInt(requireContext(), "id_usuario", -1)

            if (titulo.isNotBlank() && contenido.isNotBlank()) {
                viewModel.publicar(titulo, contenido, idAdmin)
                Toast.makeText(context, "Aviso publicado con éxito", Toast.LENGTH_SHORT).show()
                dismiss()
            } else {
                Toast.makeText(context, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // Agregar márgenes al diálogo
        val displayMetrics = resources.displayMetrics
        val width = (displayMetrics.widthPixels * 0.9).toInt() // 90% of screen width
        dialog?.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}