# Roadmap de Desarrollo de Agentes

## Fase 1: Consolidación de la Memoria Contextual (Completado)
- [x] Integrar archivos de contexto unificados en la carpeta `.agents/`.
- [x] Implementar la tool Java `AgentContextReader` en el backend.
- [x] Conectar la tool de lectura a los agentes a través de Spring AI.

## Fase 2: Expansión de Herramientas e Integración Físicas
- [ ] Implementar la escritura física en disco para las salidas del `SpringAiPoAgent` (requisitos en `docs/requirements`) y `SpringAiSreAgent` (Dockerfile/manifiestos de despliegue en workspace).
- [ ] Optimizar la autocuración (Self-Healing) del agente QA mejorando el parseo de errores de JUnit 5 y logs de Maven.
- [ ] Implementar edición de código por parches/difs en lugar de reescritura total para optimizar el consumo de tokens y evitar truncados.

## Fase 3: Seguridad y Robustez de Ejecución
- [ ] Aislar la compilación y ejecución de pruebas del agente QA dentro de un contenedor Docker Sandbox (evitando ejecución directa sobre el host).
- [ ] Migrar el orquestador recursivo síncrono a un motor de estados reactivo o basado en eventos (ej: usando base de datos o colas Redis) para evitar fallos de timeout de hilos en llamadas largas.
- [ ] Refactorizar el frontend `App.vue` dividiéndolo en componentes reutilizables y limpios en Vue 3.

## Fase 4: Memoria a Largo Plazo y RAG
- [ ] Habilitar almacenamiento semántico en PostgreSQL con PGVector para archivar el histórico de cambios y código fuente de proyectos anteriores.
- [ ] Implementar un buscador semántico de código para los agentes utilizando `VectorStoreRetriever`.
- [ ] Conectar la caché y almacenamiento de historial de conversación en Redis (actualmente configurado pero no utilizado).
