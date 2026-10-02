package com.xtri6.grimsoul.item;

import java.util.List;
import java.util.Optional;

import com.xtri6.grimsoul.Registration;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.Tags;

/**
 * Right-click a mob to capture it into the lantern. Right-click a block to release it there.
 * Later tiers (Essence Spawner, Data Cores) read the captured mob from this item.
 */
public class ReapersLanternItem extends Item {
    public ReapersLanternItem(Properties properties) {
        super(properties);
    }

    public static Optional<EntityType<?>> getCaptured(ItemStack stack) {
        ResourceLocation id = stack.get(Registration.CAPTURED_MOB.get());
        if (id == null) {
            return Optional.empty();
        }
        return BuiltInRegistries.ENTITY_TYPE.getOptional(id);
    }

    public static boolean canCapture(LivingEntity target) {
        if (!(target instanceof Mob) || !target.isAlive()) {
            return false;
        }
        EntityType<?> type = target.getType();
        return !type.is(Tags.EntityTypes.BOSSES) && !type.is(Registration.CAPTURE_BLACKLIST);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (getCaptured(stack).isPresent()) {
            return InteractionResult.PASS;
        }
        if (!canCapture(target)) {
            if (!player.level().isClientSide) {
                player.displayClientMessage(Component.translatable("message.grimsoul.lantern.cannot_capture").withStyle(ChatFormatting.RED), true);
            }
            return InteractionResult.FAIL;
        }
        Level level = player.level();
        if (!level.isClientSide) {
            ItemStack filled = stack.copy();
            filled.set(Registration.CAPTURED_MOB.get(), BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()));
            player.setItemInHand(hand, filled);
            level.playSound(null, target.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
            target.discard();
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        Optional<EntityType<?>> captured = getCaptured(stack);
        if (captured.isEmpty()) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        if (level instanceof ServerLevel serverLevel) {
            BlockPos spawnPos = context.getClickedPos().relative(context.getClickedFace());
            if (captured.get().spawn(serverLevel, spawnPos, MobSpawnType.SPAWN_EGG) != null) {
                stack.remove(Registration.CAPTURED_MOB.get());
                level.playSound(null, spawnPos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.6F);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(Registration.CAPTURED_MOB.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Optional<EntityType<?>> captured = getCaptured(stack);
        if (captured.isPresent()) {
            tooltip.add(Component.translatable("tooltip.grimsoul.lantern.holding", captured.get().getDescription()).withStyle(ChatFormatting.LIGHT_PURPLE));
            tooltip.add(Component.translatable("tooltip.grimsoul.lantern.release").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.grimsoul.lantern.empty").withStyle(ChatFormatting.GRAY));
        }
    }
}
