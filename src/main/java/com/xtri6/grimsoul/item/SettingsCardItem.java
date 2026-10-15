package com.xtri6.grimsoul.item;

import java.util.List;

import com.xtri6.grimsoul.Registration;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Copies a machine's settings (work area, whitelist, filter, redstone mode...) so another machine of
 * the same kind can be set up the same way. Right-click a machine to copy, right-click another one to
 * paste, and sneak-right-click to clear the card. Contents, upgrades and stored power are never copied.
 */
public class SettingsCardItem extends Item {
    /** NBT keys a machine saves as settings. Anything else (storage, upgrades, power, XP) stays put. */
    public static final List<String> KEYS = List.of("OffX", "OffY", "OffZ", "SizeX", "SizeY", "SizeZ",
            "Whitelist", "Void", "VoidWhenFull", "ShowArea", "AutoEject", "Magnet", "AutoArea", "Target",
            "Enabled", "Redstone", "Filter");

    private static final String BLOCK = "block";
    private static final String SETTINGS = "settings";

    public SettingsCardItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /** Called from the right-click event, so the machine's own menu never opens. Server side does the work. */
    public InteractionResult useOnMachine(Level level, Player player, InteractionHand hand, ItemStack stack, BlockPos pos) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.SUCCESS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity be = level.getBlockEntity(pos);
        BlockState state = level.getBlockState(pos);
        if (player.isShiftKeyDown()) {
            stack.remove(Registration.SETTINGS_DATA.get());
            message(player, "message.grimsoul.settings.cleared");
            return InteractionResult.SUCCESS;
        }
        if (be == null) {
            return InteractionResult.SUCCESS;
        }
        CompoundTag saved = stack.get(Registration.SETTINGS_DATA.get());
        if (saved == null) {
            CompoundTag full = be.saveWithoutMetadata(level.registryAccess());
            CompoundTag settings = new CompoundTag();
            for (String key : KEYS) {
                if (full.contains(key)) {
                    settings.put(key, full.get(key).copy());
                }
            }
            if (settings.isEmpty()) {
                message(player, "message.grimsoul.settings.none");
                return InteractionResult.SUCCESS;
            }
            CompoundTag card = new CompoundTag();
            card.putString(BLOCK, BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
            card.put(SETTINGS, settings);
            stack.set(Registration.SETTINGS_DATA.get(), card);
            message(player, "message.grimsoul.settings.copied");
            return InteractionResult.SUCCESS;
        }
        String kind = saved.getString(BLOCK);
        if (!kind.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString())) {
            player.displayClientMessage(Component.translatable("message.grimsoul.settings.wrong_machine",
                    Component.translatable(blockKey(saved.getString(BLOCK)))), true);
            return InteractionResult.SUCCESS;
        }
        CompoundTag current = be.saveWithoutMetadata(level.registryAccess());
        CompoundTag settings = saved.getCompound(SETTINGS);
        for (String key : settings.getAllKeys()) {
            current.put(key, settings.get(key).copy());
        }
        be.loadWithComponents(current, level.registryAccess());
        be.setChanged();
        level.sendBlockUpdated(pos, state, state, 3);
        message(player, "message.grimsoul.settings.pasted");
        return InteractionResult.SUCCESS;
    }

    /** "grimsoul:reaper_block" -> "block.grimsoul.reaper_block", the translation key of that block. */
    private static String blockKey(String id) {
        return "block." + id.replace(':', '.');
    }

    private static void message(Player player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.grimsoul.settings_card").withStyle(ChatFormatting.GRAY));
        CompoundTag saved = stack.get(Registration.SETTINGS_DATA.get());
        if (saved != null) {
            tooltip.add(Component.translatable("tooltip.grimsoul.settings_card.holds",
                    Component.translatable(blockKey(saved.getString(BLOCK)))).withStyle(ChatFormatting.AQUA));
        }
    }
}
