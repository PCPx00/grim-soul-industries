package com.xtri6.grimsoul.menu;

import com.xtri6.grimsoul.machine.ChargeSlotHandler;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/** GUI slot for {@link ChargeSlotHandler}: one Forge Energy item at a time. */
public class ChargeSlot extends SlotItemHandler {
    public ChargeSlot(IItemHandler handler, int x, int y) {
        super(handler, 0, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return ChargeSlotHandler.isChargeable(stack);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return 1;
    }
}
