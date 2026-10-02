package com.xtri6.grimsoul.block;

import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import com.xtri6.grimsoul.Config;
import com.xtri6.grimsoul.Registration;
import com.xtri6.grimsoul.blockentity.EnergyCellBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

public class EnergyCellBlock extends MachineBlock {
    public static final MapCodec<EnergyCellBlock> CODEC = simpleCodec(EnergyCellBlock::new);
    /** Charge gauge shown on the sides: 0 = empty, 8 = full. */
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 8);

    public EnergyCellBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(LEVEL, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LEVEL);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyCellBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, Registration.ENERGY_CELL_ENTITY.get(), EnergyCellBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof EnergyCellBlockEntity cell) {
            player.openMenu(cell, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof EnergyCellBlockEntity cell) {
            int max = Config.CELL_CAPACITY.get();
            return cell.getEnergy() <= 0 ? 0 : 1 + (int) ((long) cell.getEnergy() * 14 / Math.max(1, max));
        }
        return 0;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int stored = stack.getOrDefault(Registration.STORED_ENERGY.get(), 0);
        int capacity = Config.SPEC.isLoaded() ? Config.CELL_CAPACITY.get() : 10_000_000;
        tooltip.add(Component.translatable("tooltip.grimsoul.energy_cell",
                String.format(Locale.ROOT, "%,d", stored), String.format(Locale.ROOT, "%,d", capacity)).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.grimsoul.energy_cell.keeps").withStyle(ChatFormatting.GRAY));
    }
}
