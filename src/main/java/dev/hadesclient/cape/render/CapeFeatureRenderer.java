package dev.hadesclient.cape.render;

import dev.hadesclient.HadesClient;
import dev.hadesclient.cape.LocalCape;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerCapeModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Draws the locally-equipped {@link LocalCape} on the local player, wearing
 * the vanilla {@link PlayerCapeModel} mesh (64×32 texture layout) with its
 * pitch/roll driven by a {@link ClothSimulator} rather than vanilla's canned
 * cape animation.
 *
 * <p>Client-only: no packet is ever sent about the cape, so other players
 * won't see it. That's fine for a personal-cape system — this mirrors what
 * every "OptiFine style" client-side cape does.</p>
 *
 * <p>Registered from {@code HadesClient.onInitializeClient()} via {@link
 * #register()} using Fabric's {@code LivingEntityFeatureRendererRegistrationCallback}.</p>
 */
public final class CapeFeatureRenderer
        extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {

    private final PlayerCapeModel model;
    private final ClothSimulator simulator;
    private long lastStepMillis = -1L;

    public CapeFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context,
                               EntityRendererFactory.Context ctx) {
        super(context);
        this.model = new PlayerCapeModel(ctx.getPart(EntityModelLayers.PLAYER_CAPE));
        this.simulator = new ClothSimulator(CapePhysicsPreset.defaults(), 0.25f);
    }

    /** Hook into Fabric's feature-renderer registration. Call from client init. */
    @SuppressWarnings("unchecked")
    public static void register() {
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, helper, context) -> {
            if (entityType != EntityType.PLAYER) return;
            if (!(entityRenderer instanceof FeatureRendererContext<?, ?> playerRenderer)) return;
            helper.register(new CapeFeatureRenderer(
                    (FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel>) playerRenderer,
                    context));
        });
    }

    @Override
    public void render(MatrixStack matrices, OrderedRenderCommandQueue queue, int light,
                       PlayerEntityRenderState state, float limbAngle, float limbDistance) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity localPlayer = client.player;
        if (localPlayer == null || state.id != localPlayer.getId()) return;

        // An equipped elytra takes over the cape slot visually; don't double up.
        if (localPlayer.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) return;

        LocalCape cape = HadesClient.capes().equipped().orElse(null);
        if (cape == null) return;

        advanceSimulation(localPlayer);
        float restLean = 0.14f;
        float pitch = restLean + simulator.firstSegmentPitchRadians() * 1.35f;
        float roll = simulator.firstSegmentRollRadians() * 0.8f;

        renderMesh(matrices, queue, light, state, cape.texture(), pitch, roll);
    }

    private void renderMesh(MatrixStack matrices, OrderedRenderCommandQueue queue, int light,
                            PlayerEntityRenderState state, Identifier texture,
                            float pitch, float roll) {
        matrices.push();
        matrices.multiply(new Quaternionf().rotateX(pitch).rotateZ(roll));
        queue.submitModel(this.model, state, matrices,
                RenderLayers.entityCutoutNoCull(texture),
                light, OverlayTexture.DEFAULT_UV, state.outlineColor, null);
        matrices.pop();
    }

    private void advanceSimulation(PlayerEntity player) {
        long now = System.currentTimeMillis();
        float deltaSeconds = lastStepMillis < 0 ? 0.05f
                : Math.min(0.1f, (now - lastStepMillis) / 1000f);
        lastStepMillis = now;

        Vector3f pin = new Vector3f(0f, 0f, 0f);
        Vector3f velocity = new Vector3f(
                (float) -player.getVelocity().x,
                (float) -player.getVelocity().y * 0.2f,
                (float) -player.getVelocity().z);
        simulator.step(pin, velocity, deltaSeconds);
    }
}
