package com.xtri6.grimsoul.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.xtri6.grimsoul.GrimSoul;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Shared drawing helpers for the machines' animated parts. */
public final class Fx {
    public static final ModelResourceLocation FAN_BLADES = part("fan_blades");
    public static final ModelResourceLocation REAPER_CRYSTAL = part("reaper_crystal");
    public static final ModelResourceLocation GENERATOR_RING = part("generator_ring");

    private Fx() {}

    private static ModelResourceLocation part(String name) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(GrimSoul.MODID, "block/part/" + name));
    }

    public static void registerModels(ModelEvent.RegisterAdditional event) {
        event.register(FAN_BLADES);
        event.register(REAPER_CRYSTAL);
        event.register(GENERATOR_RING);
    }

    /** Draws one of the standalone part models with the current pose. */
    public static void part(PoseStack poseStack, MultiBufferSource buffers, ModelResourceLocation id, RenderType type, int light) {
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(id);
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(poseStack.last(), buffers.getBuffer(type), null,
                model, 1.0F, 1.0F, 1.0F, light, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, type);
    }

    public static int fullBright() {
        return LightTexture.FULL_BRIGHT;
    }

    /** Turns the pose so model-space "north" points the way the machine faces (around the block center). */
    public static void face(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-(facing.toYRot() + 180.0F)));
        poseStack.translate(-0.5F, -0.5F, -0.5F);
    }

    /** A flat glowing ring (in the XZ plane) centered on (cx, y, cz). Glowing additive color. */
    public static void ring(VertexConsumer c, PoseStack.Pose pose, float cx, float y, float cz, float radius, float width,
                            int segments, float phase, float r, float g, float b, float a, boolean dashed) {
        for (int i = 0; i < segments; i++) {
            if (dashed && i % 2 == 1) {
                continue;
            }
            float a0 = phase + (float) (i * Math.PI * 2 / segments);
            float a1 = phase + (float) ((i + 1) * Math.PI * 2 / segments);
            float ri = radius - width / 2, ro = radius + width / 2;
            float x0i = cx + (float) Math.cos(a0) * ri, z0i = cz + (float) Math.sin(a0) * ri;
            float x0o = cx + (float) Math.cos(a0) * ro, z0o = cz + (float) Math.sin(a0) * ro;
            float x1i = cx + (float) Math.cos(a1) * ri, z1i = cz + (float) Math.sin(a1) * ri;
            float x1o = cx + (float) Math.cos(a1) * ro, z1o = cz + (float) Math.sin(a1) * ro;
            quad(c, pose, x0i, y, z0i, x0o, y, z0o, x1o, y, z1o, x1i, y, z1i, r, g, b, a);
        }
    }

    /** A double-sided glowing quad. */
    public static void quad(VertexConsumer c, PoseStack.Pose pose,
                            float x0, float y0, float z0, float x1, float y1, float z1,
                            float x2, float y2, float z2, float x3, float y3, float z3,
                            float r, float g, float b, float a) {
        c.addVertex(pose, x0, y0, z0).setColor(r, g, b, a);
        c.addVertex(pose, x1, y1, z1).setColor(r, g, b, a);
        c.addVertex(pose, x2, y2, z2).setColor(r, g, b, a);
        c.addVertex(pose, x3, y3, z3).setColor(r, g, b, a);
        c.addVertex(pose, x3, y3, z3).setColor(r, g, b, a);
        c.addVertex(pose, x2, y2, z2).setColor(r, g, b, a);
        c.addVertex(pose, x1, y1, z1).setColor(r, g, b, a);
        c.addVertex(pose, x0, y0, z0).setColor(r, g, b, a);
    }

    /** A glowing cuboid. */
    public static void box(VertexConsumer c, PoseStack.Pose pose, float x0, float y0, float z0, float x1, float y1, float z1,
                           float r, float g, float b, float a) {
        quad(c, pose, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, r, g, b, a);
        quad(c, pose, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, r, g, b, a);
        quad(c, pose, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, r, g, b, a);
        quad(c, pose, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, r, g, b, a);
        quad(c, pose, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, r, g, b, a);
        quad(c, pose, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, r, g, b, a);
    }

    /** A jagged electric arc between two points (a chain of thin glowing boxes). */
    public static void arc(VertexConsumer c, PoseStack.Pose pose, float x0, float y0, float z0, float x1, float y1, float z1,
                           long seed, float r, float g, float b, float a) {
        java.util.Random random = new java.util.Random(seed);
        int steps = 9;
        float px = x0, py = y0, pz = z0;
        for (int i = 1; i <= steps; i++) {
            float t = i / (float) steps;
            float jitter = i == steps ? 0.0F : 0.11F;
            float nx = x0 + (x1 - x0) * t + (random.nextFloat() - 0.5F) * jitter;
            float ny = y0 + (y1 - y0) * t + (float) Math.sin(t * Math.PI) * 0.05F + (random.nextFloat() - 0.5F) * jitter;
            float nz = z0 + (z1 - z0) * t + (random.nextFloat() - 0.5F) * jitter;
            float w = 0.007F;
            box(c, pose, Math.min(px, nx) - w, Math.min(py, ny) - w, Math.min(pz, nz) - w,
                    Math.max(px, nx) + w, Math.max(py, ny) + w, Math.max(pz, nz) + w, r, g, b, a);
            px = nx;
            py = ny;
            pz = nz;
        }
    }
}
