package com.xtri6.grimsoul.block;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import com.xtri6.grimsoul.Registration;
import com.xtri6.grimsoul.blockentity.SoulForgeBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/** Soul Forge controller: right-click to open it. Build the 3x3x3 cube of Soul Forge Casing around it first. */
public class SoulForgeBlock extends MachineBlock {
    public static final MapCodec<SoulForgeBlock> CODEC = simpleCodec(SoulForgeBlock::new);
    /** True while the 3x3 ring is assembled. The forge then wakes up: its eye opens and the ring breathes. */
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public SoulForgeBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FORMED);
    }

    /** Soul motes rising off the assembled cube, and a heartbeat now and then. Faster while it is forging. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(FORMED)) {
            return;
        }
        boolean forging = state.getValue(ACTIVE);
        if (random.nextInt(forging ? 2 : 5) == 0) {
            double x = pos.getX() - 1 + random.nextDouble() * 3.0;
            double z = pos.getZ() - 1 + random.nextDouble() * 3.0;
            double y = pos.getY() + 1.0 + random.nextDouble() * 2.0;
            level.addParticle(forging ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.SOUL, x, y, z, 0.0, 0.03, 0.0);
        }
        if (random.nextInt(forging ? 30 : 110) == 0) {
            level.playLocalSound(pos, SoundEvents.WARDEN_HEARTBEAT, SoundSource.BLOCKS,
                    forging ? 0.9F : 0.5F, forging ? 1.2F : 0.9F, false);
        }
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SoulForgeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, Registration.SOUL_FORGE_ENTITY.get(), SoulForgeBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof SoulForgeBlockEntity forge) {
            player.openMenu(forge, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof SoulForgeBlockEntity forge) {
            forge.dismantle(level, pos);
            forge.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
