package com.example.backend.modules.production.productionstock.util;

import java.time.Duration;
import java.time.LocalTime;

public class ProductionTimeCalculator {

    private static final LocalTime BREAK_START = LocalTime.of(12, 0);
    private static final LocalTime BREAK_END = LocalTime.of(12, 20);

    public record ProductionMetrics(long rawTime, long effectiveTime, double performance) {
    }

    public static ProductionMetrics calculate(LocalTime startTime, LocalTime endTime, double quantity) {
        validateTime(startTime, endTime);
        long rawDuration = calculateRawDuration(startTime, endTime);
        long effectiveDuration = calculateEffectiveDuration(startTime, endTime);
        double performance = calculatePerformance(quantity, effectiveDuration);

        return new ProductionMetrics(rawDuration, effectiveDuration, performance);
    }

    public static void validateTime(LocalTime startTime, LocalTime endTime) {
        if (endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("End Time cannot be before Start Time.");
        }
    }

    public static long calculateRawDuration(LocalTime startTime, LocalTime endTime) {
        return Duration.between(startTime, endTime).toMinutes();
    }

    public static long calculateEffectiveDuration(LocalTime startTime, LocalTime endTime) {
        long rawDuration = calculateRawDuration(startTime, endTime);
        long breakOverlap = calculateBreakOverlap(startTime, endTime);
        long effectiveDuration = rawDuration - breakOverlap;

        return Math.max(0, effectiveDuration);
    }

    public static double calculatePerformance(double quantity, long effectiveDurationMinutes) {
        if (effectiveDurationMinutes > 0) {
            return quantity / effectiveDurationMinutes;
        }
        return 0.0;
    }

    private static long calculateBreakOverlap(LocalTime start, LocalTime end) {
        // If production is completely before or completely after the break, no overlap.
        if (end.isBefore(BREAK_START) || end.equals(BREAK_START) || start.isAfter(BREAK_END)
                || start.equals(BREAK_END)) {
            return 0;
        }

        // Calculate overlap
        LocalTime overlapStart = start.isAfter(BREAK_START) ? start : BREAK_START;
        LocalTime overlapEnd = end.isBefore(BREAK_END) ? end : BREAK_END;

        if (overlapStart.isBefore(overlapEnd)) {
            return Duration.between(overlapStart, overlapEnd).toMinutes();
        }

        return 0;
    }
}
