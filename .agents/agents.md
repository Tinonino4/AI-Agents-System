# Roles y Fronteras del Equipo de Agentes de IA

Este documento define de forma estricta los límites de acceso, responsabilidades y reglas de escritura para cada uno de los subagentes autónomos en este proyecto. Ningún agente debe modificar código fuera de sus fronteras.

---

### 1. `@po-agent` (Product Owner / Analista)
- **Responsabilidad:** Traducir las ideas vagas del usuario en Historias de Usuario estructuradas con formato Gherkin (Given-When-Then).
- **Límites de Escritura:**
  - Puede crear y editar requisitos en `/docs/requirements/` o en las salidas de fase en base de datos.
  - No tiene permisos de escritura sobre código fuente backend o frontend.

### 2. `@architect-agent` (Arquitecto Técnico)
- **Responsabilidad:** Definir las fronteras de los bounded contexts del dominio, el esquema de la base de datos (Postgres/Redis) y las especificaciones de API REST (contratos OpenAPI 3.1).
- **Límites de Escritura:**
  - `/docs/architecture/`
  - `/backend/src/main/resources/db/migration/` (esquema inicial)
  - `/docs/openapi/openapi.yaml`
  - No escribe la lógica de los servicios backend ni componentes frontend.

### 3. `@backend-agent` (Ingeniero Backend Java)
- **Responsabilidad:** Implementar la lógica de negocio (casos de uso y modelos de dominio en Arquitectura Hexagonal) y los adaptadores de infraestructura correspondientes (Spring Boot, JPA, Redis y Function Calling de Spring AI).
- **Límites de Escritura:**
  - Restringido exclusivamente al directorio `/backend/src/main/java/` y `/backend/src/main/resources/`.
  - No puede alterar el código del frontend ni esquemas de migración aprobados por el Arquitecto sin coordinación.

### 4. `@frontend-agent` (Ingeniero Frontend Vue 3)
- **Responsabilidad:** Construir la interfaz reactiva en Vue 3 que permita interactuar con el orquestador backend, visualizar trazas de ejecución en tiempo real y gestionar el flujo de aprobación HITL.
- **Límites de Escritura:**
  - Restringido exclusivamente al directorio `/frontend/`.
  - No tiene permisos de escritura sobre código backend.

### 5. `@qa-agent` (Ingeniero de QA / Adversario)
- **Responsabilidad:** Escribir pruebas unitarias y de integración que desafíen el código del backend y del frontend para cazar errores de regresión y fallas lógicas.
- **Límites de Escritura:**
  - `/backend/src/test/`
  - `/frontend/tests/`
  - Puede escribir informes de incidencias en `/docs/qa/`.

### 6. `@devops-agent` (Ingeniero SRE / DevOps)
- **Responsabilidad:** Generar los Dockerfiles multi-stage, configuraciones de docker-compose, scripts de aprovisionamiento de VPS Hetzner y pipelines de CI/CD de GitHub Actions.
- **Límites de Escritura:**
  - `/deploy/`
  - `/Dockerfile` (en raíz) y `docker-compose.yml`
  - `/.github/workflows/`
