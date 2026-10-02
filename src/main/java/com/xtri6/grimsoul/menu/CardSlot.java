package com.xtri6.grimsoul.menu;

import com.xtri6.grimsoul.item.UpgradeCardItem;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * An upgrade card slot that can never hold more than that card's max (e.g. 10 Looting).
 * Every way of putting cards in (click, right-click, drag, shift-click, swap) goes through
 * getMaxStackSize, so clamping here covers them all.
 */
public class CardSlot extends SlotItemHandler {
    public CardSlot(IItemHandler handler, int index, int x, int y) {
        super(handler, index, x, y);
    }

    private int cardMax(ItemStack stack) {
        if (getItemHandler() instanceof UpgradeItemHandler cards) {
            if (stack.getItem() instanceof UpgradeCardItem card) {
                return Math.min(cards.maxFor(card.getUpgradeType()), stack.getMaxStackSize());
            }
            return cards.getSlotLimit(getSlotIndex());
        }
        return stack.isEmpty() ? 64 : stack.getMaxStackSize();
    }

    @Override
    public int getMaxStackSize() {
        if (getItemHandler() instanceof UpgradeItemHandler cards) {
            return cards.getSlotLimit(getSlotIndex());
        }
        return super.getMaxStackSize();
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return Math.min(cardMax(stack), super.getMaxStackSize(stack));
    }
}
