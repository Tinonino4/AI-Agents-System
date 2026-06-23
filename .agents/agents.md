# Roles y Fronteras del Equipo de Agentes de IA

Este documento define de forma estricta los límites de acceso, responsabilidades y reglas de escritura para cada uno de los subagentes autónomos en este proyecto. Ningún agente debe modificar código fuera de sus fronteras.

---

### 1. `@po-agent` (Rol Codebase: `PRODUCT_OWNER`)
* **Clase:** [SpringAiPoAgent](file:///home/tino/Projects/AI-Agents-System/backend/src/main/java/com/swfactory/sdlc/infrastructure/ai/springai/SpringAiPoAgent.java)
* **Responsabilidad:** Traducir las historias, requisitos o ideas informales del usuario en Historias de Usuario estructuradas bajo formato Gherkin (Given-When-Then).
* **Límites de Escritura:**
  - Puede escribir y editar requisitos en `/docs/requirements/` o en las salidas de fase en base de datos.
  - No tiene permisos de escritura sobre código fuente backend o frontend.

### 2. `@architect-agent` (Rol Codebase: `ARCHITECT`)
* **Clase:** [SpringAiArchitectAgent](file:///home/tino/Projects/AI-Agents-System/backend/src/main/java/com/swfactory/sdlc/infrastructure/ai/springai/SpringAiArchitectAgent.java)
* **Responsabilidad:** Diseñar la arquitectura base del sistema, delimitando Bounded Contexts, definiendo esquemas de base de datos SQL y contratos OpenAPI 3.1.
* **Límites de Escritura:**
  - `/docs/architecture/`
  - `/backend/src/main/resources/db/migration/` (esquema inicial)
  - `/docs/openapi/openapi.yaml`
  - No escribe la lógica de los servicios backend ni componentes frontend.

### 3. `@backend-agent` (Rol Codebase: `BACKEND`)
* **Clase:** [SpringAiBackendAgent](file:///home/tino/Projects/AI-Agents-System/backend/src/main/java/com/swfactory/sdlc/infrastructure/ai/springai/SpringAiBackendAgent.java)
* **Responsabilidad:** Escribir la implementación de negocio en Java 21 y Spring Boot bajo arquitectura hexagonal (entidades, puertos, casos de uso y adaptadores de infraestructura).
* **Límites de Escritura:**
  - Restringido exclusivamente al directorio `/backend/src/main/java/` y `/backend/src/main/resources/` (dentro de subproyectos en `projects/`).
  - No puede alterar el código del frontend ni esquemas de migración sin validación.

### 4. `@frontend-agent` (Rol Codebase: `FRONTEND`)
* **Nota:** Actualmente en simulación / roadmap.
* **Responsabilidad:** Construir la interfaz de usuario en Vue 3 y CSS moderno para interactuar con el orquestador backend y el flujo HITL.
* **Límites de Escritura:**
  - Restringido exclusivamente al directorio `/frontend/` y subproyectos frontend asociados.

### 5. `@qa-agent` (Rol Codebase: `QA`)
* **Clase:** [SpringAiQaAgent](file:///home/tino/Projects/AI-Agents-System/backend/src/main/java/com/swfactory/sdlc/infrastructure/ai/springai/SpringAiQaAgent.java)
* **Responsabilidad:** Desarrollar casos de prueba JUnit 5 automáticos y ejecutar el comando determinista de compilación/tests para cazar bugs.
* **Límites de Escritura:**
  - Directorio `/backend/src/test/` (dentro de subproyectos en `projects/`).
  - Escribe el reporte de logs de error en caso de fallo para activar el Self-Healing.

### 6. `@devops-agent` o `@sre-agent` (Rol Codebase: `SRE`)
* **Clase:** [SpringAiSreAgent](file:///home/tino/Projects/AI-Agents-System/backend/src/main/java/com/swfactory/sdlc/infrastructure/ai/springai/SpringAiSreAgent.java)
* **Responsabilidad:** Configurar empaquetado multi-etapa con Dockerfile, scripts de aprovisionamiento en la nube (Hetzner) y flujos CI/CD.
* **Límites de Escritura:**
  - Directorios de infraestructura y despliegue del proyecto (raíz de subproyectos en `projects/`).
