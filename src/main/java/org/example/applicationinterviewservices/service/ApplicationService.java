package org.example.applicationinterviewservices.service;

import lombok.RequiredArgsConstructor;
import org.example.applicationinterviewservices.client.JobServiceClient;
import org.example.applicationinterviewservices.dto.request.CreateApplicationRequest;
import org.example.applicationinterviewservices.dto.request.UpdateApplicationStatusRequest;
import org.example.applicationinterviewservices.dto.response.ApplicationResponse;
import org.example.applicationinterviewservices.dto.response.JobResponse;
import org.example.applicationinterviewservices.entity.ApplicationEntity;
import org.example.applicationinterviewservices.entity.ApplicationStatus;
import org.example.applicationinterviewservices.entity.InterviewEntity;
import org.example.applicationinterviewservices.entity.InterviewPhase;
import org.example.applicationinterviewservices.entity.InterviewStatus;
import org.example.applicationinterviewservices.exception.ApplicationNotFoundException;
import org.example.applicationinterviewservices.exception.DuplicateApplicationException;
import org.example.applicationinterviewservices.exception.InvalidStatusTransitionException;
import org.example.applicationinterviewservices.repository.ApplicationRepository;
import org.example.applicationinterviewservices.repository.InterviewRepository;
import org.example.applicationinterviewservices.security.EmployeePrincipal;
import org.example.applicationinterviewservices.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final InterviewRepository interviewRepository;
    private final CloudinaryService cloudinaryService;
    private final JobServiceClient jobServiceClient;
    private final EmailService emailService;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private static final Map<ApplicationStatus, Set<ApplicationStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(ApplicationStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(ApplicationStatus.SOURCED,
                EnumSet.of(ApplicationStatus.IN_PROGRESS, ApplicationStatus.REJECTED)
        );
        ALLOWED_TRANSITIONS.put(
                ApplicationStatus.IN_PROGRESS,
                EnumSet.of(ApplicationStatus.INTERVIEW, ApplicationStatus.REJECTED)
        );
        ALLOWED_TRANSITIONS.put(
                ApplicationStatus.INTERVIEW,
                EnumSet.of(ApplicationStatus.HIRED, ApplicationStatus.REJECTED)
        );
        ALLOWED_TRANSITIONS.put(
                ApplicationStatus.REJECTED,
                EnumSet.of(ApplicationStatus.SOURCED)
        );
        ALLOWED_TRANSITIONS.put(
                ApplicationStatus.HIRED,
                EnumSet.of(ApplicationStatus.LAYOFF)
        );
        ALLOWED_TRANSITIONS.put(
                ApplicationStatus.LAYOFF,
                EnumSet.noneOf(ApplicationStatus.class)
        );
    }

    @Transactional
    public ApplicationResponse createApplication(CreateApplicationRequest request) {

        validateApplicationRequest(request);

        if (applicationRepository.existsByJobIdAndEmail(request.jobId(), request.email())) {
            throw new DuplicateApplicationException("An application already exists for this job with the given email");
        }

        JobResponse job = jobServiceClient.getJob(request.jobId());

        String resumeUrl = cloudinaryService.uploadDocument(request.resume());

        ApplicationEntity application = ApplicationEntity.builder()
                .id(UUID.randomUUID())
                .jobId(request.jobId())
                .organizationId(job.organizationId())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .phone(request.phone())
                .resumeUrl(resumeUrl)
                .status(ApplicationStatus.SOURCED)
                .build();

        ApplicationEntity savedApplication = applicationRepository.save(application);

        emailService.sendApplicationReceivedEmail(savedApplication, job.title());

        return toResponse(savedApplication);
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplications() {

        EmployeePrincipal principal = SecurityUtils.currentEmployee();

        return applicationRepository.findAllByOrganizationId(principal.organizationId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getAllApplications() {
        return applicationRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getApplication(UUID applicationId) {

        EmployeePrincipal principal = SecurityUtils.currentEmployee();

        ApplicationEntity application = findByIdAndOrganization(applicationId, principal.organizationId());

        return toResponse(application);
    }

    @Transactional
    public ApplicationResponse updateStatus(UUID applicationId, UpdateApplicationStatusRequest request) {

        EmployeePrincipal principal = SecurityUtils.currentEmployee();

        ApplicationEntity application = findByIdAndOrganization(applicationId, principal.organizationId());

        validateTransition(application.getStatus(), request.status());

        if (request.status() == ApplicationStatus.HIRED) {
            requireAllInterviewsPassed(application.getId());
        }

        application.setStatus(request.status());

        ApplicationEntity savedApplication = applicationRepository.save(application);

        String jobTitle = jobServiceClient.getJob(savedApplication.getJobId()).title();
        emailService.sendApplicationStatusEmail(savedApplication, jobTitle);

        return toResponse(savedApplication);
    }

    private void validateApplicationRequest(CreateApplicationRequest request) {

        if (request.jobId() == null) {
            throw new IllegalArgumentException("Job ID is required");
        }

        if (request.firstName() == null || request.firstName().isBlank()) {
            throw new IllegalArgumentException("First name is required");
        }

        if (request.lastName() == null || request.lastName().isBlank()) {
            throw new IllegalArgumentException("Last name is required");
        }

        if (request.email() == null || request.email().isBlank() || !EMAIL_PATTERN.matcher(request.email()).matches()) {
            throw new IllegalArgumentException("A valid email is required");
        }
    }

    private ApplicationEntity findByIdAndOrganization(UUID applicationId, UUID organizationId) {
        return applicationRepository.findByIdAndOrganizationId(applicationId, organizationId)
                .orElseThrow(() -> new ApplicationNotFoundException("Application not found"));
    }

    private void validateTransition(ApplicationStatus current, ApplicationStatus next) {

        if (current == next) {
            throw new InvalidStatusTransitionException("Application is already in status " + current);
        }

        Set<ApplicationStatus> allowed = ALLOWED_TRANSITIONS.get(current);

        if (allowed == null || !allowed.contains(next)) {
            throw new InvalidStatusTransitionException("Cannot transition application from " + current + " to " + next);
        }
    }

    /**
     * A candidate can only be hired once every interview phase has a
     * completed, passed attempt — otherwise HR could move Interview →
     * Hired directly from the board without any interview ever taking
     * place. Mirrors the "latest attempt wins" rule already used by
     * {@code InterviewService.requirePhaseCompleted} (a phase can have a
     * cancelled attempt followed by a rescheduled one).
     */
    private void requireAllInterviewsPassed(UUID applicationId) {
        for (InterviewPhase phase : InterviewPhase.values()) {
            List<InterviewEntity> attempts =
                    interviewRepository.findByApplicationIdAndPhaseOrderByCreatedAtDesc(applicationId, phase);

            boolean completedAndPassed = !attempts.isEmpty()
                    && attempts.get(0).getStatus() == InterviewStatus.COMPLETED
                    && Boolean.TRUE.equals(attempts.get(0).getPassed());

            if (!completedAndPassed) {
                throw new InvalidStatusTransitionException(
                        "Cannot hire this candidate until every interview round has been completed and passed"
                );
            }
        }
    }

    private ApplicationResponse toResponse(ApplicationEntity application) {

        return ApplicationResponse.builder()
                .id(application.getId())
                .jobId(application.getJobId())
                .organizationId(application.getOrganizationId())
                .firstName(application.getFirstName())
                .lastName(application.getLastName())
                .email(application.getEmail())
                .phone(application.getPhone())
                .resumeUrl(application.getResumeUrl())
                .status(application.getStatus())
                .createdAt(application.getCreatedAt())
                .updatedAt(application.getUpdatedAt())
                .build();
    }
}
