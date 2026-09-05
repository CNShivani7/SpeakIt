package com.speakit.app;

/** Small offline scheduler; intervals in minutes. Not the Anki/FSRS algorithm. */
public final class ReviewSchedule {
    private ReviewSchedule() {}
    public static long interval(long previousMinutes, int rating) {
        if (rating == 0) return 1;
        if (rating == 1) return Math.min(525600L, Math.max(10L, previousMinutes * 6 / 5));
        if (rating == 2) return Math.min(525600L, Math.max(1440L, previousMinutes * 2));
        if (rating == 3) return Math.min(525600L, Math.max(4320L, previousMinutes * 3));
        throw new IllegalArgumentException("Unknown review rating");
    }
}
