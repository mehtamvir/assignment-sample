/**
 * Perform AI Backend — Analysis Status Enum
 *
 * Represents the lifecycle states of an analysis job.
 *
 * @author Md Ehteshamul Haque Tamvir
 * @email  mtamvir@gmail.com
 */
package com.performai.model;

/**
 * Lifecycle states of an {@link AnalysisJob}.
 *   PENDING   — job has been submitted and is awaiting or undergoing processing
 *   COMPLETED — processing finished and results are available
 */
public enum AnalysisStatus {
    PENDING,
    COMPLETED
}
