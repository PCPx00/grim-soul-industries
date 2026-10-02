package com.xtri6.grimsoul.block;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import com.xtri6.grimsoul.Registration;
import com.xtri6.grimsoul.blockentity.EssenceSpawnerBlockEntity;

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

public class EssenceSpawnerBlock extends MachineBlock {
    public static final MapCodec<EssenceSpawnerBlock> CODEC = simpleCodec(EssenceSpawnerBlock::new);

    public EssenceSpawnerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EssenceSpawnerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, Registration.ESSENCE_SPAWNER_ENTITY.get(), EssenceSpawnerBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof EssenceSpawnerBlockEntity spawner) {
            player.openMenu(spawner, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Purple soul wisps rising from the cage while it is spawning. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ACTIVE)) {
            return;
        }
        for (int i = 0; i < 2; i++) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            level.addParticle(ParticleTypes.WITCH, x, y, z, 0.0, 0.0, 0.0);
        }
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.SOUL, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 0.0, 0.03, 0.0);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof EssenceSpawnerBlockEntity spawner) {
            spawner.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
