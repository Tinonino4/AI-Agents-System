package com.swfactory.sdlc.infrastructure.persistence.adapter;

import com.swfactory.sdlc.domain.model.AgentTask;
import com.swfactory.sdlc.domain.repository.AgentTaskRepository;
import com.swfactory.sdlc.infrastructure.persistence.entity.AgentTaskEntity;
import com.swfactory.sdlc.infrastructure.persistence.repository.SpringDataAgentTaskRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adaptador de persistencia para AgentTask. Implementa el puerto del dominio
 * interactuando con Spring Data JPA y realizando el mapeo de objetos.
 */
@Component
public class AgentTaskRepositoryAdapter implements AgentTaskRepository {

    private final SpringDataAgentTaskRepository springDataRepository;

    public AgentTaskRepositoryAdapter(SpringDataAgentTaskRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public AgentTask save(AgentTask task) {
        AgentTaskEntity entity = toEntity(task);
        AgentTaskEntity savedEntity = springDataRepository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    public Optional<AgentTask> findById(UUID id) {
        return springDataRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<AgentTask> findByProjectId(UUID projectId) {
        return springDataRepository.findByProjectId(projectId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private AgentTaskEntity toEntity(AgentTask domain) {
        if (domain == null) return null;
        return AgentTaskEntity.builder()
                .id(domain.getId())
                .projectId(domain.getProjectId())
                .agentRole(domain.getAgentRole())
                .title(domain.getTitle())
                .description(domain.getDescription())
                .status(domain.getStatus())
                .inputData(domain.getInputData())
                .outputData(domain.getOutputData())
                .reviewerFeedback(domain.getReviewerFeedback())
                .build();
    }

    private AgentTask toDomain(AgentTaskEntity entity) {
        if (entity == null) return null;
        return AgentTask.builder()
                .id(entity.getId())
                .projectId(entity.getProjectId())
                .agentRole(entity.getAgentRole())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .inputData(entity.getInputData())
                .outputData(entity.getOutputData())
                .reviewerFeedback(entity.getReviewerFeedback())
                .build();
    }
}
