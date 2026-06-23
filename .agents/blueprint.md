# Agentic System Blueprint: AI-Agents-System

## Stack Tecnológico y Modelos del Sistema
- **Backend & Orquestación AI:** Java 21 / Spring Boot 4.1.x / Spring AI 2.x (usando Hilos Virtuales nativos).
- **Arquitectura:** Hexagonal (Puertos y Adaptadores) + Domain-Driven Design (DDD).
- **Frontend / UI:** Vue 3 + Vite con diseño premium y reactivo.
- **Modelos soportados:** Google Gemini (vía API oficial GenAI con perfil `gemini`) y Modelos Locales (Llama 3, Qwen, etc. vía LM Studio/Ollama con perfil `lmstudio`).
- **Base de Datos & Memoria de Agentes:** PostgreSQL con PGVector (previsto para RAG/Largo plazo) y Redis (previsto para corto plazo/caché).

## Principios de Ingeniería de Agentes (Agentic Engineering)
1. **Arquitectura Hexagonal Estricta:** El dominio (`com.swfactory.sdlc.domain`) debe estar libre de dependencias de frameworks externos como Spring AI. Toda interacción con LLMs, persistencia y APIs debe hacerse a través de adaptadores de infraestructura (`com.swfactory.sdlc.infrastructure`).
2. **Java Moderno sin Lombok:** No se permite el uso de Lombok. Se deben utilizar Java Records para DTOs e inmutabilidad en clases de dominio tradicionales.
3. **Manejo de Estados con HITL (Human-in-the-Loop):** Las transiciones de fase críticas (`ARCHITECTURE` y `DEPLOYMENT`) deben detener la orquestación en estado `WAITING_FOR_HITL` para recibir aprobación/feedback del supervisor humano a través del endpoint `/api/v1/tasks/{taskId}/approve`.
4. **Pruebas Automatizadas y Autocuración (Self-Healing):** El agente QA ejecuta `mvn clean test` localmente. Si detecta fallos, el flujo retrocede a `DEVELOPMENT` inyectando los logs de error del compilador/test al agente Backend. Se establece un límite de 3 reintentos antes de derivar a HITL.

## Protocolos de Interacción de Agentes
- **Lectura de Gobernanza:** Los agentes usan la herramienta `AgentContextReader` (`readAgentContextFile`) expuesta dinámicamente como `@Tool` en Spring AI para consultar las directivas de `.agents/` en tiempo real.
- **Escritura en el Workspace:** El código y los tests se escriben en disco físico utilizando delimitadores estructurados en las respuestas de los agentes:
  ```
  === FILE: ruta_relativa_al_proyecto ===
  [contenido del archivo]
  === END FILE ===
  ```
  El adaptador `LocalWorkspaceRepository` procesa estos bloques para crear/modificar archivos.
