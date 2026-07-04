import 'package:flutter/material.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter/services.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  bool firebaseReady = false;
  String firebaseError = "";

  try {
    // Inicializar Firebase. Si los servicios de Google no están configurados
    // correctamente (p. ej. usando el placeholder de google-services.json),
    // atrapará la excepción para evitar un crash en desarrollo.
    await Firebase.initializeApp();
    firebaseReady = true;
  } catch (e) {
    firebaseError = e.toString();
  }

  runApp(MyApp(firebaseReady: firebaseReady, firebaseError: firebaseError));
}

class MyApp extends StatelessWidget {
  final bool firebaseReady;
  final String firebaseError;

  const MyApp({
    super.key,
    required this.firebaseReady,
    required this.firebaseError,
  });

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'VecinoAlerta Flutter Module',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        brightness: Brightness.dark,
        scaffoldBackgroundColor: const Color(0xFF1F1A16), // Warm cocoa
        primaryColor: const Color(0xFFD97706), // Amber
        cardColor: const Color(0xFF2C241E), // Warm chocolate
        colorScheme: const ColorScheme.dark(
          primary: Color(0xFFD97706),
          secondary: Color(0xFFF97316),
          surface: Color(0xFF2C241E),
          background: Color(0xFF1F1A16),
        ),
      ),
      home: FlutterDashboard(
        firebaseReady: firebaseReady,
        firebaseError: firebaseError,
      ),
    );
  }
}

class FlutterDashboard extends StatefulWidget {
  final bool firebaseReady;
  final String firebaseError;

  const FlutterDashboard({
    super.key,
    required this.firebaseReady,
    required this.firebaseError,
  });

  @override
  State<FlutterDashboard> createState() => _FlutterDashboardState();
}

class _FlutterDashboardState extends State<FlutterDashboard> {
  static const platform = MethodChannel('com.upn.vecinoalerta/channel');
  
  String _nativeUserInfo = "Sin datos nativos";
  List<String> _incidentesFirestore = [];
  bool _loadingFirestore = false;

  @override
  void initState() {
    super.initState();
    if (widget.firebaseReady) {
      _fetchIncidentesFromFirestore();
    }
  }

  // RF-15: Comunicarse con la base de datos local SQLite (Room) a través de Kotlin
  Future<void> _getNativeUserInfo() async {
    try {
      final String result = await platform.invokeMethod('getNativeUserInfo');
      setState(() {
        _nativeUserInfo = result;
      });
    } on PlatformException catch (e) {
      setState(() {
        _nativeUserInfo = "Error al obtener info: '${e.message}'.";
      });
    }
  }

  // RF-15: Sincronizar y obtener datos de Firebase Cloud Firestore
  Future<void> _fetchIncidentesFromFirestore() async {
    setState(() {
      _loadingFirestore = true;
    });
    try {
      final snapshot = await FirebaseFirestore.instance
          .collection('incidencias')
          .limit(5)
          .get();
      
      final list = snapshot.docs.map((doc) {
        final data = doc.data();
        return "${data['titulo'] ?? 'Sin título'} - ${data['estado'] ?? 'Pendiente'}";
      }).toList();

      setState(() {
        _incidentesFirestore = list;
        _loadingFirestore = false;
      });
    } catch (e) {
      setState(() {
        _incidentesFirestore = ["No se pudo conectar a Firestore: ${e.toString()}"];
        _loadingFirestore = false;
      });
    }
  }

  Future<void> _crearIncidenciaDemo() async {
    if (!widget.firebaseReady) return;
    try {
      await FirebaseFirestore.instance.collection('incidencias').add({
        'titulo': 'Prueba desde Flutter Module',
        'descripcion': 'Incidencia creada con Flutter y Firestore',
        'estado': 'PENDIENTE',
        'fechaCreacion': FieldValue.serverTimestamp(),
      });
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('¡Incidencia demo guardada en Firestore!')),
      );
      _fetchIncidentesFromFirestore();
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Error al guardar: $e')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('VecinoAlerta Flutter Hub'),
        backgroundColor: const Color(0xFF2C241E),
        elevation: 4,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Card: Estado de Firebase
            Card(
              elevation: 4,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(16),
                side: BorderSide(
                  color: widget.firebaseReady ? Colors.green.withOpacity(0.5) : Colors.red.withOpacity(0.5),
                  width: 1,
                ),
              ),
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Icon(
                          widget.firebaseReady ? Icons.check_circle : Icons.error,
                          color: widget.firebaseReady ? Colors.green : Colors.red,
                        ),
                        const SizedBox(width: 8),
                        Text(
                          'Estado de Firebase Cloud',
                          style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
                        ),
                      ],
                    ),
                    const SizedBox(height: 8),
                    Text(
                      widget.firebaseReady 
                          ? 'Firebase inicializado correctamente.' 
                          : 'Modo Local Activo (Firebase no configurado o sin google-services.json real).',
                      style: const TextStyle(color: Color(0xFFC4B5A8)),
                    ),
                    if (widget.firebaseError.isNotEmpty) ...[
                      const SizedBox(height: 8),
                      Text(
                        'Detalle: ${widget.firebaseError}',
                        style: const TextStyle(color: Colors.redAccent, fontSize: 12),
                      ),
                    ]
                  ],
                ),
              ),
            ),
            const SizedBox(height: 16),

            // Card: Comunicación SQLite Room via Kotlin
            Card(
              elevation: 4,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Sincronización Local (SQLite Room)',
                      style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      _nativeUserInfo,
                      style: const TextStyle(color: Color(0xFFC4B5A8)),
                    ),
                    const SizedBox(height: 12),
                    ElevatedButton.icon(
                      onPressed: _getNativeUserInfo,
                      icon: const Icon(Icons.sync),
                      label: const Text('Consultar SQLite Local'),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: const Color(0xFFD97706),
                        foregroundColor: Colors.white,
                      ),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 16),

            // Card: Firestore Cloud Sync Demo
            Card(
              elevation: 4,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Sincronización Cloud (Firestore)',
                      style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
                    ),
                    const SizedBox(height: 8),
                    if (_loadingFirestore)
                      const Center(child: CircularProgressIndicator())
                    else if (_incidentesFirestore.isEmpty)
                      const Text('No hay incidencias en la nube.', style: TextStyle(color: Color(0xFFC4B5A8)))
                    else
                      Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: _incidentesFirestore
                            .map((inc) => Padding(
                                  padding: const EdgeInsets.symmetric(vertical: 4.0),
                                  child: Text('• $inc', style: const TextStyle(color: Colors.white)),
                                ))
                            .toList(),
                      ),
                    const SizedBox(height: 12),
                    Wrap(
                      spacing: 8,
                      runSpacing: 8,
                      children: [
                        ElevatedButton.icon(
                          onPressed: widget.firebaseReady ? _fetchIncidentesFromFirestore : null,
                          icon: const Icon(Icons.refresh),
                          label: const Text('Actualizar Cloud'),
                          style: ElevatedButton.styleFrom(
                            backgroundColor: const Color(0xFFF97316),
                            foregroundColor: Colors.white,
                          ),
                        ),
                        ElevatedButton.icon(
                          onPressed: widget.firebaseReady ? _crearIncidenciaDemo : null,
                          icon: const Icon(Icons.add),
                          label: const Text('Crear Incidencia Demo'),
                          style: ElevatedButton.styleFrom(
                            backgroundColor: const Color(0xFFD97706),
                            foregroundColor: Colors.white,
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
