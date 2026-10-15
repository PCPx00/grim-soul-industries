package com.xtri6.grimsoul.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Soul Forge Casing. While its forge is assembled, the casing switches to a seamless "joined" look
 * so the ring reads as one machine. The forge sets {@link #FORMED}; nothing else should.
 */
public class SoulForgeCasingBlock extends Block {
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public SoulForgeCasingBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }
}
