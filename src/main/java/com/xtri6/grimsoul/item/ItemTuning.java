package com.xtri6.grimsoul.item;

import com.xtri6.grimsoul.Config;
import com.xtri6.grimsoul.GrimSoul;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

/** Applies each mod item's stack size from the config (items.&lt;name&gt;.maxStackSize in grimsoul-common.toml). */
public final class ItemTuning {
    private ItemTuning() {
    }

    public static void apply(ModifyDefaultComponentsEvent event) {
        if (!Config.SPEC.isLoaded()) {
            return;
        }
        event.getAllItems().forEach(item -> {
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
            if (!GrimSoul.MODID.equals(key.getNamespace())) {
                return;
            }
            var value = Config.ITEM_MAX_STACK.get(key.getPath());
            int size = value == null ? 0 : value.get();
            if (size > 0) {
                event.modify(item, builder -> builder.set(DataComponents.MAX_STACK_SIZE, size));
            }
        });
    }
}
