package com.xtri6.grimsoul.compat.jade;

import com.xtri6.grimsoul.GrimSoul;
import com.xtri6.grimsoul.blockentity.EssenceSpawnerBlockEntity;
import com.xtri6.grimsoul.blockentity.ReaperBlockEntity;
import com.xtri6.grimsoul.blockentity.SimulationChamberBlockEntity;
import com.xtri6.grimsoul.client.GuiStyle;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** The server sends a machine's live facts; the client draws them as Jade tooltip lines. */
final class MachineInfo {
    private static final String ROOT = "grimsoul";
    static final Server SERVER = new Server();
    static final Client CLIENT = new Client();

    private MachineInfo() {
    }

    static ResourceLocation uid(String path) {
        return ResourceLocation.fromNamespaceAndPath(GrimSoul.MODID, path);
    }

    static final class Server implements IServerDataProvider<BlockAccessor> {
        @Override
        public ResourceLocation getUid() {
            return uid("machine_server");
        }

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            BlockEntity be = accessor.getBlockEntity();
            CompoundTag info = new CompoundTag();
            if (be instanceof ReaperBlockEntity reaper) {
                var d = reaper.getDataAccess();
                info.putBoolean("Enabled", d.get(ReaperBlockEntity.DATA_ENABLED) != 0);
                int xp = (d.get(ReaperBlockEntity.DATA_XP_LOW) & 0xFFFF) | ((d.get(ReaperBlockEntity.DATA_XP_HIGH) & 0xFFFF) << 16);
                info.putInt("Xp", xp);
            } else if (be instanceof EssenceSpawnerBlockEntity spawner) {
                spawner.getCapturedMob().ifPresent(type -> info.putString("Mob", BuiltInRegistries.ENTITY_TYPE.getKey(type).toString()));
            } else if (be instanceof SimulationChamberBlockEntity chamber) {
                chamber.getCoreMob().ifPresent(type -> info.putString("Mob", BuiltInRegistries.ENTITY_TYPE.getKey(type).toString()));
            }
            if (be != null && be.getLevel() != null) {
                IEnergyStorage energy = be.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, be.getBlockPos(), (Direction) null);
                if (energy != null) {
                    info.putInt("Energy", energy.getEnergyStored());
                    info.putInt("MaxEnergy", energy.getMaxEnergyStored());
                }
            }
            data.put(ROOT, info);
        }
    }

    static final class Client implements IBlockComponentProvider {
        @Override
        public ResourceLocation getUid() {
            return uid("machine_client");
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!accessor.getServerData().contains(ROOT)) {
                return;
            }
            CompoundTag info = accessor.getServerData().getCompound(ROOT);
            if (info.contains("Enabled")) {
                tooltip.add(Component.translatable("jade.grimsoul.power", Component.translatable(info.getBoolean("Enabled") ? "gui.grimsoul.on" : "gui.grimsoul.off")));
            }
            if (info.contains("Xp")) {
                tooltip.add(Component.translatable("jade.grimsoul.xp", GuiStyle.compact(info.getInt("Xp"))));
            }
            if (info.contains("Mob")) {
                ResourceLocation key = ResourceLocation.parse(info.getString("Mob"));
                EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(key);
                tooltip.add(Component.translatable("jade.grimsoul.mob", type.getDescription().copy().withStyle(ChatFormatting.LIGHT_PURPLE)));
            }
            if (info.contains("Energy")) {
                tooltip.add(Component.translatable("jade.grimsoul.energy",
                        GuiStyle.compact(info.getInt("Energy")), GuiStyle.compact(info.getInt("MaxEnergy"))));
            }
        }
    }
}
