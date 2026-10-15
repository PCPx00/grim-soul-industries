package com.xtri6.grimsoul.compat.jade;

import com.xtri6.grimsoul.GrimSoul;
import com.xtri6.grimsoul.block.EssenceSpawnerBlock;
import com.xtri6.grimsoul.block.ReaperBlock;
import com.xtri6.grimsoul.block.ReaperGeneratorBlock;
import com.xtri6.grimsoul.block.SimulationChamberBlock;
import com.xtri6.grimsoul.blockentity.EssenceSpawnerBlockEntity;
import com.xtri6.grimsoul.blockentity.ReaperBlockEntity;
import com.xtri6.grimsoul.blockentity.ReaperGeneratorBlockEntity;
import com.xtri6.grimsoul.blockentity.SimulationChamberBlockEntity;

import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Jade support (loaded only when Jade is installed). Looking at a machine shows its key facts
 * without opening it: power, bound or captured mob, and Reaper Block state and stored XP.
 */
@WailaPlugin
public class GrimSoulJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        MachineInfo.Server provider = MachineInfo.SERVER;
        registration.registerBlockDataProvider(provider, ReaperBlockEntity.class);
        registration.registerBlockDataProvider(provider, EssenceSpawnerBlockEntity.class);
        registration.registerBlockDataProvider(provider, SimulationChamberBlockEntity.class);
        registration.registerBlockDataProvider(provider, ReaperGeneratorBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        MachineInfo.Client component = MachineInfo.CLIENT;
        registration.registerBlockComponent(component, ReaperBlock.class);
        registration.registerBlockComponent(component, EssenceSpawnerBlock.class);
        registration.registerBlockComponent(component, SimulationChamberBlock.class);
        registration.registerBlockComponent(component, ReaperGeneratorBlock.class);
    }
}
