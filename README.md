<div align="center">
  <img src="https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white" alt="Spring Boot" />
  <img src="https://img.shields.io/badge/Java_21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21" />
  <img src="https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white" alt="PostgreSQL" />
  <img src="https://img.shields.io/badge/Supabase-3ECF8E?style=for-the-badge&logo=supabase&logoColor=white" alt="Supabase" />
  <img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker" />
  <img src="https://img.shields.io/badge/Render-46E3B7?style=for-the-badge&logo=render&logoColor=white" alt="Render" />
</div>

<h1 align="center">💧 AquaSmart - API Backend (Gestión Hídrica Inteligente)</h1>

<p align="center">
  API RESTful robusta, segura y escalable para la administración remota de medidores, análisis de telemetría y conversación de IA en AquaSmart.
</p>

<div align="center">
  <h3>
    <a href="https://aquasmart-backend-5wfp.onrender.com/api/water/status">🚀 VER ESTADO DE LA API (DEPLOY EN RENDER)</a>
  </h3>
</div>

---

## 🌟 Sobre el Proyecto

El backend de **AquaSmart** es el motor principal encargado de gestionar la telemetría hídrica y procesar algoritmos de seguridad en tiempo real. Utilizando una arquitectura modular construida en **Spring Boot 4**, se encarga del almacenamiento relacional de consumos, la detección asíncrona de fugas en tuberías matrices, la conmutación remota de electroválvulas y la comunicación fluida de lenguaje natural respaldada por la API de **Google Gemini 1.5 Flash**.

---

## ✨ Funcionalidades Principales

El backend automatiza la seguridad hídrica del hogar y comercios, protegiendo todos los endpoints mediante un sistema de persistencia relacional en **Supabase** y un scheduler de telemetría asíncrona.

### 🤖 Inteligencia Artificial Conversacional (Gemini)
- **Grounding en Tiempo Real:** El chatbot (AquaBot) recibe un system prompt inyectado con datos vivos de la base de datos (consumo de hoy, alertas viales, estado de la válvula).
- **Control de Historial:** Soporte para conversaciones continuas sin pérdida de contexto por recargas.

### 🛡️ Telemetría y Cierre Automático por Fuga (Fase 6)
- **Scheduler Asíncrono:** Sensor en segundo plano que mide flujos hídricos de forma independiente para cada medidor registrado.
- **Cierre Inteligente Configurable:** 
  - Si el usuario **no está en casa**, la válvula se cierra automáticamente ante cualquier anomalía.
  - Si el usuario **está en casa**, la válvula se cierra automáticamente solo si tiene activa la opción `autoCierreFuga` en sus preferencias.
- **Congelamiento de Consumo:** Detiene inmediatamente el registro de caudal (`0.0 L/min`) al cerrarse la válvula para evitar cargos basura en la facturación.

### 📋 Módulo Técnico y Municipal
- **JIRA Kanban:** Tablero de ordenación y flujo de incidencias viales de bacheo y pavimentación.
- **Calendario de Cortes programados:** Visualización mensual de eventos críticos e incidentes para la gestión de Sedapal.

---

## 🛠️ Tecnologías Utilizadas

- **Framework Web:** Spring Boot 4.0.6 (Spring MVC, Spring Data JPA)
- **Lenguaje:** Java 21 (LTS)
- **Base de Datos:** PostgreSQL (Cloud: Supabase)
- **Persistencia:** Hibernate ORM
- **Contenedores:** Docker (Multi-stage build)
- **Despliegue:** Render

---

## 🚀 Despliegue Local (Para Desarrolladores)

Si deseas clonar el proyecto y ejecutar el backend localmente:

1. **Clonar el repositorio:**
```bash
git clone https://github.com/Dayanara0815/AquaSmart-Backend.git
cd AquaSmart-Backend/AquaSmart
```

2. **Crear base de datos en PostgreSQL:**
```sql
CREATE DATABASE aquasmart;
```

3. **Configurar Variables de Entorno locales o en el archivo `application.properties`:**
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/aquasmart
spring.datasource.username=postgres
spring.datasource.password=tu_contrasena_local
GEMINI_API_KEY=tu_api_key_de_gemini
```

4. **Ejecutar el servidor local:**
```bash
# Windows:
.\mvnw.cmd spring-boot:run
# Mac/Linux:
./mvnw spring-boot:run
```

---

## 🔑 Variables de Entorno en Producción (Render)

Al configurar tu Web Service en Render utilizando la opción **Docker** y apuntando al directorio raíz `AquaSmart`, define las siguientes variables:

| Key | Value (Ejemplo Supabase / Gemini) |
| :--- | :--- |
| **`SPRING_DATASOURCE_URL`** | `jdbc:postgresql://aws-1-us-east-1.pooler.supabase.com:5432/postgres` |
| **`SPRING_DATASOURCE_USERNAME`** | `postgres.kckumjkppcagqeftctyx` |
| **`SPRING_DATASOURCE_PASSWORD`** | *(Tu contraseña de Supabase)* |
| **`GEMINI_API_KEY`** | *(Tu API Key secreta de Google AI Studio)* |
| **`CORS_ALLOWED_ORIGINS`** | `https://aquasmart-1vhz.onrender.com` |