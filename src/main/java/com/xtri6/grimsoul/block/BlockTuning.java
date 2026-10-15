package com.xtri6.grimsoul.block;

import com.xtri6.grimsoul.Config;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

/**
 * Reads a mod block's hardness and blast resistance from the config (blocks.&lt;name&gt;.* in grimsoul-common.toml).
 * Both values are read each time they are needed, so a pack maker's change applies after a restart.
 */
public final class BlockTuning {
    /** The hardness every mod block was built with. Breaking speed is scaled against this. */
    public static final float DEFAULT_HARDNESS = 3.5F;

    private BlockTuning() {
    }

    public static float hardness(Block block) {
        var value = Config.BLOCK_HARDNESS.get(name(block));
        return value == null || !Config.SPEC.isLoaded() ? DEFAULT_HARDNESS : value.get().floatValue();
    }

    public static float blastResistance(Block block) {
        var value = Config.BLOCK_BLAST_RESISTANCE.get(name(block));
        return value == null || !Config.SPEC.isLoaded() ? 6.0F : value.get().floatValue();
    }

    private static String name(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).getPath();
    }
}
