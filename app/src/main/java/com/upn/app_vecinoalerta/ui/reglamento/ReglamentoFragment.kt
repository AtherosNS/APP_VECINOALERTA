package com.upn.app_vecinoalerta.ui.reglamento

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import com.upn.app_vecinoalerta.databinding.FragmentReglamentoBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * RF-12: Sección estática del Reglamento Interno del condominio.
 * RNF-05: funciona completamente offline — no requiere internet ni Room.
 * El contenido se carga desde strings.xml o assets/reglamento.txt.
 */
@AndroidEntryPoint
class ReglamentoFragment : Fragment() {

    private var _binding: FragmentReglamentoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentReglamentoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Contenido estático del reglamento — en producción cargar desde strings.xml
        // o desde un archivo assets/reglamento.txt para fácil actualización
        binding.tvReglamento.text = """
            REGLAMENTO INTERNO DEL CONDOMINIO
            ══════════════════════════════════
            
            CAPÍTULO I — NORMAS DE CONVIVENCIA
            
            Art. 1°  Los residentes deberán mantener silencio entre las 10:00 PM y las 7:00 AM.
            
            Art. 2°  Queda prohibido el uso de áreas comunes para almacenamiento de bienes personales.
            
            Art. 3°  Las mascotas deberán ser llevadas con correa en todas las áreas comunes y sus dueños
                     son responsables de limpiar sus desechos.
            
            CAPÍTULO II — ÁREAS COMUNES
            
            Art. 4°  El uso de la sala de reuniones deberá ser reservado con 24 horas de anticipación
                     a través del Administrador.
            
            Art. 5°  Los estacionamientos son de uso exclusivo de los residentes asignados.
                     Está prohibido ceder o subarrendar el espacio.
            
            CAPÍTULO III — PAGOS Y CUOTAS
            
            Art. 6°  La cuota de mantenimiento mensual vence el día 5 de cada mes.
                     Los pagos realizados después de esa fecha generarán un recargo del 5%.
            
            Art. 7°  El propietario es responsable solidario de las deudas generadas en su
                     inmueble, incluyendo las originadas por sus inquilinos.
            
            CAPÍTULO IV — SEGURIDAD
            
            Art. 8°  Toda visita debe ser registrada en portería. Los residentes son responsables
                     de las personas que ingresen bajo su autorización.
            
            Art. 9°  En caso de emergencia, utilizar el Botón de Pánico disponible en la app
                     VecinoAlerta para contactar directamente a los servicios de emergencia.
            
            CAPÍTULO V — SANCIONES
            
            Art. 10° El incumplimiento de las normas podrá derivar en amonestación escrita,
                     multa económica o, en casos graves, la suspensión de servicios comunes.
        """.trimIndent()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}