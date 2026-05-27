/**
 * Perform AI Backend — Analysis Service
 *
 * Core business logic for submitting, storing, and asynchronously
 * processing athlete analysis jobs.
 *
 * @author Md Ehteshamul Haque Tamvir
 * @email  mtamvir@gmail.com
 */
package com.performai.service;

import com.performai.model.AnalysisJob;
import com.performai.model.AnalysisResult;
import com.performai.exception.JobNotFoundException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service responsible for the full lifecycle of an {@link AnalysisJob}.
 * Jobs are stored in an in-memory {@link ConcurrentHashMap} (suitable for
 * single-instance deployments). Replace with a persistent store for production use.
 * Processing is simulated asynchronously using a dedicated thread pool
 * defined in {@code AsyncConfig} under the name {@code "analysisExecutor"}.
 */
@Service
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);

    /**
     * In-memory job store. Key: job ID, Value: job instance.
     * ConcurrentHashMap ensures thread-safe reads and writes without locking.
     */
    private final Map<String, AnalysisJob> jobStore = new ConcurrentHashMap<>();

    /** Used to generate mock biomechanical metric values. */
    private final Random random = new Random();

    /**
     * Creates a new analysis job for the given athlete, persists it,
     * and immediately dispatches it for async processing.
     *
     * @param athlete name of the athlete to analyse
     * @return the newly created {@link AnalysisJob} in {@code PENDING} state
     */
    public AnalysisJob submit(String athlete) {
        AnalysisJob job = new AnalysisJob(UUID.randomUUID().toString(), athlete);
        jobStore.put(job.getId(), job);
        log.info("Job created [id={}] for athlete: {}", job.getId(), athlete);
        processAsync(job);
        return job;
    }

    /**
     * Retrieves an existing job by its ID.
     *
     * @param id the job UUID
     * @return the matching {@link AnalysisJob}
     * @throws JobNotFoundException if no job exists with the given ID
     */
    public AnalysisJob getById(String id) {
        AnalysisJob job = jobStore.get(id);
        if (job == null) throw new JobNotFoundException(id);
        return job;
    }

    /**
     * Simulates asynchronous biomechanical analysis processing.
     * Runs on a thread from the {@code "analysisExecutor"} pool.
     * Sleeps 2 seconds to mimic real processing latency, then generates
     * randomised metric values within realistic biomechanical ranges.
     * In production, replace this with actual ML model inference or pipeline integration.
     *
     * @param job the job to process; mutated in-place via {@link AnalysisJob#complete}
     */
    @Async("analysisExecutor")
    public void processAsync(AnalysisJob job) {
        log.info("Processing started [id={}]", job.getId());
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        job.complete(new AnalysisResult(
                randomBetween(0.20, 0.50),
                randomBetween(0.90, 1.20),
                randomBetween(1.10, 1.40)
        ));
        log.info("Processing completed [id={}]", job.getId());
    }

    /**
     * Returns a random double between {@code min} and {@code max},
     * rounded to 2 decimal places.
     *
     * @param min lower bound (inclusive)
     * @param max upper bound (exclusive)
     * @return rounded random value
     */
    private double randomBetween(double min, double max) {
        return Math.round((min + random.nextDouble() * (max - min)) * 100.0) / 100.0;
    }
}
