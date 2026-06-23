# Registro de Comportamiento e Insights de Modelos

## Gemini (API Oficial)
- *Patrón observado:* Es altamente resolutivo interpretando reglas en Markdown (`SKILL.md`), pero requiere delimitadores claros (como `=== FILE ===`) para estructurar de manera óptima las modificaciones de múltiples archivos.

## Modelos Locales (Llama 3 / Qwen vía LM Studio u Ollama)
- *Patrón observado:* Presentan mayor probabilidad de bucles alucinando nombres de métodos si no se les proporciona la firma exacta del puerto de dominio.
- Se debe evitar enviarles un contexto de archivos excesivamente grande; priorizar filtros o resúmenes de código.
