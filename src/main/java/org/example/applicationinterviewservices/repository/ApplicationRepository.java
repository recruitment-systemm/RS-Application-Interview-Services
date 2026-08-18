package org.example.applicationinterviewservices.repository;

import org.example.applicationinterviewservices.entity.ApplicationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<ApplicationEntity, UUID> {
    List<ApplicationEntity> findAllByOrganizationId(UUID organizationId);
    Optional<ApplicationEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);
    boolean existsByJobIdAndEmail(UUID jobId, String email);
}
