package com.xtri6.grimsoul;

import java.util.EnumMap;
import java.util.Map;

import com.xtri6.grimsoul.block.EssenceSpawnerBlock;
import com.xtri6.grimsoul.block.MachineBlock;
import com.xtri6.grimsoul.block.MobFanBlock;
import com.xtri6.grimsoul.block.ReaperBlock;
import com.xtri6.grimsoul.block.ReaperGeneratorBlock;
import com.xtri6.grimsoul.block.EnergyCellBlock;
import com.xtri6.grimsoul.block.OutputCrateBlock;
import com.xtri6.grimsoul.block.SimulationChamberBlock;
import com.xtri6.grimsoul.block.XpTankBlock;
import com.xtri6.grimsoul.blockentity.EssenceSpawnerBlockEntity;
import com.xtri6.grimsoul.blockentity.MobFanBlockEntity;
import com.xtri6.grimsoul.blockentity.ReaperBlockEntity;
import com.xtri6.grimsoul.blockentity.ReaperGeneratorBlockEntity;
import com.xtri6.grimsoul.blockentity.EnergyCellBlockEntity;
import com.xtri6.grimsoul.blockentity.OutputCrateBlockEntity;
import com.xtri6.grimsoul.blockentity.SimulationChamberBlockEntity;
import com.xtri6.grimsoul.blockentity.XpTankBlockEntity;
import com.xtri6.grimsoul.item.ReapersLanternItem;
import com.xtri6.grimsoul.item.DataCoreItem;
import com.xtri6.grimsoul.item.UpgradeCardItem;
import com.xtri6.grimsoul.item.UpgradeType;
import com.xtri6.grimsoul.menu.CardMachineMenu;
import com.xtri6.grimsoul.menu.GeneratorMenu;
import com.xtri6.grimsoul.menu.CellMenu;
import com.xtri6.grimsoul.menu.CrateMenu;
import com.xtri6.grimsoul.menu.SimulationMenu;
import com.xtri6.grimsoul.menu.ReaperMenu;
import com.xtri6.grimsoul.menu.SpawnerMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.minecraft.core.component.DataComponentType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Every block, item, block entity, menu and data component the mod adds. */
public final class Registration {
    private static final String MODID = GrimSoul.MODID;

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, MODID);

    // ---- Liquid Experience (XP as a fluid, 20 mB = 1 XP point, so pipes and AE2 can move it) ----
    public static final int MB_PER_XP = 20;
    public static final DeferredHolder<FluidType, FluidType> XP_FLUID_TYPE = FLUID_TYPES.register("liquid_experience",
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid_type.grimsoul.liquid_experience")
                    .lightLevel(10)
                    .density(800)
                    .viscosity(1500)));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> XP_FLUID =
            FLUIDS.register("liquid_experience", () -> new BaseFlowingFluid.Source(xpFluidProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> XP_FLUID_FLOWING =
            FLUIDS.register("liquid_experience_flowing", () -> new BaseFlowingFluid.Flowing(xpFluidProperties()));

    private static BaseFlowingFluid.Properties xpFluidProperties() {
        return new BaseFlowingFluid.Properties(XP_FLUID_TYPE, XP_FLUID, XP_FLUID_FLOWING);
    }

    // ---- Data components ----
    /** The entity type stored inside a Reaper's Lantern. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> CAPTURED_MOB =
            COMPONENTS.registerComponentType("captured_mob", builder -> builder
                    .persistent(ResourceLocation.CODEC)
                    .networkSynchronized(ResourceLocation.STREAM_CODEC));

    /** Data Core: the mob it's bound to, and how much data it has collected. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> DATA_CORE_MOB =
            COMPONENTS.registerComponentType("data_core_mob", builder -> builder
                    .persistent(ResourceLocation.CODEC)
                    .networkSynchronized(ResourceLocation.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> DATA_CORE_DATA =
            COMPONENTS.registerComponentType("data_core_data", builder -> builder
                    .persistent(net.minecraft.util.ExtraCodecs.NON_NEGATIVE_INT)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT));

    /** FE kept on an Energy Cell item after it's broken. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> STORED_ENERGY =
            COMPONENTS.registerComponentType("stored_energy", builder -> builder
                    .persistent(net.minecraft.util.ExtraCodecs.NON_NEGATIVE_INT)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT));

    // ---- Blocks ----
    private static BlockBehaviour.Properties machineProps() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(3.5F, 6.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL);
    }

    public static final DeferredBlock<ReaperBlock> REAPER_BLOCK = BLOCKS.register("reaper_block", () -> new ReaperBlock(machineProps()));
    public static final DeferredBlock<MobFanBlock> MOB_FAN = BLOCKS.register("mob_fan", () -> new MobFanBlock(machineProps().noOcclusion()));
    public static final DeferredBlock<XpTankBlock> XP_TANK = BLOCKS.register("xp_tank", () -> new XpTankBlock(machineProps().noOcclusion()));
    public static final DeferredBlock<EssenceSpawnerBlock> ESSENCE_SPAWNER = BLOCKS.register("essence_spawner",
            () -> new EssenceSpawnerBlock(machineProps().noOcclusion().lightLevel(state -> state.getValue(MachineBlock.ACTIVE) ? 7 : 0)));

    public static final DeferredBlock<ReaperGeneratorBlock> REAPER_GENERATOR = BLOCKS.register("reaper_generator",
            () -> new ReaperGeneratorBlock(machineProps().lightLevel(state -> state.getValue(MachineBlock.ACTIVE) ? 9 : 0)));

    public static final DeferredBlock<SimulationChamberBlock> SIMULATION_CHAMBER = BLOCKS.register("simulation_chamber",
            () -> new SimulationChamberBlock(machineProps().noOcclusion().lightLevel(state -> state.getValue(MachineBlock.ACTIVE) ? 10 : 3)));
    public static final DeferredBlock<OutputCrateBlock> OUTPUT_CRATE = BLOCKS.register("output_crate",
            () -> new OutputCrateBlock(machineProps()));

    public static final DeferredBlock<EnergyCellBlock> ENERGY_CELL = BLOCKS.register("energy_cell",
            () -> new EnergyCellBlock(machineProps()));

    public static final DeferredItem<BlockItem> REAPER_BLOCK_ITEM = ITEMS.registerSimpleBlockItem(REAPER_BLOCK);
    public static final DeferredItem<BlockItem> MOB_FAN_ITEM = ITEMS.registerSimpleBlockItem(MOB_FAN);
    public static final DeferredItem<BlockItem> XP_TANK_ITEM = ITEMS.registerSimpleBlockItem(XP_TANK);
    public static final DeferredItem<BlockItem> ESSENCE_SPAWNER_ITEM = ITEMS.registerSimpleBlockItem(ESSENCE_SPAWNER);
    public static final DeferredItem<BlockItem> REAPER_GENERATOR_ITEM = ITEMS.registerSimpleBlockItem(REAPER_GENERATOR);
    public static final DeferredItem<BlockItem> SIMULATION_CHAMBER_ITEM = ITEMS.registerSimpleBlockItem(SIMULATION_CHAMBER);
    public static final DeferredItem<BlockItem> OUTPUT_CRATE_ITEM = ITEMS.registerSimpleBlockItem(OUTPUT_CRATE);
    public static final DeferredItem<BlockItem> ENERGY_CELL_ITEM = ITEMS.registerSimpleBlockItem(ENERGY_CELL);

    // ---- Items ----
    public static final DeferredItem<ReapersLanternItem> REAPERS_LANTERN = ITEMS.register("reapers_lantern",
            () -> new ReapersLanternItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<DataCoreItem> DATA_CORE = ITEMS.register("data_core",
            () -> new DataCoreItem(new Item.Properties()));

    public static final Map<UpgradeType, DeferredItem<UpgradeCardItem>> CARDS = new EnumMap<>(UpgradeType.class);
    static {
        for (UpgradeType type : UpgradeType.values()) {
            CARDS.put(type, ITEMS.register(type.itemName(), () -> new UpgradeCardItem(type, new Item.Properties())));
        }
    }

    // ---- Block entities ----
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReaperBlockEntity>> REAPER_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("reaper_block", () -> BlockEntityType.Builder.of(ReaperBlockEntity::new, REAPER_BLOCK.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MobFanBlockEntity>> MOB_FAN_ENTITY =
            BLOCK_ENTITIES.register("mob_fan", () -> BlockEntityType.Builder.of(MobFanBlockEntity::new, MOB_FAN.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<XpTankBlockEntity>> XP_TANK_ENTITY =
            BLOCK_ENTITIES.register("xp_tank", () -> BlockEntityType.Builder.of(XpTankBlockEntity::new, XP_TANK.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EssenceSpawnerBlockEntity>> ESSENCE_SPAWNER_ENTITY =
            BLOCK_ENTITIES.register("essence_spawner", () -> BlockEntityType.Builder.of(EssenceSpawnerBlockEntity::new, ESSENCE_SPAWNER.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReaperGeneratorBlockEntity>> REAPER_GENERATOR_ENTITY =
            BLOCK_ENTITIES.register("reaper_generator", () -> BlockEntityType.Builder.of(ReaperGeneratorBlockEntity::new, REAPER_GENERATOR.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SimulationChamberBlockEntity>> SIMULATION_CHAMBER_ENTITY =
            BLOCK_ENTITIES.register("simulation_chamber", () -> BlockEntityType.Builder.of(SimulationChamberBlockEntity::new, SIMULATION_CHAMBER.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OutputCrateBlockEntity>> OUTPUT_CRATE_ENTITY =
            BLOCK_ENTITIES.register("output_crate", () -> BlockEntityType.Builder.of(OutputCrateBlockEntity::new, OUTPUT_CRATE.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyCellBlockEntity>> ENERGY_CELL_ENTITY =
            BLOCK_ENTITIES.register("energy_cell", () -> BlockEntityType.Builder.of(EnergyCellBlockEntity::new, ENERGY_CELL.get()).build(null));

    // ---- Menus ----
    public static final DeferredHolder<MenuType<?>, MenuType<ReaperMenu>> REAPER_MENU =
            MENUS.register("reaper_block", () -> IMenuTypeExtension.create(ReaperMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<CardMachineMenu>> MOB_FAN_MENU =
            MENUS.register("mob_fan", () -> IMenuTypeExtension.create(CardMachineMenu::mobFanClient));
    public static final DeferredHolder<MenuType<?>, MenuType<CardMachineMenu>> XP_TANK_MENU =
            MENUS.register("xp_tank", () -> IMenuTypeExtension.create(CardMachineMenu::xpTankClient));
    public static final DeferredHolder<MenuType<?>, MenuType<SpawnerMenu>> SPAWNER_MENU =
            MENUS.register("essence_spawner", () -> IMenuTypeExtension.create(SpawnerMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<GeneratorMenu>> GENERATOR_MENU =
            MENUS.register("reaper_generator", () -> IMenuTypeExtension.create(GeneratorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<SimulationMenu>> SIMULATION_MENU =
            MENUS.register("simulation_chamber", () -> IMenuTypeExtension.create(SimulationMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<CrateMenu>> CRATE_MENU =
            MENUS.register("output_crate", () -> IMenuTypeExtension.create(CrateMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<CellMenu>> CELL_MENU =
            MENUS.register("energy_cell", () -> IMenuTypeExtension.create(CellMenu::new));

    // ---- Creative tab ----
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.grimsoul"))
            .icon(() -> REAPER_BLOCK_ITEM.get().getDefaultInstance())
            .displayItems((params, output) -> {
                output.accept(REAPER_BLOCK_ITEM.get());
                output.accept(MOB_FAN_ITEM.get());
                output.accept(XP_TANK_ITEM.get());
                output.accept(ESSENCE_SPAWNER_ITEM.get());
                output.accept(REAPER_GENERATOR_ITEM.get());
                output.accept(ENERGY_CELL_ITEM.get());
                output.accept(SIMULATION_CHAMBER_ITEM.get());
                output.accept(OUTPUT_CRATE_ITEM.get());
                output.accept(DATA_CORE.get());
                output.accept(REAPERS_LANTERN.get());
                for (UpgradeType type : UpgradeType.values()) {
                    output.accept(CARDS.get(type).get());
                }
            })
            .build());

    // ---- Tags ----
    /** Mobs the Reaper's Lantern can never capture. Bosses are blocked separately through c:bosses. */
    public static final TagKey<EntityType<?>> CAPTURE_BLACKLIST =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(MODID, "capture_blacklist"));
    /** Mobs the Reaper Block will never attack. */
    public static final TagKey<EntityType<?>> REAPER_IMMUNE =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(MODID, "reaper_immune"));

    /** Mobs a Data Core can never be bound to (and the Simulation Chamber will never run). */
    public static final TagKey<EntityType<?>> SIMULATION_BLACKLIST =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(MODID, "simulation_blacklist"));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        COMPONENTS.register(modBus);
        TABS.register(modBus);
        FLUID_TYPES.register(modBus);
        FLUIDS.register(modBus);
    }

    private Registration() {}
}
