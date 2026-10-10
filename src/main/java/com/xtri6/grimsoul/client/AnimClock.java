package com.xtri6.grimsoul.client;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * A smooth clock for block animations. The world's game time is re-synced from the server every
 * second and stalls or jumps when the server lags, which makes spinning and bobbing parts hitch.
 * This counts client ticks instead (and stops while the game is paused), so animations stay smooth.
 */
public final class AnimClock {
    private static long ticks;

    private AnimClock() {
    }

    public static void init() {
        NeoForge.EVENT_BUS.addListener(AnimClock::tick);
    }

    private static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && !mc.isPaused()) {
            ticks++;
        }
    }

    /** Whole client ticks so far. */
    public static long ticks() {
        return ticks;
    }

    /** Smooth animation time in ticks, including the fraction of the current tick. */
    public static float time(float partialTick) {
        // Wrap so float precision stays good in long sessions (about 14 hours per cycle).
        return (ticks % 1_000_000L) + partialTick;
    }
}
