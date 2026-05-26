// 1. Importamos librerías y nuestras clases
const express = require('express');
const cors = require('cors');
const SimuladorSensor = require('./src/simulador/generador');
const EvaluadorLogica = require('./src/logica/evaluador');

// 2. Configuramos el Servidor Express
const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors()); // Permite que el Frontend se conecte sin errores de seguridad
app.use(express.json()); // Permite recibir datos en formato JSON desde el Frontend

// 3. Instanciamos los componentes lógicos
const medidorSimulado = new SimuladorSensor('MED-PUENTE-PIEDRA-001');
const cerebroAnalitico = new EvaluadorLogica();

// 4. ESTADO GLOBAL (Ahora es dinámico y accesible)
let estadoUsuarioEnCasa = 'NO';
let ultimaLectura = null;
let ultimaAlerta = null;

// ==========================================
// RUTAS API (El Puente con el Exterior)
// ==========================================

// A. Ruta POST: El Frontend nos avisa cuando el usuario presiona el botón
app.post('/api/estado', (req, res) => {
    const { estado } = req.body;
    
    if (estado === 'SI' || estado === 'NO') {
        estadoUsuarioEnCasa = estado;
        console.log(`[API] 📲 El usuario cambió su estado a: ${estadoUsuarioEnCasa}`);
        res.status(200).json({ mensaje: `Estado actualizado correctamente a ${estado}` });
    } else {
        res.status(400).json({ error: 'Estado inválido. Debe ser "SI" o "NO".' });
    }
});

// B. Ruta GET: La aplicación / Base de datos viene a consultar cómo está el agua
app.get('/api/monitoreo', (req, res) => {
    res.status(200).json({
        estado_usuario: estadoUsuarioEnCasa,
        lectura_actual: ultimaLectura,
        alerta_activa: ultimaAlerta
    });
});

// ==========================================
// EL MOTOR DEL SIMULADOR (En segundo plano)
// ==========================================
setInterval(() => {
    // Generamos datos y evaluamos
    ultimaLectura = medidorSimulado.generarDatos(estadoUsuarioEnCasa);
    ultimaAlerta = cerebroAnalitico.evaluarLectura(ultimaLectura, estadoUsuarioEnCasa);

    // Mantenemos un log limpio en consola para que sepas que sigue vivo
    console.log(`[SENSOR] Flujo: ${ultimaLectura.volumen_registrado} L/min | Usuario en casa: ${estadoUsuarioEnCasa}`);
    
    if (ultimaAlerta) {
        console.log(`   🚨 [ALERTA] ${ultimaAlerta.descripcion}`);
    }
}, 3000);

// ==========================================
// INICIO DEL SERVIDOR
// ==========================================
app.listen(PORT, () => {
    console.log(`\n=== SISTEMA AQUASMART: SERVIDOR INICIADO ===`);
    console.log(`🚀 API escuchando en http://localhost:${PORT}`);
    console.log(`📡 Esperando conexión del Frontend...\n`);
});