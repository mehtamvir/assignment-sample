/**
 * Perform AI Backend — Job Not Found Exception
 *
 * Thrown when a requested analysis job ID does not exist in the store.
 * Mapped to HTTP {@code 404 Not Found} by {@link GlobalExceptionHandler}.
 *
 * @author Md Ehteshamul Haque Tamvir
 * @email  mtamvir@gmail.com
 */
package com.performai.exception;

/**
 * Signals that no {@code AnalysisJob} exists for the given ID.
 * Extends {@link RuntimeException} so it propagates without
 * requiring explicit checked-exception handling at call sites.
 */
public class JobNotFoundException extends RuntimeException {

    /**
     * Constructs the exception with a descriptive message including the missing job ID.
     *
     * @param jobId the ID that was not found
     */
    public JobNotFoundException(String jobId) {
        super("Analysis job not found: " + jobId);
    }
}
