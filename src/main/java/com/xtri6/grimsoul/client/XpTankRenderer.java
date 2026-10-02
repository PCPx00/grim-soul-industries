package com.xtri6.grimsoul.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xtri6.grimsoul.GrimSoul;
import com.xtri6.grimsoul.blockentity.XpTankBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.AABB;

/**
 * Draws the swirling liquid inside the XP Tank, rising and falling with how full it is, and a
 * green outline of the magnet area when "Area" is on in its GUI.
 */
public class XpTankRenderer implements BlockEntityRenderer<XpTankBlockEntity> {
    private static final ResourceLocation LIQUID = ResourceLocation.fromNamespaceAndPath(GrimSoul.MODID, "block/xp_tank_liquid");
    private static final float MIN = 3.0F / 16.0F;
    private static final float MAX = 13.0F / 16.0F;
    private static final float BOTTOM = 2.0F / 16.0F;
    private static final float TOP = 14.0F / 16.0F;

    public XpTankRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(XpTankBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        if (be.getLevel() != null && be.getBlockState().getValue(com.xtri6.grimsoul.block.MachineBlock.ACTIVE)) {
            // Green XP motes orbit the tank while the magnet pulls.
            float t = be.getLevel().getGameTime() + partialTick;
            com.mojang.blaze3d.vertex.VertexConsumer glow = buffers.getBuffer(RenderType.lightning());
            for (int i = 0; i < 6; i++) {
                double a = t * 0.06 + i * Math.PI / 3;
                float x = 0.5F + (float) Math.cos(a) * 0.62F;
                float z = 0.5F + (float) Math.sin(a) * 0.62F;
                float y = 0.3F + ((t * 0.01F + i * 0.17F) % 1.0F) * 0.6F;
                float s = 0.025F + 0.01F * (float) Math.sin(t * 0.3 + i);
                Fx.box(glow, poseStack.last(), x - s, y - s, z - s, x + s, y + s, z + s, 0.5F, 1.0F, 0.3F, 0.8F);
            }
        }
        if (be.isShowingArea()) {
            BlockPos pos = be.getBlockPos();
            AABB area = be.getMagnetArea().move(-pos.getX(), -pos.getY(), -pos.getZ()).deflate(0.01);
            LevelRenderer.renderLineBox(poseStack, buffers.getBuffer(RenderType.lines()), area, 0.4F, 1.0F, 0.55F, 1.0F);
        }
        float fill = be.getFillFraction();
        if (fill <= 0.0F) {
            return;
        }
        float height = BOTTOM + (TOP - BOTTOM) * Math.max(fill, 0.04F);
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(LIQUID);
        VertexConsumer consumer = buffers.getBuffer(Sheets.solidBlockSheet());
        PoseStack.Pose pose = poseStack.last();
        int light = 0xF000F0; // The liquid glows.

        float u0 = sprite.getU0(), u1 = sprite.getU1(), v0 = sprite.getV0(), v1 = sprite.getV1();
        float vTop = v0 + (v1 - v0) * (1.0F - (height - BOTTOM) / (TOP - BOTTOM));
        // Four sides
        quad(consumer, pose, MIN, BOTTOM, MIN, MAX, height, MIN, u0, vTop, u1, v1, light, 0, 0, -1);
        quad(consumer, pose, MAX, BOTTOM, MAX, MIN, height, MAX, u0, vTop, u1, v1, light, 0, 0, 1);
        quad(consumer, pose, MIN, BOTTOM, MAX, MIN, height, MIN, u0, vTop, u1, v1, light, -1, 0, 0);
        quad(consumer, pose, MAX, BOTTOM, MIN, MAX, height, MAX, u0, vTop, u1, v1, light, 1, 0, 0);
        // Top surface
        top(consumer, pose, height, u0, v0, u1, v1, light);
    }

    @Override
    public boolean shouldRenderOffScreen(XpTankBlockEntity be) {
        return be.isShowingArea();
    }

    @Override
    public AABB getRenderBoundingBox(XpTankBlockEntity be) {
        return be.isShowingArea() ? AABB.INFINITE : new AABB(be.getBlockPos()).inflate(0.5);
    }

    /** A vertical quad from (x1,y1,z1) at the bottom-left to (x2,y2,z2) at the top-right. */
    private static void quad(VertexConsumer c, PoseStack.Pose pose, float x1, float y1, float z1, float x2, float y2, float z2,
                             float u0, float v0, float u1, float v1, int light, float nx, float ny, float nz) {
        // Drawn in both windings so the face is solid from every viewing side.
        vertex(c, pose, x1, y1, z1, u0, v1, light, nx, ny, nz);
        vertex(c, pose, x2, y1, z2, u1, v1, light, nx, ny, nz);
        vertex(c, pose, x2, y2, z2, u1, v0, light, nx, ny, nz);
        vertex(c, pose, x1, y2, z1, u0, v0, light, nx, ny, nz);
        vertex(c, pose, x1, y2, z1, u0, v0, light, nx, ny, nz);
        vertex(c, pose, x2, y2, z2, u1, v0, light, nx, ny, nz);
        vertex(c, pose, x2, y1, z2, u1, v1, light, nx, ny, nz);
        vertex(c, pose, x1, y1, z1, u0, v1, light, nx, ny, nz);
    }

    private static void top(VertexConsumer c, PoseStack.Pose pose, float y, float u0, float v0, float u1, float v1, int light) {
        vertex(c, pose, MIN, y, MIN, u0, v0, light, 0, 1, 0);
        vertex(c, pose, MIN, y, MAX, u0, v1, light, 0, 1, 0);
        vertex(c, pose, MAX, y, MAX, u1, v1, light, 0, 1, 0);
        vertex(c, pose, MAX, y, MIN, u1, v0, light, 0, 1, 0);
        vertex(c, pose, MAX, y, MIN, u1, v0, light, 0, 1, 0);
        vertex(c, pose, MAX, y, MAX, u1, v1, light, 0, 1, 0);
        vertex(c, pose, MIN, y, MAX, u0, v1, light, 0, 1, 0);
        vertex(c, pose, MIN, y, MIN, u0, v0, light, 0, 1, 0);
    }

    private static void vertex(VertexConsumer c, PoseStack.Pose pose, float x, float y, float z, float u, float v, int light,
                               float nx, float ny, float nz) {
        c.addVertex(pose, x, y, z)
                .setColor(1.0F, 1.0F, 1.0F, 1.0F)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }
}
