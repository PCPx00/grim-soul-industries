package com.xtri6.grimsoul.blockentity;

import java.util.Arrays;
import java.util.List;
import java.util.EnumSet;
import java.util.Optional;

import javax.annotation.Nullable;

import com.xtri6.grimsoul.Config;
import com.xtri6.grimsoul.Registration;
import com.xtri6.grimsoul.block.MachineBlock;
import com.xtri6.grimsoul.item.DataCoreItem;
import com.xtri6.grimsoul.item.UpgradeType;
import com.xtri6.grimsoul.machine.MachineControl;
import com.xtri6.grimsoul.machine.RedstoneMode;
import com.xtri6.grimsoul.menu.SimulationMenu;
import com.xtri6.grimsoul.menu.UpgradeItemHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Reaper Simulation Chamber: runs the mob on a bound Data Core through its real loot table, with
 * no entity ever spawned. Uses FE per run; Speed, Looting and Efficiency cards tune it. Drops go
 * into its output slots and get pushed to any adjacent storage (an Output Crate, chest, pipe or
 * AE2). XP goes to an adjacent Reaper XP Tank or can be taken from the GUI.
 */
public class SimulationChamberBlockEntity extends BlockEntity implements MenuProvider {
    public static final UpgradeType[] CARD_SLOTS = {UpgradeType.SPEED, UpgradeType.LOOTING, UpgradeType.EFFICIENCY, UpgradeType.PLAYER_KILL};
    public static final int CARD_COUNT = CARD_SLOTS.length;
    public static final int OUTPUT_SLOTS = 18;
    private static final int PUSH_INTERVAL = 10;

    public static final int STATUS_RUNNING = 0, STATUS_NO_CORE = 1, STATUS_UNBOUND = 2, STATUS_NO_POWER = 3,
            STATUS_FULL = 4, STATUS_OFF = 5, STATUS_BLOCKED = 6;

    public static final int BTN_POWER = 0, BTN_REDSTONE = 1, BTN_EJECT = 2, BTN_TAKE_XP = 3;
    public static final int DATA_STATUS = 0, DATA_PROGRESS = 1, DATA_INTERVAL = 2, DATA_ENERGY_LO = 3, DATA_ENERGY_HI = 4,
            DATA_CAPACITY_LO = 5, DATA_CAPACITY_HI = 6, DATA_COST_LO = 7, DATA_COST_HI = 8, DATA_CHANCE = 9,
            DATA_XP_LO = 10, DATA_XP_HI = 11, DATA_ENABLED = 12, DATA_REDSTONE = 13, DATA_EJECT = 14,
            DATA_RUNS_LO = 15, DATA_RUNS_HI = 16, DATA_COUNT = 17;

    private final ItemStackHandler core = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof DataCoreItem;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            progress = 0;
            setChanged();
            syncToClient();
        }
    };
    private final UpgradeItemHandler cards = createCardHandler(this::setChanged);
    private final ItemStackHandler output = new ItemStackHandler(OUTPUT_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final ChamberEnergy energy = new ChamberEnergy(Config.SIM_CAPACITY.get());
    private final MachineControl control = new MachineControl(RedstoneMode.IGNORE);

    private boolean autoEject = true;
    private int progress = 0;
    private int storedXp = 0;
    private int status = STATUS_NO_CORE;
    private int runs = 0;
    private int pushTimer = 0;
    private boolean overfillChecked = false;

    /** Server: a reusable, never-spawned copy of the mob, used to roll its loot. */
    @Nullable
    private LivingEntity simEntity;

    /** Client: which mob to show as a hologram inside the chamber. */
    @Nullable
    private ResourceLocation clientMobId;
    @Nullable
    private Entity displayEntity;

    private final IItemHandler outputHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return OUTPUT_SLOTS;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return output.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return output.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return output.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    };

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_STATUS -> status;
                case DATA_PROGRESS -> Math.min(progress, 0x7FFF);
                case DATA_INTERVAL -> Math.min(getInterval(), 0x7FFF);
                case DATA_ENERGY_LO -> energy.getEnergyStored() & 0xFFFF;
                case DATA_ENERGY_HI -> energy.getEnergyStored() >>> 16;
                case DATA_CAPACITY_LO -> energy.getMaxEnergyStored() & 0xFFFF;
                case DATA_CAPACITY_HI -> energy.getMaxEnergyStored() >>> 16;
                case DATA_COST_LO -> getRunCost() & 0xFFFF;
                case DATA_COST_HI -> getRunCost() >>> 16;
                case DATA_CHANCE -> DataCoreItem.chance(DataCoreItem.getTier(core.getStackInSlot(0)));
                case DATA_XP_LO -> storedXp & 0xFFFF;
                case DATA_XP_HI -> storedXp >>> 16;
                case DATA_ENABLED -> control.isEnabled() ? 1 : 0;
                case DATA_REDSTONE -> control.getRedstoneMode().ordinal();
                case DATA_EJECT -> autoEject ? 1 : 0;
                case DATA_RUNS_LO -> runs & 0xFFFF;
                case DATA_RUNS_HI -> runs >>> 16;
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

    public SimulationChamberBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.SIMULATION_CHAMBER_ENTITY.get(), pos, state);
    }

    public static UpgradeItemHandler createCardHandler(Runnable onChanged) {
        return new UpgradeItemHandler(CARD_COUNT, EnumSet.copyOf(Arrays.asList(CARD_SLOTS)), onChanged).dedicated(CARD_SLOTS);
    }

    // ------------------------------------------------------------------------------------------
    // Settings derived from cards and the core
    // ------------------------------------------------------------------------------------------

    public int getInterval() {
        int base = Config.SIM_BASE_INTERVAL.get();
        int fastest = Math.min(base, Config.SIM_MIN_INTERVAL.get());
        int speed = cards.count(UpgradeType.SPEED);
        return Math.max(fastest, base - Math.round((base - fastest) * (speed / (float) UpgradeType.SPEED.maxCards())));
    }

    public Optional<EntityType<?>> getCoreMob() {
        return DataCoreItem.getMob(core.getStackInSlot(0));
    }

    /** FE per run: the base cost scaled by the mob's max health (20 health = 1x), minus Efficiency. */
    public int getRunCost() {
        Optional<EntityType<?>> mob = getCoreMob();
        if (mob.isEmpty()) {
            return 0;
        }
        double health = 20.0;
        if (level instanceof ServerLevel serverLevel) {
            LivingEntity entity = getSimEntity(serverLevel, mob.get());
            if (entity != null) {
                health = entity.getMaxHealth();
            }
        }
        double factor = Math.max(1.0, health / 20.0);
        double efficiency = Config.SIM_MAX_EFFICIENCY.get() * cards.count(UpgradeType.EFFICIENCY) / UpgradeType.EFFICIENCY.maxCards();
        return (int) Math.min(Integer.MAX_VALUE, Math.round(Config.SIM_BASE_COST.get() * factor * (1.0 - efficiency)));
    }

    private int costPerTick() {
        return (int) Math.ceil(getRunCost() / (double) Math.max(1, getInterval()));
    }

    @Nullable
    private LivingEntity getSimEntity(ServerLevel level, EntityType<?> type) {
        if (simEntity == null || simEntity.getType() != type) {
            Entity created = type.create(level);
            simEntity = created instanceof LivingEntity living ? living : null;
            if (created != null && simEntity == null) {
                created.discard();
            }
        }
        return simEntity;
    }

    // ------------------------------------------------------------------------------------------
    // Ticking
    // ------------------------------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, SimulationChamberBlockEntity be) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!be.overfillChecked) {
            be.overfillChecked = true;
            be.cards.dropExtraCards(level, pos);
        }
        if (++be.pushTimer >= PUSH_INTERVAL) {
            be.pushTimer = 0;
            if (be.autoEject) {
                be.pushOutput(serverLevel);
            }
            be.pushXpToTanks(serverLevel);
        }

        ItemStack coreStack = be.core.getStackInSlot(0);
        Optional<EntityType<?>> mob = be.getCoreMob();
        int newStatus;
        if (!be.control.isActive(level, pos)) {
            newStatus = STATUS_OFF;
        } else if (coreStack.isEmpty()) {
            newStatus = STATUS_NO_CORE;
        } else if (mob.isEmpty()) {
            newStatus = STATUS_UNBOUND;
        } else if (!DataCoreItem.canBind(mob.get()) || be.getSimEntity(serverLevel, mob.get()) == null) {
            newStatus = STATUS_BLOCKED;
        } else if (!be.hasRoom()) {
            newStatus = STATUS_FULL;
        } else if (be.energy.getEnergyStored() < be.costPerTick()) {
            newStatus = STATUS_NO_POWER;
        } else {
            newStatus = STATUS_RUNNING;
        }
        be.status = newStatus;
        MachineBlock.setActive(level, pos, state, newStatus == STATUS_RUNNING);
        if (newStatus != STATUS_RUNNING) {
            if (newStatus == STATUS_NO_CORE || newStatus == STATUS_UNBOUND || newStatus == STATUS_OFF) {
                be.progress = 0;
            }
            return;
        }
        be.energy.use(be.costPerTick());
        be.setChanged();
        if (++be.progress >= be.getInterval()) {
            be.progress = 0;
            be.simulate(serverLevel, mob.get());
        }
    }

    /** One simulated kill: success roll by core tier, then the mob's real loot table and XP. */
    private void simulate(ServerLevel level, EntityType<?> type) {
        LivingEntity entity = getSimEntity(level, type);
        if (entity == null) {
            return;
        }
        ItemStack coreStack = core.getStackInSlot(0);
        int chance = DataCoreItem.chance(DataCoreItem.getTier(coreStack));
        if (level.random.nextInt(100) >= chance) {
            return; // Failed run: the power is spent, nothing drops.
        }
        runs++;
        FakePlayer player = FakePlayerFactory.get(level, ReaperBlockEntity.FAKE_PLAYER_PROFILE);
        Vec3 center = Vec3.atCenterOf(worldPosition);
        player.setPos(center.x, center.y, center.z);
        entity.setPos(center.x, center.y + 1.0, center.z);
        ItemStack weapon = new ItemStack(Items.NETHERITE_SWORD);
        int looting = cards.count(UpgradeType.LOOTING);
        if (looting > 0) {
            weapon.enchant(level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.LOOTING), looting);
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, weapon);
        DamageSource source = level.damageSources().playerAttack(player);

        // Player Kill card: the run counts as a player kill, so player-only drops (wither skeleton
        // skulls, blaze rods, rare zombie drops, ...) and XP drop. Without it, only the drops any death gives.
        boolean playerKill = cards.count(UpgradeType.PLAYER_KILL) > 0;
        // A fresh copy of the mob for every run runs its real death code: loot table, special
        // drops, equipment and drops added by other mods. It is never added to the world.
        Entity created = type.create(level);
        if (created instanceof LivingEntity victim) {
            victim.setPos(center.x, center.y + 1.0, center.z);
            victim.lastHurtByPlayer = playerKill ? player : null;
            victim.lastHurtByPlayerTime = playerKill ? 100 : 0;
            victim.skipDropExperience();
            List<ItemStack> drops = SimDrops.capture(victim, () -> SimDrops.dropAllDeathLoot(victim, level, source));
            // If the output fills up mid-run, the rest of that run's drops are lost (never spilled into the world).
            for (ItemStack stack : drops) {
                ItemHandlerHelper.insertItemStacked(output, stack, false);
            }
            victim.discard();
        } else if (created != null) {
            created.discard();
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

        int xp = playerKill ? entity.getExperienceReward(level, player) : 0;
        if (xp > 0) {
            storedXp = (int) Math.min(Integer.MAX_VALUE, (long) storedXp + xp);
        }

        int dataPerRun = Config.SIM_DATA_PER_RUN.get();
        if (dataPerRun > 0 && DataCoreItem.getTier(coreStack) < DataCoreItem.MAX_TIER) {
            ItemStack updated = coreStack.copy();
            DataCoreItem.addData(updated, dataPerRun);
            core.setStackInSlot(0, updated);
        }
        level.playSound(null, worldPosition, SoundEvents.AMETHYST_CLUSTER_STEP, SoundSource.BLOCKS, 0.4F, 1.6F);
        setChanged();
    }

    /** Room for at least one more run's drops: any empty output slot. */
    private boolean hasRoom() {
        for (int i = 0; i < output.getSlots(); i++) {
            if (output.getStackInSlot(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void pushOutput(ServerLevel level) {
        for (Direction dir : Direction.values()) {
            IItemHandler target = level.getCapability(Capabilities.ItemHandler.BLOCK, worldPosition.relative(dir), dir.getOpposite());
            if (target == null) {
                continue;
            }
            for (int i = 0; i < output.getSlots(); i++) {
                ItemStack stack = output.getStackInSlot(i);
                if (stack.isEmpty()) {
                    continue;
                }
                ItemStack left = ItemHandlerHelper.insertItemStacked(target, stack.copy(), false);
                if (left.getCount() != stack.getCount()) {
                    output.setStackInSlot(i, left);
                }
            }
        }
    }

    private void pushXpToTanks(ServerLevel level) {
        if (storedXp <= 0) {
            return;
        }
        for (Direction dir : Direction.values()) {
            if (level.getBlockEntity(worldPosition.relative(dir)) instanceof XpTankBlockEntity tank) {
                int before = storedXp;
                storedXp = tank.addXp(storedXp);
                if (storedXp != before) {
                    setChanged();
                }
                if (storedXp <= 0) {
                    return;
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------
    // GUI
    // ------------------------------------------------------------------------------------------

    public void handleButton(int id, Player player) {
        switch (id) {
            case BTN_POWER -> control.togglePower();
            case BTN_REDSTONE -> control.cycleRedstone();
            case BTN_EJECT -> autoEject = !autoEject;
            case BTN_TAKE_XP -> {
                if (storedXp > 0) {
                    player.giveExperiencePoints(storedXp);
                    storedXp = 0;
                }
            }
            default -> {
                return;
            }
        }
        setChanged();
    }

    public ItemStackHandler getCore() {
        return core;
    }

    public UpgradeItemHandler getCards() {
        return cards;
    }

    public ItemStackHandler getOutput() {
        return output;
    }

    public IItemHandler getOutputHandler() {
        return outputHandler;
    }

    public IEnergyStorage getEnergyHandler() {
        return energy;
    }

    public ContainerData getDataAccess() {
        return dataAccess;
    }

    public void dropContents(Level level, BlockPos pos) {
        for (ItemStackHandler handler : new ItemStackHandler[] {core, cards, output}) {
            for (int i = 0; i < handler.getSlots(); i++) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), handler.getStackInSlot(i).copy());
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.grimsoul.simulation_chamber");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new SimulationMenu(containerId, playerInventory, this);
    }

    // ------------------------------------------------------------------------------------------
    // Client hologram
    // ------------------------------------------------------------------------------------------

    private void syncToClient() {
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    @Nullable
    public Entity getDisplayEntity(Level level) {
        if (clientMobId == null) {
            displayEntity = null;
            return null;
        }
        if (displayEntity == null || !BuiltInRegistries.ENTITY_TYPE.getKey(displayEntity.getType()).equals(clientMobId)) {
            displayEntity = BuiltInRegistries.ENTITY_TYPE.getOptional(clientMobId).map(t -> (Entity) t.create(level)).orElse(null);
        }
        return displayEntity;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        getCoreMob().ifPresent(type -> tag.putString("Mob", BuiltInRegistries.ENTITY_TYPE.getKey(type).toString()));
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // ------------------------------------------------------------------------------------------
    // Saving
    // ------------------------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Core", core.serializeNBT(registries));
        tag.put("Cards", cards.serializeNBT(registries));
        tag.put("Output", output.serializeNBT(registries));
        tag.putInt("Energy", energy.getEnergyStored());
        tag.putInt("Progress", progress);
        tag.putInt("Xp", storedXp);
        tag.putInt("Runs", runs);
        tag.putBoolean("AutoEject", autoEject);
        control.save(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Core")) {
            core.deserializeNBT(registries, tag.getCompound("Core"));
        }
        if (tag.contains("Cards")) {
            // Read into a temporary handler, then copy each card into its own slot. Older saves had
            // fewer card slots, and loading them straight in would shrink the handler.
            ItemStackHandler saved = new ItemStackHandler();
            saved.deserializeNBT(registries, tag.getCompound("Cards"));
            for (int i = 0; i < cards.getSlots(); i++) {
                cards.setStackInSlot(i, ItemStack.EMPTY);
            }
            for (int i = 0; i < saved.getSlots(); i++) {
                ItemStack stack = saved.getStackInSlot(i);
                if (stack.getItem() instanceof com.xtri6.grimsoul.item.UpgradeCardItem card) {
                    int slot = cards.slotFor(card.getUpgradeType());
                    if (slot >= 0) {
                        cards.setStackInSlot(slot, stack);
                    }
                }
            }
        }
        if (tag.contains("Output")) {
            output.deserializeNBT(registries, tag.getCompound("Output"));
        }
        energy.set(tag.getInt("Energy"));
        progress = tag.getInt("Progress");
        storedXp = tag.getInt("Xp");
        runs = tag.getInt("Runs");
        if (tag.contains("AutoEject")) {
            autoEject = tag.getBoolean("AutoEject");
        }
        control.load(tag);
        clientMobId = tag.contains("Mob") ? ResourceLocation.tryParse(tag.getString("Mob")) : null;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        clientMobId = tag.contains("Mob") ? ResourceLocation.tryParse(tag.getString("Mob")) : null;
    }

    private class ChamberEnergy extends EnergyStorage {
        ChamberEnergy(int capacity) {
            super(capacity, Integer.MAX_VALUE, 0);
        }

        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            int received = super.receiveEnergy(toReceive, simulate);
            if (received > 0 && !simulate) {
                setChanged();
            }
            return received;
        }

        void use(int amount) {
            energy = Math.max(0, energy - amount);
        }

        void set(int amount) {
            energy = Math.max(0, Math.min(capacity, amount));
        }
    }
}
