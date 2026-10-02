package com.xtri6.grimsoul.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.xtri6.grimsoul.block.MachineBlock;
import com.xtri6.grimsoul.blockentity.EssenceSpawnerBlockEntity;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Draws a small spinning copy of the captured mob inside the spawner cage (faster while it is
 * running), and a gold outline of the spawn area when "Area" is on.
 */
public class EssenceSpawnerRenderer implements BlockEntityRenderer<EssenceSpawnerBlockEntity> {
    private final EntityRenderDispatcher entityRenderer;

    public EssenceSpawnerRenderer(BlockEntityRendererProvider.Context context) {
        this.entityRenderer = context.getEntityRenderer();
    }

    @Override
    public void render(EssenceSpawnerBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
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
            poseStack.translate(0.5F, 0.4F + (active ? (float) Math.sin(time * 0.1F) * 0.05F : 0.0F), 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(spin));
            poseStack.translate(0.0F, -0.2F, 0.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(-30.0F));
            poseStack.scale(scale, scale, scale);
            entityRenderer.render(entity, 0.0, 0.0, 0.0, 0.0F, partialTick, poseStack, buffers, packedLight);
            poseStack.popPose();
        }
        boolean running = be.getBlockState().getValue(MachineBlock.ACTIVE);
        if (running) {
            float t = level.getGameTime() + partialTick;
            float charge = (t * 0.015F) % 1.0F;
            com.mojang.blaze3d.vertex.VertexConsumer glow = buffers.getBuffer(RenderType.lightning());
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.5F, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(t * 3.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(25.0F));
            Fx.ring(glow, poseStack.last(), 0.0F, 0.0F, 0.0F, 0.42F, 0.025F, 32, t * 0.1F, 0.55F, 0.35F, 1.0F, 0.55F, true);
            poseStack.mulPose(Axis.XP.rotationDegrees(-50.0F));
            Fx.ring(glow, poseStack.last(), 0.0F, 0.0F, 0.0F, 0.38F, 0.02F, 32, -t * 0.12F, 0.3F, 0.9F, 0.9F, 0.4F, true);
            poseStack.popPose();
            // A glow band climbs the cage over and over while it runs.
            float h = 3.0F / 16.0F + charge * (10.0F / 16.0F);
            Fx.ring(glow, poseStack.last(), 0.5F, h, 0.5F, 0.44F, 0.03F, 4, (float) (Math.PI / 4), 0.8F, 0.5F, 1.0F, 0.6F, false);
        }
        if (be.isShowingArea()) {
            BlockPos pos = be.getBlockPos();
            AABB area = be.getClientSpawnArea().move(-pos.getX(), -pos.getY(), -pos.getZ()).deflate(0.01);
            LevelRenderer.renderLineBox(poseStack, buffers.getBuffer(RenderType.lines()), area, 1.0F, 0.8F, 0.3F, 1.0F);
        }
    }

    @Override
    public boolean shouldRenderOffScreen(EssenceSpawnerBlockEntity be) {
        return be.isShowingArea();
    }

    @Override
    public AABB getRenderBoundingBox(EssenceSpawnerBlockEntity be) {
        return be.isShowingArea() ? AABB.INFINITE : new AABB(be.getBlockPos()).inflate(0.5);
    }
}
