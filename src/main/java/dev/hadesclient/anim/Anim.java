package dev.hadesclient.anim;

/**
 * Animated float that eases toward a target. Supports both the original
 * linear interpolation mode and new easing curve mode.
 * Frame-rate independent via elapsed seconds.
 */
public final class Anim {

    private float value;
    private float target;
    private float speed = 14f;
    private Easing easing = null; // null = use original lerp, non-null = use easing curve
    private float progress = 1f;  // 0-1 progress through easing curve
    private float from;           // start value for easing

    public Anim(float initial) {
        this.value = initial;
        this.target = initial;
        this.from = initial;
    }

    public Anim speed(float speed) { this.speed = speed; return this; }
    public Anim easing(Easing easing) { this.easing = easing; return this; }

    public float get() { return value; }
    public float target() { return target; }

    public void to(float target) {
        if (this.target != target) {
            this.from = this.value;
            this.target = target;
            this.progress = 0f;
        }
    }

    public void snap(float value) {
        this.value = value;
        this.target = value;
        this.from = value;
        this.progress = 1f;
    }

    public void update(float dt) {
        if (easing != null && progress < 1f) {
            // Easing curve mode
            progress = Math.min(1f, progress + dt * speed * 0.5f);
            value = easing.lerp(from, target, progress);
        } else {
            // Original smooth lerp mode
            float diff = target - value;
            if (Math.abs(diff) < 0.001f) {
                value = target;
            } else {
                value += diff * Math.min(1f, dt * speed);
            }
        }
    }
}
