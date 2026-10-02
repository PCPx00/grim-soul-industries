package com.xtri6.grimsoul.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

/**
 * The on/off switch and redstone setting every machine shares. The redstone signal is checked
 * at most every 10 ticks to keep machines cheap.
 */
public class MachineControl {
    private static final int SIGNAL_CHECK_INTERVAL = 10;

    private boolean enabled = true;
    private RedstoneMode redstoneMode;
    private long lastSignalCheck = Long.MIN_VALUE;
    private boolean powered = false;

    public MachineControl(RedstoneMode defaultMode) {
        this(defaultMode, true);
    }

    /** @param enabledByDefault false for machines that should start switched off when placed */
    public MachineControl(RedstoneMode defaultMode, boolean enabledByDefault) {
        this.redstoneMode = defaultMode;
        this.enabled = enabledByDefault;
    }

    public boolean isActive(Level level, BlockPos pos) {
        long now = level.getGameTime();
        if (now - lastSignalCheck >= SIGNAL_CHECK_INTERVAL || now < lastSignalCheck) {
            powered = level.hasNeighborSignal(pos);
            lastSignalCheck = now;
        }
        return enabled && redstoneMode.allows(powered);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public RedstoneMode getRedstoneMode() {
        return redstoneMode;
    }

    public void togglePower() {
        enabled = !enabled;
        lastSignalCheck = Long.MIN_VALUE;
    }

    public void cycleRedstone() {
        redstoneMode = redstoneMode.next();
        lastSignalCheck = Long.MIN_VALUE;
    }

    public void save(CompoundTag tag) {
        tag.putBoolean("Enabled", enabled);
        tag.putInt("Redstone", redstoneMode.ordinal());
    }

    public void load(CompoundTag tag) {
        if (tag.contains("Enabled")) {
            enabled = tag.getBoolean("Enabled");
        }
        if (tag.contains("Redstone")) {
            redstoneMode = RedstoneMode.byIndex(tag.getInt("Redstone"));
        }
    }
}
