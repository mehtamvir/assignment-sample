/**
 * Perform AI Backend — Analysis Job Model
 *
 * Domain model representing a single athlete analysis job,
 * tracking its lifecycle from submission through completion.
 *
 * @author Md Ehteshamul Haque Tamvir
 * @email  mtamvir@gmail.com
 */
package com.performai.model;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents a single analysis job submitted for an athlete.
 *
 * Thread safety:
 *   - status uses {@link AtomicReference} for lock-free state transitions
 *   - result is volatile to ensure visibility across threads
 *     when set before the status flip to COMPLETED
 *
 * State transition: PENDING → COMPLETED (one-way, irreversible)
 */
public class AnalysisJob {

    /** Unique identifier for this job (UUID). */
    private final String id;

    /** Name of the athlete this analysis belongs to. */
    private final String athlete;

    /** Timestamp when the job was submitted. */
    private final Instant submittedAt;

    /** Current lifecycle status of the job. */
    private final AtomicReference<AnalysisStatus> status;

    /**
     * Computed result metrics. Null while PENDING, populated on completion.
     * Declared volatile to guarantee visibility after the async write.
     */
    private volatile AnalysisResult result;

    /**
     * Creates a new job in {@code PENDING} state.
     *
     * @param id      unique job identifier
     * @param athlete name of the athlete being analysed
     */
    public AnalysisJob(String id, String athlete) {
        this.id = id;
        this.athlete = athlete;
        this.submittedAt = Instant.now();
        this.status = new AtomicReference<>(AnalysisStatus.PENDING);
    }

    /**
     * Marks the job as completed and stores the computed metrics.
     * The result is written before the status is flipped to ensure
     * any thread reading {@code COMPLETED} also sees the result.
     *
     * @param result the computed biomechanical metrics
     */
    public void complete(AnalysisResult result) {
        this.result = result;                          // write result first
        this.status.set(AnalysisStatus.COMPLETED);     // then flip status
    }

    public String getId()             { return id; }
    public String getAthlete()        { return athlete; }
    public Instant getSubmittedAt()   { return submittedAt; }
    public AnalysisStatus getStatus() { return status.get(); }

    /**
     * Returns the analysis result, or {@code null} if the job is still PENDING.
     *
     * @return {@link AnalysisResult} when COMPLETED, {@code null} otherwise
     */
    public AnalysisResult getResult() { return result; }
}
