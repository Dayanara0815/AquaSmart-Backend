class SimuladorSensor {
    constructor(idMedidor) {
        this.idMedidor = idMedidor;
        this.flujoActual = 0;
    }

    // Método para obtener la fecha y hora con el formato que requiere PostgreSQL
    obtenerFechaHoraActual() {
        const ahora = new Date();
        const fecha = ahora.toISOString().split('T')[0]; // Formato: YYYY-MM-DD
        const hora = ahora.toTimeString().split(' ')[0]; // Formato: HH:MM:SS
        return { fecha, hora };
    }

    // Genera el volumen de agua consumido por minuto
    generarDatos(estadoUsuario) {
        const { fecha, hora } = this.obtenerFechaHoraActual();
        
        // Contexto: El usuario está en casa (Sí se encuentra en casa)
        if (estadoUsuario === 'SI') {
            // Flujo variable normal entre 1 y 8 litros por minuto
            this.flujoActual = parseFloat((Math.random() * (8 - 1) + 1).toFixed(2)); 
        } 
        // Contexto: El usuario NO está en casa
        else if (estadoUsuario === 'NO') {
            // Simulamos un 15% de probabilidad de que ocurra una anomalía/fuga
            if (Math.random() > 0.85) {
                this.flujoActual = parseFloat((Math.random() * (3 - 0.5) + 0.5).toFixed(2)); 
            } else {
                this.flujoActual = 0.00; 
            }
        }

        // Estructura idéntica a la tabla "Lectura_Consumo" de tus compañeros
        return {
            id_medidor: this.idMedidor,
            fecha: fecha,
            hora: hora,
            volumen_registrado: this.flujoActual,
            id_tipo_flujo: this.flujoActual > 0 ? 1 : 2 // 1: Activo, 2: Cerrado
        };
    }
}

module.exports = SimuladorSensor;