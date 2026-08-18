package org.example.applicationinterviewservices.service;

import lombok.RequiredArgsConstructor;
import org.example.applicationinterviewservices.dto.request.CompleteInterviewRequest;
import org.example.applicationinterviewservices.dto.request.CreateInterviewRequest;
import org.example.applicationinterviewservices.dto.response.InterviewResponse;
import org.example.applicationinterviewservices.entity.ApplicationEntity;
import org.example.applicationinterviewservices.entity.ApplicationStatus;
import org.example.applicationinterviewservices.entity.InterviewEntity;
import org.example.applicationinterviewservices.entity.InterviewPhase;
import org.example.applicationinterviewservices.entity.InterviewStatus;
import org.example.applicationinterviewservices.exception.ApplicationNotFoundException;
import org.example.applicationinterviewservices.exception.DuplicateInterviewException;
import org.example.applicationinterviewservices.exception.InterviewNotFoundException;
import org.example.applicationinterviewservices.exception.InvalidInterviewPhaseException;
import org.example.applicationinterviewservices.repository.ApplicationRepository;
import org.example.applicationinterviewservices.repository.InterviewRepository;
import org.example.applicationinterviewservices.security.EmployeePrincipal;
import org.example.applicationinterviewservices.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final ApplicationRepository applicationRepository;

    @Transactional
    public InterviewResponse createInterview(CreateInterviewRequest request) {

        EmployeePrincipal principal = SecurityUtils.currentEmployee();

        ApplicationEntity application = applicationRepository
                .findByIdAndOrganizationId(request.applicationId(), principal.organizationId())
                .orElseThrow(() -> new ApplicationNotFoundException("Application not found"));

        if (application.getStatus() != ApplicationStatus.INTERVIEW) {
            throw new InvalidInterviewPhaseException("Interviews can only be created for applications with INTERVIEW status");
        }

        InterviewPhase phase = request.phase();

        validatePhaseOrder(application.getId(), phase);

        if (interviewRepository.existsByApplicationIdAndPhaseAndStatusNot(application.getId(), phase, InterviewStatus.CANCELLED)) {
            throw new DuplicateInterviewException("An interview for phase " + phase + " already exists for this application");
        }

        InterviewEntity interview = InterviewEntity.builder()
                .id(UUID.randomUUID())
                .applicationId(application.getId())
                .interviewerId(principal.employeeId())
                .phase(phase)
                .status(InterviewStatus.SCHEDULED)
                .scheduledAt(request.scheduledAt())
                .notes(null)
                .passed(null)
                .build();

        return toResponse(interviewRepository.save(interview));
    }

    @Transactional(readOnly = true)
    public InterviewResponse getInterview(UUID interviewId) {

        EmployeePrincipal principal = SecurityUtils.currentEmployee();

        InterviewEntity interview = findInterview(interviewId);

        applicationRepository.findByIdAndOrganizationId(interview.getApplicationId(), principal.organizationId())
                .orElseThrow(() -> new InterviewNotFoundException("Interview not found"));

        return toResponse(interview);
    }

    @Transactional(readOnly = true)
    public List<InterviewResponse> getApplicationInterviews(UUID applicationId) {
        EmployeePrincipal principal = SecurityUtils.currentEmployee();
        applicationRepository.findByIdAndOrganizationId(applicationId, principal.organizationId()).orElseThrow(() -> new ApplicationNotFoundException("Application not found"));
        return interviewRepository.findByApplicationId(applicationId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InterviewResponse> getOrganizationInterviews() {
        EmployeePrincipal principal = SecurityUtils.currentEmployee();
        return interviewRepository.findAllByOrganizationId(principal.organizationId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InterviewResponse> getInterviewerInterviews(UUID interviewerId) {
        SecurityUtils.currentEmployee();
        return interviewRepository.findByInterviewerId(interviewerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public InterviewResponse cancelInterview(UUID interviewId) {
        EmployeePrincipal principal = SecurityUtils.currentEmployee();
        InterviewEntity interview = findInterview(interviewId);
        applicationRepository.findByIdAndOrganizationId(interview.getApplicationId(), principal.organizationId())
                .orElseThrow(() -> new InterviewNotFoundException("Interview not found"));

        if (interview.getStatus() != InterviewStatus.SCHEDULED) {
            throw new InvalidInterviewPhaseException("Only scheduled interviews can be cancelled");
        }

        interview.setStatus(InterviewStatus.CANCELLED);
        return toResponse(interviewRepository.save(interview));
    }

    @Transactional
    public InterviewResponse completeInterview(UUID interviewId, CompleteInterviewRequest request) {
        EmployeePrincipal principal = SecurityUtils.currentEmployee();
        InterviewEntity interview = findInterview(interviewId);
        ApplicationEntity application = applicationRepository
                .findByIdAndOrganizationId(interview.getApplicationId(), principal.organizationId())
                .orElseThrow(() -> new InterviewNotFoundException("Interview not found"));
        if (interview.getStatus() != InterviewStatus.SCHEDULED) {
            throw new InvalidInterviewPhaseException("Only scheduled interviews can be completed");
        }

        interview.setStatus(InterviewStatus.COMPLETED);
        interview.setPassed(request.passed());
        interview.setNotes(request.notes());
        InterviewEntity savedInterview = interviewRepository.save(interview);
        applyInterviewResult(application, savedInterview);
        return toResponse(savedInterview);
    }

    private void applyInterviewResult(ApplicationEntity application, InterviewEntity interview) {
        if (Boolean.FALSE.equals(interview.getPassed())) {
            application.setStatus(ApplicationStatus.REJECTED);
            applicationRepository.save(application);
            return;
        }
        if (interview.getPhase() == InterviewPhase.TECHNICAL) {
            application.setStatus(ApplicationStatus.HIRED);
            applicationRepository.save(application);
        }
    }

    private void validatePhaseOrder(UUID applicationId, InterviewPhase requestedPhase) {

        if (requestedPhase == InterviewPhase.SCREENING) {
            return;
        }

        if (requestedPhase == InterviewPhase.HIRING_MANAGER) {
            requirePhaseCompleted(applicationId, InterviewPhase.SCREENING, "Screening");
            return;
        }

        if (requestedPhase == InterviewPhase.TECHNICAL) {
            requirePhaseCompleted(applicationId, InterviewPhase.HIRING_MANAGER, "Hiring Manager");
        }
    }

    private void requirePhaseCompleted(UUID applicationId, InterviewPhase phase, String label) {

        // Most recent record for this phase — a phase can have prior
        // cancelled attempts, so "completed and passed" must be judged by
        // the latest row, not an arbitrary/ambiguous single match.
        List<InterviewEntity> attempts =
                interviewRepository.findByApplicationIdAndPhaseOrderByCreatedAtDesc(applicationId, phase);

        boolean completedAndPassed = !attempts.isEmpty()
                && attempts.get(0).getStatus() == InterviewStatus.COMPLETED
                && Boolean.TRUE.equals(attempts.get(0).getPassed());

        if (!completedAndPassed) {
            throw new InvalidInterviewPhaseException(label + " interview must be completed and passed first");
        }
    }

    private InterviewEntity findInterview(UUID interviewId) {
        return interviewRepository.findById(interviewId).orElseThrow(() -> new InterviewNotFoundException("Interview not found"));
    }

    private InterviewResponse toResponse(InterviewEntity interview) {

        return InterviewResponse.builder()
                .id(interview.getId())
                .applicationId(interview.getApplicationId())
                .interviewerId(interview.getInterviewerId())
                .phase(interview.getPhase())
                .status(interview.getStatus())
                .scheduledAt(interview.getScheduledAt())
                .notes(interview.getNotes())
                .passed(interview.getPassed())
                .createdAt(interview.getCreatedAt())
                .build();
    }
}
