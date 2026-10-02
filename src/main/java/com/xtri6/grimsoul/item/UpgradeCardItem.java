package com.xtri6.grimsoul.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class UpgradeCardItem extends Item {
    private final UpgradeType type;

    public UpgradeCardItem(UpgradeType type, Properties properties) {
        super(properties.stacksTo(64));
        this.type = type;
    }

    public UpgradeType getUpgradeType() {
        return type;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.grimsoul.card." + type.id()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.grimsoul.card.max", type.maxCards()).withStyle(ChatFormatting.DARK_PURPLE));
    }
}
