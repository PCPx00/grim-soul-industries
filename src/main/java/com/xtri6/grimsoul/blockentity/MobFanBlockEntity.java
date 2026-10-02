package com.xtri6.grimsoul.blockentity;

import javax.annotation.Nullable;

import com.xtri6.grimsoul.Config;
import com.xtri6.grimsoul.Registration;
import com.xtri6.grimsoul.block.MachineBlock;
import com.xtri6.grimsoul.machine.RedstoneMode;
import com.xtri6.grimsoul.menu.CardMachineMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Mob Fan: pushes mobs, items and XP orbs away from its face. Its push area's length, width and
 * height are set in its GUI, and Range cards (up to 10) raise how big each can go.
 */
public class MobFanBlockEntity extends CardMachineBlockEntity {

    public static final int BTN_LENGTH = 0;   // 0 = shorter, 1 = longer
    public static final int BTN_WIDTH = 2;    // 2 = narrower, 3 = wider
    public static final int BTN_HEIGHT = 4;   // 4 = lower, 5 = taller
    public static final int BTN_SHOW_AREA = 6, BTN_POWER = 7, BTN_REDSTONE = 8, BTN_MAX = 9;
    public static final int DATA_LENGTH = 0, DATA_WIDTH = 1, DATA_HEIGHT = 2, DATA_MAX_LENGTH = 3, DATA_MAX_WIDTH = 4,
            DATA_MAX_HEIGHT = 5, DATA_SHOW_AREA = 6, DATA_ENABLED = 7, DATA_REDSTONE = 8, DATA_CARDS = 9, DATA_COUNT = 10;

    private int length = 4;
    private int width = 3;
    private int height = 2;
    private boolean showArea = false;

    // Client only: blade animation (spins up and coasts down smoothly).
    private float bladeSpin = 0.0F;
    private float bladeAngle = 0.0F;
    private double lastBladeTime = -1.0;

    /** Client: the blades' current angle in degrees, advanced by real time. */
    public float bladeAngle(float partialTick, boolean running) {
        double now = (level != null ? level.getGameTime() : 0) + partialTick;
        double dt = lastBladeTime < 0 ? 0 : Math.max(0.0, Math.min(5.0, now - lastBladeTime));
        lastBladeTime = now;
        bladeSpin += ((running ? 1.0F : 0.0F) - bladeSpin) * (float) Math.min(1.0, dt * 0.04);
        bladeAngle = (float) ((bladeAngle + dt * bladeSpin * 50.0) % 360.0);
        return bladeAngle;
    }

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_LENGTH -> length;
                case DATA_WIDTH -> width;
                case DATA_HEIGHT -> height;
                case DATA_MAX_LENGTH -> getMaxLength();
                case DATA_MAX_WIDTH -> getMaxWidth();
                case DATA_MAX_HEIGHT -> getMaxHeight();
                case DATA_SHOW_AREA -> showArea ? 1 : 0;
                case DATA_ENABLED -> control.isEnabled() ? 1 : 0;
                case DATA_REDSTONE -> control.getRedstoneMode().ordinal();
                case DATA_CARDS -> getRangeCards();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public MobFanBlockEntity(BlockPos pos, BlockState state) {
        // Stops on a redstone signal by default, like the first version.
        super(Registration.MOB_FAN_ENTITY.get(), pos, state, RedstoneMode.LOW);
    }

    // ---- Size limits (grow with Range cards) ----

    public int getMaxLength() {
        return Config.FAN_BASE_RANGE.get() + getRangeCards() * Config.FAN_RANGE_PER_CARD.get();
    }

    /** Always odd, so the push area stays centered on the fan: 3 with no cards, 13 with 10. */
    public int getMaxWidth() {
        return 3 + 2 * (getRangeCards() / 2);
    }

    public int getMaxHeight() {
        return 2 + getRangeCards();
    }

    private void clampSize() {
        length = Mth.clamp(length, 1, getMaxLength());
        width = Mth.clamp(width, 1, getMaxWidth());
        if (width % 2 == 0) {
            width--;
        }
        height = Mth.clamp(height, 1, getMaxHeight());
    }

    // ---- Ticking ----

    public static void serverTick(Level level, BlockPos pos, BlockState state, MobFanBlockEntity be) {
        be.checkOverfill(level, pos);
        boolean active = be.control.isActive(level, pos);
        MachineBlock.setActive(level, pos, state, active);
        if (!active) {
            return;
        }
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        AABB area = be.getPushArea(facing);
        double strength = Config.FAN_PUSH_STRENGTH.get();
        double maxSpeed = Config.FAN_MAX_SPEED.get();
        for (Entity entity : level.getEntitiesOfClass(Entity.class, area, e -> !(e instanceof Player) && !e.isSpectator() && e.isAlive())) {
            Vec3 motion = entity.getDeltaMovement();
            double x = facing.getStepX() == 0 ? motion.x : clampTowards(motion.x + facing.getStepX() * strength, facing.getStepX(), maxSpeed);
            double z = facing.getStepZ() == 0 ? motion.z : clampTowards(motion.z + facing.getStepZ() * strength, facing.getStepZ(), maxSpeed);
            entity.setDeltaMovement(x, motion.y, z);
            entity.hasImpulse = true;
        }
    }

    private static double clampTowards(double value, int step, double maxSpeed) {
        return step > 0 ? Math.min(value, maxSpeed) : Math.max(value, -maxSpeed);
    }

    public AABB getPushArea(Direction facing) {
        BlockPos start = worldPosition.relative(facing);
        BlockPos end = worldPosition.relative(facing, length);
        double half = (width - 1) / 2.0;
        double minX = Math.min(start.getX(), end.getX());
        double minZ = Math.min(start.getZ(), end.getZ());
        double maxX = Math.max(start.getX(), end.getX()) + 1;
        double maxZ = Math.max(start.getZ(), end.getZ()) + 1;
        if (facing.getAxis() == Direction.Axis.X) {
            minZ -= half;
            maxZ += half;
        } else {
            minX -= half;
            maxX += half;
        }
        double minY = worldPosition.getY();
        return new AABB(minX, minY, minZ, maxX, minY + height, maxZ);
    }

    public boolean isShowingArea() {
        return showArea;
    }

    @Override
    public void handleButton(int id, Player player) {
        switch (id) {
            case BTN_LENGTH -> length--;
            case BTN_LENGTH + 1 -> length++;
            case BTN_WIDTH -> width -= 2;
            case BTN_WIDTH + 1 -> width += 2;
            case BTN_HEIGHT -> height--;
            case BTN_HEIGHT + 1 -> height++;
            case BTN_SHOW_AREA -> showArea = !showArea;
            case BTN_POWER -> control.togglePower();
            case BTN_REDSTONE -> control.cycleRedstone();
            case BTN_MAX -> {
                length = getMaxLength();
                width = getMaxWidth();
                height = getMaxHeight();
            }
            default -> {
                return;
            }
        }
        clampSize();
        setChanged();
        syncToClient();
    }

    @Override
    protected void onCardsChanged() {
        clampSize();
        super.onCardsChanged();
    }

    @Override
    public ContainerData getDataAccess() {
        return dataAccess;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.grimsoul.mob_fan");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return CardMachineMenu.mobFan(containerId, playerInventory, this);
    }

    @Override
    protected void writeClientData(CompoundTag tag) {
        tag.putInt("Length", length);
        tag.putInt("Width", width);
        tag.putInt("Height", height);
        tag.putBoolean("ShowArea", showArea);
    }

    @Override
    protected void readClientData(CompoundTag tag) {
        if (tag.contains("Length")) {
            length = tag.getInt("Length");
            width = tag.getInt("Width");
            height = tag.getInt("Height");
        }
        showArea = tag.getBoolean("ShowArea");
    }
}
