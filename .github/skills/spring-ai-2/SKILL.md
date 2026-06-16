# Skill: Desarrollo con Spring AI 2.x

Esta guía técnica define los patrones, componentes y reglas arquitectónicas necesarias para desarrollar aplicaciones basadas en Inteligencia Artificial utilizando Spring AI 2.x. 

## 1. Requisitos y Entorno
* **Versiones Base:** Spring AI 2.x requiere Java 21+ y Spring Boot 4.0+ (junto con Spring Framework 7.0) [1, 2].
* **Dependencias:** Utiliza el BOM de Spring AI (`spring-ai-bom`) en su versión `2.0.x` [1, 3]. Las dependencias para componentes específicos (modelos de chat, vector stores, etc.) se agregan sin versión explícita.

## 2. ChatClient: La API Fluida
La interacción principal con los modelos se realiza a través de `ChatClient`, que ofrece una API fluida inspirada en `WebClient` [4, 5].

* **Creación:** Utiliza siempre el `ChatClient.Builder` autoconfigurado por Spring Boot para conservar la observabilidad y los customizadores [6, 7].
  ```java
  @RestController
  public class ChatController {
      private final ChatClient chatClient;
      
      public ChatController(ChatClient.Builder builder) {
          this.chatClient = builder.build();
      }
      // ...
  }
  ```
* **Uso Básico:** La API permite encadenar `.prompt()`, `.system()`, `.user()`, `.advisors()`, `.tools()` y definir la salida con `.call()` o `.stream()`.
* **Retorno Estructurado (POJOs):** Utiliza `.entity(MiClase.class)` en lugar de procesar JSON manualmente. Spring AI soporta Native Structured Output invocando a las APIs nativas del modelo (ej. OpenAI, Gemini) habilitando `AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT` o mediante el configurador `Consumer<EntityParamSpec>`.

## 3. Novedades y Reglas en Tool Calling (Llamada a Herramientas)
La ejecución de herramientas sufrió una refactorización mayor en 2.0.

* **El Bucle de Ejecución es Externo:** Las implementaciones de `ChatModel` ya no ejecutan las herramientas internamente (se eliminó `internalToolExecutionEnabled`).
* **ToolCallingAdvisor:** La ejecución automática de herramientas ahora la maneja el `ToolCallingAdvisor`, que se auto-registra por defecto en `ChatClient`.
* **Declaración de Herramientas:** Usa la anotación `@Tool` en métodos públicos de tus beans en lugar de los antiguos callbacks. Define siempre el atributo description, ya que el LLM lo necesita para entender la herramienta. Los parámetros pueden tener descripciones usando `@ToolParam`.
* **Registro de Herramientas:** Pasa las herramientas explícitamente mediante `.tools(miServicio)` o colecciones de `ToolCallback`. Se eliminaron las APIs basadas en nombres como `toolNames()` y `SpringBeanToolCallbackResolver`.
* **Descubrimiento Dinámico:** Para sistemas con muchas herramientas, utiliza `ToolSearchToolCallingAdvisor` (con índices Lucene, Vectoriales o Regex). Esto permite que el LLM descubra herramientas bajo demanda en lugar de cargar todo el catálogo en el prompt.

## 4. Bases de Datos Vectoriales y RAG
El framework soporta decenas de Vector Stores (Chroma, PGvector, Redis, Milvus, etc.) a través de una API unificada.

* **BREAKING CHANGE (Esquemas):** A partir de Spring AI 2.0, la inicialización automática de esquemas en las bases de datos (creación de tablas/índices) está deshabilitada por defecto. Debe habilitarse explícitamente vía propiedades (ej. `spring.ai.vectorstore.pgvector.initialize-schema=true`) o en los constructores.
* **Filtros de Metadatos:** Utiliza el API de `Filter.Expression` para construir filtros portables entre distintas bases de datos vectoriales. Se pueden escribir mediante DSL programático (`b.eq("pais", "ES")`) o expresiones estilo SQL (`"pais == 'ES' && año >= 2020"`).
* **Estrategia de Batching:** Para evitar superar la ventana de contexto al guardar muchos documentos, el sistema usa `TokenCountBatchingStrategy` por defecto.
* **VectorStore vs VectorStoreRetriever:** `VectorStore` permite operaciones de mutación (escribir, borrar). Utiliza la interfaz de solo lectura `VectorStoreRetriever` en aplicaciones RAG para aplicar el principio de mínimo privilegio en los componentes de consulta.

## 5. Advisors (Asesores) e Interceptores
Los Advisors interceptan la petición/respuesta para enriquecer la interacción con el modelo. Interfaces clave: `CallAdvisor` y `StreamAdvisor` (reemplazan a los antiguos `CallAroundAdvisor` etc.).

* **Memoria de Conversación:** Utiliza `MessageChatMemoryAdvisor` para mantener historial. **Obligatorio:** Debes suministrar el parámetro `ChatMemory.CONVERSATION_ID` en el request mediante `.param()`, de lo contrario lanzará una excepción `IllegalArgumentException`.
* **SafeGuardAdvisor:** Para evitar respuestas dañinas o inyecciones de prompt.
* **QuestionAnswerAdvisor:** Implementa el patrón "Naive RAG" adjuntando documentos relevantes recuperados de un `VectorStore` al prompt del usuario.

## 6. Model Context Protocol (MCP)
Spring AI 2.x introduce soporte de primera clase (clientes y servidores) para MCP, un protocolo estándar para exponer datos y herramientas a agentes de IA locales y remotos. Las implementaciones de transporte en Spring ahora están en `org.springframework.ai.mcp`.

* **@McpTool:** Expone métodos Java como herramientas (acciones ejecutables). Utiliza `@McpToolParam` para documentar argumentos.
* **@McpResource:** Expone datos de solo lectura, usando plantillas URI (ej. `@McpResource("logs://{service}/{date}")`).
* **@McpPrompt:** Define plantillas de prompts estandarizadas y reutilizables.
* **@McpComplete:** Permite habilitar autocompletado en los argumentos de prompts para clientes que lo soporten.
* **Restricción de Logs:** Si utilizas el transporte STDIO en un servidor MCP, asegúrate de silenciar los logs a consola (`logging.level.root=off`), ya que cualquier texto ajeno al protocolo corromperá el intercambio de mensajes JSON.

## 7. Caché Semántica
Soportada de forma nativa a través de Redis. Permite cachear respuestas basándose en la similitud semántica del prompt del usuario en lugar de buscar un string exacto.

* Utiliza el `SemanticCacheAdvisor` dentro del `ChatClient`.
* Configura la similitud mínima deseada (`spring.ai.vectorstore.redis.semantic-cache.similarity-threshold`, por defecto 0.95).

## 8. Migración y Deprecaciones Comunes
* **Jackson 3:** Spring AI 2.x migró a Jackson 3 (paquete `tools.jackson` en lugar de `com.fasterxml.jackson`).
* **Anthropic SDK:** La integración de Anthropic usa ahora el SDK oficial nativo de Anthropic en Java en lugar de la implementación anterior vía WebClient.
* **Opciones en Constructores:** Los métodos como `N()` se renombraron a `n()` por convenciones Java.
* **ChatClientCustomizer:** Ha sido deprecado y reemplazado por `ChatClientBuilderCustomizer`.
