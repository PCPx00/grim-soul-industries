package com.xtri6.grimsoul.block;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import com.xtri6.grimsoul.blockentity.OutputCrateBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class OutputCrateBlock extends BaseEntityBlock {
    public static final MapCodec<OutputCrateBlock> CODEC = simpleCodec(OutputCrateBlock::new);

    public OutputCrateBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OutputCrateBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof OutputCrateBlockEntity crate) {
            player.openMenu(crate, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof OutputCrateBlockEntity crate) {
            crate.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof OutputCrateBlockEntity crate) {
            int used = crate.getDataAccess().get(OutputCrateBlockEntity.DATA_USED);
            return used == 0 ? 0 : 1 + used * 14 / OutputCrateBlockEntity.SLOTS;
        }
        return 0;
    }
}
