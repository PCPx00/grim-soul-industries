package com.xtri6.grimsoul.block;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import com.xtri6.grimsoul.Registration;
import com.xtri6.grimsoul.blockentity.SimulationChamberBlockEntity;

import net.minecraft.core.BlockPos;
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

public class SimulationChamberBlock extends MachineBlock {
    public static final MapCodec<SimulationChamberBlock> CODEC = simpleCodec(SimulationChamberBlock::new);

    public SimulationChamberBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SimulationChamberBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, Registration.SIMULATION_CHAMBER_ENTITY.get(), SimulationChamberBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof SimulationChamberBlockEntity chamber) {
            player.openMenu(chamber, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Purple sparks drift off the chamber while it simulates. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ACTIVE)) {
            return;
        }
        if (random.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.PORTAL, pos.getX() + random.nextDouble(), pos.getY() + 1.0, pos.getZ() + random.nextDouble(), 0.0, -0.3, 0.0);
        }
        if (random.nextInt(5) == 0) {
            level.addParticle(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                    random.nextGaussian() * 0.3, -0.5, random.nextGaussian() * 0.3);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof SimulationChamberBlockEntity chamber) {
            chamber.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
