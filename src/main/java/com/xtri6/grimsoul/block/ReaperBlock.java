package com.xtri6.grimsoul.block;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import com.xtri6.grimsoul.Registration;
import com.xtri6.grimsoul.blockentity.ReaperBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class ReaperBlock extends MachineBlock {
    public static final MapCodec<ReaperBlock> CODEC = simpleCodec(ReaperBlock::new);

    public ReaperBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReaperBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, Registration.REAPER_BLOCK_ENTITY.get(), ReaperBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ReaperBlockEntity reaper) {
            player.openMenu(reaper, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** A faint purple glow drifting off the emitter while the block is running. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ACTIVE) || random.nextInt(3) != 0) {
            return;
        }
        Direction facing = state.getValue(FACING);
        double x = pos.getX() + 0.5 + facing.getStepX() * 0.65 + (random.nextDouble() - 0.5) * 0.3;
        double y = pos.getY() + 0.5 + (random.nextDouble() - 0.5) * 0.3;
        double z = pos.getZ() + 0.5 + facing.getStepZ() * 0.65 + (random.nextDouble() - 0.5) * 0.3;
        level.addParticle(ParticleTypes.WITCH, x, y, z, 0.0, 0.0, 0.0);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ReaperBlockEntity reaper) {
            reaper.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
