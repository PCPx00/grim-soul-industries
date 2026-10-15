package com.xtri6.grimsoul.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

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
}
