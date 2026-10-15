package com.xtri6.grimsoul.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.xtri6.grimsoul.blockentity.SoulForgeBlockEntity;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * The assembled Soul Forge's core: a skull floats in the shaft above the forge, with two totems and two chains
 * circling it. Everything turns and bobs, and it all moves faster while the forge is working.
 */
public class SoulForgeRenderer implements BlockEntityRenderer<SoulForgeBlockEntity> {
    private static final ItemStack SKULL = new ItemStack(Items.SKELETON_SKULL);
    private static final ItemStack TOTEM = new ItemStack(Items.TOTEM_OF_UNDYING);
    private static final ItemStack CHAIN = new ItemStack(Items.CHAIN);
    /** Height of the skull's centre, in the shaft above the forge. */
    private static final double CORE_Y = 1.6;

    private final ItemRenderer items;

    public SoulForgeRenderer(BlockEntityRendererProvider.Context context) {
        this.items = context.getItemRenderer();
    }

    @Override
    public void render(SoulForgeBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        Level level = be.getLevel();
        if (level == null || !be.isFormed()) {
            return;
        }
        float t = AnimClock.time(partialTick);
        float speed = be.isForging() ? 2.5F : 1.0F;
        int bright = LightTexture.FULL_BRIGHT;

        // The skull: bobs up and down and turns slowly.
        poseStack.pushPose();
        poseStack.translate(0.5, CORE_Y + 0.08 * Math.sin(t * 0.07F * speed), 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(t * 2.0F * speed));
        poseStack.scale(0.6F, 0.6F, 0.6F);
        items.renderStatic(SKULL, ItemDisplayContext.FIXED, bright, packedOverlay, poseStack, buffers, level, 0);
        poseStack.popPose();

        // Two totems circle the skull, clockwise.
        for (int k = 0; k < 2; k++) {
            float angle = t * 0.05F * speed + k * (float) Math.PI;
            orbit(poseStack, buffers, level, TOTEM, angle, 0.34, 0.35F, bright, packedOverlay, t, k, 0.06F);
        }
        // Two chains circle the other way, a little lower and tighter.
        for (int k = 0; k < 2; k++) {
            float angle = -t * 0.035F * speed + k * (float) Math.PI + 0.5F;
            orbit(poseStack, buffers, level, CHAIN, angle, 0.22, 0.42F, bright, packedOverlay, t, k, -0.1F);
        }
    }

    private void orbit(PoseStack poseStack, MultiBufferSource buffers, Level level, ItemStack stack, float angle,
                       double radius, float scale, int bright, int overlay, float t, int k, float tilt) {
        double x = 0.5 + radius * Math.cos(angle);
        double z = 0.5 + radius * Math.sin(angle);
        double y = CORE_Y - 0.2 + 0.05 * Math.sin(t * 0.1F + k * 2.0F);
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.YP.rotationDegrees(-angle * 57.29578F + 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(tilt * 57.29578F + t * 3.0F));
        poseStack.scale(scale, scale, scale);
        items.renderStatic(stack, ItemDisplayContext.FIXED, bright, overlay, poseStack, buffers, level, k);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(SoulForgeBlockEntity be) {
        return new AABB(be.getBlockPos()).expandTowards(0, 2, 0);
    }
}
