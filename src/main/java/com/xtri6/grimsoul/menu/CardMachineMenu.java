package com.xtri6.grimsoul.menu;

import com.xtri6.grimsoul.Registration;
import com.xtri6.grimsoul.blockentity.CardMachineBlockEntity;
import com.xtri6.grimsoul.blockentity.MobFanBlockEntity;
import com.xtri6.grimsoul.blockentity.XpTankBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Menu for the Mob Fan and XP Tank: one Range card slot, the player inventory, and synced
 * settings. Slot 0 is the card slot; 1-36 are the player inventory and hotbar.
 */
public class CardMachineMenu extends AbstractContainerMenu {
    public static final int WIDTH = 250;
    public static final int CARD_X = 12;
    public static final int FAN_CARD_Y = 22, FAN_INV_Y = 132;
    public static final int TANK_CARD_Y = 40, TANK_INV_Y = 176;
    private static final int INV_X = (WIDTH - 162) / 2;

    private final ContainerLevelAccess access;
    private final Block block;
    private final BlockPos pos;
    private final ContainerData data;

    // ---- Factories ----

    public static CardMachineMenu mobFan(int id, Inventory inv, MobFanBlockEntity be) {
        return new CardMachineMenu(Registration.MOB_FAN_MENU.get(), id, inv, be.getBlockPos(), be.getCards(), be.getDataAccess(),
                Registration.MOB_FAN.get(), FAN_CARD_Y, FAN_INV_Y);
    }

    public static CardMachineMenu mobFanClient(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        return new CardMachineMenu(Registration.MOB_FAN_MENU.get(), id, inv, buf.readBlockPos(), CardMachineBlockEntity.clientCardHandler(),
                new SimpleContainerData(MobFanBlockEntity.DATA_COUNT), Registration.MOB_FAN.get(), FAN_CARD_Y, FAN_INV_Y);
    }

    public static CardMachineMenu xpTank(int id, Inventory inv, XpTankBlockEntity be) {
        return new CardMachineMenu(Registration.XP_TANK_MENU.get(), id, inv, be.getBlockPos(), be.getCards(), be.getDataAccess(),
                Registration.XP_TANK.get(), TANK_CARD_Y, TANK_INV_Y);
    }

    public static CardMachineMenu xpTankClient(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        return new CardMachineMenu(Registration.XP_TANK_MENU.get(), id, inv, buf.readBlockPos(), CardMachineBlockEntity.clientCardHandler(),
                new SimpleContainerData(XpTankBlockEntity.DATA_COUNT), Registration.XP_TANK.get(), TANK_CARD_Y, TANK_INV_Y);
    }

    private CardMachineMenu(MenuType<?> type, int id, Inventory inv, BlockPos pos, IItemHandler cards, ContainerData data,
                            Block block, int cardY, int invY) {
        super(type, id);
        this.pos = pos;
        this.block = block;
        this.data = data;
        this.access = ContainerLevelAccess.create(inv.player.level(), pos);

        addSlot(new CardSlot(cards, 0, CARD_X, cardY));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, INV_X + col * 18, invY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, INV_X + col * 18, invY + 58));
        }
        addDataSlots(data);
    }

    public int getData(int index) {
        return data.get(index);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!player.level().isClientSide && player.level().getBlockEntity(pos) instanceof CardMachineBlockEntity be) {
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
        if (index == 0) {
            if (!moveItemStackTo(stack, 1, 37, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, 1, false)) {
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
        return stillValid(access, player, block);
    }
}
