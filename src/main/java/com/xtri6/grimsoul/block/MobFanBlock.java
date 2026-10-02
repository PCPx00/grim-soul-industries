package com.xtri6.grimsoul.block;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import com.xtri6.grimsoul.Registration;
import com.xtri6.grimsoul.blockentity.MobFanBlockEntity;

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

public class MobFanBlock extends MachineBlock {
    public static final MapCodec<MobFanBlock> CODEC = simpleCodec(MobFanBlock::new);

    public MobFanBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MobFanBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, Registration.MOB_FAN_ENTITY.get(), MobFanBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MobFanBlockEntity fan) {
            player.openMenu(fan, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Wisps of air blowing out of the front while the fan is running. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ACTIVE) || random.nextInt(2) != 0) {
            return;
        }
        Direction facing = state.getValue(FACING);
        double x = pos.getX() + 0.5 + facing.getStepX() * 0.6 + (random.nextDouble() - 0.5) * 0.8;
        double y = pos.getY() + 0.2 + random.nextDouble() * 0.6;
        double z = pos.getZ() + 0.5 + facing.getStepZ() * 0.6 + (random.nextDouble() - 0.5) * 0.8;
        level.addParticle(ParticleTypes.CLOUD, x, y, z, facing.getStepX() * 0.25, 0.0, facing.getStepZ() * 0.25);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof MobFanBlockEntity fan) {
            fan.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
