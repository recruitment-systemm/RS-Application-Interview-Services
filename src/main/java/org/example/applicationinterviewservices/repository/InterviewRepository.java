package org.example.applicationinterviewservices.repository;

import org.example.applicationinterviewservices.entity.InterviewEntity;
import org.example.applicationinterviewservices.entity.InterviewPhase;
import org.example.applicationinterviewservices.entity.InterviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface InterviewRepository extends JpaRepository<InterviewEntity, UUID> {
    List<InterviewEntity> findByApplicationId(UUID applicationId);
    /**
     * A phase can have more than one row over time (a cancelled attempt plus
     * a rescheduled one), so this must return a list, not `Optional` — a
     * single-result lookup here throws `IncorrectResultSizeDataAccessException`
     * once a phase has been cancelled and rescheduled even once, since it
     * then has 2+ rows for the same (applicationId, phase) pair.
     */
    List<InterviewEntity> findByApplicationIdAndPhaseOrderByCreatedAtDesc(UUID applicationId, InterviewPhase phase);
    boolean existsByApplicationIdAndPhase(UUID applicationId, InterviewPhase phase);
    boolean existsByApplicationIdAndPhaseAndStatusNot(UUID applicationId, InterviewPhase phase, InterviewStatus status);
    List<InterviewEntity> findByInterviewerId(UUID interviewerId);

    @Query("""
            SELECT i FROM InterviewEntity i
            WHERE i.applicationId IN (
                SELECT a.id FROM ApplicationEntity a WHERE a.organizationId = :organizationId
            )
            """)
    List<InterviewEntity> findAllByOrganizationId(@Param("organizationId") UUID organizationId);
}
