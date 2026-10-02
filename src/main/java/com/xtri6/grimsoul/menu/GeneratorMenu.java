package com.xtri6.grimsoul.menu;

import com.xtri6.grimsoul.Registration;
import com.xtri6.grimsoul.blockentity.ReaperGeneratorBlockEntity;
import com.xtri6.grimsoul.generator.GeneratorFuel;

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
 * Reaper Generator menu. Slots 0-8: fuel. Slots 9-17: pass-through output (take only).
 * Slots 18-53: player inventory and hotbar.
 */
public class GeneratorMenu extends AbstractContainerMenu {
    public static final int WIDTH = 260;
    public static final int FUEL_X = 12, FUEL_Y = 22;
    public static final int OUTPUT_X = (WIDTH - 162) / 2, OUTPUT_Y = 96;
    public static final int INV_Y = 192;
    private static final int INV_X = (WIDTH - 162) / 2;
    public static final int OUTPUT_START = ReaperGeneratorBlockEntity.FUEL_SLOTS;
    public static final int PLAYER_START = OUTPUT_START + ReaperGeneratorBlockEntity.OUTPUT_SLOTS;
    public static final int PLAYER_END = PLAYER_START + 36;

    private final ContainerLevelAccess access;
    private final BlockPos pos;
    private final ContainerData data;

    public GeneratorMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, buf.readBlockPos(), new ItemStackHandler(ReaperGeneratorBlockEntity.FUEL_SLOTS),
                new ItemStackHandler(ReaperGeneratorBlockEntity.OUTPUT_SLOTS), new SimpleContainerData(ReaperGeneratorBlockEntity.DATA_COUNT));
    }

    public GeneratorMenu(int id, Inventory inv, ReaperGeneratorBlockEntity be) {
        this(id, inv, be.getBlockPos(), be.getFuel(), be.getOutput(), be.getDataAccess());
    }

    private GeneratorMenu(int id, Inventory inv, BlockPos pos, IItemHandler fuel, IItemHandler output, ContainerData data) {
        super(Registration.GENERATOR_MENU.get(), id);
        this.pos = pos;
        this.data = data;
        this.access = ContainerLevelAccess.create(inv.player.level(), pos);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new SlotItemHandler(fuel, row * 3 + col, FUEL_X + col * 18, FUEL_Y + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return GeneratorFuel.of(stack) != null;
                    }
                });
            }
        }
        for (int col = 0; col < ReaperGeneratorBlockEntity.OUTPUT_SLOTS; col++) {
            addSlot(new SlotItemHandler(output, col, OUTPUT_X + col * 18, OUTPUT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
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

    /** Reads an int the block entity split into two 16-bit halves. */
    public int getSplit(int lowIndex) {
        return (data.get(lowIndex) & 0xFFFF) | ((data.get(lowIndex + 1) & 0xFFFF) << 16);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!player.level().isClientSide && player.level().getBlockEntity(pos) instanceof ReaperGeneratorBlockEntity be) {
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
        } else if (GeneratorFuel.of(stack) == null || !moveItemStackTo(stack, 0, OUTPUT_START, false)) {
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
        return stillValid(access, player, Registration.REAPER_GENERATOR.get());
    }
}
