package com.xtri6.grimsoul.block;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import com.xtri6.grimsoul.Registration;
import com.xtri6.grimsoul.blockentity.ReaperGeneratorBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class ReaperGeneratorBlock extends MachineBlock {
    public static final MapCodec<ReaperGeneratorBlock> CODEC = simpleCodec(ReaperGeneratorBlock::new);

    public ReaperGeneratorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReaperGeneratorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, Registration.REAPER_GENERATOR_ENTITY.get(), ReaperGeneratorBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ReaperGeneratorBlockEntity generator) {
            player.openMenu(generator, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Soul flames flicker out of the firebox and sparks rise off the coil while it burns. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ACTIVE)) {
            return;
        }
        Direction facing = state.getValue(FACING);
        double x = pos.getX() + 0.5 + facing.getStepX() * 0.52;
        double z = pos.getZ() + 0.5 + facing.getStepZ() * 0.52;
        double spread = (random.nextDouble() - 0.5) * 0.5;
        double ox = facing.getAxis() == Direction.Axis.Z ? spread : 0.0;
        double oz = facing.getAxis() == Direction.Axis.X ? spread : 0.0;
        level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x + ox, pos.getY() + 0.3 + random.nextDouble() * 0.4, z + oz, 0.0, 0.0, 0.0);
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.WITCH, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 0.0, 0.05, 0.0);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ReaperGeneratorBlockEntity generator) {
            generator.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
