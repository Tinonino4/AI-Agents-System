# Registro de Decisiones Arquitectónicas (ADR)

Este archivo sirve como la "memoria compartida" de diseño de este proyecto. Todas las decisiones arquitectónicas importantes tomadas durante el desarrollo autónomo o con asistencia de Antigravity deben registrarse aquí con su justificación técnica.

---

### [ADR-001] [2026-06-15] - Uso de Arquitectura Hexagonal y DDD

*   **Estado:** Aceptado
*   **Contexto:** Los sistemas agénticos tienden a acoplar la lógica de orquestación de LLMs (infraestructura) directamente con los modelos de dominio. Esto dificulta migrar de proveedor de LLM (ej. de OpenAI a Vertex AI Gemini/Claude) o cambiar el motor de persistencia.
*   **Decisión:** Adoptar de manera estricta Arquitectura Hexagonal y DDD. El dominio (`/domain/`) es libre de dependencias de Spring AI y de cualquier framework externo. La inyección de dependencias y los adaptadores de LLM residen únicamente en la infraestructura (`/infrastructure/`).
*   **Consecuencias:**
    *   **Positivas:** Fácil mantenimiento y extensibilidad de agentes. El backend puede compilarse y testearse aisladamente usando mocks en lugar de dependencias externas.
    *   **Negativas:** Incrementa la verbosidad inicial al requerir adaptadores de mapeo entre objetos del dominio y entidades JPA de base de datos.

---

### [ADR-002] [2026-06-15] - Patrón Human-in-the-Loop (HITL) Centralizado

*   **Estado:** Aceptado
*   **Contexto:** Las fases del SDLC automatizado como la Arquitectura Técnica o el Despliegue DevOps no deben completarse sin validación humana debido al riesgo de alucinación crítica en APIs o infraestructura cloud.
*   **Decisión:** Detener la orquestación del caso de uso en fases críticas configurando el estado de la tarea como `WAITING_FOR_HITL`. Proveer un controlador REST en la infraestructura (`POST /api/v1/tasks/{taskId}/approve`) para reanudar o rechazar el flujo con retroalimentación.
*   **Consecuencias:**
    *   **Positivas:** Seguridad y consistencia antes de la ejecución de código.
    *   **Negativas:** El flujo ya no es 100% asíncrono y requiere la intervención activa del supervisor para progresar.

---

### [ADR-003] [2026-06-23] - Escritura Física de Resultados en Workspace para Todos los Agentes

*   **Estado:** Aceptado
*   **Contexto:** Los agentes `PRODUCT_OWNER` y `SRE` actualmente generan especificaciones e infraestructura (Dockerfile/manifiestos) pero solo se persisten en base de datos. Esto rompe la consistencia de tener el proyecto de software completo autocontenido en la carpeta `/projects/`.
*   **Decisión:** Modificar `SpringAiPoAgent` y `SpringAiSreAgent` para que utilicen el protocolo de bloques `=== FILE ===` de forma que escriban físicamente sus productos de trabajo en disco (ej: historias de usuario en `docs/requirements.md` y archivos de despliegue en la raíz del subproyecto).
*   **Consecuencias:**
    *   **Positivas:** El workspace de salida almacena el 100% de los entregables del ciclo SDLC, haciéndolo ejecutable de forma autónoma.
    *   **Negativas:** Se añade lógica de escritura física y parsing en fases que antes solo procesaban texto simple.

---

### [ADR-004] [2026-06-23] - Aislamiento en Sandbox Docker para Compilación y Pruebas

*   **Estado:** Propuesto
*   **Contexto:** Ejecutar `mvn clean test` directamente en el sistema host del usuario mediante subprocesos de Java en `LocalWorkspaceRepository` puede ser peligroso si el agente de desarrollo introduce comandos destructivos, bugs o código malicioso de forma involuntaria.
*   **Decisión:** Encapsular la compilación y ejecución del agente QA dentro de contenedores Docker efímeros montados en modo lectura/escritura únicamente sobre el subdirectorio del proyecto generado.
*   **Consecuencias:**
    *   **Positivas:** Seguridad absoluta sobre la máquina host que ejecuta el backend.
    *   **Negativas:** Incremento en el tiempo de ejecución debido al arranque de contenedores Docker y configuración de volúmenes compartidos.
