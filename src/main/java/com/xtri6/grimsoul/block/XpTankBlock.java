package com.xtri6.grimsoul.block;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import com.xtri6.grimsoul.Registration;
import com.xtri6.grimsoul.blockentity.XpTankBlockEntity;

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

public class XpTankBlock extends MachineBlock {
    public static final MapCodec<XpTankBlock> CODEC = simpleCodec(XpTankBlock::new);

    public XpTankBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new XpTankBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, Registration.XP_TANK_ENTITY.get(), XpTankBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof XpTankBlockEntity tank) {
            player.openMenu(tank, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Enchanting glyphs swirling in toward the tank while its magnet is pulling. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ACTIVE) || random.nextInt(2) != 0) {
            return;
        }
        double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 1.2;
        double y = pos.getY() + 0.3 + random.nextDouble() * 0.9;
        double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 1.2;
        level.addParticle(ParticleTypes.ENCHANT, x, y, z, 0.0, 0.2, 0.0);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof XpTankBlockEntity tank) {
            tank.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
