package com.swfactory.sdlc.domain.model;

/**
 * Representa el resultado de la construcción y ejecución de pruebas del workspace.
 */
public record BuildResult(
    boolean success,
    String logs
) {}
