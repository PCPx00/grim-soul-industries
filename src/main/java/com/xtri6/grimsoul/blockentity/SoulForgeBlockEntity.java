package com.xtri6.grimsoul.blockentity;

import javax.annotation.Nullable;

import com.xtri6.grimsoul.Config;
import com.xtri6.grimsoul.Registration;
import com.xtri6.grimsoul.block.MachineBlock;
import com.xtri6.grimsoul.block.SoulForgeBlock;
import com.xtri6.grimsoul.block.SoulForgeCasingBlock;
import com.xtri6.grimsoul.menu.SoulForgeMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Soul Forge controller. It only works when the multiblock is built: a 3x3x3 cube with the forge at the bottom
 * centre. Soul Forge Casing fills every other spot except the two directly above the forge, which stay empty as
 * a shaft so the forge can still be reached from above. When the cube is complete the casings join up into one machine, with a
 * sound, particles and a message for nearby players.
 */
public class SoulForgeBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_WORK = 0, SLOT_STEEL = 1, SLOT_OUTPUT = 2;
    public static final int STATUS_CRAFTING = 0, STATUS_INCOMPLETE = 1, STATUS_NO_WORK = 2, STATUS_NO_STEEL = 3,
            STATUS_NO_POWER = 4, STATUS_OUTPUT_FULL = 5, STATUS_NO_RECIPE = 6, STATUS_IDLE = 7;
    public static final int DATA_FORMED = 0, DATA_MISSING = 1, DATA_PROGRESS = 2, DATA_MAX = 3, DATA_STATUS = 4,
            DATA_ENERGY_LO = 5, DATA_ENERGY_HI = 6, DATA_COUNT = 7;
    /** Every spot in the 3x3x3 cube except the forge: 24 casings and the two empty spots above the forge. */
    public static final int CUBE_SPOTS = 26;
    /** How close a player must be to hear the assembly message. */
    private static final double MESSAGE_RANGE_SQR = 24.0 * 24.0;

    private final ItemStackHandler items = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot != SLOT_OUTPUT;
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == SLOT_WORK ? 1 : super.getSlotLimit(slot);
        }
    };
    private final ForgeEnergy energy = new ForgeEnergy();
    private boolean formed;
    private int missing = CUBE_SPOTS;
    private int progress;
    private int maxProgress;
    private int status = STATUS_IDLE;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_FORMED -> formed ? 1 : 0;
                case DATA_MISSING -> missing;
                case DATA_PROGRESS -> progress;
                case DATA_MAX -> maxProgress;
                case DATA_STATUS -> status;
                case DATA_ENERGY_LO -> energy.getEnergyStored() & 0xFFFF;
                case DATA_ENERGY_HI -> energy.getEnergyStored() >>> 16;
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

    public SoulForgeBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.SOUL_FORGE_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SoulForgeBlockEntity be) {
        // Check every tick: a broken casing stops a craft at once.
        be.checkStructure(level, pos);
        ItemStack work = be.items.getStackInSlot(SLOT_WORK);
        ItemStack steel = be.items.getStackInSlot(SLOT_STEEL);
        SoulForgeRecipe recipe = null;
        if (!be.formed) {
            be.status = STATUS_INCOMPLETE;
        } else if (work.isEmpty()) {
            be.status = STATUS_NO_WORK;
        } else if (steel.isEmpty()) {
            be.status = STATUS_NO_STEEL;
        } else if ((recipe = SoulForgeRecipe.find(work)) == null) {
            be.status = STATUS_NO_RECIPE;
        } else if (!be.outputAccepts(recipe.result(work))) {
            be.status = STATUS_OUTPUT_FULL;
        } else if (be.energy.getEnergyStored() < recipe.perTick()) {
            be.status = STATUS_NO_POWER;
        } else {
            be.status = STATUS_CRAFTING;
        }

        boolean crafting = be.status == STATUS_CRAFTING;
        if (crafting) {
            be.energy.drain(recipe.perTick());
            be.maxProgress = recipe.time();
            be.progress++;
            if (be.progress >= recipe.time()) {
                be.finish(recipe);
                be.progress = 0;
            }
            if (be.progress % 20 == 0) {
                be.setChanged();
            }
        } else if (be.status != STATUS_NO_POWER) {
            // Keep progress through a power dip, but start over when the work item or structure changes.
            be.progress = 0;
            be.maxProgress = 0;
        }
        // Read the state again: checkStructure may have just changed the FORMED property.
        MachineBlock.setActive(level, pos, level.getBlockState(pos), crafting);
    }

    /**
     * Looks at the 3x3x3 cube around the controller. Every spot must hold Soul Forge Casing, except the two spots
     * directly above the forge, which must be empty. When the cube completes or breaks, the casings join or
     * split and the players nearby are told.
     */
    private void checkStructure(Level level, BlockPos pos) {
        int bad = 0;
        for (int dy = 0; dy <= 2; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (isForge(dx, dy, dz)) {
                        continue;
                    }
                    BlockState here = level.getBlockState(pos.offset(dx, dy, dz));
                    boolean ok = isAccessSpot(dx, dy, dz) ? here.isAir() : here.is(Registration.SOUL_FORGE_CASING.get());
                    if (!ok) {
                        bad++;
                    }
                }
            }
        }
        missing = bad;
        boolean nowFormed = bad == 0;
        if (nowFormed != formed) {
            formed = nowFormed;
            setChanged();
            announce(level, pos);
        }
        BlockState self = level.getBlockState(pos);
        if (self.hasProperty(SoulForgeBlock.FORMED) && self.getValue(SoulForgeBlock.FORMED) != formed) {
            level.setBlock(pos, self.setValue(SoulForgeBlock.FORMED, formed), Block.UPDATE_ALL);
        }
        syncCasings(level, pos, formed);
    }

    private static boolean isForge(int dx, int dy, int dz) {
        return dx == 0 && dy == 0 && dz == 0;
    }

    /** The two spots directly above the forge: kept empty as a shaft, so the forge can be reached from above. */
    private static boolean isAccessSpot(int dx, int dy, int dz) {
        return dx == 0 && dz == 0 && dy >= 1;
    }

    /** Sets every casing spot's FORMED state to match, so casings placed later join up too. */
    private static void syncCasings(Level level, BlockPos pos, boolean joined) {
        for (int dy = 0; dy <= 2; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (isForge(dx, dy, dz) || isAccessSpot(dx, dy, dz)) {
                        continue;
                    }
                    BlockPos spot = pos.offset(dx, dy, dz);
                    BlockState here = level.getBlockState(spot);
                    if (here.is(Registration.SOUL_FORGE_CASING.get())
                            && here.getValue(SoulForgeCasingBlock.FORMED) != joined) {
                        level.setBlock(spot, here.setValue(SoulForgeCasingBlock.FORMED, joined), Block.UPDATE_ALL);
                    }
                }
            }
        }
    }

    /** Called when the controller is broken: the casings stop looking joined. */
    public void dismantle(Level level, BlockPos pos) {
        formed = false;
        syncCasings(level, pos, false);
    }

    /** Sound, particles and a message when the cube is completed or broken. */
    private void announce(Level level, BlockPos pos) {
        boolean joined = formed;
        level.playSound(null, pos, joined ? SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_DEACTIVATE,
                SoundSource.BLOCKS, 1.0F, joined ? 1.0F : 0.7F);
        if (level instanceof ServerLevel server) {
            for (int dy = 0; dy <= 2; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (isForge(dx, dy, dz) || isAccessSpot(dx, dy, dz)) {
                            continue;
                        }
                        // Particles rise from each casing, so the pieces seem to come together.
                        server.sendParticles(joined ? ParticleTypes.PORTAL : ParticleTypes.SMOKE,
                                pos.getX() + dx + 0.5, pos.getY() + dy + 0.5, pos.getZ() + dz + 0.5,
                                joined ? 8 : 4, 0.25, 0.4, 0.25, joined ? 0.25 : 0.02);
                    }
                }
            }
            if (joined) {
                server.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 2.5, pos.getZ() + 0.5,
                        50, 1.4, 1.0, 1.4, 0.05);
            }
        }
        Component message = Component.translatable(joined
                ? "message.grimsoul.soul_forge.formed" : "message.grimsoul.soul_forge.broken");
        for (Player player : level.players()) {
            if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5) < MESSAGE_RANGE_SQR) {
                player.displayClientMessage(message, true);
            }
        }
    }

    private boolean outputAccepts(ItemStack result) {
        ItemStack out = items.getStackInSlot(SLOT_OUTPUT);
        return out.isEmpty() || (ItemStack.isSameItemSameComponents(out, result)
                && out.getCount() + result.getCount() <= out.getMaxStackSize());
    }

    private void finish(SoulForgeRecipe recipe) {
        ItemStack result = recipe.result(items.getStackInSlot(SLOT_WORK));
        items.extractItem(SLOT_WORK, 1, false);
        items.extractItem(SLOT_STEEL, 1, false);
        if (items.getStackInSlot(SLOT_OUTPUT).isEmpty()) {
            items.setStackInSlot(SLOT_OUTPUT, result);
        } else {
            items.getStackInSlot(SLOT_OUTPUT).grow(result.getCount());
        }
        setChanged();
    }

    /** True while the 3x3x3 cube is assembled. */
    public boolean isFormed() {
        return formed;
    }

    /** True while the forge is making something. */
    public boolean isForging() {
        return status == STATUS_CRAFTING;
    }

    public IItemHandler getItems() {
        return items;
    }

    public ContainerData getDataAccess() {
        return dataAccess;
    }

    public IEnergyStorage getEnergyHandler() {
        return energy;
    }

    /** Drops what's in the slots when the controller is broken. */
    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < items.getSlots(); i++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), items.getStackInSlot(i).copy());
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", items.serializeNBT(registries));
        tag.putInt("Energy", energy.getEnergyStored());
        tag.putInt("Progress", progress);
        tag.putBoolean("Formed", formed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("Items"));
        energy.setStored(tag.getInt("Energy"));
        progress = tag.getInt("Progress");
        // Restored so a forge that was already assembled doesn't announce itself again after a restart.
        formed = tag.getBoolean("Formed");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.grimsoul.soul_forge");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new SoulForgeMenu(id, inventory, this);
    }

    /** Power in from generators or cables; nothing comes back out. */
    private static class ForgeEnergy extends EnergyStorage {
        ForgeEnergy() {
            super(Config.FORGE_ENERGY_CAPACITY.get(), Config.FORGE_MAX_INPUT.get(), 0);
        }

        void setStored(int value) {
            this.energy = Math.max(0, Math.min(capacity, value));
        }

        void drain(int amount) {
            this.energy = Math.max(0, this.energy - amount);
        }
    }
}
