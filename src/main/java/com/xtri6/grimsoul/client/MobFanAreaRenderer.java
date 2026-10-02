package com.xtri6.grimsoul.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xtri6.grimsoul.blockentity.MobFanBlockEntity;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;

/** Draws a light-blue outline of the Mob Fan's push area when "Area" is on in its GUI. */
public class MobFanAreaRenderer implements BlockEntityRenderer<MobFanBlockEntity> {
    public MobFanAreaRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(MobFanBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        renderBlades(be, partialTick, poseStack, buffers, packedLight);
        if (!be.isShowingArea()) {
            return;
        }
        BlockPos pos = be.getBlockPos();
        AABB area = be.getPushArea(be.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING))
                .move(-pos.getX(), -pos.getY(), -pos.getZ()).deflate(0.01);
        LevelRenderer.renderLineBox(poseStack, buffers.getBuffer(RenderType.lines()), area, 0.55F, 0.8F, 1.0F, 1.0F);
    }

    /** The blades spin up to full speed while the fan runs and coast down when it stops. */
    private void renderBlades(MobFanBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        if (be.getLevel() == null) {
            return;
        }
        boolean active = be.getBlockState().getValue(com.xtri6.grimsoul.block.MachineBlock.ACTIVE);
        float angle = be.bladeAngle(partialTick, active);
        poseStack.pushPose();
        Fx.face(poseStack, be.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING));
        poseStack.translate(0.5F, 0.5F, 0.0F);
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(angle));
        poseStack.translate(-0.5F, -0.5F, -0.2F);
        Fx.part(poseStack, buffers, Fx.FAN_BLADES, RenderType.cutout(), light);
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(MobFanBlockEntity be) {
        return be.isShowingArea();
    }

    @Override
    public AABB getRenderBoundingBox(MobFanBlockEntity be) {
        return be.isShowingArea() ? AABB.INFINITE : new AABB(be.getBlockPos()).inflate(0.5);
    }
}
