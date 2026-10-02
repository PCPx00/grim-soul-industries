package com.xtri6.grimsoul;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.xtri6.grimsoul.generator.GeneratorFuel;
import com.xtri6.grimsoul.item.DataCoreItem;
import com.xtri6.grimsoul.item.ReapersLanternItem;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

@Mod(GrimSoul.MODID)
public class GrimSoul {
    public static final String MODID = "grimsoul";
    public static final Logger LOGGER = LogUtils.getLogger();

    public GrimSoul(IEventBus modBus, ModContainer container) {
        Registration.register(modBus);
        modBus.addListener(this::registerCapabilities);
        modBus.addListener(this::registerDataMaps);
        NeoForge.EVENT_BUS.addListener(this::onEntityInteract);
        NeoForge.EVENT_BUS.addListener(this::onLivingDeath);
        com.xtri6.grimsoul.blockentity.SimDrops.register();
        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    /** Lets pipes, hoppers, AE2, Refined Storage and other mods pull items out of the Reaper Block. */
    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, Registration.REAPER_BLOCK_ENTITY.get(),
                (be, side) -> be.getOutputHandler(side));
        // XP Tank: pipes, AE2 and other fluid mods can pump Liquid Experience (or any c:experience fluid) in and out.
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, Registration.XP_TANK_ENTITY.get(),
                (be, side) -> be.getFluidHandler());
        // Reaper Spawner: takes FE in from any side.
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, Registration.ESSENCE_SPAWNER_ENTITY.get(),
                (be, side) -> be.getEnergyHandler());
        // Simulation Chamber: FE in from any side, drops out to pipes. Output Crate: items in and out.
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, Registration.SIMULATION_CHAMBER_ENTITY.get(),
                (be, side) -> be.getEnergyHandler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, Registration.SIMULATION_CHAMBER_ENTITY.get(),
                (be, side) -> be.getOutputHandler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, Registration.OUTPUT_CRATE_ENTITY.get(),
                (be, side) -> be.getPipeHandler());
        // Energy Cell: FE in or out per side, as set in its GUI.
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, Registration.ENERGY_CELL_ENTITY.get(),
                (be, side) -> be.getEnergyHandler(side));
        // Reaper Generator: FE out and items in/out, per side, as set in its GUI.
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, Registration.REAPER_GENERATOR_ENTITY.get(),
                (be, side) -> be.getEnergyHandler(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, Registration.REAPER_GENERATOR_ENTITY.get(),
                (be, side) -> be.getItemHandler(side));
    }

    /** Generator fuel values: data/<namespace>/data_maps/item/generator_fuel.json. */
    private void registerDataMaps(RegisterDataMapTypesEvent event) {
        event.register(GeneratorFuel.DATA_MAP);
    }

    /**
     * Runs the lantern's capture before the mob's own right-click action, so villagers and other
     * mobs that open a screen or react to clicks can still be captured.
     */
    private void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        ItemStack stack = event.getItemStack();
        if (stack.getItem() instanceof DataCoreItem core && event.getTarget() instanceof LivingEntity target) {
            InteractionResult result = core.interactLivingEntity(stack, event.getEntity(), target, event.getHand());
            if (result != InteractionResult.PASS) {
                event.setCancellationResult(result);
                event.setCanceled(true);
            }
            return;
        }
        if (stack.getItem() instanceof ReapersLanternItem lantern && event.getTarget() instanceof LivingEntity target) {
            InteractionResult result = lantern.interactLivingEntity(stack, event.getEntity(), target, event.getHand());
            if (result != InteractionResult.PASS) {
                event.setCancellationResult(result);
                event.setCanceled(true);
            }
        }
    }

    /** A real player's kill adds one data to every Data Core in their inventory bound to that mob. */
    private void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer) {
            return;
        }
        EntityType<?> type = event.getEntity().getType();
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (DataCoreItem.isBoundTo(stack, type)) {
                if (DataCoreItem.addData(stack, 1)) {
                    player.displayClientMessage(Component.translatable("message.grimsoul.data_core.tier_up",
                            type.getDescription(), DataCoreItem.tierName(DataCoreItem.getTier(stack))), true);
                }
            }
        }
    }
}
