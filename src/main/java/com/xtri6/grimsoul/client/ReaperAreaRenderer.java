package com.xtri6.grimsoul.client;

import java.util.Iterator;
import java.util.Map;

import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xtri6.grimsoul.blockentity.ReaperBlockEntity;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Reaper Block visuals:
 * - purple laser beams from the emitter to every mob it hits (fading over a few ticks)
 * - when "Show area" is on: a purple outline of the work area (where it attacks and collects)
 */
public class ReaperAreaRenderer implements BlockEntityRenderer<ReaperBlockEntity> {
    private static final float OUTER_WIDTH = 0.09F;
    private static final float CORE_WIDTH = 0.035F;

    public ReaperAreaRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ReaperBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        BlockPos pos = be.getBlockPos();
        renderBeams(be, partialTick, poseStack, buffers, pos);
        renderCrystal(be, partialTick, poseStack, buffers);

        if (be.isShowingArea()) {
            VertexConsumer lines = buffers.getBuffer(RenderType.lines());
            AABB area = be.getCollectionArea().move(-pos.getX(), -pos.getY(), -pos.getZ()).deflate(0.01);
            LevelRenderer.renderLineBox(poseStack, lines, area, 0.75F, 0.3F, 1.0F, 1.0F);
        }
    }

    /** A soul crystal floats and spins above the block; it spins fast and glows a halo while running. */
    private void renderCrystal(ReaperBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource buffers) {
        Level level = be.getLevel();
        if (level == null) {
            return;
        }
        boolean active = be.getBlockState().getValue(com.xtri6.grimsoul.block.MachineBlock.ACTIVE);
        float time = level.getGameTime() + partialTick;
        float bob = (float) Math.sin(time * 0.08F) * 0.04F;
        poseStack.pushPose();
        poseStack.translate(0.5F, 1.18F + bob, 0.5F);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(time * (active ? 4.0F : 0.8F)));
        poseStack.scale(0.42F, 0.42F, 0.42F);
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        Fx.part(poseStack, buffers, Fx.REAPER_CRYSTAL, RenderType.cutout(), Fx.fullBright());
        poseStack.popPose();
        if (active) {
            VertexConsumer glow = buffers.getBuffer(RenderType.lightning());
            float pulse = 0.5F + 0.5F * (float) Math.sin(time * 0.25F);
            Fx.ring(glow, poseStack.last(), 0.5F, 1.02F, 0.5F, 0.26F + pulse * 0.04F, 0.035F, 24, time * 0.05F, 0.6F, 0.3F, 1.0F, 0.55F, true);
            Fx.ring(glow, poseStack.last(), 0.5F, 1.18F + bob, 0.5F, 0.34F, 0.02F, 24, -time * 0.08F, 0.85F, 0.6F, 1.0F, 0.35F * pulse, true);
        }
    }

    private void renderBeams(ReaperBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource buffers, BlockPos pos) {
        Level level = be.getLevel();
        Map<Integer, Long> beams = be.getBeamTargets();
        if (level == null || beams.isEmpty()) {
            return;
        }
        Direction facing = be.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        // The emitter nozzle sticks out of the front face.
        Vec3 start = new Vec3(0.5 + facing.getStepX() * 0.62, 0.5, 0.5 + facing.getStepZ() * 0.62);
        long now = level.getGameTime();
        VertexConsumer consumer = buffers.getBuffer(RenderType.lightning());
        PoseStack.Pose pose = poseStack.last();

        Iterator<Map.Entry<Integer, Long>> it = beams.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Long> entry = it.next();
            float age = (now - entry.getValue()) + partialTick;
            Entity target = level.getEntity(entry.getKey());
            if (age > ReaperBlockEntity.BEAM_TICKS || target == null) {
                it.remove();
                continue;
            }
            float fade = 1.0F - age / ReaperBlockEntity.BEAM_TICKS;
            Vec3 end = target.getPosition(partialTick)
                    .add(0.0, target.getBbHeight() * 0.5, 0.0)
                    .subtract(pos.getX(), pos.getY(), pos.getZ());
            drawBeam(consumer, pose, start, end, OUTER_WIDTH, 0.55F, 0.2F, 1.0F, 0.45F * fade);
            drawBeam(consumer, pose, start, end, CORE_WIDTH, 0.95F, 0.8F, 1.0F, 0.9F * fade);
        }
    }

    /** A beam made of two crossed flat strips so it looks solid from every angle. */
    private static void drawBeam(VertexConsumer consumer, PoseStack.Pose pose, Vec3 start, Vec3 end,
                                 float width, float r, float g, float b, float a) {
        Vector3f dir = new Vector3f((float) (end.x - start.x), (float) (end.y - start.y), (float) (end.z - start.z));
        if (dir.lengthSquared() < 1.0E-4F) {
            return;
        }
        dir.normalize();
        Vector3f side1 = new Vector3f(dir).cross(0.0F, 1.0F, 0.0F);
        if (side1.lengthSquared() < 1.0E-4F) {
            side1 = new Vector3f(dir).cross(1.0F, 0.0F, 0.0F);
        }
        side1.normalize().mul(width);
        Vector3f side2 = new Vector3f(dir).cross(side1).normalize().mul(width);
        strip(consumer, pose, start, end, side1, r, g, b, a);
        strip(consumer, pose, start, end, side2, r, g, b, a);
    }

    private static void strip(VertexConsumer consumer, PoseStack.Pose pose, Vec3 start, Vec3 end, Vector3f side,
                              float r, float g, float b, float a) {
        float sx = (float) start.x, sy = (float) start.y, sz = (float) start.z;
        float ex = (float) end.x, ey = (float) end.y, ez = (float) end.z;
        // Front face
        consumer.addVertex(pose, sx - side.x, sy - side.y, sz - side.z).setColor(r, g, b, a);
        consumer.addVertex(pose, sx + side.x, sy + side.y, sz + side.z).setColor(r, g, b, a);
        consumer.addVertex(pose, ex + side.x, ey + side.y, ez + side.z).setColor(r, g, b, a);
        consumer.addVertex(pose, ex - side.x, ey - side.y, ez - side.z).setColor(r, g, b, a);
        // Back face (same quad, reversed) so it is visible from both sides
        consumer.addVertex(pose, ex - side.x, ey - side.y, ez - side.z).setColor(r, g, b, a);
        consumer.addVertex(pose, ex + side.x, ey + side.y, ez + side.z).setColor(r, g, b, a);
        consumer.addVertex(pose, sx + side.x, sy + side.y, sz + side.z).setColor(r, g, b, a);
        consumer.addVertex(pose, sx - side.x, sy - side.y, sz - side.z).setColor(r, g, b, a);
    }

    @Override
    public boolean shouldRenderOffScreen(ReaperBlockEntity be) {
        return be.isShowingArea() || !be.getBeamTargets().isEmpty();
    }

    @Override
    public AABB getRenderBoundingBox(ReaperBlockEntity be) {
        return be.isShowingArea() || !be.getBeamTargets().isEmpty() ? AABB.INFINITE : new AABB(be.getBlockPos()).expandTowards(0, 1, 0).inflate(0.5);
    }
}
