package com.swfactory.sdlc.domain.model;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Entidad de dominio que representa el contexto global de un proyecto de software
 * gestionado por la factoría de agentes.
 */
public class ProjectContext {
    private UUID id;
    private String name;
    private String description;
    private String repositoryUrl;
    private String currentPhase;
    private Map<String, String> phaseOutputs;
    private int rejectionCount; // Contador para controlar el ciclo de reintentos de QA
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ProjectContext() {}

    public ProjectContext(UUID id, String name, String description, String repositoryUrl, 
                          String currentPhase, Map<String, String> phaseOutputs, int rejectionCount, 
                          LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.repositoryUrl = repositoryUrl;
        this.currentPhase = currentPhase;
        this.phaseOutputs = phaseOutputs;
        this.rejectionCount = rejectionCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getRepositoryUrl() { return repositoryUrl; }
    public void setRepositoryUrl(String repositoryUrl) { this.repositoryUrl = repositoryUrl; }

    public String getCurrentPhase() { return currentPhase; }
    public void setCurrentPhase(String currentPhase) { this.currentPhase = currentPhase; }

    public Map<String, String> getPhaseOutputs() { return phaseOutputs; }
    public void setPhaseOutputs(Map<String, String> phaseOutputs) { this.phaseOutputs = phaseOutputs; }

    public int getRejectionCount() { return rejectionCount; }
    public void setRejectionCount(int rejectionCount) { this.rejectionCount = rejectionCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static ProjectContextBuilder builder() {
        return new ProjectContextBuilder();
    }

    public static class ProjectContextBuilder {
        private UUID id;
        private String name;
        private String description;
        private String repositoryUrl;
        private String currentPhase;
        private Map<String, String> phaseOutputs;
        private int rejectionCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public ProjectContextBuilder id(UUID id) { this.id = id; return this; }
        public ProjectContextBuilder name(String name) { this.name = name; return this; }
        public ProjectContextBuilder description(String description) { this.description = description; return this; }
        public ProjectContextBuilder repositoryUrl(String repositoryUrl) { this.repositoryUrl = repositoryUrl; return this; }
        public ProjectContextBuilder currentPhase(String currentPhase) { this.currentPhase = currentPhase; return this; }
        public ProjectContextBuilder phaseOutputs(Map<String, String> phaseOutputs) { this.phaseOutputs = phaseOutputs; return this; }
        public ProjectContextBuilder rejectionCount(int rejectionCount) { this.rejectionCount = rejectionCount; return this; }
        public ProjectContextBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public ProjectContextBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public ProjectContext build() {
            return new ProjectContext(id, name, description, repositoryUrl, currentPhase, phaseOutputs, rejectionCount, createdAt, updatedAt);
        }
    }
}
