package com.xtri6.grimsoul.menu;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import com.xtri6.grimsoul.item.UpgradeCardItem;
import com.xtri6.grimsoul.item.UpgradeType;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Upgrade slots for a machine. Only accepts cards the machine supports, never two slots of the
 * same card, and never more than that card's max count per machine. A machine can also give each
 * slot to one specific card type (see {@link #dedicated}).
 */
public class UpgradeItemHandler extends ItemStackHandler {
    private final Set<UpgradeType> accepted;
    private final Runnable onChanged;
    private final Map<UpgradeType, Integer> maxOverrides = new EnumMap<>(UpgradeType.class);
    /** When set, slot i only takes slotTypes[i]. */
    private UpgradeType[] slotTypes = null;

    public UpgradeItemHandler(int slots, Set<UpgradeType> accepted, Runnable onChanged) {
        super(slots);
        this.accepted = accepted;
        this.onChanged = onChanged;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (!(stack.getItem() instanceof UpgradeCardItem card) || !accepted.contains(card.getUpgradeType())) {
            return false;
        }
        if (slotTypes != null) {
            return slot < slotTypes.length && slotTypes[slot] == card.getUpgradeType();
        }
        for (int i = 0; i < getSlots(); i++) {
            if (i != slot && getStackInSlot(i).is(stack.getItem())) {
                return false;
            }
        }
        return true;
    }

    /** Gives each slot to one card type, in the order given (slot 0 = first type, and so on). */
    public UpgradeItemHandler dedicated(UpgradeType... types) {
        this.slotTypes = types.clone();
        return this;
    }

    /** The card type a dedicated slot takes, or null if the slot takes any accepted card. */
    public UpgradeType getSlotType(int slot) {
        return slotTypes != null && slot < slotTypes.length ? slotTypes[slot] : null;
    }

    /** Index of the slot dedicated to this card type, or -1. */
    public int slotFor(UpgradeType type) {
        if (slotTypes != null) {
            for (int i = 0; i < slotTypes.length; i++) {
                if (slotTypes[i] == type) {
                    return i;
                }
            }
        }
        return -1;
    }

    /** Lets one machine take more (or fewer) of a card than the card's default max. */
    public UpgradeItemHandler withMax(UpgradeType type, int max) {
        maxOverrides.put(type, max);
        return this;
    }

    public int maxFor(UpgradeType type) {
        return maxOverrides.getOrDefault(type, type.maxCards());
    }

    /**
     * The slot's size as the GUI sees it. Minecraft's slots use this number when you click or
     * drag cards in, so it must be the card's max (e.g. 10 for Looting), not 64.
     */
    @Override
    public int getSlotLimit(int slot) {
        UpgradeType type = getSlotType(slot);
        if (type == null && accepted.size() == 1) {
            type = accepted.iterator().next();
        }
        return type != null ? maxFor(type) : super.getSlotLimit(slot);
    }

    /** Cards stack to 64 in the inventory, but a slot only takes up to that card's max. */
    @Override
    protected int getStackLimit(int slot, ItemStack stack) {
        if (stack.getItem() instanceof UpgradeCardItem card) {
            return Math.min(maxFor(card.getUpgradeType()), stack.getMaxStackSize());
        }
        return super.getStackLimit(slot, stack);
    }

    @Override
    protected void onContentsChanged(int slot) {
        onChanged.run();
    }

    /**
     * Drops any cards above a slot's max on top of the machine (cards placed by an older version
     * of the mod could hold up to 64). Nothing is lost: the extra cards pop out as items.
     */
    public void dropExtraCards(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        for (int i = 0; i < getSlots(); i++) {
            ItemStack stack = getStackInSlot(i);
            int max = getSlotLimit(i);
            if (stack.getItem() instanceof UpgradeCardItem card) {
                max = Math.min(max, maxFor(card.getUpgradeType()));
            }
            if (stack.getCount() > max) {
                ItemStack extra = stack.copyWithCount(stack.getCount() - max);
                setStackInSlot(i, stack.copyWithCount(max));
                net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, extra);
            }
        }
    }

    /** Total number of cards of this type across all slots. */
    public int count(UpgradeType type) {
        int total = 0;
        for (int i = 0; i < getSlots(); i++) {
            ItemStack stack = getStackInSlot(i);
            if (stack.getItem() instanceof UpgradeCardItem card && card.getUpgradeType() == type) {
                total += stack.getCount();
            }
        }
        return Math.min(total, maxFor(type));
    }
}
