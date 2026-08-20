package org.example.applicationinterviewservices.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.applicationinterviewservices.entity.ApplicationEntity;
import org.example.applicationinterviewservices.entity.ApplicationStatus;
import org.example.applicationinterviewservices.entity.InterviewEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    /**
     * Interviews are always scheduled and read by Egypt-based HR teams, but
     * `InterviewEntity.scheduledAt` is stored as UTC (the frontend sends
     * `Date.toISOString()`, which is always UTC regardless of the picker's
     * local timezone). Formatting it without converting first showed the
     * candidate a time 3 hours behind what HR actually picked. There's no
     * per-organization timezone setting anywhere in this system yet, so this
     * is a fixed choice, not a general solution — revisit if orgs outside
     * Egypt ever onboard.
     */
    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Africa/Cairo");
    private static final DateTimeFormatter SCHEDULE_FORMAT = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy 'at' h:mm a", Locale.ENGLISH);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    public void sendApplicationReceivedEmail(ApplicationEntity application, String jobTitle) {
        String headline = "We've received your application";
        String message = "Thanks for applying — we've received your application and the hiring team will review it shortly.";
        send(application.getEmail(), headline, application.getFirstName(), headline, message, jobTitle, "Submitted", null);
    }

    public void sendApplicationStatusEmail(ApplicationEntity application, String jobTitle) {
        String headline;
        String message;

        switch (application.getStatus()) {
            case IN_PROGRESS -> {
                headline = "Your application is being reviewed";
                message = "Good news — your application is now being reviewed by the hiring team.";
            }
            case INTERVIEW -> {
                headline = "You've been moved to interviews";
                message = "Congratulations! You've progressed to the interview stage. The hiring team will reach out to schedule your interview.";
            }
            case HIRED -> {
                headline = "Congratulations, you got the job!";
                message = "We're excited to let you know that you've been selected for this position. The hiring team will follow up with next steps.";
            }
            case REJECTED -> {
                headline = "Update on your application";
                message = "Thank you for your interest. After careful consideration, we've decided not to move forward with your application at this time.";
            }
            case LAYOFF -> {
                headline = "Update on your employment status";
                message = "We're writing to inform you of a change to your employment status. Please reach out to the organization directly for details.";
            }
            default -> {
                headline = "Your application status has changed";
                message = "There's an update on your application.";
            }
        }

        send(application.getEmail(), headline, application.getFirstName(), headline, message, jobTitle, formatStatus(application.getStatus()), null);
    }

    public void sendInterviewScheduledEmail(ApplicationEntity application, InterviewEntity interview, String jobTitle) {
        String headline = "Your interview has been scheduled";
        String scheduledAt = interview.getScheduledAt() != null
                ? interview.getScheduledAt().atZoneSameInstant(DISPLAY_ZONE).format(SCHEDULE_FORMAT)
                : "a time to be confirmed";
        String message = "Your " + formatPhase(interview.getPhase().name())
                + " interview has been scheduled for <strong>" + scheduledAt + "</strong>.";

        send(application.getEmail(), headline, application.getFirstName(), headline, message, jobTitle, formatPhase(interview.getPhase().name()) + " interview", null);
    }

    public void sendInterviewCancelledEmail(ApplicationEntity application, InterviewEntity interview, String jobTitle) {
        String headline = "Your interview has been cancelled";
        String scheduledAt = interview.getScheduledAt() != null
                ? interview.getScheduledAt().atZoneSameInstant(DISPLAY_ZONE).format(SCHEDULE_FORMAT)
                : null;
        String message = "Your " + formatPhase(interview.getPhase().name()) + " interview"
                + (scheduledAt != null ? " scheduled for <strong>" + scheduledAt + "</strong>" : "")
                + " has been cancelled. The hiring team will follow up if it needs to be rescheduled.";

        send(application.getEmail(), headline, application.getFirstName(), headline, message, jobTitle, formatPhase(interview.getPhase().name()) + " interview — Cancelled", null);
    }

    public void sendInterviewResultEmail(ApplicationEntity application, InterviewEntity interview, String jobTitle) {
        boolean passed = Boolean.TRUE.equals(interview.getPassed());
        String headline = passed ? "You passed your interview" : "Update on your interview";
        String message = passed
                ? "Great news — you passed your " + formatPhase(interview.getPhase().name()) + " interview."
                : "Thank you for taking the time to interview with us. Unfortunately, you did not pass the "
                        + formatPhase(interview.getPhase().name()) + " interview.";

        send(application.getEmail(), headline, application.getFirstName(), headline, message, jobTitle, passed ? "Passed" : "Not passed", interview.getNotes());
    }

    private void send(String to, String subject, String firstName, String headline, String message, String jobTitle, String statusLabel, String notes) {
        try {
            Context context = new Context();
            context.setVariable("headline", headline);
            context.setVariable("greeting", "Hi " + firstName + ",");
            context.setVariable("message", message);
            context.setVariable("jobTitle", jobTitle != null ? jobTitle : "—");
            context.setVariable("statusLabel", statusLabel);
            context.setVariable("notes", notes);

            String html = templateEngine.process("email/application-status", context);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            log.error("Failed to send status email to {}", to, e);
        }
    }

    private String formatStatus(ApplicationStatus status) {
        String[] words = status.name().split("_");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            builder.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1).toLowerCase(Locale.ENGLISH))
                    .append(' ');
        }
        return builder.toString().trim();
    }

    private String formatPhase(String phase) {
        String[] words = phase.split("_");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            builder.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1).toLowerCase(Locale.ENGLISH))
                    .append(' ');
        }
        return builder.toString().trim();
    }
}
