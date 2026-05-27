/**
 * Perform AI Backend — Analysis Controller
 *
 * REST controller exposing the athlete analysis API endpoints.
 * Handles job submission and status polling following standard
 * HTTP async request/response patterns.
 *
 * @author Md Ehteshamul Haque Tamvir
 * @email  mtamvir@gmail.com
 */
package com.performai.controller;

import com.performai.model.AnalysisJob;
import com.performai.model.AnalysisResult;
import com.performai.model.AnalysisStatus;
import com.performai.service.AnalysisService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Instant;

/**
 * Exposes two endpoints for the analysis workflow:
 *   POST /api/v1/analysis       — submit a new analysis job
 *   GET  /api/v1/analysis/{id} — poll the status/result of a job
 *
 * Follows the async HTTP pattern:
 * submit returns {@code 202 Accepted} + {@code Location} header;
 * the client polls the Location URL until status is {@code COMPLETED}.
 */
@RestController
@RequestMapping("/api/v1/analysis")
public class AnalysisController {

    private static final Logger log = LoggerFactory.getLogger(AnalysisController.class);

    private final AnalysisService analysisService;

    /**
     * Constructor injection — preferred over field injection for testability.
     *
     * @param analysisService the service handling job lifecycle
     */
    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    /**
     * Submits a new analysis job for the specified athlete.
     * The job is queued for async processing. Response includes 202 Accepted,
     * a Location header pointing to the polling URL, and the initial job state.
     *
     * @param request validated request body containing the athlete name
     * @return {@code 202 Accepted} with job details and Location header
     */
    @PostMapping
    public ResponseEntity<AnalysisResponse> submit(@Valid @RequestBody AnalysisRequest request) {
        log.info("Analysis job submitted for athlete: {}", request.athlete());
        AnalysisJob job = analysisService.submit(request.athlete());

        // Build the polling URL: /api/v1/analysis/{id}
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(job.getId())
                .toUri();

        return ResponseEntity.accepted()
                .location(location)
                .body(AnalysisResponse.from(job));
    }

    /**
     * Polls the current status and result of an existing analysis job.
     * Returns 200 OK in both PENDING and COMPLETED states.
     * When PENDING, metrics will be null. When COMPLETED, metrics contain the full result.
     *
     * @param id the UUID of the analysis job
     * @return {@code 200 OK} with current job state, or {@code 404} if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<AnalysisResponse> getStatus(@PathVariable String id) {
        log.info("Polling analysis job status for id: {}", id);
        AnalysisJob job = analysisService.getById(id);
        return ResponseEntity.ok(AnalysisResponse.from(job));
    }

    // ── Request / Response DTOs ──────────────────────────────────────────────

    /**
     * Incoming request body for job submission.
     * athlete name is required and capped at 100 characters to prevent oversized input (OWASP A08).
     *
     * @param athlete name of the athlete to analyse; must not be blank, max 100 chars
     */
    record AnalysisRequest(
            @NotBlank(message = "athlete name must not be blank")
            @Size(max = 100, message = "athlete name must not exceed 100 characters")
            String athlete
    ) {}

    /**
     * Outgoing response body representing the current state of a job.
     *
     * @param id          unique job identifier
     * @param athlete     name of the athlete
     * @param status      current job status ({@code PENDING} or {@code COMPLETED})
     * @param submittedAt UTC timestamp of job submission
     * @param metrics     computed metrics — {@code null} while PENDING
     */
    record AnalysisResponse(
            String id,
            String athlete,
            String status,
            Instant submittedAt,
            Metrics metrics
    ) {
        /**
         * Maps an {@link AnalysisJob} to its response representation.
         * Metrics are only included when the job is COMPLETED.
         */
        static AnalysisResponse from(AnalysisJob job) {
            return new AnalysisResponse(
                    job.getId(),
                    job.getAthlete(),
                    job.getStatus().name(),
                    job.getSubmittedAt(),
                    job.getStatus() == AnalysisStatus.COMPLETED
                            ? Metrics.from(job.getResult())
                            : null
            );
        }
    }

    /**
     * Biomechanical metrics returned when analysis is complete.
     *
     * @param foot_contact  time (s) of foot ground contact
     * @param foot_off      time (s) of foot leaving the ground
     * @param turning_point time (s) of athlete turning point
     */
    record Metrics(
            double foot_contact,
            double foot_off,
            double turning_point
    ) {
        /** Maps an {@link AnalysisResult} to the API-facing Metrics record. */
        static Metrics from(AnalysisResult result) {
            return new Metrics(result.footContact(), result.footOff(), result.turningPoint());
        }
    }
}
