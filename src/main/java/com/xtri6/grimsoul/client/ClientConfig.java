package com.xtri6.grimsoul.client;

import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Client-only settings (config/grimsoul-client.toml). */
public final class ClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.EnumValue<GuiStyle.Theme> THEME = BUILDER
            .comment("Look of the Grim Soul Industries machine screens: REAPER (light gray and purple), VANILLA (classic Minecraft) or DARK.",
                    "You can also switch it with the small button in the top-right corner of any machine screen.")
            .defineEnum("gui.theme", GuiStyle.Theme.REAPER);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ClientConfig() {}

    static void onLoad(ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) {
            GuiStyle.apply(THEME.get());
        }
    }

    static void saveTheme(GuiStyle.Theme theme) {
        if (SPEC.isLoaded()) {
            THEME.set(theme);
            THEME.save();
        }
    }
}
