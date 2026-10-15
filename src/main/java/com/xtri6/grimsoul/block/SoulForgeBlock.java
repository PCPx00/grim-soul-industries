package com.xtri6.grimsoul.block;

import com.xtri6.grimsoul.Registration;
import com.xtri6.grimsoul.item.DataCoreItem;
import com.xtri6.grimsoul.item.UpgradeType;
import com.xtri6.grimsoul.item.UpgradeCardItem;
import com.xtri6.grimsoul.item.SettingsCardItem;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Soul Forge: hand-forge endgame items on it. Hold the item in your main hand and Soul Steel in your
 * off hand, then right-click the forge.
 * <ul>
 *   <li>Soulforged Data Core of a mob with a spawn egg: a spawn egg for that mob (uses 1 core, 1 Soul Steel).</li>
 *   <li>Soulforged Data Core of a boss: a Boss Core card for the Virtualization Chamber (uses the same).</li>
 *   <li>Any other single item: a soulbound copy, kept when you die (uses 1 Soul Steel).</li>
 * </ul>
 */
public class SoulForgeBlock extends Block {
    public SoulForgeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || held.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        ItemStack steel = player.getOffhandItem();
        if (!steel.is(Registration.SOUL_STEEL.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (held.getItem() instanceof DataCoreItem) {
            if (DataCoreItem.getTier(held) < DataCoreItem.MAX_TIER) {
                return fail(player, "message.grimsoul.forge.needs_soulforged");
            }
            EntityType<?> mob = DataCoreItem.getMob(held).orElse(null);
            if (mob == null) {
                return fail(player, "message.grimsoul.forge.no_mob");
            }
            ItemStack result;
            if (DataCoreItem.isBoss(mob)) {
                result = new ItemStack(Registration.CARDS.get(UpgradeType.BOSS_CORE).get());
            } else {
                SpawnEggItem egg = SpawnEggItem.byId(mob);
                if (egg == null) {
                    return fail(player, "message.grimsoul.forge.no_egg");
                }
                result = new ItemStack(egg);
            }
            if (!level.isClientSide) {
                held.shrink(1);
                steel.shrink(1);
                give(player, result);
                level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0F, 0.8F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (held.getCount() != 1 || held.getItem() instanceof UpgradeCardItem || held.has(Registration.SOULBOUND.get())
                || held.getItem() instanceof SettingsCardItem) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            held.set(Registration.SOULBOUND.get(), net.minecraft.util.Unit.INSTANCE);
            steel.shrink(1);
            level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.2F);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private static ItemInteractionResult fail(Player player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
        return ItemInteractionResult.FAIL;
    }

    private static void give(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
