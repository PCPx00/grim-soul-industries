package com.xtri6.grimsoul.client.guide;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.xtri6.grimsoul.client.guide.GuideBook.Category;
import com.xtri6.grimsoul.client.guide.GuideBook.Entry;
import com.xtri6.grimsoul.client.guide.GuideBook.Page;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.lwjgl.glfw.GLFW;

/**
 * The Grim Soul Codex: a two-page book. The contents page lists the categories in progression
 * order; each category lists its entries; entries are text and live crafting grids read from the
 * game's recipes (so they always match the pack, even if a pack maker changes a recipe).
 */
public class GuideScreen extends Screen {
    // Book layout
    private static final int BOOK_W = 300, BOOK_H = 200;
    private static final int PAGE_W = 118, PAGE_TOP = 18, PAGE_BOTTOM = 172, LINE = 10;
    private static final int LEFT_X = 20, RIGHT_X = 162;
    private static final int HEADER_H = 24;

    // Colors: dark purple leather cover, pale lavender pages
    private static final int EDGE = 0xFF07050C, COVER = 0xFF2A1450, COVER_TRIM = 0xFF5A2AA8, COVER_GEM = 0xFFB98CFF;
    private static final int PAPER = 0xFFF1EBF7, PAPER_EDGE = 0xFFD6CBE3, PAPER_SHADE = 0xFFE2D8EE;
    private static final int INK = 0xFF2A2333, INK_DIM = 0xFF6B6178, INK_ACCENT = 0xFF5A2AA8, INK_LINK = 0xFF6A2FC4;
    private static final int HOVER = 0x306A2FC4, SLOT = 0xFFC9BCD9, SLOT_DARK = 0xFF8F82A0;

    private static GuideBook cached;
    // Where the reader left off, so reopening the book returns to the same page.
    private static int lastCategory = -1, lastEntry = -1, lastSpread = 0;

    private final GuideBook book;
    private int category = -1;   // -1 = contents
    private int entry = -1;      // -1 = category index
    private int spread = 0;
    private List<RenderPage> pages = List.of();

    private int left, top;
    private final List<Click> clicks = new ArrayList<>();
    private ItemStack hoveredStack = ItemStack.EMPTY;

    private record Click(int x1, int y1, int x2, int y2, Runnable action) {
        boolean contains(double mx, double my) {
            return mx >= x1 && mx < x2 && my >= y1 && my < y2;
        }
    }

    /** One physical page: either text lines, or a recipe plus caption lines. */
    private record RenderPage(boolean header, List<FormattedCharSequence> lines, Optional<ResourceLocation> recipe) {
    }

    public GuideScreen() {
        super(Component.translatable("item.grimsoul.guide_book"));
        if (cached == null) {
            cached = GuideBook.load();
        }
        this.book = cached;
        this.category = lastCategory;
        this.entry = lastEntry;
        this.spread = lastSpread;
    }

    /** Drops the cached book so a resource reload (F3+T) picks up edited guide files. */
    public static void invalidate() {
        cached = null;
    }

    @Override
    protected void init() {
        left = (width - BOOK_W) / 2;
        top = (height - BOOK_H) / 2;
        if (category >= book.categories.size() || (category >= 0 && entry >= book.categories.get(category).entries().size())) {
            category = -1;
            entry = -1;
        }
        rebuildPages();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ------------------------------------------------------------------------------------------
    // Navigation
    // ------------------------------------------------------------------------------------------

    private void open(int cat, int ent, int spr) {
        category = cat;
        entry = ent;
        spread = spr;
        lastCategory = cat;
        lastEntry = ent;
        lastSpread = spr;
        rebuildPages();
        playTurn();
    }

    private void playTurn() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
    }

    private int spreadCount() {
        return Math.max(1, (pages.size() + 1) / 2);
    }

    private void next() {
        if (category < 0) {
            if (!book.categories.isEmpty()) {
                open(0, -1, 0);
            }
        } else if (entry < 0) {
            if (!book.categories.get(category).entries().isEmpty()) {
                open(category, 0, 0);
            }
        } else if (spread < spreadCount() - 1) {
            open(category, entry, spread + 1);
        } else if (entry < book.categories.get(category).entries().size() - 1) {
            open(category, entry + 1, 0);
        } else if (category < book.categories.size() - 1) {
            open(category + 1, -1, 0);
        }
    }

    private void previous() {
        if (category < 0) {
            return;
        }
        if (entry < 0) {
            if (category == 0) {
                open(-1, -1, 0);
            } else {
                Category prev = book.categories.get(category - 1);
                if (prev.entries().isEmpty()) {
                    open(category - 1, -1, 0);
                } else {
                    openLastSpread(category - 1, prev.entries().size() - 1);
                }
            }
        } else if (spread > 0) {
            open(category, entry, spread - 1);
        } else if (entry > 0) {
            openLastSpread(category, entry - 1);
        } else {
            open(category, -1, 0);
        }
    }

    private void openLastSpread(int cat, int ent) {
        open(cat, ent, 0);
        open(cat, ent, spreadCount() - 1);
    }

    private void back() {
        if (entry >= 0) {
            open(category, -1, 0);
        } else if (category >= 0) {
            open(-1, -1, 0);
        }
    }

    private boolean hasNext() {
        if (category < 0) {
            return !book.categories.isEmpty();
        }
        if (entry < 0) {
            return !book.categories.get(category).entries().isEmpty();
        }
        return spread < spreadCount() - 1 || entry < book.categories.get(category).entries().size() - 1
                || category < book.categories.size() - 1;
    }

    // ------------------------------------------------------------------------------------------
    // Text layout
    // ------------------------------------------------------------------------------------------

    private List<FormattedCharSequence> wrap(String text, int width) {
        List<FormattedCharSequence> out = new ArrayList<>();
        for (String paragraph : text.split("\n", -1)) {
            if (paragraph.isEmpty()) {
                out.add(FormattedCharSequence.EMPTY);
            } else {
                out.addAll(font.split(styled(paragraph), width));
            }
        }
        return out;
    }

    /**
     * Turns text with section-sign codes (colors, bold, italic, reset) into real styled pieces, so
     * styles stop exactly where the text says, even across wrapped lines.
     */
    static Component styled(String text) {
        net.minecraft.network.chat.MutableComponent out = Component.empty();
        net.minecraft.network.chat.Style style = net.minecraft.network.chat.Style.EMPTY;
        StringBuilder run = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u00a7' && i + 1 < text.length()) {
                if (!run.isEmpty()) {
                    out.append(Component.literal(run.toString()).withStyle(style));
                    run.setLength(0);
                }
                net.minecraft.ChatFormatting format = net.minecraft.ChatFormatting.getByCode(text.charAt(++i));
                if (format == net.minecraft.ChatFormatting.RESET || format == null) {
                    style = net.minecraft.network.chat.Style.EMPTY;
                } else if (format.isColor()) {
                    style = net.minecraft.network.chat.Style.EMPTY.withColor(format);
                } else {
                    style = style.applyFormat(format);
                }
                continue;
            }
            run.append(c);
        }
        if (!run.isEmpty()) {
            out.append(Component.literal(run.toString()).withStyle(style));
        }
        return out;
    }

    /** Turns the current entry's pages into physical pages, spilling long text onto extra pages. */
    private void rebuildPages() {
        if (category < 0 || entry < 0) {
            pages = List.of();
            return;
        }
        Entry current = book.categories.get(category).entries().get(entry);
        int headerH = headerHeight(current.title(), !current.icon().isEmpty());
        List<RenderPage> out = new ArrayList<>();
        boolean first = true;
        for (Page page : current.pages()) {
            List<FormattedCharSequence> lines = wrap(page.text(), PAGE_W);
            if (page.recipe().isPresent()) {
                int room = (PAGE_BOTTOM - PAGE_TOP - (first ? headerH : 0) - 82) / LINE;
                out.add(new RenderPage(first, new ArrayList<>(lines.subList(0, Math.min(room, lines.size()))), page.recipe()));
                first = false;
                if (lines.size() > room) {
                    lines = lines.subList(room, lines.size());
                } else {
                    continue;
                }
            }
            while (!lines.isEmpty()) {
                int room = (PAGE_BOTTOM - PAGE_TOP - (first ? headerH : 0)) / LINE;
                int take = Math.min(room, lines.size());
                // Don't start a page with a blank line.
                List<FormattedCharSequence> chunk = new ArrayList<>(lines.subList(0, take));
                out.add(new RenderPage(first, chunk, Optional.empty()));
                first = false;
                lines = lines.subList(take, lines.size());
                while (!lines.isEmpty() && lines.get(0) == FormattedCharSequence.EMPTY) {
                    lines = lines.subList(1, lines.size());
                }
            }
        }
        if (out.isEmpty()) {
            out.add(new RenderPage(true, List.of(), Optional.empty()));
        }
        pages = out;
        spread = Math.min(spread, spreadCount() - 1);
    }

    // ------------------------------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------------------------------

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        clicks.clear();
        hoveredStack = ItemStack.EMPTY;
        drawBook(g);

        if (category < 0) {
            drawContents(g, mouseX, mouseY);
        } else if (entry < 0) {
            drawCategory(g, mouseX, mouseY);
        } else {
            drawEntry(g, mouseX, mouseY);
        }
        drawNavigation(g, mouseX, mouseY);

        if (!hoveredStack.isEmpty()) {
            g.renderTooltip(font, hoveredStack, mouseX, mouseY);
        }
    }

    private void drawBook(GuiGraphics g) {
        int x = left, y = top;
        // Cover
        g.fill(x, y, x + BOOK_W, y + BOOK_H, EDGE);
        g.fill(x + 2, y + 2, x + BOOK_W - 2, y + BOOK_H - 2, COVER);
        g.fill(x + 4, y + 4, x + BOOK_W - 4, y + 5, COVER_TRIM);
        g.fill(x + 4, y + BOOK_H - 5, x + BOOK_W - 4, y + BOOK_H - 4, COVER_TRIM);
        for (int[] c : new int[][] {{4, 4}, {BOOK_W - 8, 4}, {4, BOOK_H - 8}, {BOOK_W - 8, BOOK_H - 8}}) {
            g.fill(x + c[0], y + c[1], x + c[0] + 4, y + c[1] + 4, COVER_GEM);
        }
        // Pages
        int pageY1 = y + 8, pageY2 = y + BOOK_H - 8;
        g.fill(x + 10, pageY1, x + BOOK_W / 2 - 1, pageY2, PAPER_EDGE);
        g.fill(x + BOOK_W / 2 + 1, pageY1, x + BOOK_W - 10, pageY2, PAPER_EDGE);
        g.fill(x + 11, pageY1 + 1, x + BOOK_W / 2 - 1, pageY2 - 1, PAPER);
        g.fill(x + BOOK_W / 2 + 1, pageY1 + 1, x + BOOK_W - 11, pageY2 - 1, PAPER);
        // Spine shading
        g.fill(x + BOOK_W / 2 - 6, pageY1 + 1, x + BOOK_W / 2 - 1, pageY2 - 1, PAPER_SHADE);
        g.fill(x + BOOK_W / 2 + 1, pageY1 + 1, x + BOOK_W / 2 + 6, pageY2 - 1, PAPER_SHADE);
        g.fill(x + BOOK_W / 2 - 1, pageY1, x + BOOK_W / 2 + 1, pageY2, COVER);
    }

    private void drawLines(GuiGraphics g, List<FormattedCharSequence> lines, int x, int y) {
        for (FormattedCharSequence line : lines) {
            g.drawString(font, line, x, y, INK, false);
            y += LINE;
        }
    }

    private List<FormattedCharSequence> titleLines(String title, boolean icon) {
        List<FormattedCharSequence> lines = font.split(styled(title).copy().withStyle(s -> s.withBold(true)), PAGE_W - (icon ? 20 : 0));
        return lines.size() > 2 ? lines.subList(0, 2) : lines;
    }

    /** Height a page header takes: one line of title, or two when the title wraps. */
    private int headerHeight(String title, boolean icon) {
        return titleLines(title, icon).size() > 1 ? HEADER_H + LINE : HEADER_H;
    }

    private int drawHeader(GuiGraphics g, ItemStack icon, String title, int x, int y, int mouseX, int mouseY) {
        int tx = x;
        List<FormattedCharSequence> lines = titleLines(title, !icon.isEmpty());
        if (!icon.isEmpty()) {
            item(g, icon, x, y - 4 + (lines.size() - 1) * LINE / 2, mouseX, mouseY);
            tx = x + 20;
        }
        int ly = y;
        for (FormattedCharSequence line : lines) {
            g.drawString(font, line, tx, ly, INK_ACCENT, false);
            ly += LINE;
        }
        int bottom = y + 13 + (lines.size() - 1) * LINE;
        g.fill(x, bottom, x + PAGE_W, bottom + 1, PAPER_EDGE);
        return y + headerHeight(title, !icon.isEmpty());
    }

    private void drawContents(GuiGraphics g, int mouseX, int mouseY) {
        int lx = left + LEFT_X, ty = top + PAGE_TOP;
        // Title page
        ItemStack bookIcon = new ItemStack(com.xtri6.grimsoul.Registration.GUIDE_BOOK.get());
        g.pose().pushPose();
        g.pose().translate(lx + PAGE_W / 2.0 - 16, ty, 0);
        g.pose().scale(2.0F, 2.0F, 1.0F);
        g.renderItem(bookIcon, 0, 0);
        g.pose().popPose();
        int y = ty + 38;
        for (FormattedCharSequence line : font.split(Component.literal(book.title).withStyle(s -> s.withBold(true)), PAGE_W)) {
            g.drawString(font, line, lx + (PAGE_W - font.width(line)) / 2, y, INK_ACCENT, false);
            y += LINE;
        }
        g.fill(lx + 20, y + 2, lx + PAGE_W - 20, y + 3, PAPER_EDGE);
        List<FormattedCharSequence> intro = wrap(book.intro, PAGE_W);
        int room = (top + PAGE_BOTTOM - (y + 8)) / LINE;
        drawLines(g, intro.subList(0, Math.min(room, intro.size())), lx, y + 8);

        // Category list
        int rx = left + RIGHT_X;
        g.drawString(font, Component.translatable("guide.grimsoul.contents").withStyle(s -> s.withBold(true)), rx, ty, INK_ACCENT, false);
        g.fill(rx, ty + 13, rx + PAGE_W, ty + 14, PAPER_EDGE);
        int rowY = ty + 20;
        for (int i = 0; i < book.categories.size(); i++) {
            Category cat = book.categories.get(i);
            int index = i;
            rowY = listRow(g, cat.icon(), (i + 1) + ". " + cat.title(), rx, rowY, mouseX, mouseY, () -> open(index, -1, 0));
        }
    }

    /** A clickable list row with an icon; returns the next row's y. */
    private int listRow(GuiGraphics g, ItemStack icon, String label, int x, int y, int mouseX, int mouseY, Runnable action) {
        List<FormattedCharSequence> lines = font.split(styled(label), PAGE_W - 22);
        int h = Math.max(18, lines.size() * LINE + 6);
        boolean hover = mouseX >= x - 2 && mouseX < x + PAGE_W + 2 && mouseY >= y && mouseY < y + h;
        if (hover) {
            g.fill(x - 2, y, x + PAGE_W + 2, y + h, HOVER);
        }
        if (!icon.isEmpty()) {
            g.renderItem(icon, x, y + 1);
        }
        int ly = y + (h - lines.size() * LINE) / 2 + 1;
        for (FormattedCharSequence line : lines) {
            g.drawString(font, line, x + 20, ly, hover ? INK_LINK : INK, false);
            ly += LINE;
        }
        clicks.add(new Click(x - 2, y, x + PAGE_W + 2, y + h, action));
        return y + h + 1;
    }

    private void drawCategory(GuiGraphics g, int mouseX, int mouseY) {
        Category cat = book.categories.get(category);
        int lx = left + LEFT_X, ty = top + PAGE_TOP;
        int textY = drawHeader(g, cat.icon(), cat.title(), lx, ty, mouseX, mouseY);
        drawLines(g, wrap(cat.description(), PAGE_W), lx, textY);

        int rx = left + RIGHT_X;
        g.drawString(font, Component.translatable("guide.grimsoul.entries").withStyle(s -> s.withBold(true)), rx, ty, INK_ACCENT, false);
        g.fill(rx, ty + 13, rx + PAGE_W, ty + 14, PAPER_EDGE);
        int rowY = ty + 18;
        List<Entry> entries = cat.entries();
        for (int i = 0; i < entries.size(); i++) {
            int index = i;
            rowY = listRow(g, entries.get(i).icon(), entries.get(i).title(), rx, rowY, mouseX, mouseY, () -> open(category, index, 0));
        }
    }

    private void drawEntry(GuiGraphics g, int mouseX, int mouseY) {
        Entry current = book.categories.get(category).entries().get(entry);
        for (int side = 0; side < 2; side++) {
            int index = spread * 2 + side;
            if (index >= pages.size()) {
                break;
            }
            RenderPage page = pages.get(index);
            int px = left + (side == 0 ? LEFT_X : RIGHT_X);
            int y = top + PAGE_TOP;
            if (page.header()) {
                y = drawHeader(g, current.icon(), current.title(), px, y, mouseX, mouseY);
            }
            if (page.recipe().isPresent()) {
                y = drawRecipe(g, page.recipe().get(), px, y, mouseX, mouseY);
            }
            drawLines(g, page.lines(), px, y);
            // Page number
            String number = String.valueOf(index + 1);
            g.drawString(font, number, px + (PAGE_W - font.width(number)) / 2, top + BOOK_H - 20, INK_DIM, false);
        }
    }

    /** Draws a crafting grid for the recipe; returns the y below it. */
    private int drawRecipe(GuiGraphics g, ResourceLocation id, int x, int y, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        Optional<RecipeHolder<?>> holder = mc.level == null ? Optional.empty() : mc.level.getRecipeManager().byKey(id);
        if (holder.isEmpty()) {
            for (FormattedCharSequence line : font.split(Component.translatable("guide.grimsoul.no_recipe"), PAGE_W)) {
                g.drawString(font, line, x, y, INK_DIM, false);
                y += LINE;
            }
            return y + 4;
        }
        Recipe<?> recipe = holder.get().value();
        ItemStack result = recipe.getResultItem(mc.level.registryAccess());
        // Result name
        Component name = result.getHoverName().copy().withStyle(s -> s.withBold(true));
        List<FormattedCharSequence> nameLines = font.split(name, PAGE_W);
        for (int i = 0; i < Math.min(2, nameLines.size()); i++) {
            g.drawString(font, nameLines.get(i), x, y, INK_LINK, false);
            y += LINE;
        }
        y += 2;

        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        int w = 3, h = 3;
        if (recipe instanceof ShapedRecipe shaped) {
            w = shaped.getWidth();
            h = shaped.getHeight();
        }
        int gridX = x + 4, gridY = y + 2;
        long tick = System.currentTimeMillis() / 1000;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int sx = gridX + col * 18, sy = gridY + row * 18;
                slot(g, sx, sy);
                int index = recipe instanceof ShapedRecipe ? (col < w && row < h ? row * w + col : -1) : row * 3 + col;
                if (index >= 0 && index < ingredients.size()) {
                    ItemStack[] options = ingredients.get(index).getItems();
                    if (options.length > 0) {
                        item(g, options[(int) (tick % options.length)], sx + 1, sy + 1, mouseX, mouseY);
                    }
                }
            }
        }
        // Arrow
        int ax = gridX + 58, ay = gridY + 22;
        g.fill(ax, ay + 3, ax + 12, ay + 6, INK_DIM);
        for (int i = 0; i < 5; i++) {
            g.fill(ax + 12 + i, ay - 1 + i, ax + 13 + i, ay + 10 - i, INK_DIM);
        }
        // Result
        int rx = gridX + 80, ry = gridY + 17;
        slot(g, rx - 2, ry - 2);
        g.fill(rx - 3, ry - 3, rx + 21, ry - 2, INK_ACCENT);
        g.fill(rx - 3, ry + 20, rx + 21, ry + 21, INK_ACCENT);
        item(g, result, rx, ry, mouseX, mouseY);
        if (result.getCount() > 1) {
            g.renderItemDecorations(font, result, rx, ry);
        }
        return gridY + 58 + (nameLines.size() > 1 ? 0 : LINE);
    }

    private void slot(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, SLOT_DARK);
        g.fill(x + 1, y + 1, x + 18, y + 18, PAPER);
        g.fill(x + 1, y + 1, x + 17, y + 17, SLOT);
    }

    private void item(GuiGraphics g, ItemStack stack, int x, int y, int mouseX, int mouseY) {
        g.renderItem(stack, x, y);
        if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
            hoveredStack = stack;
        }
    }

    private void drawNavigation(GuiGraphics g, int mouseX, int mouseY) {
        int by = top + BOOK_H - 21;
        if (category >= 0) {
            arrow(g, left + 14, by, false, mouseX, mouseY, this::previous);
            String label = entry >= 0 ? Component.translatable("guide.grimsoul.back").getString()
                    : Component.translatable("guide.grimsoul.contents").getString();
            textButton(g, label, left + 32, by + 2, mouseX, mouseY, this::back);
        }
        if (hasNext()) {
            arrow(g, left + BOOK_W - 30, by, true, mouseX, mouseY, this::next);
        }
    }

    private void arrow(GuiGraphics g, int x, int y, boolean right, int mouseX, int mouseY, Runnable action) {
        boolean hover = mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 12;
        int color = hover ? INK_LINK : INK_DIM;
        for (int i = 0; i < 6; i++) {
            int col = right ? x + 4 + i : x + 11 - i;
            g.fill(col, y + i, col + 1, y + 12 - i, color);
        }
        g.fill(right ? x : x + 9, y + 5, right ? x + 5 : x + 14, y + 7, color);
        clicks.add(new Click(x - 2, y - 2, x + 18, y + 14, action));
    }

    private void textButton(GuiGraphics g, String label, int x, int y, int mouseX, int mouseY, Runnable action) {
        int w = font.width(label);
        boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y - 2 && mouseY < y + 10;
        g.drawString(font, label, x, y, hover ? INK_LINK : INK_DIM, false);
        if (hover) {
            g.fill(x, y + 9, x + w, y + 10, INK_LINK);
        }
        clicks.add(new Click(x, y - 2, x + w, y + 10, action));
    }

    // ------------------------------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (Click click : List.copyOf(clicks)) {
                if (click.contains(mouseX, mouseY)) {
                    click.action().run();
                    return true;
                }
            }
        } else if (button == 1 && category >= 0) {
            back();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY < 0 && hasNext()) {
            next();
        } else if (scrollY > 0 && category >= 0) {
            previous();
        }
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        switch (key) {
            case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_PAGE_DOWN -> {
                if (hasNext()) {
                    next();
                }
                return true;
            }
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_PAGE_UP -> {
                previous();
                return true;
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                back();
                return true;
            }
            default -> {
                return super.keyPressed(key, scanCode, modifiers);
            }
        }
    }
}
