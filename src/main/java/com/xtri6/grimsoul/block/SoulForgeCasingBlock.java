package com.xtri6.grimsoul.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import com.xtri6.grimsoul.Registration;
import com.xtri6.grimsoul.blockentity.SoulForgeBlockEntity;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Soul Forge Casing. Once its forge is assembled, each casing takes on its place in the cube: {@link #PART} says
 * whether it is a face, an edge or a corner. Faces disappear, edges show chain links and corners show bone, so the
 * finished cube becomes a cage. The forge sets both properties; nothing else should.
 */
public class SoulForgeCasingBlock extends Block {
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    /** 0 = face, 1 = edge, 2 = corner. */
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 2);

    public SoulForgeCasingBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FORMED, false).setValue(PART, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED, PART);
    }

    /** A completed cage glows. */
    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(FORMED) ? 7 : 0;
    }

    /** Right-clicking any casing of a completed cube opens its forge. Sneak to place blocks against it instead. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown() || !state.getValue(FORMED)) {
            return InteractionResult.PASS;
        }
        BlockPos forge = findFormedForge(level, pos);
        if (forge == null) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && level.getBlockEntity(forge) instanceof SoulForgeBlockEntity be) {
            player.openMenu(be, forge);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** The completed forge whose cube holds this casing, or null. */
    private static BlockPos findFormedForge(Level level, BlockPos casing) {
        for (int dy = 0; dy <= 2; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos candidate = casing.offset(-dx, -dy, -dz);
                    if (level.getBlockState(candidate).is(Registration.SOUL_FORGE.get())
                            && level.getBlockEntity(candidate) instanceof SoulForgeBlockEntity be && be.isFormed()) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    /** Blast resistance from the config (blocks.&lt;name&gt;.blastResistance). */
    @Override
    public float getExplosionResistance() {
        return BlockTuning.blastResistance(this);
    }

    /** Break speed from the config (blocks.&lt;name&gt;.hardness): harder blocks take longer. */
    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        float vanilla = super.getDestroyProgress(state, player, level, pos);
        float hardness = BlockTuning.hardness(this);
        return hardness <= 0.0F ? vanilla * 1000.0F : vanilla * (BlockTuning.DEFAULT_HARDNESS / hardness);
    }
}
