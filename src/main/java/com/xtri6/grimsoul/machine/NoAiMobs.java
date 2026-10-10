package com.xtri6.grimsoul.machine;

import javax.annotation.Nullable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingConversionEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

/**
 * Mobs from a Reaper Spawner with a No AI card. Turning their AI off alone isn't enough (some mobs
 * still hurt on touch, make noise, or lose the setting when they convert), so these mobs are marked
 * and kept completely still, silent and harmless:
 * - no AI and silent
 * - any damage they would deal (melee, contact like slimes, projectiles, explosions) is cancelled
 * - they never pick a target, and creepers among them never explode
 * - the mark carries over if the mob converts (a piglin zombifying, a zombie drowning...)
 */
public final class NoAiMobs {
    public static final String TAG = "grimsoul_no_ai";

    private NoAiMobs() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(NoAiMobs::onDamage);
        NeoForge.EVENT_BUS.addListener(NoAiMobs::onTarget);
        NeoForge.EVENT_BUS.addListener(NoAiMobs::onExplosion);
        NeoForge.EVENT_BUS.addListener(NoAiMobs::onConvert);
        NeoForge.EVENT_BUS.addListener(NoAiMobs::onJoin);
    }

    /** Marks a mob (and anything riding it) as a No AI mob. */
    public static void apply(Mob mob) {
        mob.addTag(TAG);
        freeze(mob);
        for (Entity rider : mob.getPassengers()) {
            if (rider instanceof Mob riderMob) {
                riderMob.addTag(TAG);
                freeze(riderMob);
            }
        }
    }

    private static void freeze(Mob mob) {
        mob.setNoAi(true);
        mob.setSilent(true);
        mob.setTarget(null);
        mob.setAggressive(false);
        mob.getNavigation().stop();
    }

    public static boolean isNoAi(@Nullable Entity entity) {
        return entity != null && entity.getTags().contains(TAG);
    }

    /** True if the damage comes from a No AI mob, directly or through its arrow, fireball and so on. */
    private static boolean fromNoAiMob(@Nullable Entity direct, @Nullable Entity owner) {
        if (isNoAi(direct) || isNoAi(owner)) {
            return true;
        }
        return direct instanceof Projectile projectile && isNoAi(projectile.getOwner());
    }

    private static void onDamage(LivingIncomingDamageEvent event) {
        if (fromNoAiMob(event.getSource().getDirectEntity(), event.getSource().getEntity())) {
            event.setCanceled(true);
        }
    }

    private static void onTarget(LivingChangeTargetEvent event) {
        if (isNoAi(event.getEntity()) && event.getNewAboutToBeSetTarget() != null) {
            event.setCanceled(true);
        }
    }

    private static void onExplosion(ExplosionEvent.Start event) {
        if (fromNoAiMob(event.getExplosion().getDirectSourceEntity(), event.getExplosion().getIndirectSourceEntity())) {
            event.setCanceled(true);
        }
    }

    private static void onConvert(LivingConversionEvent.Post event) {
        if (isNoAi(event.getEntity()) && event.getOutcome() instanceof Mob converted) {
            apply(converted);
        }
    }

    /** Re-applies the settings when a marked mob is loaded back in (another mod may have changed them). */
    private static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Mob mob && isNoAi(mob)) {
            freeze(mob);
        }
    }
}
