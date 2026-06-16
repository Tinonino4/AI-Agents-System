# Constitución del Proyecto (AGENTS.md)

Este documento es la constitución oficial del proyecto para la orquestación del sistema multiagente. Aquí se declaran las reglas críticas, el stack tecnológico y los comandos deterministas del flujo de trabajo.

## Stack Tecnológico del Sistema
- **Backend & Orquestación AI:** Java 21 / Spring Boot 4.1.x / Spring AI 2.x (Arquitectura Hexagonal + DDD)
- **Frontend / UI:** Vue 3 + Vite (Interfaz reactiva para traza de estados agénticos)
- **Base de Datos & Memoria:** PostgreSQL (con PGVector para memoria a largo plazo) y Redis (para memoria a corto plazo / sesiones)
- **Control de Versiones & DevOps:** Docker, Hetzner Cloud VPS, GitHub Actions

## Base de Datos Local
Para operaciones de lectura/escritura directas sobre la base de datos por parte de los agentes, utilicen:
- **URL JDBC:** `jdbc:postgresql://localhost:5432/ai_agentic_system_db`
- **Base de datos:** `ai_agentic_system_db`
- **Usuario:** `postgres`
- **Contraseña:** `postgrespassword`
- **Redis Host/Port:** `localhost:6379`

*Ejemplo de acceso rápido por terminal:*
```bash
PGPASSWORD="postgrespassword" psql -U postgres -h localhost -d ai_agentic_system_db -c "SELECT * FROM project_contexts;"
```

## Comandos de Construcción Deterministas
- **Backend (Spring Boot):** `mvn clean install` o `mvn clean test` (dentro de la carpeta `/backend/`)
- **Frontend (Vue 3 / Vite):** `npm run build` o `npm run dev` (dentro de la carpeta `/frontend/`)

## Enrutamiento Automático de Agentes (Triggers)
El orquestador enruta las tareas de desarrollo basándose en los siguientes disparadores de archivos:
- Modificaciones en `/backend/src/main/java/**` ➔ Invocar a `@backend-agent` o `@architect-agent`
- Modificaciones en `/backend/src/main/resources/db/migration/**` ➔ Invocar a `@db-admin`
- Modificaciones en `/frontend/src/**` ➔ Invocar a `@frontend-agent`
- Errores de compilación o fallos en pruebas de integración ➔ Invocar a `@qa-agent`
- Modificaciones de Dockerfiles y scripts de despliegue ➔ Invocar a `@devops-agent`
