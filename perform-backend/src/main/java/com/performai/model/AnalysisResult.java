/**
 * Perform AI Backend — Analysis Result Model
 *
 * Immutable value object holding the computed biomechanical metrics
 * produced after an analysis job completes.
 *
 * @author Md Ehteshamul Haque Tamvir
 * @email  mtamvir@gmail.com
 */
package com.performai.model;

/**
 * Immutable record representing the output metrics of a completed analysis.
 * All values are in seconds and represent key biomechanical events
 * detected during athlete motion analysis:
 *
 * @param footContact  time (s) at which the foot makes ground contact
 * @param footOff      time (s) at which the foot leaves the ground
 * @param turningPoint time (s) at which the athlete reaches the turning point
 */
public record AnalysisResult(
        double footContact,
        double footOff,
        double turningPoint
) {}
