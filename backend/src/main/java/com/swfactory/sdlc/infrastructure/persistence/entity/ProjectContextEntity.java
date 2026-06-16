package com.swfactory.sdlc.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Entidad de base de datos JPA para almacenar el estado del contexto de proyecto.
 */
@Entity
@Table(name = "project_contexts")
public class ProjectContextEntity {

    @Id
    private UUID id;

    private String name;
    
    @Column(length = 2000)
    private String description;

    private String repositoryUrl;

    private String currentPhase;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "project_phase_outputs", joinColumns = @JoinColumn(name = "project_context_id"))
    @MapKeyColumn(name = "phase_name")
    @Column(name = "output_data", length = 65535)
    private Map<String, String> phaseOutputs;

    private int rejectionCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ProjectContextEntity() {}

    public ProjectContextEntity(UUID id, String name, String description, String repositoryUrl, 
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

    public static ProjectContextEntityBuilder builder() {
        return new ProjectContextEntityBuilder();
    }

    public static class ProjectContextEntityBuilder {
        private UUID id;
        private String name;
        private String description;
        private String repositoryUrl;
        private String currentPhase;
        private Map<String, String> phaseOutputs;
        private int rejectionCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public ProjectContextEntityBuilder id(UUID id) { this.id = id; return this; }
        public ProjectContextEntityBuilder name(String name) { this.name = name; return this; }
        public ProjectContextEntityBuilder description(String description) { this.description = description; return this; }
        public ProjectContextEntityBuilder repositoryUrl(String repositoryUrl) { this.repositoryUrl = repositoryUrl; return this; }
        public ProjectContextEntityBuilder currentPhase(String currentPhase) { this.currentPhase = currentPhase; return this; }
        public ProjectContextEntityBuilder phaseOutputs(Map<String, String> phaseOutputs) { this.phaseOutputs = phaseOutputs; return this; }
        public ProjectContextEntityBuilder rejectionCount(int rejectionCount) { this.rejectionCount = rejectionCount; return this; }
        public ProjectContextEntityBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public ProjectContextEntityBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public ProjectContextEntity build() {
            return new ProjectContextEntity(id, name, description, repositoryUrl, currentPhase, phaseOutputs, rejectionCount, createdAt, updatedAt);
        }
    }
}
