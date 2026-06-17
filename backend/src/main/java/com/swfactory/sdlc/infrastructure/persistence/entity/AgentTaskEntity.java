package com.swfactory.sdlc.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * Entidad de base de datos JPA para almacenar el estado de las tareas agénticas.
 */
@Entity
@Table(name = "agent_tasks")
public class AgentTaskEntity {

    @Id
    private UUID id;

    private UUID projectId;

    private String agentRole;

    private String title;

    @Column(length = 2000)
    private String description;

    private String status;

    @Column(columnDefinition = "TEXT")
    private String inputData;

    @Column(columnDefinition = "TEXT")
    private String outputData;

    @Column(length = 2000)
    private String reviewerFeedback;

    public AgentTaskEntity() {}

    public AgentTaskEntity(UUID id, UUID projectId, String agentRole, String title, 
                           String description, String status, String inputData, 
                           String outputData, String reviewerFeedback) {
        this.id = id;
        this.projectId = projectId;
        this.agentRole = agentRole;
        this.title = title;
        this.description = description;
        this.status = status;
        this.inputData = inputData;
        this.outputData = outputData;
        this.reviewerFeedback = reviewerFeedback;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }

    public String getAgentRole() { return agentRole; }
    public void setAgentRole(String agentRole) { this.agentRole = agentRole; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getInputData() { return inputData; }
    public void setInputData(String inputData) { this.inputData = inputData; }

    public String getOutputData() { return outputData; }
    public void setOutputData(String outputData) { this.outputData = outputData; }

    public String getReviewerFeedback() { return reviewerFeedback; }
    public void setReviewerFeedback(String reviewerFeedback) { this.reviewerFeedback = reviewerFeedback; }

    public static AgentTaskEntityBuilder builder() {
        return new AgentTaskEntityBuilder();
    }

    public static class AgentTaskEntityBuilder {
        private UUID id;
        private UUID projectId;
        private String agentRole;
        private String title;
        private String description;
        private String status;
        private String inputData;
        private String outputData;
        private String reviewerFeedback;

        public AgentTaskEntityBuilder id(UUID id) { this.id = id; return this; }
        public AgentTaskEntityBuilder projectId(UUID projectId) { this.projectId = projectId; return this; }
        public AgentTaskEntityBuilder agentRole(String agentRole) { this.agentRole = agentRole; return this; }
        public AgentTaskEntityBuilder title(String title) { this.title = title; return this; }
        public AgentTaskEntityBuilder description(String description) { this.description = description; return this; }
        public AgentTaskEntityBuilder status(String status) { this.status = status; return this; }
        public AgentTaskEntityBuilder inputData(String inputData) { this.inputData = inputData; return this; }
        public AgentTaskEntityBuilder outputData(String outputData) { this.outputData = outputData; return this; }
        public AgentTaskEntityBuilder reviewerFeedback(String reviewerFeedback) { this.reviewerFeedback = reviewerFeedback; return this; }

        public AgentTaskEntity build() {
            return new AgentTaskEntity(id, projectId, agentRole, title, description, status, inputData, outputData, reviewerFeedback);
        }
    }
}
