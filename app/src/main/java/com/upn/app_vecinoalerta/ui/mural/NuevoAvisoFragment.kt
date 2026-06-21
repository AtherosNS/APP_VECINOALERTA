package com.upn.app_vecinoalerta.ui.mural

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NuevoAvisoBottomSheet : BottomSheetDialogFragment() {

    private val viewModel: MuralViewModel by viewModels({ requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val context = requireContext()
        val layout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 32, 48, 48)
            setBackgroundColor(android.graphics.Color.parseColor("#242B3D"))
        }

        val etTitulo = EditText(context).apply {
            hint = "Título del aviso"
            setHintTextColor(android.graphics.Color.GRAY)
            setTextColor(android.graphics.Color.WHITE)
            layoutParams = android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 24 }
        }

        val etContenido = EditText(context).apply {
            hint = "Contenido del aviso..."
            setHintTextColor(android.graphics.Color.GRAY)
            setTextColor(android.graphics.Color.WHITE)
            minLines = 3
            layoutParams = android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 32 }
        }

        val btnPublicar = Button(context).apply {
            text = "Publicar Aviso"
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(android.graphics.Color.parseColor("#6366F1"))
            layoutParams = android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        layout.addView(etTitulo)
        layout.addView(etContenido)
        layout.addView(btnPublicar)

        btnPublicar.setOnClickListener {
            val titulo = etTitulo.text.toString()
            val contenido = etContenido.text.toString()
            val sesion = requireActivity().getSharedPreferences("sesion", 0)
            val idAdmin = sesion.getInt("id_usuario", -1)

            if (titulo.isNotBlank() && contenido.isNotBlank()) {
                viewModel.publicar(titulo, contenido, idAdmin)
                Toast.makeText(context, "Aviso publicado", Toast.LENGTH_SHORT).show()
                dismiss()
            } else {
                Toast.makeText(context, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        return layout
    }
}