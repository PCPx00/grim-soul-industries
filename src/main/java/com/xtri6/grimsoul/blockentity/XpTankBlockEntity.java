package com.xtri6.grimsoul.blockentity;

import javax.annotation.Nullable;

import com.xtri6.grimsoul.Config;
import com.xtri6.grimsoul.Registration;
import com.xtri6.grimsoul.block.MachineBlock;
import com.xtri6.grimsoul.machine.RedstoneMode;
import com.xtri6.grimsoul.menu.CardMachineMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * XP Tank: stores XP, pulls in XP orbs from an adjustable area while its magnet is on, accepts XP
 * pushed in by an adjacent Reaper Block, and works as a fluid tank of Liquid Experience
 * (20 mB = 1 XP) so pipes, AE2 and other mods can pump XP in and out.
 */
public class XpTankBlockEntity extends CardMachineBlockEntity {
    private static final int ABSORB_INTERVAL = 10;
    /** The client's liquid level updates in this many steps. */
    private static final int FILL_STEPS = 48;

    public static final int BTN_MAGNET = 0, BTN_POWER = 1, BTN_REDSTONE = 2, BTN_TAKE_ALL = 3, BTN_TAKE_LEVEL = 4;
    /** 5..10: area X-, X+, Y-, Y+, Z-, Z+ */
    public static final int BTN_AREA_BASE = 5;
    public static final int BTN_SHOW_AREA = 11, BTN_AREA_MAX = 12;

    public static final int DATA_XP_LOW = 0, DATA_XP_HIGH = 1, DATA_FILL = 2, DATA_MAGNET = 3, DATA_ENABLED = 4,
            DATA_REDSTONE = 5, DATA_RADIUS = 6, DATA_CARDS = 7, DATA_AREA_X = 8, DATA_AREA_Y = 9, DATA_AREA_Z = 10,
            DATA_SHOW_AREA = 11, DATA_COUNT = 12;

    private int storedXp = 0;
    private boolean magnet = true;
    private boolean showArea = false;
    /** How far the magnet reaches from the tank along each axis, in blocks. */
    private int areaX = 3, areaY = 3, areaZ = 3;
    private int timer = 0;
    private int lastSyncedFill = -1;
    /** Client copy of the capacity, sent by the server (configs are not synced to clients). */
    private int clientCapacity = 1;

    private final IFluidHandler fluidHandler = new XpFluidHandler();

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_XP_LOW -> storedXp & 0xFFFF;
                case DATA_XP_HIGH -> (storedXp >>> 16) & 0xFFFF;
                case DATA_FILL -> (int) (1000L * storedXp / Math.max(1, getCapacity()));
                case DATA_MAGNET -> magnet ? 1 : 0;
                case DATA_ENABLED -> control.isEnabled() ? 1 : 0;
                case DATA_REDSTONE -> control.getRedstoneMode().ordinal();
                case DATA_RADIUS -> getRadius();
                case DATA_CARDS -> getRangeCards();
                case DATA_AREA_X -> areaX;
                case DATA_AREA_Y -> areaY;
                case DATA_AREA_Z -> areaZ;
                case DATA_SHOW_AREA -> showArea ? 1 : 0;
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

    public XpTankBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.XP_TANK_ENTITY.get(), pos, state, RedstoneMode.IGNORE);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, XpTankBlockEntity be) {
        be.checkOverfill(level, pos);
        if (!(level instanceof ServerLevel serverLevel) || ++be.timer < ABSORB_INTERVAL) {
            return;
        }
        be.timer = 0;
        boolean active = be.control.isActive(level, pos) && be.magnet;
        MachineBlock.setActive(level, pos, state, active);
        if (!active || be.storedXp >= be.getCapacity()) {
            return;
        }
        for (ExperienceOrb orb : serverLevel.getEntitiesOfClass(ExperienceOrb.class, be.getMagnetArea(), ExperienceOrb::isAlive)) {
            CompoundTag tag = orb.saveWithoutId(new CompoundTag());
            long total = (long) orb.getValue() * Math.max(1, tag.getInt("Count"));
            int leftover = be.addXp((int) Math.min(Integer.MAX_VALUE, total));
            if (leftover > 0) {
                break; // Full: leave the orb where it is.
            }
            orb.discard();
        }
    }

    // ---- Area ----

    /** The farthest the magnet can reach along each axis: grows with Range cards. */
    public int getRadius() {
        return Config.TANK_BASE_RADIUS.get() + getRangeCards() * Config.TANK_RADIUS_PER_CARD.get();
    }

    public AABB getMagnetArea() {
        return new AABB(worldPosition).inflate(areaX, areaY, areaZ);
    }

    public boolean isShowingArea() {
        return showArea;
    }

    private void clampArea() {
        int max = getRadius();
        areaX = Mth.clamp(areaX, 0, max);
        areaY = Mth.clamp(areaY, 0, max);
        areaZ = Mth.clamp(areaZ, 0, max);
    }

    @Override
    protected void onCardsChanged() {
        clampArea();
        super.onCardsChanged();
    }

    // ---- XP storage ----

    public int getCapacity() {
        return level != null && level.isClientSide ? clientCapacity : Config.TANK_CAPACITY.get();
    }

    /** Fill level from 0 to 1, for the liquid renderer. */
    public float getFillFraction() {
        return Math.min(1.0F, storedXp / (float) Math.max(1, getCapacity()));
    }

    /** Adds XP and returns whatever did not fit. */
    public int addXp(int amount) {
        if (amount <= 0) {
            return 0;
        }
        int space = getCapacity() - storedXp;
        int accepted = Math.max(0, Math.min(space, amount));
        if (accepted > 0) {
            storedXp += accepted;
            onXpChanged();
        }
        return amount - accepted;
    }

    private int take(int amount) {
        int taken = Math.max(0, Math.min(amount, storedXp));
        if (taken > 0) {
            storedXp -= taken;
            onXpChanged();
        }
        return taken;
    }

    /** Removes and returns all stored XP. */
    public int takeAll() {
        return take(storedXp);
    }

    private void onXpChanged() {
        setChanged();
        int fill = (int) ((long) storedXp * FILL_STEPS / Math.max(1, getCapacity()));
        if (fill != lastSyncedFill) {
            lastSyncedFill = fill;
            syncToClient();
        }
    }

    public IFluidHandler getFluidHandler() {
        return fluidHandler;
    }

    /** Our own Liquid Experience, or any fluid other mods tag as c:experience. */
    private static boolean isXpFluid(FluidStack stack) {
        return !stack.isEmpty() && (stack.is(Tags.Fluids.EXPERIENCE)
                || stack.getFluid() == Registration.XP_FLUID.get()
                || stack.getFluid() == Registration.XP_FLUID_FLOWING.get());
    }

    /** The tank seen as a fluid tank: 20 mB of Liquid Experience = 1 XP point. */
    private class XpFluidHandler implements IFluidHandler {
        private static final int MB = Registration.MB_PER_XP;

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            if (storedXp <= 0) {
                return FluidStack.EMPTY;
            }
            return new FluidStack(Registration.XP_FLUID.get(), (int) Math.min(Integer.MAX_VALUE, (long) storedXp * MB));
        }

        @Override
        public int getTankCapacity(int tank) {
            return (int) Math.min(Integer.MAX_VALUE, (long) getCapacity() * MB);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return isXpFluid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!isXpFluid(resource)) {
                return 0;
            }
            int points = Math.min(resource.getAmount() / MB, getCapacity() - storedXp);
            if (points <= 0) {
                return 0;
            }
            if (action.execute()) {
                addXp(points);
            }
            return points * MB;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (!isXpFluid(resource)) {
                return FluidStack.EMPTY;
            }
            return drain(resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            int points = Math.min(maxDrain / MB, storedXp);
            if (points <= 0) {
                return FluidStack.EMPTY;
            }
            if (action.execute()) {
                take(points);
            }
            return new FluidStack(Registration.XP_FLUID.get(), points * MB);
        }
    }

    // ---- GUI ----

    @Override
    public void handleButton(int id, Player player) {
        if (id >= BTN_AREA_BASE && id < BTN_AREA_BASE + 6) {
            int axis = (id - BTN_AREA_BASE) / 2;
            int delta = (id - BTN_AREA_BASE) % 2 == 0 ? -1 : 1;
            switch (axis) {
                case 0 -> areaX += delta;
                case 1 -> areaY += delta;
                default -> areaZ += delta;
            }
        } else {
            switch (id) {
                case BTN_MAGNET -> magnet = !magnet;
                case BTN_POWER -> control.togglePower();
                case BTN_REDSTONE -> control.cycleRedstone();
                case BTN_TAKE_ALL -> giveXp(player, takeAll());
                case BTN_TAKE_LEVEL -> {
                    int needed = player.getXpNeededForNextLevel();
                    int progress = (int) (player.experienceProgress * needed);
                    giveXp(player, take(Math.max(1, needed - progress)));
                }
                case BTN_SHOW_AREA -> showArea = !showArea;
                case BTN_AREA_MAX -> {
                    int max = getRadius();
                    areaX = max;
                    areaY = max;
                    areaZ = max;
                }
                default -> {
                    return;
                }
            }
        }
        clampArea();
        setChanged();
        syncToClient();
    }

    private void giveXp(Player player, int amount) {
        if (amount > 0) {
            player.giveExperiencePoints(amount);
            if (level != null) {
                level.playSound(null, worldPosition, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.6F, 1.0F);
            }
        }
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        super.dropContents(level, pos);
        if (storedXp > 0 && level instanceof ServerLevel serverLevel) {
            ExperienceOrb.award(serverLevel, Vec3.atCenterOf(pos), storedXp);
            storedXp = 0;
        }
    }

    @Override
    public ContainerData getDataAccess() {
        return dataAccess;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.grimsoul.xp_tank");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return CardMachineMenu.xpTank(containerId, playerInventory, this);
    }

    @Override
    protected void writeClientData(CompoundTag tag) {
        tag.putInt("Xp", storedXp);
        tag.putInt("Capacity", Config.TANK_CAPACITY.get());
        tag.putBoolean("Magnet", magnet);
        tag.putBoolean("ShowArea", showArea);
        tag.putInt("AreaX", areaX);
        tag.putInt("AreaY", areaY);
        tag.putInt("AreaZ", areaZ);
    }

    @Override
    protected void readClientData(CompoundTag tag) {
        storedXp = tag.getInt("Xp");
        clientCapacity = Math.max(1, tag.getInt("Capacity"));
        if (tag.contains("Magnet")) {
            magnet = tag.getBoolean("Magnet");
        }
        showArea = tag.getBoolean("ShowArea");
        if (tag.contains("AreaX")) {
            areaX = tag.getInt("AreaX");
            areaY = tag.getInt("AreaY");
            areaZ = tag.getInt("AreaZ");
        }
    }
}
