# ⚙️ AquaSmart - Backend (Spring Boot + PostgreSQL)

Esta es la API RESTful de la plataforma inteligente **AquaSmart**. El backend gestiona el almacenamiento relacional de consumos de agua, procesamiento de alertas por paso de aire y fugas persistentes, control remoto seguro de la válvula principal de paso de agua, y el motor de conversación contextual interactivo asistido por Inteligencia Artificial.

---

## 🚀 Tecnologías Principales

- **Lenguaje**: Java 21 (LTS)
- **Framework**: Spring Boot 4.0.6 (con Spring MVC, Spring Data JPA)
- **Base de Datos**: PostgreSQL 16+
- **Control de Versiones y Dependencias**: Apache Maven
- **Persistencia**: Hibernate ORM (con optimizaciones de consultas mediante `@EntityGraph`)
- **Controladores**: REST Controllers con mapeos DTO basados en Java Records

---

## ✨ Características Principales

1. **Simulador de Telemetría Acuática**: Genera lecturas inteligentes simuladas cada 5 segundos de forma automatizada. Si la válvula está abierta y hay presencia en casa, se simula consumo dinámico. Si no hay nadie y se detecta una alerta de fuga activa, simula un caudal de fuga constante.
2. **Control de Válvula Físico-Lógico**: Endpoint `/water/valve` para abrir y cerrar la válvula. Al cerrarse, se registra una lectura instantánea forzada de `0.0 L/min` y se añade una entrada de "Cierre Manual" al historial de control.
3. **Motor NLP de IA Contextual**: Procesamiento de lenguaje natural mediante consultas SQL directas. El bot responde preguntas inteligentes como *"¿Cuánto he consumido hoy?"* o *"¿Tengo alertas de fugas?"* extrayendo el contexto real de la base de datos PostgreSQL.
4. **Manejador Robusto de Excepciones**: Captura de excepciones integrada en `GlobalExceptionHandler` con impresión detallada de trazas de depuración (`exception.printStackTrace()`) en el servidor local para una auditoría técnica inmediata.
5. **Autosebrado de Datos Inteligente (`DataSeeder`)**: Detecta si la base de datos está vacía al primer arranque y siembra automáticamente el titular por defecto (**María Fernanda Quispe Rojas**), un medidor asignado, lecturas de consumo históricas de los últimos 7 días, alertas persistentes y recibos mensuales.

---

## 🛠️ Requisitos de Entorno

- **Java Development Kit (JDK)**: Versión 21 (LTS) instalada.
- **Maven**: Si no tienes Maven instalado de manera global, puedes usar el wrapper incluido en el proyecto (`mvnw` para Unix/macOS o `mvnw.cmd` para Windows).
- **PostgreSQL**: Servidor ejecutándose localmente en el puerto `5432`.

---

## ⚙️ Configuración y Despliegue Local

### 1. Configurar la Base de Datos PostgreSQL
Crea una base de datos relacional llamada `aquasmart` utilizando tu consola de PostgreSQL, pgAdmin o tu cliente SQL preferido:
```sql
CREATE DATABASE aquasmart;
```

### 2. Configurar Propiedades de Conexión
Asegúrate de que las credenciales en el archivo [application.properties](file:///d:/Workspace/utp/AquaSmart/Backend/AquaSmart/src/main/resources/application.properties) coincidan con la configuración de tu servidor local PostgreSQL:
```properties
# DATABASE CONNECTION (POSTGRESQL)
spring.datasource.url=jdbc:postgresql://localhost:5432/aquasmart
spring.datasource.username=postgres
spring.datasource.password=admin  # <- Cambia por tu contraseña de PostgreSQL
spring.datasource.driver-class-name=org.postgresql.Driver
```

### 3. Compilar y Ejecutar el Servidor
Accede al directorio raíz del proyecto backend e inicia la aplicación utilizando el wrapper de Maven:

**En Windows (PowerShell / CMD):**
```powershell
cd Backend\AquaSmart
.\mvnw.cmd spring-boot:run
```

**En Linux / macOS:**
```bash
cd Backend/AquaSmart
chmod +x mvnw
./mvnw spring-boot:run
```

El servidor compilará las clases, se conectará a PostgreSQL, creará la estructura de tablas automáticamente mediante el parámetro `ddl-auto=update` y seedeará los registros iniciales.

El backend estará disponible escuchando peticiones en:
👉 [**http://localhost:8080/api**](http://localhost:8080/api)

---

## 🔍 Endpoints RESTful Clave

- `GET /api/water/status` -> Obtiene la telemetría actual, costo acumulado de hoy, caudal instantáneo y estado general.
- `PUT /api/water/valve` -> Cambia el estado físico de la válvula principal. Cuerpo JSON: `{"open": true/false}`.
- `GET /api/water/valve/history` -> Retorna el historial de aperturas y cierres registrados.
- `POST /api/ai/chat` -> Envía una pregunta en lenguaje natural al motor contextual de la IA. Cuerpo JSON: `{"question": "..."}`.
- `GET /api/reports/weekly` -> Recupera lecturas semanales ordenadas de consumo diario para el motor del PDF.
- `GET /api/alerts` -> Retorna la lista completa de alertas críticas activas e inactivas.