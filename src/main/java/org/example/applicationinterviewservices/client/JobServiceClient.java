package org.example.applicationinterviewservices.client;

import org.example.applicationinterviewservices.dto.response.JobResponse;
import org.example.applicationinterviewservices.exception.ApplicationNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.UUID;

@Component
public class JobServiceClient {

    private final RestClient restClient;

    public JobServiceClient(@Value("${services.job-service.url}") String jobServiceUrl) {
        this.restClient = RestClient.builder().baseUrl(jobServiceUrl).build();
    }

    public JobResponse getJob(UUID jobId) {
        try {
            JobServiceApiResponse response = restClient.get()
                    .uri("/api/v1/jobs")
                    .retrieve()
                    .body(JobServiceApiResponse.class);

            List<JobResponse> jobs = response == null ? List.of() : response.data();

            return jobs.stream()
                    .filter(job -> jobId.equals(job.id()))
                    .findFirst()
                    .orElseThrow(() -> new ApplicationNotFoundException("Job not found"));

        } catch (RestClientException e) {
            throw new ApplicationNotFoundException("Job not found or job-service is unavailable");
        }
    }

    private record JobServiceApiResponse(boolean success, int statusCode, String message, List<JobResponse> data) {}
}
