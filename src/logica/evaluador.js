class EvaluadorLogica {
    constructor() {
        this.tiempoFlujoContinuo = 0; // Para medir caños abiertos por olvido
    }

    evaluarLectura(lectura, estadoUsuario) {
        const volumen = lectura.volumen_registrado;
        
        // REGLA CRÍTICA 1: Fuga en ausencia del usuario
        if (estadoUsuario === 'NO' && volumen > 0) {
            return {
                fecha: lectura.fecha,
                hora: lectura.hora,
                descripcion: `Análisis IA: Se detectó un consumo anómalo de ${volumen} L/min mientras el usuario se encontraba fuera de casa.`,
                id_tipo_alerta: 1, // 1: Fuga Crítica
                id_tipo_estado: 1, // 1: Activa
                id_medidor: lectura.id_medidor
            };
        }

        // REGLA CRÍTICA 2: Monitoreo de flujo prolongado (Usuario en casa pero caño abierto)
        if (estadoUsuario === 'SI' && volumen > 0) {
            this.tiempoFlujoContinuo++;
            // Si el agua corre por más de 45 minutos seguidos, asumimos un olvido o fuga interna
            if (this.tiempoFlujoContinuo > 45) {
                return {
                    fecha: lectura.fecha,
                    hora: lectura.hora,
                    descripcion: "Análisis IA: Tu patrón de consumo indica un flujo continuo inusual. Solicitamos revisar si dejó un caño abierto.",
                    id_tipo_alerta: 2, // 2: Advertencia por olvido
                    id_tipo_estado: 1, 
                    id_medidor: lectura.id_medidor
                };
            }
        } else {
            // Si el flujo se detiene, reiniciamos el contador de tiempo continuo
            this.tiempoFlujoContinuo = 0;
        }

        return null; // Todo normal, no se genera ninguna alerta
    }
}

module.exports = EvaluadorLogica;