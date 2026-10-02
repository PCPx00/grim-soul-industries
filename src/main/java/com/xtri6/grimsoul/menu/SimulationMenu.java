package com.xtri6.grimsoul.menu;

import com.xtri6.grimsoul.Registration;
import com.xtri6.grimsoul.blockentity.SimulationChamberBlockEntity;
import com.xtri6.grimsoul.item.DataCoreItem;
import com.xtri6.grimsoul.item.UpgradeCardItem;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Simulation Chamber menu. Slot 0: Data Core. Slots 1-3: Speed, Looting, Efficiency cards.
 * Slots 4-21: output (take only). Slots 22-57: player inventory and hotbar.
 */
public class SimulationMenu extends AbstractContainerMenu {
    public static final int WIDTH = 260;
    public static final int CORE_X = 12, CORE_Y = 22;
    public static final int CARDS_X = 12, CARDS_Y = 56, CARD_SPACING = 22;
    public static final int OUTPUT_X = (WIDTH - 162) / 2, OUTPUT_Y = 114;
    public static final int INV_Y = 198;
    private static final int INV_X = (WIDTH - 162) / 2;
    public static final int CARD_START = 1;
    public static final int OUTPUT_START = CARD_START + SimulationChamberBlockEntity.CARD_COUNT;
    public static final int PLAYER_START = OUTPUT_START + SimulationChamberBlockEntity.OUTPUT_SLOTS;
    public static final int PLAYER_END = PLAYER_START + 36;

    private final ContainerLevelAccess access;
    private final BlockPos pos;
    private final ContainerData data;

    public SimulationMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, buf.readBlockPos(), new ItemStackHandler(1), SimulationChamberBlockEntity.createCardHandler(() -> {}),
                new ItemStackHandler(SimulationChamberBlockEntity.OUTPUT_SLOTS), new SimpleContainerData(SimulationChamberBlockEntity.DATA_COUNT));
    }

    public SimulationMenu(int id, Inventory inv, SimulationChamberBlockEntity be) {
        this(id, inv, be.getBlockPos(), be.getCore(), be.getCards(), be.getOutput(), be.getDataAccess());
    }

    private SimulationMenu(int id, Inventory inv, BlockPos pos, IItemHandler core, IItemHandler cards, IItemHandler output, ContainerData data) {
        super(Registration.SIMULATION_MENU.get(), id);
        this.pos = pos;
        this.data = data;
        this.access = ContainerLevelAccess.create(inv.player.level(), pos);

        addSlot(new SlotItemHandler(core, 0, CORE_X, CORE_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof DataCoreItem;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int i = 0; i < SimulationChamberBlockEntity.CARD_COUNT; i++) {
            addSlot(new CardSlot(cards, i, CARDS_X + i * CARD_SPACING, CARDS_Y));
        }
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new SlotItemHandler(output, row * 9 + col, OUTPUT_X + col * 18, OUTPUT_Y + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, INV_X + col * 18, INV_Y + 58));
        }
        addDataSlots(data);
    }

    public int getData(int index) {
        return data.get(index);
    }

    public int getSplit(int lowIndex) {
        return (data.get(lowIndex) & 0xFFFF) | ((data.get(lowIndex + 1) & 0xFFFF) << 16);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!player.level().isClientSide && player.level().getBlockEntity(pos) instanceof SimulationChamberBlockEntity be) {
            be.handleButton(id, player);
        }
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < PLAYER_START) {
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof DataCoreItem) {
            if (!moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof UpgradeCardItem) {
            if (!moveItemStackTo(stack, CARD_START, OUTPUT_START, false)) {
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
        return stillValid(access, player, Registration.SIMULATION_CHAMBER.get());
    }
}
