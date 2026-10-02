package com.xtri6.grimsoul.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.xtri6.grimsoul.block.MachineBlock;
import com.xtri6.grimsoul.blockentity.ReaperGeneratorBlockEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** Reaper Generator: an energy ring spins around the coil and sparks arc to the four posts while it burns. */
public class GeneratorRenderer implements BlockEntityRenderer<ReaperGeneratorBlockEntity> {
    public GeneratorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ReaperGeneratorBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Level level = be.getLevel();
        if (level == null || !be.getBlockState().getValue(MachineBlock.ACTIVE)) {
            return;
        }
        float t = level.getGameTime() + partialTick;
        float bob = (float) Math.sin(t * 0.15F) * 0.06F;
        poseStack.pushPose();
        poseStack.translate(0.5F, 1.05F + bob, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(t * 9.0F));
        poseStack.scale(0.55F, 0.55F, 0.55F);
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        Fx.part(poseStack, buffers, Fx.GENERATOR_RING, RenderType.cutout(), Fx.fullBright());
        poseStack.popPose();

        VertexConsumer glow = buffers.getBuffer(RenderType.lightning());
        Fx.ring(glow, poseStack.last(), 0.5F, 1.05F + bob, 0.5F, 0.2F, 0.05F, 20, t * 0.2F, 0.65F, 0.35F, 1.0F, 0.5F, true);
        // Arcs from the coil to a post, changing every few ticks.
        long seed = (long) (t / 3);
        float[][] posts = {{2.75F, 2.75F}, {13.25F, 2.75F}, {2.75F, 13.25F}, {13.25F, 13.25F}};
        float[] post = posts[(int) (seed % 4)];
        Fx.arc(glow, poseStack.last(), 0.5F, 1.1F, 0.5F, post[0] / 16.0F, 18.0F / 16.0F, post[1] / 16.0F, seed, 0.8F, 0.6F, 1.0F, 0.85F);
    }

    @Override
    public AABB getRenderBoundingBox(ReaperGeneratorBlockEntity be) {
        return new AABB(be.getBlockPos()).expandTowards(0, 1, 0).inflate(0.25);
    }
}
