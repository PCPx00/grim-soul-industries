package com.xtri6.grimsoul.item;

import java.util.List;
import java.util.Optional;

import com.xtri6.grimsoul.Config;
import com.xtri6.grimsoul.Registration;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.common.Tags;

/**
 * Reaper Data Core. Right-click a mob to bind the core to that mob type. Kills of that mob (by
 * you, or by a Reaper Block holding the core) add data; more data means a higher tier, and a
 * higher tier means the Simulation Chamber succeeds more often.
 */
public class DataCoreItem extends Item {
    public static final int MAX_TIER = 4;
    private static final String[] TIER_NAMES = {"hollow", "whispering", "restless", "wraithbound", "soulforged"};
    private static final ChatFormatting[] TIER_COLORS = {
            ChatFormatting.GRAY, ChatFormatting.AQUA, ChatFormatting.DARK_PURPLE, ChatFormatting.LIGHT_PURPLE, ChatFormatting.GOLD};

    public DataCoreItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    // ---- Stored data ----

    public static Optional<EntityType<?>> getMob(ItemStack stack) {
        ResourceLocation id = stack.get(Registration.DATA_CORE_MOB.get());
        return id == null ? Optional.empty() : BuiltInRegistries.ENTITY_TYPE.getOptional(id);
    }

    public static boolean isBoundTo(ItemStack stack, EntityType<?> type) {
        return stack.getItem() instanceof DataCoreItem && getMob(stack).map(t -> t == type).orElse(false);
    }

    public static int getData(ItemStack stack) {
        return stack.getOrDefault(Registration.DATA_CORE_DATA.get(), 0);
    }

    /** Adds data, stopping at the Soulforged threshold. Returns true if the tier went up. */
    public static boolean addData(ItemStack stack, int amount) {
        if (amount <= 0) {
            return false;
        }
        int before = getTier(stack);
        int max = threshold(MAX_TIER);
        stack.set(Registration.DATA_CORE_DATA.get(), Math.min(max, getData(stack) + amount));
        return getTier(stack) > before;
    }

    /** Total data needed to reach a tier (0 for Hollow). */
    public static int threshold(int tier) {
        if (tier <= 0) {
            return 0;
        }
        List<? extends Integer> list = Config.CORE_TIER_DATA.get();
        int index = Math.min(tier, list.size()) - 1;
        return index < 0 ? 0 : list.get(index);
    }

    public static int getTier(ItemStack stack) {
        int data = getData(stack);
        int tier = 0;
        while (tier < MAX_TIER && data >= threshold(tier + 1)) {
            tier++;
        }
        return tier;
    }

    /** Simulation success chance in percent for this tier. */
    public static int chance(int tier) {
        List<? extends Integer> list = Config.CORE_TIER_CHANCE.get();
        return list.isEmpty() ? 100 : list.get(Math.max(0, Math.min(tier, list.size() - 1)));
    }

    public static Component tierName(int tier) {
        int t = Math.max(0, Math.min(MAX_TIER, tier));
        return Component.translatable("tier.grimsoul." + TIER_NAMES[t]).withStyle(TIER_COLORS[t]);
    }

    /** Mobs a core may be bound to: no bosses (unless the config allows them) and nothing on the blacklist tag. */
    public static boolean canBind(EntityType<?> type) {
        if (type.is(Registration.SIMULATION_BLACKLIST)) {
            return false;
        }
        return Config.CORE_ALLOW_BOSSES.get() || !type.is(Tags.EntityTypes.BOSSES);
    }

    // ---- Use ----

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (getMob(stack).isPresent()) {
            return InteractionResult.PASS;
        }
        if (!(target instanceof Mob) || !canBind(target.getType())) {
            if (!player.level().isClientSide) {
                player.displayClientMessage(Component.translatable("message.grimsoul.data_core.cannot_bind").withStyle(ChatFormatting.RED), true);
            }
            return InteractionResult.FAIL;
        }
        if (!player.level().isClientSide) {
            ItemStack bound = stack.copy();
            bound.set(Registration.DATA_CORE_MOB.get(), BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()));
            bound.set(Registration.DATA_CORE_DATA.get(), 0);
            player.setItemInHand(hand, bound);
            player.level().playSound(null, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.2F);
            player.displayClientMessage(Component.translatable("message.grimsoul.data_core.bound", target.getType().getDescription()), true);
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return getTier(stack) >= MAX_TIER;
    }

    @Override
    public Component getName(ItemStack stack) {
        Optional<EntityType<?>> mob = getMob(stack);
        return mob.<Component>map(type -> Component.translatable("item.grimsoul.data_core.bound", type.getDescription()))
                .orElseGet(() -> super.getName(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Optional<EntityType<?>> mob = getMob(stack);
        if (mob.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.grimsoul.data_core.unbound").withStyle(ChatFormatting.GRAY));
            return;
        }
        int tier = getTier(stack);
        int data = getData(stack);
        tooltip.add(Component.translatable("tooltip.grimsoul.data_core.tier", tierName(tier)));
        if (tier < MAX_TIER) {
            tooltip.add(Component.translatable("tooltip.grimsoul.data_core.data", data, threshold(tier + 1), tierName(tier + 1))
                    .withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("tooltip.grimsoul.data_core.chance", chance(tier)).withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("tooltip.grimsoul.data_core.how").withStyle(ChatFormatting.DARK_GRAY));
    }
}
