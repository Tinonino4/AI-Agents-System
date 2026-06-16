package com.swfactory.sdlc.domain.repository;

import java.util.Map;

/**
 * Puerto de Dominio. Define las operaciones permitidas sobre el espacio de trabajo
 * donde se escribe y verifica el código generado por la factoría de agentes.
 */
public interface WorkspaceRepository {
    
    /**
     * Escribe un archivo con el contenido especificado en el workspace.
     */
    void writeFile(String path, String content);

    /**
     * Lee el contenido de un archivo del workspace.
     */
    String readFile(String path);

    /**
     * Lista todos los archivos del workspace que coinciden con un patrón.
     */
    Map<String, String> listFiles(String directoryPattern);

    /**
     * Ejecuta los tests del proyecto en un entorno sandbox (o terminal local)
     * y retorna true si compila y pasa todas las pruebas con éxito.
     */
    boolean executeBuildAndTest();
}
