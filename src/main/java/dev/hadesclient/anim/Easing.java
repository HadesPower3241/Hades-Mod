package dev.hadesclient.anim;

/**
 * Easing functions for smooth GUI animations. Each function takes a
 * progress value t in [0,1] and returns the eased value in [0,1].
 * Written from standard easing math — sine, quad, cubic, etc.
 */
public enum Easing {
    LINEAR(t -> t),
    SINE_IN_OUT(t -> 0.5 - Math.cos(t * Math.PI) / 2.0),
    SINE_OUT(t -> Math.sin(t * Math.PI / 2.0)),
    QUAD_OUT(t -> 1.0 - (1.0 - t) * (1.0 - t)),
    QUAD_IN_OUT(t -> t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2),
    CUBIC_OUT(t -> 1.0 - Math.pow(1.0 - t, 3)),
    CUBIC_IN_OUT(t -> t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2),
    EXPO_OUT(t -> t >= 1.0 ? 1.0 : 1.0 - Math.pow(2, -10 * t)),
    BACK_OUT(t -> {
        double c = 1.70158;
        double x = t - 1;
        return 1 + (c + 1) * x * x * x + c * x * x;
    });

    private final java.util.function.DoubleUnaryOperator fn;

    Easing(java.util.function.DoubleUnaryOperator fn) { this.fn = fn; }

    /** Apply easing to progress t (0 to 1). Returns eased value (0 to 1). */
    public double apply(double t) {
        return fn.applyAsDouble(Math.max(0, Math.min(1, t)));
    }

    /** Interpolate between start and end with easing. */
    public double lerp(double start, double end, double t) {
        return start + (end - start) * apply(t);
    }

    /** Interpolate float version. */
    public float lerp(float start, float end, float t) {
        return (float) lerp((double) start, (double) end, (double) t);
    }
}
