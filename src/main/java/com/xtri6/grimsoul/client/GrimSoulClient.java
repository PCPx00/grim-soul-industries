package com.xtri6.grimsoul.client;

import com.xtri6.grimsoul.GrimSoul;
import com.xtri6.grimsoul.Config;
import com.xtri6.grimsoul.Registration;
import com.xtri6.grimsoul.generator.GeneratorFuel;
import com.xtri6.grimsoul.item.DataCoreItem;

import net.minecraft.client.renderer.item.ItemProperties;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** Client-only setup: machine screens and block renderers (lasers, outlines, tank liquid). */
@Mod(value = GrimSoul.MODID, dist = Dist.CLIENT)
public class GrimSoulClient {
    public GrimSoulClient(IEventBus modBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.CLIENT, ClientConfig.SPEC);
        modBus.addListener(net.neoforged.fml.event.config.ModConfigEvent.Loading.class, ClientConfig::onLoad);
        modBus.addListener(net.neoforged.fml.event.config.ModConfigEvent.Reloading.class, ClientConfig::onLoad);
        modBus.addListener(GrimSoulClient::registerScreens);
        modBus.addListener(GrimSoulClient::registerRenderers);
        modBus.addListener(GrimSoulClient::registerClientExtensions);
        modBus.addListener(GrimSoulClient::clientSetup);
        modBus.addListener(Fx::registerModels);
        NeoForge.EVENT_BUS.addListener(GrimSoulClient::addFuelTooltip);
    }

    /** Shows how much power a mob drop makes in the Reaper Generator, on every item that is fuel. */
    private static void addFuelTooltip(ItemTooltipEvent event) {
        GeneratorFuel fuel = GeneratorFuel.of(event.getItemStack());
        if (fuel == null) {
            return;
        }
        double multiplier = Config.SPEC.isLoaded() ? Config.GENERATOR_FUEL_MULTIPLIER.get() : 1.0;
        long rate = Math.round(fuel.fePerTick() * multiplier);
        event.getToolTip().add(Component.translatable("tooltip.grimsoul.generator_fuel",
                GuiStyle.compact((int) Math.min(Integer.MAX_VALUE, rate)),
                String.format(java.util.Locale.ROOT, "%.0f", fuel.burnTime() / 20.0),
                GuiStyle.compact((int) Math.min(Integer.MAX_VALUE, rate * fuel.burnTime())))
                .withStyle(ChatFormatting.DARK_PURPLE));
    }

    /** Data Core textures change with its tier: 0 = blank, 0.2-1.0 = Hollow to Soulforged. */
    private static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(Registration.DATA_CORE.get(),
                ResourceLocation.fromNamespaceAndPath(GrimSoul.MODID, "tier"),
                (stack, level, entity, seed) -> DataCoreItem.getMob(stack).isEmpty() ? 0.0F : (DataCoreItem.getTier(stack) + 1) / 5.0F));
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(Registration.REAPER_MENU.get(), ReaperScreen::new);
        event.register(Registration.MOB_FAN_MENU.get(), MobFanScreen::new);
        event.register(Registration.XP_TANK_MENU.get(), XpTankScreen::new);
        event.register(Registration.SPAWNER_MENU.get(), SpawnerScreen::new);
        event.register(Registration.GENERATOR_MENU.get(), GeneratorScreen::new);
        event.register(Registration.SIMULATION_MENU.get(), SimulationScreen::new);
        event.register(Registration.CRATE_MENU.get(), CrateScreen::new);
        event.register(Registration.CELL_MENU.get(), CellScreen::new);
    }

    /** How Liquid Experience looks in fluid tanks, pipes and AE2 terminals. */
    private static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(GrimSoul.MODID, "block/xp_tank_liquid");
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return texture;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return texture;
            }
        }, Registration.XP_FLUID_TYPE.get());
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Registration.REAPER_BLOCK_ENTITY.get(), ReaperAreaRenderer::new);
        event.registerBlockEntityRenderer(Registration.MOB_FAN_ENTITY.get(), MobFanAreaRenderer::new);
        event.registerBlockEntityRenderer(Registration.XP_TANK_ENTITY.get(), XpTankRenderer::new);
        event.registerBlockEntityRenderer(Registration.ESSENCE_SPAWNER_ENTITY.get(), EssenceSpawnerRenderer::new);
        event.registerBlockEntityRenderer(Registration.SIMULATION_CHAMBER_ENTITY.get(), SimulationChamberRenderer::new);
        event.registerBlockEntityRenderer(Registration.REAPER_GENERATOR_ENTITY.get(), GeneratorRenderer::new);
        event.registerBlockEntityRenderer(Registration.ENERGY_CELL_ENTITY.get(), EnergyCellRenderer::new);
    }
}
