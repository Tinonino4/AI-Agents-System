package com.swfactory.sdlc.domain.model;

import java.util.UUID;

/**
 * Entidad de dominio que representa una tarea asignada a un agente o subagente,
 * soportando estados de revisión humana (Human-in-the-Loop).
 */
public class AgentTask {
    private UUID id;
    private UUID projectId;
    private String agentRole;
    private String title;
    private String description;
    private String status;
    private String inputData;
    private String outputData;
    private String reviewerFeedback;

    public AgentTask() {}

    public AgentTask(UUID id, UUID projectId, String agentRole, String title, 
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

    public static AgentTaskBuilder builder() {
        return new AgentTaskBuilder();
    }

    public static class AgentTaskBuilder {
        private UUID id;
        private UUID projectId;
        private String agentRole;
        private String title;
        private String description;
        private String status;
        private String inputData;
        private String outputData;
        private String reviewerFeedback;

        public AgentTaskBuilder id(UUID id) { this.id = id; return this; }
        public AgentTaskBuilder projectId(UUID projectId) { this.projectId = projectId; return this; }
        public AgentTaskBuilder agentRole(String agentRole) { this.agentRole = agentRole; return this; }
        public AgentTaskBuilder title(String title) { this.title = title; return this; }
        public AgentTaskBuilder description(String description) { this.description = description; return this; }
        public AgentTaskBuilder status(String status) { this.status = status; return this; }
        public AgentTaskBuilder inputData(String inputData) { this.inputData = inputData; return this; }
        public AgentTaskBuilder outputData(String outputData) { this.outputData = outputData; return this; }
        public AgentTaskBuilder reviewerFeedback(String reviewerFeedback) { this.reviewerFeedback = reviewerFeedback; return this; }

        public AgentTask build() {
            return new AgentTask(id, projectId, agentRole, title, description, status, inputData, outputData, reviewerFeedback);
        }
    }
}
