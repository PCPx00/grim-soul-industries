package com.xtri6.grimsoul.blockentity;

import java.util.EnumSet;
import java.util.Optional;

import javax.annotation.Nullable;

import com.xtri6.grimsoul.Config;
import com.xtri6.grimsoul.Registration;
import com.xtri6.grimsoul.block.MachineBlock;
import com.xtri6.grimsoul.item.ReapersLanternItem;
import com.xtri6.grimsoul.item.UpgradeType;
import com.xtri6.grimsoul.machine.MachineControl;
import com.xtri6.grimsoul.machine.RedstoneMode;
import com.xtri6.grimsoul.menu.SpawnerMenu;
import com.xtri6.grimsoul.menu.UpgradeItemHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Essence Spawner: spawns the mob stored in the Reaper's Lantern placed in its GUI. Cards change
 * how fast (Speed), how many (Count), how far (Range), and which spawn rules it ignores.
 * It pauses when its own mob count nearby reaches the entity cap.
 */
public class EssenceSpawnerBlockEntity extends BlockEntity implements MenuProvider {
    public static final UpgradeType[] CARD_SLOTS = {
            UpgradeType.SPEED, UpgradeType.COUNT, UpgradeType.RANGE,
            UpgradeType.NO_PLAYER_NEEDED, UpgradeType.IGNORE_LIGHT, UpgradeType.IGNORE_SPAWN_CONDITIONS};
    public static final int CARD_COUNT = CARD_SLOTS.length;
    private static final int TRIES_PER_MOB = 4;

    // Status codes shown in the GUI
    public static final int STATUS_RUNNING = 0, STATUS_NO_LANTERN = 1, STATUS_EMPTY_LANTERN = 2, STATUS_NO_PLAYER = 3,
            STATUS_CAP = 4, STATUS_OFF = 5, STATUS_NO_SPACE = 6, STATUS_NO_POWER = 7;

    public static final int BTN_SHOW_AREA = 0, BTN_POWER = 1, BTN_REDSTONE = 2;
    public static final int DATA_STATUS = 0, DATA_INTERVAL = 1, DATA_PROGRESS = 2, DATA_PER_SPAWN = 3, DATA_RADIUS = 4,
            DATA_NEARBY = 5, DATA_CAP = 6, DATA_SHOW_AREA = 7, DATA_ENABLED = 8, DATA_REDSTONE = 9,
            DATA_ENERGY_LO = 10, DATA_ENERGY_HI = 11, DATA_CAPACITY_LO = 12, DATA_CAPACITY_HI = 13, DATA_COST_LO = 14,
            DATA_COST_HI = 15, DATA_COUNT = 16;

    private final ItemStackHandler lantern = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof ReapersLanternItem;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            onSetupChanged();
        }
    };
    private final UpgradeItemHandler cards = createCardHandler(this::onSetupChanged);
    private boolean overfillChecked = false;
    private final MachineControl control = new MachineControl(RedstoneMode.IGNORE);
    /** FE buffer, filled by the Reaper Generator, cables or any FE mod. Machines can't pull power back out. */
    private final SpawnerEnergy energy = new SpawnerEnergy(Config.SPAWNER_ENERGY_CAPACITY.get());

    private boolean showArea = false;
    private int timer = 0;
    private int status = STATUS_NO_LANTERN;
    private int nearby = 0;

    /** Client only: the captured mob's id and a cached copy of it for the spinning display. */
    @Nullable
    private ResourceLocation clientMobId;
    @Nullable
    private Entity displayEntity;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_STATUS -> status;
                case DATA_INTERVAL -> getInterval();
                case DATA_PROGRESS -> timer;
                case DATA_PER_SPAWN -> getPerSpawn();
                case DATA_RADIUS -> getRadius();
                case DATA_NEARBY -> nearby;
                case DATA_CAP -> Config.SPAWNER_ENTITY_CAP.get();
                case DATA_SHOW_AREA -> showArea ? 1 : 0;
                case DATA_ENABLED -> control.isEnabled() ? 1 : 0;
                case DATA_REDSTONE -> control.getRedstoneMode().ordinal();
                case DATA_ENERGY_LO -> energy.getEnergyStored() & 0xFFFF;
                case DATA_ENERGY_HI -> energy.getEnergyStored() >>> 16;
                case DATA_CAPACITY_LO -> energy.getMaxEnergyStored() & 0xFFFF;
                case DATA_CAPACITY_HI -> energy.getMaxEnergyStored() >>> 16;
                case DATA_COST_LO -> spawnCost() & 0xFFFF;
                case DATA_COST_HI -> spawnCost() >>> 16;
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

    public EssenceSpawnerBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.ESSENCE_SPAWNER_ENTITY.get(), pos, state);
    }

    public static UpgradeItemHandler createCardHandler(Runnable onChanged) {
        return new UpgradeItemHandler(CARD_COUNT, EnumSet.copyOf(java.util.Arrays.asList(CARD_SLOTS)), onChanged).dedicated(CARD_SLOTS);
    }

    // ------------------------------------------------------------------------------------------
    // Settings derived from cards
    // ------------------------------------------------------------------------------------------

    public int getInterval() {
        int base = Config.SPAWNER_BASE_INTERVAL.get();
        int fastest = Math.min(base, Config.SPAWNER_MIN_INTERVAL.get());
        int cards = this.cards.count(UpgradeType.SPEED);
        int interval = base - Math.round((base - fastest) * (cards / (float) UpgradeType.SPEED.maxCards()));
        return Math.max(fastest, interval);
    }

    public int getPerSpawn() {
        return 1 + cards.count(UpgradeType.COUNT);
    }

    public int getRadius() {
        return Config.SPAWNER_BASE_RADIUS.get() + cards.count(UpgradeType.RANGE);
    }

    /** Where mobs appear: around the spawner, from one block below to one block above it. */
    public AABB getSpawnArea() {
        int r = getRadius();
        return new AABB(worldPosition).inflate(r, 1, r);
    }

    public boolean isShowingArea() {
        return showArea;
    }

    public Optional<EntityType<?>> getCapturedMob() {
        return ReapersLanternItem.getCaptured(lantern.getStackInSlot(0));
    }

    // ------------------------------------------------------------------------------------------
    // Ticking
    // ------------------------------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, EssenceSpawnerBlockEntity be) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!be.overfillChecked) {
            be.overfillChecked = true;
            be.cards.dropExtraCards(level, pos);
        }
        boolean enabled = be.control.isActive(level, pos);
        Optional<EntityType<?>> mob = be.getCapturedMob();
        int interval = be.getInterval();
        boolean charged = be.timer >= interval;
        if (!enabled) {
            be.status = STATUS_OFF;
        } else if (be.lantern.getStackInSlot(0).isEmpty()) {
            be.status = STATUS_NO_LANTERN;
        } else if (mob.isEmpty()) {
            be.status = STATUS_EMPTY_LANTERN;
        } else if (be.cards.count(UpgradeType.NO_PLAYER_NEEDED) <= 0
                && !level.hasNearbyAlivePlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, Config.SPAWNER_PLAYER_RANGE.get())) {
            be.status = STATUS_NO_PLAYER;
        } else if (!charged && !be.hasPowerFor(be.chargePerTick())) {
            be.status = STATUS_NO_POWER;
        } else if (be.status == STATUS_CAP || be.status == STATUS_NO_SPACE) {
            // Keep showing the last spawn result until the next attempt.
        } else {
            be.status = STATUS_RUNNING;
        }
        boolean running = be.status == STATUS_RUNNING || be.status == STATUS_CAP || be.status == STATUS_NO_SPACE;
        MachineBlock.setActive(level, pos, state, running);
        if (!running) {
            // The charge built so far is kept, so no power is wasted while it waits.
            return;
        }
        if (!charged) {
            // Power is fed in a little every tick while the spawn timer fills, until the full
            // spawn cost has been paid; then the mobs come out.
            be.usePower(be.chargePerTick());
            be.timer++;
            be.setChanged();
            if (be.timer < interval) {
                return;
            }
        } else if (be.status != STATUS_RUNNING && level.getGameTime() % 20 != 0) {
            return; // Fully charged but blocked (cap or no room): retry once a second.
        }
        if (be.trySpawn(serverLevel, mob.get())) {
            be.timer = 0;
        } else {
            be.timer = interval; // Stay charged and try again soon.
        }
    }

    /** Returns true if at least one mob spawned. */
    private boolean trySpawn(ServerLevel level, EntityType<?> type) {
        AABB area = getSpawnArea();
        nearby = level.getEntitiesOfClass(Entity.class, area.inflate(1.0), e -> e.getType() == type && e.isAlive()).size();
        int cap = Config.SPAWNER_ENTITY_CAP.get();
        if (nearby >= cap) {
            status = STATUS_CAP;
            return false;
        }
        boolean ignoreRules = cards.count(UpgradeType.IGNORE_SPAWN_CONDITIONS) > 0;
        boolean ignoreLight = cards.count(UpgradeType.IGNORE_LIGHT) > 0;
        RandomSource random = level.getRandom();
        int radius = getRadius();
        int toSpawn = Math.min(getPerSpawn(), cap - nearby);
        int spawned = 0;

        for (int i = 0; i < toSpawn; i++) {
            for (int attempt = 0; attempt < TRIES_PER_MOB; attempt++) {
                double x = worldPosition.getX() + 0.5 + (random.nextDouble() * 2.0 - 1.0) * radius;
                double y = worldPosition.getY() + random.nextInt(3) - 1;
                double z = worldPosition.getZ() + 0.5 + (random.nextDouble() * 2.0 - 1.0) * radius;
                if (spawnOne(level, type, x, y, z, ignoreRules, ignoreLight)) {
                    spawned++;
                    break;
                }
            }
        }
        if (spawned > 0) {
            nearby += spawned;
            status = STATUS_RUNNING;
            level.levelEvent(2004, worldPosition, 0); // Vanilla spawner flame puff
            return true;
        }
        status = STATUS_NO_SPACE;
        return false;
    }

    // ------------------------------------------------------------------------------------------
    // Power
    // ------------------------------------------------------------------------------------------

    private static boolean requiresPower() {
        return Config.SPAWNER_REQUIRE_POWER.get();
    }

    /** Total FE one spawn costs: a base amount plus a share for every mob it spawns. */
    public int spawnCost() {
        if (!requiresPower()) {
            return 0;
        }
        long cost = Config.SPAWNER_ENERGY_PER_SPAWN.get() + (long) Config.SPAWNER_ENERGY_PER_MOB.get() * getPerSpawn();
        return (int) Math.min(Integer.MAX_VALUE, cost);
    }

    /** The spawn cost spread across the spawn timer. */
    private int chargePerTick() {
        return (int) Math.ceil(spawnCost() / (double) Math.max(1, getInterval()));
    }

    private boolean hasPowerFor(int amount) {
        return !requiresPower() || (energy.getEnergyStored() > 0 && energy.getEnergyStored() >= amount);
    }

    private void usePower(int amount) {
        if (amount > 0) {
            energy.use(amount);
            setChanged();
        }
    }

    /** For cables, Pipez, the Reaper Generator and other FE mods: power goes in from any side. */
    public IEnergyStorage getEnergyHandler() {
        return energy;
    }

    private class SpawnerEnergy extends EnergyStorage {
        SpawnerEnergy(int capacity) {
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

    private boolean spawnOne(ServerLevel level, EntityType<?> type, double x, double y, double z, boolean ignoreRules, boolean ignoreLight) {
        BlockPos at = BlockPos.containing(x, y, z);
        if (!level.noCollision(type.getSpawnAABB(x, y, z))) {
            return false;
        }
        Entity entity = type.create(level);
        if (entity == null) {
            return false;
        }
        entity.moveTo(x, y, z, level.getRandom().nextFloat() * 360.0F, 0.0F);
        if (entity instanceof Mob mob) {
            if (ignoreRules) {
                // Only needs room to stand.
            } else if (ignoreLight) {
                // Needs a solid block to stand on and room, but any light level.
                BlockPos below = at.below();
                if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP) || !mob.checkSpawnObstruction(level)) {
                    entity.discard();
                    return false;
                }
            } else if (!EventHooks.checkSpawnPosition(mob, level, MobSpawnType.SPAWNER)) {
                entity.discard();
                return false;
            }
            EventHooks.finalizeMobSpawn(mob, level, level.getCurrentDifficultyAt(at), MobSpawnType.SPAWNER, null);
            mob.spawnAnim();
        }
        return level.tryAddFreshEntityWithPassengers(entity);
    }

    // ------------------------------------------------------------------------------------------
    // GUI and sync
    // ------------------------------------------------------------------------------------------

    private void onSetupChanged() {
        if (status == STATUS_CAP || status == STATUS_NO_SPACE) {
            status = STATUS_RUNNING;
        }
        setChanged();
        syncToClient();
    }

    public void handleButton(int id, Player player) {
        switch (id) {
            case BTN_SHOW_AREA -> showArea = !showArea;
            case BTN_POWER -> control.togglePower();
            case BTN_REDSTONE -> control.cycleRedstone();
            default -> {
                return;
            }
        }
        setChanged();
        syncToClient();
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    public ItemStackHandler getLantern() {
        return lantern;
    }

    public UpgradeItemHandler getCards() {
        return cards;
    }

    public ContainerData getDataAccess() {
        return dataAccess;
    }

    public void dropContents(Level level, BlockPos pos) {
        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), lantern.getStackInSlot(0).copy());
        for (int i = 0; i < cards.getSlots(); i++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), cards.getStackInSlot(i).copy());
        }
    }

    /** Client side: a cached copy of the captured mob for the spinning display inside the cage. */
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
    public Component getDisplayName() {
        return Component.translatable("block.grimsoul.essence_spawner");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new SpawnerMenu(containerId, playerInventory, this);
    }

    private void writeClientData(CompoundTag tag) {
        tag.putBoolean("ShowArea", showArea);
        tag.putInt("Radius", getRadius());
        getCapturedMob().ifPresent(type -> tag.putString("Mob", BuiltInRegistries.ENTITY_TYPE.getKey(type).toString()));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Lantern", lantern.serializeNBT(registries));
        tag.put("Cards", cards.serializeNBT(registries));
        tag.putBoolean("ShowArea", showArea);
        tag.putInt("Energy", energy.getEnergyStored());
        control.save(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Lantern")) {
            lantern.deserializeNBT(registries, tag.getCompound("Lantern"));
        }
        if (tag.contains("Cards")) {
            cards.deserializeNBT(registries, tag.getCompound("Cards"));
        }
        showArea = tag.getBoolean("ShowArea");
        if (tag.contains("Energy")) {
            energy.set(tag.getInt("Energy"));
        }
        control.load(tag);
        // Client sync data
        clientMobId = tag.contains("Mob") ? ResourceLocation.tryParse(tag.getString("Mob")) : null;
        if (tag.contains("Radius")) {
            clientRadius = tag.getInt("Radius");
        }
    }

    /** Client copy of the radius (the client does not see the card slots outside the GUI). */
    private int clientRadius = 4;

    public AABB getClientSpawnArea() {
        return new AABB(worldPosition).inflate(clientRadius, 1, clientRadius);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        writeClientData(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
