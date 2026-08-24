package dev.hadesclient.cape.render;

/**
 * Tunable parameters for {@link ClothSimulator}. Immutable value type;
 * change the physics feel by swapping the whole preset, not by mutating one.
 */
public record CapePhysicsPreset(
        int segments,
        float gravityMultiplier,
        float windResponsiveness,
        float stiffness,
        float damping) {

    /** Sensible defaults: 8 segments, calm sway, modest response to motion. */
    public static CapePhysicsPreset defaults() {
        return new CapePhysicsPreset(8, 1.0f, 0.6f, 0.4f, 0.985f);
    }

    /** Heavier, slower cape. */
    public static CapePhysicsPreset heavy() {
        return new CapePhysicsPreset(10, 1.4f, 0.35f, 0.5f, 0.975f);
    }

    /** Light silky cape. */
    public static CapePhysicsPreset silk() {
        return new CapePhysicsPreset(8, 0.7f, 0.9f, 0.3f, 0.99f);
    }
}
