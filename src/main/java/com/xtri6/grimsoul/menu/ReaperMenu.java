package com.xtri6.grimsoul.menu;

import com.xtri6.grimsoul.Registration;
import com.xtri6.grimsoul.blockentity.ReaperBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Slot layout (indices):
 *   0-53   machine storage (take-out only; slots past the Storage card limit are locked)
 *   54-63  upgrade cards (two rows of five)
 *   64-72  filter (ghost slots: clicking sets a copy, the real item stays with the player)
 *   73     Data Core
 *   74-109 player inventory and hotbar
 */
public class ReaperMenu extends AbstractContainerMenu {
    public static final int STORAGE_START = 0;
    public static final int UPGRADE_START = STORAGE_START + ReaperBlockEntity.MAX_SLOTS;
    public static final int FILTER_START = UPGRADE_START + ReaperBlockEntity.UPGRADE_SLOTS;
    public static final int CORE_SLOT = FILTER_START + ReaperBlockEntity.FILTER_SLOTS;
    public static final int PLAYER_START = CORE_SLOT + 1;
    public static final int CORE_X = 322, CORE_Y = 29;
    public static final int PLAYER_END = PLAYER_START + 36;

    // Screen positions (shared with ReaperScreen).
    public static final int STORAGE_X = 8, STORAGE_Y = 18;
    public static final int PLAYER_INV_Y = 140, HOTBAR_Y = 198;
    public static final int SIDE_X = 188;
    public static final int CARD_SPACING = 22;
    public static final int UPGRADE_Y = 18;
    public static final int FILTER_Y = 68;
    public static final int UPGRADES_PER_ROW = 5;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final BlockPos pos;

    /** Client constructor, called when the server opens the menu. */
    public ReaperMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readBlockPos(),
                new ItemStackHandler(ReaperBlockEntity.MAX_SLOTS),
                ReaperBlockEntity.createUpgradeHandler(() -> {}),
                new ItemStackHandler(ReaperBlockEntity.FILTER_SLOTS),
                new ItemStackHandler(1),
                new SimpleContainerData(ReaperBlockEntity.DATA_COUNT));
    }

    /** Server constructor. */
    public ReaperMenu(int containerId, Inventory playerInventory, ReaperBlockEntity be) {
        this(containerId, playerInventory, be.getBlockPos(), be.getStorage(), be.getUpgrades(), be.getFilter(), be.getCoreSlot(), be.getDataAccess());
    }

    private ReaperMenu(int containerId, Inventory playerInventory, BlockPos pos, IItemHandler storage,
                       IItemHandler upgrades, IItemHandler filter, IItemHandler core, ContainerData data) {
        super(Registration.REAPER_MENU.get(), containerId);
        this.pos = pos;
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), pos);
        this.data = data;

        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                int index = row * 9 + col;
                addSlot(new StorageSlot(storage, index, STORAGE_X + col * 18, STORAGE_Y + row * 18));
            }
        }
        for (int i = 0; i < ReaperBlockEntity.UPGRADE_SLOTS; i++) {
            addSlot(new CardSlot(upgrades, i,
                    SIDE_X + (i % UPGRADES_PER_ROW) * CARD_SPACING, UPGRADE_Y + (i / UPGRADES_PER_ROW) * CARD_SPACING));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new FilterSlot(filter, row * 3 + col, SIDE_X + col * 18, FILTER_Y + row * 18));
            }
        }
        addSlot(new SlotItemHandler(core, 0, CORE_X, CORE_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof com.xtri6.grimsoul.item.DataCoreItem;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, PLAYER_INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, HOTBAR_Y));
        }
        addDataSlots(data);
    }

    public int getData(int index) {
        return data.get(index);
    }

    public int getStoredXp() {
        return (data.get(ReaperBlockEntity.DATA_XP_LOW) & 0xFFFF) | ((data.get(ReaperBlockEntity.DATA_XP_HIGH) & 0xFFFF) << 16);
    }

    public int getActiveSlots() {
        return data.get(ReaperBlockEntity.DATA_ACTIVE_SLOTS);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!player.level().isClientSide && player.level().getBlockEntity(pos) instanceof ReaperBlockEntity be) {
            be.handleButton(id, player);
        }
        return true;
    }

    /** Ghost filter slots: set a single-item copy of whatever is on the cursor, or clear it. */
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= FILTER_START && slotId < CORE_SLOT) {
            Slot slot = slots.get(slotId);
            if (clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE) {
                ItemStack carried = getCarried();
                slot.set(carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1));
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index < FILTER_START || index == CORE_SLOT) {
            // Storage, upgrades or the core -> player inventory.
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= PLAYER_START) {
            // Player inventory -> core slot or upgrade slots (cards only). Never into storage or filter.
            boolean isCore = stack.getItem() instanceof com.xtri6.grimsoul.item.DataCoreItem;
            if (isCore ? !moveItemStackTo(stack, CORE_SLOT, CORE_SLOT + 1, false)
                    : !moveItemStackTo(stack, UPGRADE_START, FILTER_START, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, Registration.REAPER_BLOCK.get());
    }

    /** Machine storage: players can take items out but never put them in. */
    private static class StorageSlot extends SlotItemHandler {
        StorageSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }

    /** Filter entries: never hold a real item, only a marker copy. */
    private static class FilterSlot extends SlotItemHandler {
        FilterSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }
}
