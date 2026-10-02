package com.xtri6.grimsoul.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.xtri6.grimsoul.block.MachineBlock;
import com.xtri6.grimsoul.blockentity.SimulationChamberBlockEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** Draws a small spinning copy of the Data Core's mob inside the chamber's glass (faster while it runs). */
public class SimulationChamberRenderer implements BlockEntityRenderer<SimulationChamberBlockEntity> {
    private final EntityRenderDispatcher entityRenderer;

    public SimulationChamberRenderer(BlockEntityRendererProvider.Context context) {
        this.entityRenderer = context.getEntityRenderer();
    }

    @Override
    public void render(SimulationChamberBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Level level = be.getLevel();
        if (level == null) {
            return;
        }
        Entity entity = be.getDisplayEntity(level);
        if (entity != null) {
            boolean active = be.getBlockState().getValue(MachineBlock.ACTIVE);
            float time = level.getGameTime() + partialTick;
            float spin = time * (active ? 6.0F : 1.5F);
            float scale = 0.53125F;
            float size = Math.max(entity.getBbWidth(), entity.getBbHeight());
            if (size > 1.0F) {
                scale /= size;
            }
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.45F + (active ? (float) Math.sin(time * 0.1F) * 0.05F : 0.0F), 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(spin));
            poseStack.translate(0.0F, -0.2F, 0.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(-30.0F));
            poseStack.scale(scale, scale, scale);
            entityRenderer.render(entity, 0.0, 0.0, 0.0, 0.0F, partialTick, poseStack, buffers, packedLight);
            poseStack.popPose();
        }
        if (be.getBlockState().getValue(MachineBlock.ACTIVE)) {
            float t = level.getGameTime() + partialTick;
            com.mojang.blaze3d.vertex.VertexConsumer glow = buffers.getBuffer(RenderType.lightning());
            // A scan plane sweeps up and down through the hologram.
            float y = 0.22F + (0.5F + 0.5F * (float) Math.sin(t * 0.09F)) * 0.56F;
            float lo = 2.5F / 16.0F, hi = 13.5F / 16.0F;
            Fx.quad(glow, poseStack.last(), lo, y, lo, hi, y, lo, hi, y, hi, lo, y, hi, 0.25F, 0.85F, 1.0F, 0.22F);
            Fx.ring(glow, poseStack.last(), 0.5F, y, 0.5F, 0.4F, 0.015F, 4, (float) (Math.PI / 4), 0.5F, 0.95F, 1.0F, 0.6F, false);
            // Projector beam from the ceiling emitter.
            Fx.box(glow, poseStack.last(), 0.47F, 0.78F, 0.47F, 0.53F, 1.0F, 0.53F, 0.4F, 0.9F, 1.0F, 0.35F);
        }
    }

    @Override
    public AABB getRenderBoundingBox(SimulationChamberBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(0.5);
    }
}
