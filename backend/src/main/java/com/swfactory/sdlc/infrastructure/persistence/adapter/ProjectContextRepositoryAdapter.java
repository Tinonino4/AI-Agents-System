package com.swfactory.sdlc.infrastructure.persistence.adapter;

import com.swfactory.sdlc.domain.model.ProjectContext;
import com.swfactory.sdlc.domain.repository.ProjectContextRepository;
import com.swfactory.sdlc.infrastructure.persistence.entity.ProjectContextEntity;
import com.swfactory.sdlc.infrastructure.persistence.repository.SpringDataProjectContextRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adaptador de persistencia para ProjectContext. Implementa el puerto del dominio
 * interactuando con Spring Data JPA y mapeando entidades.
 */
@Component
public class ProjectContextRepositoryAdapter implements ProjectContextRepository {

    private final SpringDataProjectContextRepository springDataRepository;

    public ProjectContextRepositoryAdapter(SpringDataProjectContextRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public ProjectContext save(ProjectContext context) {
        ProjectContextEntity entity = toEntity(context);
        ProjectContextEntity savedEntity = springDataRepository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    public Optional<ProjectContext> findById(UUID id) {
        return springDataRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<ProjectContext> findAll() {
        return springDataRepository.findAll().stream()
                .map(this::toDomain)
                .sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()))
                .collect(Collectors.toList());
    }

    private ProjectContextEntity toEntity(ProjectContext domain) {
        if (domain == null) return null;
        return ProjectContextEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .description(domain.getDescription())
                .repositoryUrl(domain.getRepositoryUrl())
                .currentPhase(domain.getCurrentPhase())
                .phaseOutputs(domain.getPhaseOutputs())
                .rejectionCount(domain.getRejectionCount())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    private ProjectContext toDomain(ProjectContextEntity entity) {
        if (entity == null) return null;
        return ProjectContext.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .repositoryUrl(entity.getRepositoryUrl())
                .currentPhase(entity.getCurrentPhase())
                .phaseOutputs(entity.getPhaseOutputs())
                .rejectionCount(entity.getRejectionCount())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
