package com.xtri6.grimsoul.blockentity;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import com.mojang.authlib.GameProfile;
import com.xtri6.grimsoul.Config;
import com.xtri6.grimsoul.Registration;
import com.xtri6.grimsoul.block.MachineBlock;
import com.xtri6.grimsoul.item.UpgradeCardItem;
import com.xtri6.grimsoul.item.UpgradeType;
import com.xtri6.grimsoul.machine.MachineControl;
import com.xtri6.grimsoul.machine.RedstoneMode;
import com.xtri6.grimsoul.machine.TargetMode;
import com.xtri6.grimsoul.menu.ReaperMenu;
import com.xtri6.grimsoul.menu.UpgradeItemHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.item.ItemEntity;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * The Reaper Block: shoots lasers at entities inside its work area, then pulls the drops and XP
 * from that same area into its own inventory, filtering and voiding as set in its GUI.
 * The work area grows with Range cards (Auto) or can be set by hand. Pipes, AE2 and other
 * storage mods can pull items out of any side.
 */
public class ReaperBlockEntity extends BlockEntity implements MenuProvider {
    public static final int MAX_SLOTS = 54;
    public static final int BASE_SLOTS = 9;
    public static final int SLOTS_PER_STORAGE_CARD = 9;
    public static final int UPGRADE_SLOTS = 10;
    public static final int FILTER_SLOTS = 9;
    public static final int BASE_MAX_SIZE = 3;
    public static final int BASE_MAX_OFFSET = 3;
    public static final int LIMIT_PER_RANGE_CARD = 2;
    private static final int EJECT_STACKS_PER_SWEEP = 4;

    public static final GameProfile FAKE_PLAYER_PROFILE =
            new GameProfile(UUID.fromString("5f1c0b7e-2d4a-4c6e-9b1a-7e3f0a9d2c41"), "[GrimSoul]");

    /** The Reaper Block's card slots, in GUI order. */
    public static final UpgradeType[] CARD_SLOTS = {
            UpgradeType.SPEED, UpgradeType.RANGE, UpgradeType.DAMAGE, UpgradeType.LOOTING, UpgradeType.PLAYER_KILL,
            UpgradeType.STORAGE, UpgradeType.VOID, UpgradeType.FIRE_ASPECT, UpgradeType.SMITE, UpgradeType.BANE_OF_ARTHROPODS};
    public static final EnumSet<UpgradeType> ACCEPTED_CARDS = EnumSet.copyOf(java.util.Arrays.asList(CARD_SLOTS));

    /** One slot per card type, in UpgradeType order. Shared by the block and the client menu. */
    public static UpgradeItemHandler createUpgradeHandler(Runnable onChanged) {
        return new UpgradeItemHandler(UPGRADE_SLOTS, ACCEPTED_CARDS, onChanged).dedicated(CARD_SLOTS);
    }

    /** Block event id: the server tells clients which mob a laser just hit (param = entity id). */
    public static final int EVENT_LASER = 1;
    /** How long a laser beam stays visible, in ticks. */
    public static final int BEAM_TICKS = 8;
    private static final int MAX_BEAMS_PER_CYCLE = 8;
    private static final float SMITE_BANE_DAMAGE_PER_LEVEL = 2.5F;

    // ---- Button ids sent from the GUI (see ReaperMenu#clickMenuButton) ----
    public static final int BTN_OFFSET_BASE = 0;   // 0..5: offset X-,X+,Y-,Y+,Z-,Z+
    public static final int BTN_SIZE_BASE = 6;     // 6..11: size X-,X+,Y-,Y+,Z-,Z+
    public static final int BTN_WHITELIST = 12;
    public static final int BTN_VOID = 13;
    public static final int BTN_VOID_WHEN_FULL = 14;
    public static final int BTN_SHOW_AREA = 15;
    public static final int BTN_AUTO_EJECT = 16;
    public static final int BTN_TAKE_XP = 17;
    public static final int BTN_AUTO_AREA = 18;
    public static final int BTN_POWER = 19;
    public static final int BTN_REDSTONE = 20;
    public static final int BTN_TARGET = 21;
    public static final int BTN_MAGNET = 22;

    // ---- Synced data indices (see ContainerData below) ----
    public static final int DATA_OFF_X = 0, DATA_OFF_Y = 1, DATA_OFF_Z = 2;
    public static final int DATA_SIZE_X = 3, DATA_SIZE_Y = 4, DATA_SIZE_Z = 5;
    public static final int DATA_WHITELIST = 6, DATA_VOID = 7, DATA_VOID_WHEN_FULL = 8, DATA_SHOW_AREA = 9, DATA_AUTO_EJECT = 10;
    public static final int DATA_XP_LOW = 11, DATA_XP_HIGH = 12;
    public static final int DATA_ACTIVE_SLOTS = 13, DATA_MAX_SIZE = 14, DATA_MAX_OFFSET = 15, DATA_HAS_VOID_CARD = 16;
    public static final int DATA_AUTO_AREA = 17, DATA_ENABLED = 18, DATA_REDSTONE = 19, DATA_TARGET = 20, DATA_MAGNET = 21;
    public static final int DATA_COUNT = 22;

    private final ItemStackHandler storage = new ItemStackHandler(MAX_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final UpgradeItemHandler upgrades = createUpgradeHandler(this::onUpgradesChanged);
    private final ItemStackHandler filter = new ItemStackHandler(FILTER_SLOTS) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    /** Optional Data Core: gains data from every kill of its mob made by this block. */
    private final ItemStackHandler coreSlot = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof com.xtri6.grimsoul.item.DataCoreItem;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final IItemHandler outputHandler = new OutputOnlyHandler(storage);

    // Collection area, relative to this block, in world axes.
    private int offX, offY, offZ;
    private int sizeX = 3, sizeY = 3, sizeZ = 3;
    private boolean whitelist = false;
    private boolean voidEnabled = false;
    private boolean voidWhenFull = false;
    private boolean showArea = false;
    private boolean autoEject = false;
    /** When off, the block still kills but leaves drops and XP on the ground. */
    private boolean magnet = true;
    private int storedXp = 0;
    /** When on, the work area is sized automatically from the Range cards. */
    private boolean autoArea = true;
    // Safe defaults for a freshly placed block: hostile mobs only, and switched off until the player turns it on.
    private TargetMode targetMode = TargetMode.HOSTILE;
    private final MachineControl control = new MachineControl(RedstoneMode.IGNORE, false);

    /** Saves from older versions could hold more cards than the max; extras are popped out once. */
    private boolean overfillChecked = false;
    private int attackTimer = 0;
    private int collectTimer = 0;

    /** Client only: entity id -> game time it was hit, for drawing laser beams. */
    private final Map<Integer, Long> beamTargets = new HashMap<>();

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_OFF_X -> offX;
                case DATA_OFF_Y -> offY;
                case DATA_OFF_Z -> offZ;
                case DATA_SIZE_X -> sizeX;
                case DATA_SIZE_Y -> sizeY;
                case DATA_SIZE_Z -> sizeZ;
                case DATA_WHITELIST -> whitelist ? 1 : 0;
                case DATA_VOID -> voidEnabled ? 1 : 0;
                case DATA_VOID_WHEN_FULL -> voidWhenFull ? 1 : 0;
                case DATA_SHOW_AREA -> showArea ? 1 : 0;
                case DATA_AUTO_EJECT -> autoEject ? 1 : 0;
                case DATA_XP_LOW -> storedXp & 0xFFFF;
                case DATA_XP_HIGH -> (storedXp >>> 16) & 0xFFFF;
                case DATA_ACTIVE_SLOTS -> getActiveSlots();
                case DATA_MAX_SIZE -> getMaxSize();
                case DATA_MAX_OFFSET -> getMaxOffset();
                case DATA_HAS_VOID_CARD -> upgrades.count(UpgradeType.VOID) > 0 ? 1 : 0;
                case DATA_AUTO_AREA -> autoArea ? 1 : 0;
                case DATA_ENABLED -> control.isEnabled() ? 1 : 0;
                case DATA_REDSTONE -> control.getRedstoneMode().ordinal();
                case DATA_TARGET -> targetMode.ordinal();
                case DATA_MAGNET -> magnet ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Server-side data is read-only; the client uses its own SimpleContainerData copy.
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public ReaperBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.REAPER_BLOCK_ENTITY.get(), pos, state);
        // Default work area: the 3x3x3 space in front of the block.
        fitAreaToFront(state.getValue(BlockStateProperties.HORIZONTAL_FACING));
    }

    // ------------------------------------------------------------------------------------------
    // Ticking
    // ------------------------------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, ReaperBlockEntity be) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!be.overfillChecked) {
            be.overfillChecked = true;
            be.upgrades.dropExtraCards(level, pos);
        }
        boolean active = be.control.isActive(level, pos);
        MachineBlock.setActive(level, pos, state, active);
        if (!active) {
            return;
        }
        if (++be.collectTimer >= Config.REAPER_COLLECT_INTERVAL.get()) {
            be.collectTimer = 0;
            if (be.magnet) {
                be.collect(serverLevel);
            }
            if (be.autoEject) {
                be.eject(serverLevel, state);
            }
            be.pushXpToTanks(serverLevel);
        }
        if (++be.attackTimer >= be.getAttackInterval()) {
            be.attackTimer = 0;
            be.attack(serverLevel, state);
        }
    }

    private int getAttackInterval() {
        // Speed cards close the gap between the base and the fastest interval evenly: max cards = fastest.
        int base = Config.REAPER_BASE_INTERVAL.get();
        int fastest = Math.min(base, Config.REAPER_MIN_INTERVAL.get());
        int cards = upgrades.count(UpgradeType.SPEED);
        int interval = base - Math.round((base - fastest) * (cards / (float) UpgradeType.SPEED.maxCards()));
        return Math.max(fastest, interval);
    }

    private void attack(ServerLevel level, BlockState state) {
        // Item cap: stop killing when there is nowhere to put the drops (unless they get voided).
        if (magnet && !hasRoom() && !(voidWhenFull && hasVoidCard())) {
            return;
        }
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, getCollectionArea(), this::canAttack);
        if (targets.isEmpty()) {
            return; // Sleep when idle: nothing else to do this cycle.
        }
        float baseDamage = (float) (Config.REAPER_BASE_DAMAGE.get()
                + upgrades.count(UpgradeType.DAMAGE) * Config.REAPER_DAMAGE_PER_CARD.get());
        int smite = upgrades.count(UpgradeType.SMITE);
        int bane = upgrades.count(UpgradeType.BANE_OF_ARTHROPODS);
        int fireAspect = upgrades.count(UpgradeType.FIRE_ASPECT);
        DamageSource mobSource = createDamageSource(level);
        DamageSource playerSource = level.damageSources().generic();
        int beams = 0;
        for (LivingEntity mob : targets) {
            float damage = baseDamage;
            if (smite > 0 && mob.getType().is(EntityTypeTags.SENSITIVE_TO_SMITE)) {
                damage += smite * SMITE_BANE_DAMAGE_PER_LEVEL;
            }
            if (bane > 0 && mob.getType().is(EntityTypeTags.SENSITIVE_TO_BANE_OF_ARTHROPODS)) {
                damage += bane * SMITE_BANE_DAMAGE_PER_LEVEL;
            }
            // Fire Aspect: 4 seconds of fire per level, like the enchantment. Burning mobs drop cooked meat.
            if (fireAspect > 0) {
                mob.igniteForSeconds(4.0F * fireAspect);
            }
            mob.invulnerableTime = 0;
            mob.hurt(mob instanceof Player ? playerSource : mobSource, damage);
            if (mob.isDeadOrDying()) {
                creditCore(mob.getType());
            }
            if (beams < MAX_BEAMS_PER_CYCLE) {
                level.blockEvent(worldPosition, state.getBlock(), EVENT_LASER, mob.getId());
                beams++;
            }
        }
        level.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.35F, 1.8F);
    }

    /** Adds one kill of data to the Data Core in this block, if it's bound to that mob. */
    private void creditCore(net.minecraft.world.entity.EntityType<?> type) {
        ItemStack stack = coreSlot.getStackInSlot(0);
        if (com.xtri6.grimsoul.item.DataCoreItem.isBoundTo(stack, type)) {
            ItemStack updated = stack.copy();
            com.xtri6.grimsoul.item.DataCoreItem.addData(updated, 1);
            coreSlot.setStackInSlot(0, updated);
        }
    }

    /** Client side: remember which mob the laser hit so the renderer can draw the beam, and spark at it. */
    @Override
    public boolean triggerEvent(int id, int param) {
        if (id != EVENT_LASER) {
            return super.triggerEvent(id, param);
        }
        if (level != null && level.isClientSide) {
            beamTargets.put(param, level.getGameTime());
            Entity target = level.getEntity(param);
            if (target != null) {
                for (int i = 0; i < 4; i++) {
                    level.addParticle(ParticleTypes.WITCH,
                            target.getRandomX(0.5), target.getRandomY(), target.getRandomZ(0.5), 0.0, 0.0, 0.0);
                }
            }
        }
        return true;
    }

    /** Client side: live beams (entity id -> time of hit). Old entries are dropped by the renderer. */
    public Map<Integer, Long> getBeamTargets() {
        return beamTargets;
    }

    private DamageSource createDamageSource(ServerLevel level) {
        if (upgrades.count(UpgradeType.PLAYER_KILL) <= 0) {
            return level.damageSources().generic();
        }
        // A fake player makes kills count as player kills: XP drops, player-only loot and Looting.
        FakePlayer player = FakePlayerFactory.get(level, FAKE_PLAYER_PROFILE);
        Vec3 center = Vec3.atCenterOf(worldPosition);
        player.setPos(center.x, center.y, center.z);
        ItemStack weapon = new ItemStack(Items.NETHERITE_SWORD);
        int looting = upgrades.count(UpgradeType.LOOTING);
        if (looting > 0) {
            weapon.enchant(level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.LOOTING), looting);
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, weapon);
        return level.damageSources().playerAttack(player);
    }

    /** Every living thing counts, bosses included, filtered by the GUI's target mode. */
    private boolean canAttack(LivingEntity entity) {
        if (!entity.isAlive() || entity instanceof ArmorStand || entity instanceof FakePlayer
                || entity.getType().is(Registration.REAPER_IMMUNE)) {
            return false;
        }
        if (entity instanceof Player player) {
            if (player.isCreative() || player.isSpectator()) {
                return false;
            }
            return targetMode == TargetMode.ALL || targetMode == TargetMode.PLAYERS;
        }
        boolean hostile = entity instanceof Enemy;
        boolean matches = switch (targetMode) {
            case ALL -> true;
            case PLAYERS -> false;
            case FRIENDLY -> !hostile;
            case HOSTILE -> hostile;
        };
        if (!matches) {
            return false;
        }
        if (!Config.REAPER_KILL_NAMED.get() && entity.hasCustomName()) {
            return false;
        }
        if (!Config.REAPER_KILL_PETS.get() && entity instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null) {
            return false;
        }
        return true;
    }

    private void collect(ServerLevel level) {
        AABB area = getCollectionArea();
        boolean changed = false;

        for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, area, e -> e.isAlive() && !e.getItem().isEmpty())) {
            ItemStack stack = itemEntity.getItem();
            if (!passesFilter(stack)) {
                if (canVoid()) {
                    itemEntity.discard();
                }
                continue;
            }
            ItemStack remainder = insertIntoActiveSlots(stack.copy());
            if (remainder.isEmpty() || (voidWhenFull && hasVoidCard())) {
                itemEntity.discard();
            } else {
                itemEntity.setItem(remainder);
            }
            changed = true;
        }

        for (ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class, area, ExperienceOrb::isAlive)) {
            addXp(orbTotal(orb));
            orb.discard();
            changed = true;
        }

        if (changed) {
            setChanged();
        }
    }

    /** Merged orbs carry a count; it is only exposed through the orb's saved data. */
    private static int orbTotal(ExperienceOrb orb) {
        CompoundTag tag = orb.saveWithoutId(new CompoundTag());
        int count = Math.max(1, tag.getInt("Count"));
        long total = (long) orb.getValue() * count;
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    private void addXp(int amount) {
        storedXp = (int) Math.min(Integer.MAX_VALUE, (long) storedXp + amount);
    }

    private void eject(ServerLevel level, BlockState state) {
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        Direction back = facing.getOpposite();
        IItemHandler target = level.getCapability(Capabilities.ItemHandler.BLOCK, worldPosition.relative(back), facing);
        if (target == null) {
            return;
        }
        int moved = 0;
        for (int i = 0; i < storage.getSlots() && moved < EJECT_STACKS_PER_SWEEP; i++) {
            ItemStack simulated = storage.extractItem(i, 64, true);
            if (simulated.isEmpty()) {
                continue;
            }
            ItemStack notAccepted = ItemHandlerHelper.insertItem(target, simulated, true);
            int amount = simulated.getCount() - notAccepted.getCount();
            if (amount > 0) {
                ItemStack extracted = storage.extractItem(i, amount, false);
                ItemStack leftover = ItemHandlerHelper.insertItem(target, extracted, false);
                if (!leftover.isEmpty()) {
                    storage.insertItem(i, leftover, false);
                }
                moved++;
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
    // Inventory, filter and void helpers
    // ------------------------------------------------------------------------------------------

    public int getActiveSlots() {
        return Math.min(MAX_SLOTS, BASE_SLOTS + upgrades.count(UpgradeType.STORAGE) * SLOTS_PER_STORAGE_CARD);
    }

    private boolean hasRoom() {
        int active = getActiveSlots();
        for (int i = 0; i < active; i++) {
            ItemStack stack = storage.getStackInSlot(i);
            if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    /** Fills existing stacks first, then empty slots, but only inside the unlocked slots. */
    private ItemStack insertIntoActiveSlots(ItemStack stack) {
        int active = getActiveSlots();
        for (int pass = 0; pass < 2 && !stack.isEmpty(); pass++) {
            for (int i = 0; i < active && !stack.isEmpty(); i++) {
                boolean slotEmpty = storage.getStackInSlot(i).isEmpty();
                if ((pass == 0) != slotEmpty) {
                    stack = storage.insertItem(i, stack, false);
                }
            }
        }
        return stack;
    }

    private boolean passesFilter(ItemStack stack) {
        boolean listed = false;
        for (int i = 0; i < filter.getSlots(); i++) {
            ItemStack entry = filter.getStackInSlot(i);
            if (!entry.isEmpty() && ItemStack.isSameItem(entry, stack)) {
                listed = true;
                break;
            }
        }
        return whitelist == listed;
    }

    private boolean hasVoidCard() {
        return upgrades.count(UpgradeType.VOID) > 0;
    }

    private boolean canVoid() {
        return voidEnabled && hasVoidCard();
    }

    // ------------------------------------------------------------------------------------------
    // Areas
    // ------------------------------------------------------------------------------------------

    public int getMaxSize() {
        return BASE_MAX_SIZE + upgrades.count(UpgradeType.RANGE) * LIMIT_PER_RANGE_CARD;
    }

    public int getMaxOffset() {
        return BASE_MAX_OFFSET + upgrades.count(UpgradeType.RANGE) * LIMIT_PER_RANGE_CARD;
    }

    /** Auto work-area height: 3, 5, 7... blocks (always odd so the area stays centered). */
    private static int autoHeight(int range) {
        return 3 + 2 * (range / 2);
    }

    /**
     * Sizes the work area to the space in front of the block: 3x3x3 with no Range cards, and
     * 2 blocks wider and deeper per Range card (13x7x13 with 5 cards).
     */
    private void fitAreaToFront(Direction facing) {
        int range = upgrades.count(UpgradeType.RANGE);
        int width = 3 + 2 * range;
        int depth = 3 + 2 * range;
        int height = autoHeight(range);
        int forward = (depth + 1) / 2;
        offX = facing.getStepX() * forward;
        offZ = facing.getStepZ() * forward;
        offY = (height - 1) / 2;
        boolean alongX = facing.getAxis() == Direction.Axis.X;
        sizeX = alongX ? depth : width;
        sizeZ = alongX ? width : depth;
        sizeY = height;
    }

    private Direction getFacing() {
        return getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
    }

    public AABB getCollectionArea() {
        BlockPos center = worldPosition.offset(offX, offY, offZ);
        return new AABB(center).inflate((sizeX - 1) / 2.0, (sizeY - 1) / 2.0, (sizeZ - 1) / 2.0);
    }

    public boolean isShowingArea() {
        return showArea;
    }

    private void clampArea() {
        int maxSize = getMaxSize();
        int maxOffset = getMaxOffset();
        sizeX = clampSize(sizeX, maxSize);
        sizeY = clampSize(sizeY, maxSize);
        sizeZ = clampSize(sizeZ, maxSize);
        offX = Mth.clamp(offX, -maxOffset, maxOffset);
        offY = Mth.clamp(offY, -maxOffset, maxOffset);
        offZ = Mth.clamp(offZ, -maxOffset, maxOffset);
    }

    /** Sizes stay odd so the area always lines up with whole blocks around its center. */
    private static int clampSize(int size, int max) {
        int clamped = Mth.clamp(size, 1, max);
        return clamped % 2 == 0 ? clamped - 1 : clamped;
    }

    // ------------------------------------------------------------------------------------------
    // GUI actions
    // ------------------------------------------------------------------------------------------

    public void handleButton(int id, Player player) {
        boolean manualAreaChange = (id >= BTN_OFFSET_BASE && id < BTN_SIZE_BASE + 6);
        if (manualAreaChange) {
            autoArea = false;
        }
        if (id >= BTN_OFFSET_BASE && id < BTN_OFFSET_BASE + 6) {
            int axis = (id - BTN_OFFSET_BASE) / 2;
            int delta = (id - BTN_OFFSET_BASE) % 2 == 0 ? -1 : 1;
            switch (axis) {
                case 0 -> offX += delta;
                case 1 -> offY += delta;
                default -> offZ += delta;
            }
        } else if (id >= BTN_SIZE_BASE && id < BTN_SIZE_BASE + 6) {
            int axis = (id - BTN_SIZE_BASE) / 2;
            int delta = (id - BTN_SIZE_BASE) % 2 == 0 ? -2 : 2;
            switch (axis) {
                case 0 -> sizeX += delta;
                case 1 -> sizeY += delta;
                default -> sizeZ += delta;
            }
        } else if (id == BTN_WHITELIST) {
            whitelist = !whitelist;
        } else if (id == BTN_VOID) {
            voidEnabled = !voidEnabled;
        } else if (id == BTN_VOID_WHEN_FULL) {
            voidWhenFull = !voidWhenFull;
        } else if (id == BTN_SHOW_AREA) {
            showArea = !showArea;
        } else if (id == BTN_AUTO_EJECT) {
            autoEject = !autoEject;
        } else if (id == BTN_MAGNET) {
            magnet = !magnet;
        } else if (id == BTN_AUTO_AREA) {
            autoArea = !autoArea;
            if (autoArea) {
                fitAreaToFront(getFacing());
            }
        } else if (id == BTN_POWER) {
            control.togglePower();
        } else if (id == BTN_REDSTONE) {
            control.cycleRedstone();
        } else if (id == BTN_TARGET) {
            targetMode = targetMode.next();
        } else if (id == BTN_TAKE_XP) {
            if (storedXp > 0) {
                player.giveExperiencePoints(storedXp);
                storedXp = 0;
            }
        }
        clampArea();
        setChanged();
        syncToClient();
    }

    private void onUpgradesChanged() {
        if (autoArea) {
            fitAreaToFront(getFacing());
        }
        clampArea();
        setChanged();
        syncToClient();
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    // ------------------------------------------------------------------------------------------
    // Accessors for the menu and capabilities
    // ------------------------------------------------------------------------------------------

    public ItemStackHandler getStorage() {
        return storage;
    }

    public UpgradeItemHandler getUpgrades() {
        return upgrades;
    }

    public ItemStackHandler getFilter() {
        return filter;
    }

    public ItemStackHandler getCoreSlot() {
        return coreSlot;
    }

    public ContainerData getDataAccess() {
        return dataAccess;
    }

    /** What pipes, hoppers and storage mods see: extract only, from any side. */
    public IItemHandler getOutputHandler(@Nullable Direction side) {
        return outputHandler;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.grimsoul.reaper_block");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ReaperMenu(containerId, playerInventory, this);
    }

    /** Drops inventory, cards and stored XP when the block is broken. */
    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < storage.getSlots(); i++) {
            net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), storage.getStackInSlot(i));
        }
        for (int i = 0; i < upgrades.getSlots(); i++) {
            net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), upgrades.getStackInSlot(i));
        }
        net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), coreSlot.getStackInSlot(0).copy());
        if (storedXp > 0 && level instanceof ServerLevel serverLevel) {
            ExperienceOrb.award(serverLevel, Vec3.atCenterOf(pos), storedXp);
            storedXp = 0;
        }
    }

    // ------------------------------------------------------------------------------------------
    // Saving and client sync
    // ------------------------------------------------------------------------------------------

    private void writeSettings(CompoundTag tag) {
        tag.putInt("OffX", offX);
        tag.putInt("OffY", offY);
        tag.putInt("OffZ", offZ);
        tag.putInt("SizeX", sizeX);
        tag.putInt("SizeY", sizeY);
        tag.putInt("SizeZ", sizeZ);
        tag.putBoolean("Whitelist", whitelist);
        tag.putBoolean("Void", voidEnabled);
        tag.putBoolean("VoidWhenFull", voidWhenFull);
        tag.putBoolean("ShowArea", showArea);
        tag.putBoolean("AutoEject", autoEject);
        tag.putBoolean("Magnet", magnet);
        tag.putBoolean("AutoArea", autoArea);
        tag.putInt("Target", targetMode.ordinal());
        control.save(tag);
    }

    private void readSettings(CompoundTag tag) {
        control.load(tag);
        if (tag.contains("Target")) {
            targetMode = TargetMode.byIndex(tag.getInt("Target"));
        }
        if (!tag.contains("SizeX")) {
            return;
        }
        offX = tag.getInt("OffX");
        offY = tag.getInt("OffY");
        offZ = tag.getInt("OffZ");
        sizeX = tag.getInt("SizeX");
        sizeY = tag.getInt("SizeY");
        sizeZ = tag.getInt("SizeZ");
        whitelist = tag.getBoolean("Whitelist");
        voidEnabled = tag.getBoolean("Void");
        voidWhenFull = tag.getBoolean("VoidWhenFull");
        showArea = tag.getBoolean("ShowArea");
        autoEject = tag.getBoolean("AutoEject");
        magnet = !tag.contains("Magnet") || tag.getBoolean("Magnet");
        // Blocks placed before Auto existed keep their hand-set area.
        autoArea = tag.contains("AutoArea") && tag.getBoolean("AutoArea");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Storage", storage.serializeNBT(registries));
        tag.put("Upgrades", upgrades.serializeNBT(registries));
        tag.put("Filter", filter.serializeNBT(registries));
        tag.put("DataCore", coreSlot.serializeNBT(registries));
        tag.putInt("Xp", storedXp);
        writeSettings(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Storage")) {
            storage.deserializeNBT(registries, tag.getCompound("Storage"));
        }
        if (tag.contains("DataCore")) {
            coreSlot.deserializeNBT(registries, tag.getCompound("DataCore"));
        }
        if (tag.contains("Upgrades")) {
            // Read into a temporary handler, then put every card into its own dedicated slot.
            // This also sorts saves from older versions, where cards could sit in any slot.
            ItemStackHandler saved = new ItemStackHandler();
            saved.deserializeNBT(registries, tag.getCompound("Upgrades"));
            ItemStack[] sorted = new ItemStack[upgrades.getSlots()];
            java.util.Arrays.fill(sorted, ItemStack.EMPTY);
            for (int i = 0; i < saved.getSlots(); i++) {
                ItemStack stack = saved.getStackInSlot(i);
                if (stack.getItem() instanceof UpgradeCardItem card) {
                    int slot = upgrades.slotFor(card.getUpgradeType());
                    if (slot >= 0) {
                        // Not capped here: anything above the max is dropped on the first tick (ejectExtraCards), so nothing is lost.
                        int count = Math.min(sorted[slot].getCount() + stack.getCount(), stack.getMaxStackSize());
                        sorted[slot] = stack.copyWithCount(count);
                    }
                }
            }
            for (int i = 0; i < sorted.length; i++) {
                upgrades.setStackInSlot(i, sorted[i]);
            }
        }
        if (tag.contains("Filter")) {
            filter.deserializeNBT(registries, tag.getCompound("Filter"));
        }
        if (tag.contains("Xp")) {
            storedXp = tag.getInt("Xp");
        }
        readSettings(tag);
    }

    /** Only the area settings go to the client (for the area outline), not the inventory. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        writeSettings(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Extract-only view of the storage, so pipes and storage mods can pull but never push in. */
    private record OutputOnlyHandler(IItemHandler inner) implements IItemHandler {
        @Override
        public int getSlots() {
            return inner.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return inner.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return inner.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return inner.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    }
}
