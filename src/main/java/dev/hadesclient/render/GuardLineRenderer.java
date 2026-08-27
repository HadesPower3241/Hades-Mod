package dev.hadesclient.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public final class GuardLineRenderer {
    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(ctx -> {
            try { draw(ctx); } catch (Throwable ignored) {}
        });
    }

    private static void draw(WorldRenderContext ctx) {
        if (!GuardHighlighter.isEnabled() || !GuardHighlighter.isLineEnabled()) return;
        var mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null || mc.world == null) return;
        Vec3d cam = ctx.camera().getPos();
        double range = GuardHighlighter.getRange();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.lineWidth(2f);

        var tess = Tessellator.getInstance();
        var buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        boolean any = false;

        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player || !GuardHighlighter.isGuardEntity(e)) continue;
            double d = e.distanceTo(mc.player);
            if (d > range) continue;
            float t = 1f - (float)(d / range);
            buf.vertex((float)(mc.player.getX()-cam.x), (float)(mc.player.getY()+.5-cam.y), (float)(mc.player.getZ()-cam.z)).color((int)(255*t), (int)(255*(1-t)), 40, 180);
            buf.vertex((float)(e.getX()-cam.x), (float)(e.getY()+e.getHeight()/2-cam.y), (float)(e.getZ()-cam.z)).color((int)(255*t), (int)(255*(1-t)), 40, 180);
            any = true;
        }

        if (any) BufferRenderer.drawWithGlobalProgram(buf.end());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.lineWidth(1f);
    }
}
