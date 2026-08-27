package dev.hadesclient.cape.render;

import org.joml.Vector3f;

/**
 * A one-dimensional Verlet-integrated cloth chain. Point 0 is pinned to the
 * wearer's back; every other point is free, connected to its neighbour by a
 * distance constraint, and pushed each step by gravity plus a caller-supplied
 * wind vector. The resulting first-segment lean drives the cape mesh's pitch
 * and roll.
 *
 * <p>Not a full 2-D cloth grid — this only sways along the vertical chain,
 * which is enough to look alive without a mesh renderer.</p>
 */
public final class ClothSimulator {

    private final Vector3f[] current;
    private final Vector3f[] previous;
    private final float segmentLength;
    private CapePhysicsPreset preset;

    public ClothSimulator(CapePhysicsPreset preset, float segmentLength) {
        this.preset = preset;
        this.segmentLength = segmentLength;
        int points = preset.segments() + 1;
        this.current = new Vector3f[points];
        this.previous = new Vector3f[points];
        for (int i = 0; i < points; i++) {
            current[i] = new Vector3f(0f, -i * segmentLength, 0f);
            previous[i] = new Vector3f(current[i]);
        }
    }

    public void setPreset(CapePhysicsPreset preset) { this.preset = preset; }

    public CapePhysicsPreset preset() { return preset; }

    /**
     * Advance the simulation by one step.
     *
     * @param pin       pin-point position in the simulator's local space
     * @param wind      wind + inverse player motion for this step
     * @param deltaSec  step length, typically ≤ one tick
     */
    public void step(Vector3f pin, Vector3f wind, float deltaSec) {
        current[0].set(pin);
        previous[0].set(pin);

        float dt2 = deltaSec * deltaSec;
        Vector3f gravity = new Vector3f(0f, -9.8f * preset.gravityMultiplier() * dt2, 0f);
        Vector3f wpush = new Vector3f(wind).mul(preset.windResponsiveness() * dt2);

        for (int i = 1; i < current.length; i++) {
            Vector3f velocity = new Vector3f(current[i]).sub(previous[i]).mul(preset.damping());
            Vector3f next = new Vector3f(current[i]).add(velocity).add(gravity).add(wpush);
            previous[i].set(current[i]);
            current[i].set(next);
        }

        int iterations = 1 + Math.round(preset.stiffness() * 4f);
        for (int iter = 0; iter < iterations; iter++) {
            for (int i = 0; i < current.length - 1; i++) satisfy(i, i + 1);
        }
    }

    private void satisfy(int a, int b) {
        Vector3f delta = new Vector3f(current[b]).sub(current[a]);
        float distance = delta.length();
        if (distance < 1e-5f) return;
        float diff = (distance - segmentLength) / distance;
        Vector3f correction = new Vector3f(delta).mul(0.5f * diff);
        if (a > 0) {
            current[a].add(correction);
        } else {
            // Pinned end doesn't move; push the free point twice as hard.
            correction.mul(2f);
        }
        current[b].sub(correction);
    }

    /**
     * Angle (radians) between rest pose (straight down) and the direction from
     * point 0 to point 1 — the single number the mesh renderer needs to lean
     * the cape.
     */
    public float firstSegmentPitchRadians() {
        Vector3f direction = new Vector3f(current[1]).sub(current[0]);
        if (direction.lengthSquared() < 1e-6f) return 0f;
        direction.normalize();
        return (float) Math.atan2(direction.z, -direction.y);
    }

    public float firstSegmentRollRadians() {
        Vector3f direction = new Vector3f(current[1]).sub(current[0]);
        if (direction.lengthSquared() < 1e-6f) return 0f;
        direction.normalize();
        return (float) Math.atan2(direction.x, -direction.y);
    }
}
