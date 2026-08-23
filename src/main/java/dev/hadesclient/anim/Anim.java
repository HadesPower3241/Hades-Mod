package dev.hadesclient.anim;

/**
 * A float that eases toward a target instead of snapping. Frame-rate
 * independent: the step comes from elapsed seconds, so a 30fps machine and a
 * 240fps machine see the same motion over the same wall-clock time.
 */
public final class Anim {

    private float value;
    private float target;
    private float speed = 14f;

    public Anim(float initial) {
        this.value = initial;
        this.target = initial;
    }

    /** Higher is snappier. 14 suits hover states; 20+ feels instant. */
    public Anim speed(float speed) {
        this.speed = speed;
        return this;
    }

    public float get() { return value; }

    public float target() { return target; }

    public void to(float target) { this.target = target; }

    public void snap(float value) {
        this.value = value;
        this.target = value;
    }

    public void update(float dt) {
        if (dt <= 0f) return;
        float t = 1f - (float) Math.exp(-speed * dt);
        value += (target - value) * t;
        if (Math.abs(target - value) < 0.0005f) value = target;
    }
}
