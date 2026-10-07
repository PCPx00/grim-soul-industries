package com.xtri6.grimsoul.client.guide;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xtri6.grimsoul.GrimSoul;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The Grim Soul Codex's contents, read from assets/grimsoul/guide/&lt;language&gt;.json (falls back to
 * en_us). Categories run in progression order: the basics first, Virtualization last.
 */
public final class GuideBook {
    public record Page(String text, Optional<ResourceLocation> recipe) {
    }

    public record Entry(String title, ItemStack icon, List<Page> pages) {
    }

    public record Category(String title, ItemStack icon, String description, List<Entry> entries) {
    }

    public final String title;
    public final String intro;
    public final List<Category> categories;

    private GuideBook(String title, String intro, List<Category> categories) {
        this.title = title;
        this.intro = intro;
        this.categories = categories;
    }

    public static GuideBook load() {
        Minecraft mc = Minecraft.getInstance();
        String lang = mc.getLanguageManager().getSelected();
        for (String code : new String[] {lang, "en_us"}) {
            ResourceLocation file = ResourceLocation.fromNamespaceAndPath(GrimSoul.MODID, "guide/" + code + ".json");
            Optional<Resource> resource = mc.getResourceManager().getResource(file);
            if (resource.isEmpty()) {
                continue;
            }
            try (Reader reader = resource.get().openAsReader()) {
                return parse(JsonParser.parseReader(reader).getAsJsonObject());
            } catch (Exception e) {
                GrimSoul.LOGGER.error("Could not read the Grim Soul Codex from {}", file, e);
            }
        }
        return new GuideBook("Grim Soul Codex", "The guide could not be loaded.", List.of());
    }

    private static GuideBook parse(JsonObject root) {
        List<Category> categories = new ArrayList<>();
        for (JsonElement c : root.getAsJsonArray("categories")) {
            JsonObject cat = c.getAsJsonObject();
            List<Entry> entries = new ArrayList<>();
            for (JsonElement e : cat.getAsJsonArray("entries")) {
                JsonObject entry = e.getAsJsonObject();
                List<Page> pages = new ArrayList<>();
                JsonArray pageArray = entry.getAsJsonArray("pages");
                for (JsonElement p : pageArray) {
                    if (p.isJsonPrimitive()) {
                        pages.add(new Page(p.getAsString(), Optional.empty()));
                        continue;
                    }
                    JsonObject page = p.getAsJsonObject();
                    String text = page.has("text") ? page.get("text").getAsString() : "";
                    Optional<ResourceLocation> recipe = page.has("recipe")
                            ? Optional.ofNullable(ResourceLocation.tryParse(page.get("recipe").getAsString()))
                            : Optional.empty();
                    pages.add(new Page(text, recipe));
                }
                entries.add(new Entry(entry.get("title").getAsString(), icon(entry), pages));
            }
            categories.add(new Category(cat.get("title").getAsString(), icon(cat),
                    cat.has("description") ? cat.get("description").getAsString() : "", entries));
        }
        return new GuideBook(root.get("title").getAsString(), root.has("intro") ? root.get("intro").getAsString() : "", categories);
    }

    private static ItemStack icon(JsonObject object) {
        if (!object.has("icon")) {
            return ItemStack.EMPTY;
        }
        ResourceLocation id = ResourceLocation.tryParse(object.get("icon").getAsString());
        if (id == null) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.get(id);
        return new ItemStack(item);
    }
}
