package com.xtri6.grimsoul.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xtri6.grimsoul.block.EnergyCellBlock;
import com.xtri6.grimsoul.block.MachineBlock;
import com.xtri6.grimsoul.blockentity.EnergyCellBlockEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** Reaper Energy Cell: electricity jumps between the terminals while power flows; a halo shows the charge. */
public class EnergyCellRenderer implements BlockEntityRenderer<EnergyCellBlockEntity> {
    public EnergyCellRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(EnergyCellBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Level level = be.getLevel();
        if (level == null) {
            return;
        }
        float t = level.getGameTime() + partialTick;
        int fill = be.getBlockState().getValue(EnergyCellBlock.LEVEL);
        VertexConsumer glow = buffers.getBuffer(RenderType.lightning());
        if (be.getBlockState().getValue(MachineBlock.ACTIVE)) {
            long seed = (long) t;
            Fx.arc(glow, poseStack.last(), 4.5F / 16, 16.2F / 16, 0.5F, 11.5F / 16, 16.2F / 16, 0.5F, seed, 1.0F, 0.85F, 0.3F, 0.8F);
            Fx.arc(glow, poseStack.last(), 4.5F / 16, 16.2F / 16, 0.5F, 11.5F / 16, 16.2F / 16, 0.5F, seed * 31 + 7, 0.6F, 0.8F, 1.0F, 0.5F);
        }
        if (fill > 0) {
            float pulse = 0.6F + 0.4F * (float) Math.sin(t * 0.1F);
            float y = 1.5F / 16 + fill * (11.5F / 16) / 8.0F;
            Fx.ring(glow, poseStack.last(), 0.5F, y, 0.5F, 0.47F, 0.02F, 4, (float) (Math.PI / 4), 1.0F, 0.85F, 0.3F, 0.4F * pulse, false);
        }
    }

    @Override
    public AABB getRenderBoundingBox(EnergyCellBlockEntity be) {
        return new AABB(be.getBlockPos()).expandTowards(0, 0.5, 0);
    }
}
