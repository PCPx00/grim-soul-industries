package com.xtri6.grimsoul.blockentity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/**
 * Catches the drops of a Virtualization Chamber's virtual kill. The mob runs its full vanilla
 * death-drop code (loot table, player-kill drops, equipment, and drops other mods add), then this
 * takes the items before anything lands in the world.
 */
public final class SimDrops {
    private static final Set<Entity> CAPTURING = Collections.newSetFromMap(new IdentityHashMap<>());
    private static List<ItemStack> captured = new ArrayList<>();

    private SimDrops() {}

    public static void register() {
        // Lowest priority: let every other mod add or change drops first.
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, LivingDropsEvent.class, SimDrops::onDrops);
    }

    /** Runs the drop code and returns the items it produced. */
    public static List<ItemStack> capture(Entity entity, Runnable dropCode) {
        List<ItemStack> result = new ArrayList<>();
        captured = result;
        CAPTURING.add(entity);
        try {
            dropCode.run();
        } finally {
            CAPTURING.remove(entity);
        }
        return result;
    }

    private static final java.lang.reflect.Method DROP_ALL_DEATH_LOOT = net.neoforged.fml.util.ObfuscationReflectionHelper.findMethod(
            net.minecraft.world.entity.LivingEntity.class, "dropAllDeathLoot",
            net.minecraft.server.level.ServerLevel.class, net.minecraft.world.damagesource.DamageSource.class);

    /** Calls the mob's own (protected) death-drop method, the same one vanilla runs when it dies. */
    public static void dropAllDeathLoot(net.minecraft.world.entity.LivingEntity entity, net.minecraft.server.level.ServerLevel level,
                                        net.minecraft.world.damagesource.DamageSource source) {
        try {
            DROP_ALL_DEATH_LOOT.invoke(entity, level, source);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Grim Soul Industries: could not run death drops for " + entity.getType(), e);
        }
    }

    private static void onDrops(LivingDropsEvent event) {
        if (!CAPTURING.contains(event.getEntity())) {
            return;
        }
        for (ItemEntity drop : event.getDrops()) {
            captured.add(drop.getItem().copy());
        }
        event.setCanceled(true);
    }
}
