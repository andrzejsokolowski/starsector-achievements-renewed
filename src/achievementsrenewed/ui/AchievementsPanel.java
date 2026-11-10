package achievementsrenewed.ui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CustomVisualDialogDelegate.DialogCallbacks;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;

import achievementsrenewed.AchievementNoticeIntel;
import achievementsrenewed.Catalog;
import achievementsrenewed.Ids;
import achievementsrenewed.Settings;
import achievementsrenewed.api.AchievementSpec;
import achievementsrenewed.api.Achievements;
import achievementsrenewed.compat.ExternalAchievement;
import achievementsrenewed.compat.ExternalSources;

/**
 * The achievements screen: one scrolling list per tab. The Achievements tab lists this mod's own system,
 * grouped by mod, then category, then chain. The Other mods tab (only there when something is found)
 * lists what MagicLib and the milestone skills show, read-only.
 *
 * A row that is not done is greyed out and shows its hint, or the exact condition when that LunaLib
 * setting is on. A done row shows the name, the description and the reward line.
 *
 * Each tab is one container panel built once; a tab switch moves the other container off screen (a
 * scrolling TooltipMakerAPI cannot be taken out of a panel again). The list is a scrolling
 * TooltipMakerAPI holding a content panel with the labels; row backgrounds, icons, bars and buttons are
 * drawn and hit-tested by this panel (as in Rule Book - Renewed and Gambling Den), which reads back each
 * frame where the scrolled content really is on screen. Nothing changes while the screen is open, so
 * every text is laid out once.
 */
public final class AchievementsPanel extends BaseCustomUIPanelPlugin {

    private static final float PAD = 16f, TITLE_H = 34f, TAB_H = 28f, FOOT_H = 36f;
    private static final float SCROLLBAR_W = 22f, HIDDEN_X = -20000f;
    private static final float MOD_H = 34f, CATEGORY_H = 26f, CHAIN_H = 22f, GAP = 4f, MOD_GAP = 12f;
    private static final float ROW_X = 10f, ROW_PAD = 8f, ICON = 48f, TEXT_GAP = 12f, LINE_GAP = 3f;
    private static final float STATUS_W = 150f, CHAIN_INDENT = 22f, BAR_W = 110f, BAR_H = 6f;

    private static final Color BG = new Color(10, 13, 19);
    private static final Color PANEL = new Color(20, 25, 34);
    private static final Color ROW_FILL = new Color(24, 30, 40);
    private static final Color BAR_BACK = new Color(48, 56, 68);
    private static final Color GOLD = new Color(235, 185, 100);
    private static final Color GREEN = new Color(120, 210, 130);
    private static final Color DIM = new Color(150, 157, 173);
    private static final Color FAINT = new Color(105, 110, 121);
    private static final Color TEXT = new Color(220, 224, 232);

    private static final int MOD = 0, CATEGORY = 1, CHAIN = 2, ROW = 3;

    /** One line of a list: a heading or an achievement. The texts first, then where it sits. */
    private static final class Item {
        final int kind;
        String title = "", line2 = "", line3 = "", status = "", status2 = "", icon;
        Color titleColor = TEXT, line2Color = DIM, statusColor = DIM;
        boolean done, hidden, chained, chainStart, chainEnd;
        int progress, goal;
        float y, h, titleH, line2H, line3H;

        Item(int kind) {
            this.kind = kind;
        }
    }

    private static final class Page {
        final List<Item> items = new ArrayList<>();
        int done, total;
        CustomPanelAPI container, content;
        float contentLeft, contentTop;
    }

    private static final class Btn {
        final String action;
        final float x, y, w, h;
        boolean checked, hover;

        Btn(String action, float x, float y, float w, float h) {
            this.action = action;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
    }

    private final float panelW, panelH, listW;
    private final List<Btn> buttons = new ArrayList<>();
    private final List<Page> pages = new ArrayList<>();
    private final Map<String, SpriteAPI> sprites = new HashMap<>();
    private CustomPanelAPI panel, scratch;
    private DialogCallbacks callbacks;
    private PositionAPI position;
    private LabelAPI summary;
    private float listTop, listH;
    private int tab;
    private boolean dismissed, wasLeftDown;

    public AchievementsPanel() {
        float screenW = Global.getSettings().getScreenWidth();
        float screenH = Global.getSettings().getScreenHeight();
        panelW = Math.max(Math.min(1000f, screenW - 80f), Math.min(760f, screenW - 10f));
        panelH = Math.max(Math.min(940f, screenH - 80f), Math.min(600f, screenH - 10f));
        listW = panelW - PAD * 2f;
    }

    public float width() {
        return panelW;
    }

    public float height() {
        return panelH;
    }

    public void init(CustomPanelAPI panel, DialogCallbacks callbacks) {
        this.panel = panel;
        this.callbacks = callbacks;
        wasLeftDown = Mouse.isCreated() && Mouse.isButtonDown(0);
        scratch = panel.createCustomPanel(listW, 100f, null);

        Page own = ownPage();
        Page other = otherPage();
        boolean tabs = !other.items.isEmpty();
        listTop = PAD + TITLE_H + (tabs ? TAB_H + 8f : 0f);
        listH = panelH - listTop - PAD - FOOT_H - 8f;

        label(panel, "ACHIEVEMENTS", GOLD, PAD, PAD + 2f, 320f, 26f, Fonts.ORBITRON_20AA, Alignment.LMID);
        summary = label(panel, "", TEXT, panelW - PAD - 320f, PAD + 5f, 320f, 20f, Fonts.DEFAULT_SMALL, Alignment.RMID);

        build(own);
        if (tabs) {
            build(other);
            button("tab:0", "Achievements", PAD, PAD + TITLE_H, 150f, TAB_H);
            button("tab:1", "Other mods", PAD + 156f, PAD + TITLE_H, 150f, TAB_H);
        }

        float footY = panelH - PAD - FOOT_H;
        if (!Settings.showHowTo()) {
            label(panel, "Exact conditions: LunaLib setting \"Show how to get achievements\".", FAINT,
                    PAD, footY + 9f, listW - 140f, 18f, Fonts.DEFAULT_SMALL, Alignment.LMID);
        }
        button("close", "Close", panelW - PAD - 120f, footY, 120f, FOOT_H);
        showTab(0);
    }

    // ---- what the lists say ----

    /** This mod's own system: mod, then category, then chain. */
    private Page ownPage() {
        Page page = new Page();
        boolean howTo = Settings.showHowTo();
        List<AchievementSpec> all = Achievements.all();
        Map<String, int[]> perMod = new HashMap<>();
        for (AchievementSpec spec : all) {
            int[] counts = perMod.computeIfAbsent(spec.modId, id -> new int[2]);
            if (spec.isDone()) counts[0]++;
            counts[1]++;
        }

        String mod = null, category = null, chain = null;
        Item lastChained = null;
        for (AchievementSpec spec : all) {
            boolean newMod = !spec.modId.equals(mod);
            boolean newCategory = newMod || !spec.category.equals(category);
            boolean newChain = newCategory || !spec.chain.equals(chain);
            if (newChain && lastChained != null) {
                lastChained.chainEnd = true;
                lastChained = null;
            }
            if (newMod) {
                mod = spec.modId;
                int[] counts = perMod.get(mod);
                page.items.add(heading(MOD, Catalog.modName(mod), counts[0] + " / " + counts[1]));
            }
            if (newCategory) {
                category = spec.category;
                if (!category.isEmpty()) page.items.add(heading(CATEGORY, category, ""));
            }
            if (newChain) {
                chain = spec.chain;
                if (!chain.isEmpty()) page.items.add(heading(CHAIN, chain, ""));
            }

            Item row = new Item(ROW);
            row.icon = spec.icon;
            row.done = spec.isDone();
            if (row.done) {
                row.title = spec.name;
                row.titleColor = Color.WHITE;
                row.line2 = AchievementNoticeIntel.body(spec);
                row.line3 = spec.reward;
                row.status = "Done";
                row.statusColor = GREEN;
                String date = Achievements.doneDate(spec.id);
                row.status2 = date == null ? "" : date;
                page.done++;
            } else {
                boolean revealed = spec.isRevealed();
                row.hidden = !revealed && !howTo;
                row.title = howTo ? spec.howTo : revealed ? spec.hint : "???";
                row.titleColor = DIM;
                if (howTo && !spec.reward.isEmpty()) {
                    row.line2 = spec.reward;
                    row.line2Color = FAINT;
                }
                if (spec.isCounted() && !row.hidden) {
                    row.goal = spec.goal;
                    row.progress = Math.max(0, Math.min(spec.goal, spec.progress()));
                    row.status = row.progress + " / " + row.goal;
                    row.statusColor = TEXT;
                }
            }
            page.total++;
            if (!spec.chain.isEmpty()) {
                row.chained = true;
                row.chainStart = lastChained == null;
                lastChained = row;
            }
            page.items.add(row);
        }
        if (lastChained != null) lastChained.chainEnd = true;
        return page;
    }

    /** What other systems show, grouped by the mod each achievement comes from. */
    private Page otherPage() {
        Page page = new Page();
        List<ExternalAchievement> all = ExternalSources.readAll();
        Map<String, int[]> perGroup = new HashMap<>();
        for (ExternalAchievement a : all) {
            int[] counts = perGroup.computeIfAbsent(a.group, id -> new int[2]);
            if (a.done) counts[0]++;
            counts[1]++;
        }
        String group = null;
        for (ExternalAchievement a : all) {
            if (!a.group.equals(group)) {
                group = a.group;
                int[] counts = perGroup.get(group);
                page.items.add(heading(MOD, group, counts[0] + " / " + counts[1]));
            }
            Item row = new Item(ROW);
            row.icon = a.icon.isEmpty() ? Ids.ICON : a.icon;
            row.done = a.done;
            row.hidden = a.hidden;
            row.title = a.hidden || a.name.isEmpty() ? "???" : a.name;
            row.titleColor = a.done ? Color.WHITE : DIM;
            row.line2 = a.hidden ? "" : a.description;
            row.line2Color = a.done ? DIM : FAINT;
            if (a.done) {
                row.status = "Done";
                row.statusColor = GREEN;
                page.done++;
            } else if (a.goal > 1 && !a.hidden) {
                row.goal = a.goal;
                row.progress = Math.max(0, Math.min(a.goal, a.progress));
                row.status = row.progress + " / " + row.goal;
                row.statusColor = TEXT;
            }
            page.total++;
            page.items.add(row);
        }
        return page;
    }

    private static Item heading(int kind, String title, String status) {
        Item item = new Item(kind);
        item.title = title;
        item.status = status;
        return item;
    }

    // ---- building ----

    private float contentW() {
        return listW - SCROLLBAR_W;
    }

    private float rowLeft(Item item) {
        return item.chained ? CHAIN_INDENT : 0f;
    }

    private float textX(Item item) {
        return rowLeft(item) + ROW_X + ICON + TEXT_GAP;
    }

    private float textW(Item item) {
        return contentW() - textX(item) - STATUS_W - TEXT_GAP * 2f;
    }

    /** The height a text takes when wrapped to a width, measured on a label that is never shown. */
    private float measure(String text, String font, float width) {
        LabelAPI label = Global.getSettings().createLabel(text, font);
        scratch.addComponent((UIComponentAPI) label);
        label.autoSizeToWidth(width);
        return label.getPosition().getHeight();
    }

    /** Lays the items out top to bottom, then builds the container, the scroller and every label. */
    private void build(Page page) {
        float y = 0f;
        for (Item item : page.items) {
            if (item.kind == MOD) {
                if (y > 0f) y += MOD_GAP;
                item.h = MOD_H;
            } else if (item.kind == CATEGORY) {
                item.h = CATEGORY_H;
            } else if (item.kind == CHAIN) {
                item.h = CHAIN_H;
            } else {
                float w = textW(item);
                item.titleH = measure(item.title, Fonts.INSIGNIA_LARGE, w);
                float text = item.titleH;
                if (!item.line2.isEmpty()) {
                    item.line2H = measure(item.line2, Fonts.DEFAULT_SMALL, w);
                    text += LINE_GAP + item.line2H;
                }
                if (!item.line3.isEmpty()) {
                    item.line3H = measure(item.line3, Fonts.DEFAULT_SMALL, w);
                    text += LINE_GAP + item.line3H;
                }
                item.h = Math.max(ICON, text) + ROW_PAD * 2f;
            }
            item.y = y;
            y += item.h + GAP;
        }
        float contentH = Math.max(listH, y + GAP);

        page.container = panel.createCustomPanel(listW, listH, null);
        panel.addComponent(page.container).inTL(PAD, listTop);
        page.contentLeft = PAD;
        page.contentTop = listTop;

        TooltipMakerAPI scroller = page.container.createUIElement(listW, listH, true);
        page.content = page.container.createCustomPanel(listW, contentH, null);
        for (Item item : page.items) addLabels(page.content, item);
        scroller.addCustom(page.content, 0f);
        scroller.setHeightSoFar(contentH);
        page.container.addUIElement(scroller).inTL(0f, 0f);
        pages.add(page);
    }

    private void addLabels(CustomPanelAPI content, Item item) {
        float statusX = contentW() - STATUS_W - TEXT_GAP;
        if (item.kind == MOD) {
            label(content, item.title.toUpperCase(), GOLD, 12f, item.y + 8f, contentW() - STATUS_W - 40f, 18f,
                    Fonts.ORBITRON_12, Alignment.LMID);
            label(content, item.status, TEXT, statusX, item.y + 8f, STATUS_W, 18f, Fonts.DEFAULT_SMALL, Alignment.RMID);
            return;
        }
        if (item.kind == CATEGORY) {
            label(content, item.title, TEXT, 12f, item.y + 4f, contentW() - 24f, 18f, Fonts.DEFAULT_SMALL, Alignment.LMID);
            return;
        }
        if (item.kind == CHAIN) {
            label(content, item.title, DIM, CHAIN_INDENT + ROW_X, item.y + 3f, contentW() - 60f, 18f,
                    Fonts.DEFAULT_SMALL, Alignment.LMID);
            return;
        }

        float x = textX(item), w = textW(item);
        float text = item.titleH + (item.line2.isEmpty() ? 0f : LINE_GAP + item.line2H)
                + (item.line3.isEmpty() ? 0f : LINE_GAP + item.line3H);
        float y = item.y + (item.h - text) / 2f;
        wrapped(content, item.title, item.titleColor, Fonts.INSIGNIA_LARGE, x, y, w);
        y += item.titleH + LINE_GAP;
        if (!item.line2.isEmpty()) {
            wrapped(content, item.line2, item.line2Color, Fonts.DEFAULT_SMALL, x, y, w);
            y += item.line2H + LINE_GAP;
        }
        if (!item.line3.isEmpty()) wrapped(content, item.line3, GOLD, Fonts.DEFAULT_SMALL, x, y, w);

        // The status sits right of the text: one line centred, or two lines (or a line and a bar).
        boolean second = !item.status2.isEmpty() || (item.goal > 1 && !item.done);
        float mid = item.y + item.h / 2f;
        if (!item.status.isEmpty()) {
            label(content, item.status, item.statusColor, statusX, second ? mid - 19f : mid - 9f, STATUS_W, 18f,
                    Fonts.DEFAULT_SMALL, Alignment.RMID);
        }
        if (!item.status2.isEmpty()) {
            label(content, item.status2, FAINT, statusX, mid + 1f, STATUS_W, 18f, Fonts.DEFAULT_SMALL, Alignment.RMID);
        }
    }

    private LabelAPI label(CustomPanelAPI parent, String text, Color color, float x, float y, float w, float h,
                           String font, Alignment align) {
        LabelAPI label = Global.getSettings().createLabel(text, font);
        label.setColor(color);
        label.setAlignment(align);
        parent.addComponent((UIComponentAPI) label).inTL(x, y).setSize(w, h);
        return label;
    }

    private void wrapped(CustomPanelAPI parent, String text, Color color, String font, float x, float y, float w) {
        LabelAPI label = Global.getSettings().createLabel(text, font);
        label.setColor(color);
        label.setAlignment(Alignment.TL);
        parent.addComponent((UIComponentAPI) label);
        label.autoSizeToWidth(w);
        label.getPosition().inTL(x, y);
    }

    /** A self-drawn button on the panel itself. */
    private void button(String action, String text, float x, float y, float w, float h) {
        buttons.add(new Btn(action, x, y, w, h));
        label(panel, text, Color.WHITE, x, y + (h - 18f) / 2f, w, 18f, Fonts.DEFAULT_SMALL, Alignment.MID);
    }

    // ---- tabs and input ----

    private void showTab(int next) {
        tab = next;
        for (int i = 0; i < pages.size(); i++) {
            pages.get(i).container.getPosition().inTL(i == tab ? PAD : HIDDEN_X, listTop);
        }
        Page page = pages.get(tab);
        summary.setText(page.done + " / " + page.total + " done");
        for (Btn b : buttons) {
            if (b.action.startsWith("tab:")) b.checked = Integer.parseInt(b.action.substring(4)) == tab;
        }
    }

    private void act(String action) {
        if (dismissed) return;
        if (action.equals("close")) {
            close();
            return;
        }
        if (action.startsWith("tab:")) {
            int next = Integer.parseInt(action.substring(4));
            if (next != tab && next < pages.size()) {
                Global.getSoundPlayer().playUISound("ui_button_pressed", 1f, 0.6f);
                showTab(next);
            }
        }
    }

    private void close() {
        if (dismissed) return;
        dismissed = true;
        if (callbacks != null) callbacks.dismissDialog();
    }

    /** Reads where the shown list's scrolled content sits now, in this panel's top-down coordinates. */
    private void updateContent() {
        if (position == null || pages.isEmpty()) return;
        Page page = pages.get(tab);
        PositionAPI p = page.content == null ? null : page.content.getPosition();
        if (p == null) return;
        page.contentLeft = p.getX() - position.getX();
        page.contentTop = (position.getY() + panelH) - (p.getY() + p.getHeight());
    }

    private static boolean inBox(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public void advance(float amount) {
        updateContent();
        if (dismissed || position == null || !Mouse.isCreated()) return;
        float scale = Global.getSettings().getScreenScaleMult();
        if (scale <= 0f) return;
        float mx = Mouse.getX() / scale - position.getX();
        float my = position.getY() + panelH - Mouse.getY() / scale;
        boolean leftDown = Mouse.isButtonDown(0);
        boolean clicked = leftDown && !wasLeftDown;
        wasLeftDown = leftDown;
        for (Btn b : buttons) b.hover = inBox(mx, my, b.x, b.y, b.w, b.h);
        if (!clicked) return;
        for (Btn b : new ArrayList<>(buttons)) {
            if (b.hover) {
                act(b.action);
                return;
            }
        }
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        if (events == null || dismissed) return;
        for (InputEventAPI e : events) {
            if (!e.isConsumed() && e.isKeyDownEvent() && e.getEventValue() == Keyboard.KEY_ESCAPE) {
                e.consume();
                close();
                return;
            }
        }
    }

    @Override
    public void positionChanged(PositionAPI p) {
        position = p;
    }

    // ---- drawing ----

    private float x(float n) {
        return position.getX() + n;
    }

    private float y(float topDown) {
        return position.getY() + panelH - topDown;
    }

    private void rect(float left, float top, float w, float h, Color c, float alpha) {
        GLDraw.quad(x(left), y(top + h), w, h, c, alpha);
    }

    private void frame(float left, float top, float w, float h, Color c, float thickness, float alpha) {
        GLDraw.frame(x(left), y(top + h), w, h, c, thickness, alpha);
    }

    /** The sprite for a path, loading its texture if nothing has yet; the trophy when it cannot be had. */
    private SpriteAPI sprite(String path) {
        if (sprites.containsKey(path)) return sprites.get(path);
        SpriteAPI s = load(path);
        if (s == null && !Ids.ICON.equals(path)) s = load(Ids.ICON);
        sprites.put(path, s);
        return s;
    }

    private static SpriteAPI load(String path) {
        if (path == null || path.isEmpty()) return null;
        String clean = path.startsWith("/") ? path.substring(1) : path;
        try {
            SpriteAPI s = Global.getSettings().getSprite(clean);
            if (s != null && s.getTextureId() != 0) return s;
        } catch (Throwable ignored) {
            // Not loaded yet.
        }
        try {
            Global.getSettings().loadTexture(clean);
            SpriteAPI s = Global.getSettings().getSprite(clean);
            if (s != null && s.getTextureId() != 0) return s;
        } catch (Throwable ignored) {
            // No such picture.
        }
        return null;
    }

    private void drawIcon(String path, float left, float top, float size, Color tint, float alpha) {
        SpriteAPI s = sprite(path);
        if (s == null) return;
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        s.setSize(size, size);
        s.setColor(tint);
        s.setAlphaMult(alpha);
        s.render(x(left), y(top + size));
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    @Override
    public void renderBelow(float alpha) {
        if (position == null || pages.isEmpty()) return;
        updateContent();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT
                | GL11.GL_TEXTURE_BIT | GL11.GL_LINE_BIT | GL11.GL_SCISSOR_BIT);
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            rect(0f, 0f, panelW, panelH, BG, alpha);
            frame(0f, 0f, panelW, panelH, GOLD, 2f, alpha * 0.6f);

            drawList(pages.get(tab), alpha);
            for (Btn b : buttons) drawButton(b, alpha);
        } finally {
            GL11.glPopAttrib();
        }
    }

    private void drawList(Page page, float alpha) {
        rect(PAD, listTop, listW, listH, PANEL, alpha * 0.6f);
        frame(PAD, listTop, listW, listH, DIM, 1f, alpha * 0.25f);

        float scale = Global.getSettings().getScreenScaleMult();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int) (x(PAD) * scale), (int) (y(listTop + listH) * scale),
                (int) Math.ceil(listW * scale), (int) Math.ceil(listH * scale));
        float left = page.contentLeft, w = contentW();
        for (Item item : page.items) {
            float top = page.contentTop + item.y;
            if (top + item.h + GAP < listTop || top - GAP > listTop + listH) continue;
            if (item.kind == MOD) {
                rect(left, top, w, MOD_H, PANEL, alpha);
                rect(left, top + MOD_H - 1f, w, 1f, GOLD, alpha * 0.45f);
            } else if (item.kind == CATEGORY) {
                rect(left + 12f, top + CATEGORY_H - 2f, w - 24f, 1f, DIM, alpha * 0.3f);
            } else if (item.kind == ROW) {
                drawRow(item, left, top, w, alpha);
            }
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private void drawRow(Item item, float left, float top, float w, float alpha) {
        float indent = rowLeft(item);
        Color fill = item.done ? GLDraw.darken(GREEN, 0.8f) : ROW_FILL;
        rect(left + indent, top, w - indent, item.h, fill, alpha);
        rect(left + indent, top, 3f, item.h, item.done ? GREEN : FAINT, alpha * (item.done ? 0.9f : 0.5f));

        if (item.chained) {
            // A line down the chain with a mark at every step, filled once the step is done.
            float lineX = left + CHAIN_INDENT / 2f - 1f, mid = top + item.h / 2f;
            float from = item.chainStart ? mid : top - GAP, to = item.chainEnd ? mid : top + item.h;
            rect(lineX, from, 2f, to - from, DIM, alpha * 0.45f);
            rect(lineX - 3f, mid - 4f, 8f, 8f, item.done ? GREEN : BAR_BACK, alpha);
        }

        float iconX = left + indent + ROW_X, iconY = top + (item.h - ICON) / 2f;
        rect(iconX, iconY, ICON, ICON, BG, alpha * 0.7f);
        if (item.done) drawIcon(item.icon, iconX, iconY, ICON, Color.WHITE, alpha);
        else drawIcon(item.icon, iconX, iconY, ICON, new Color(170, 170, 170), alpha * (item.hidden ? 0.12f : 0.35f));
        frame(iconX, iconY, ICON, ICON, item.done ? GOLD : DIM, 1f, alpha * (item.done ? 0.7f : 0.3f));

        if (item.goal > 1 && !item.done) {
            float barX = left + w - TEXT_GAP - BAR_W, barY = top + item.h / 2f + 6f;
            rect(barX, barY, BAR_W, BAR_H, BAR_BACK, alpha);
            rect(barX, barY, BAR_W * item.progress / (float) item.goal, BAR_H, GOLD, alpha * 0.9f);
        }
    }

    private void drawButton(Btn b, float alpha) {
        Color fill = b.checked ? new Color(98, 76, 38) : b.hover ? new Color(60, 67, 78) : new Color(32, 40, 50);
        GLDraw.bevelledPanel(x(b.x), y(b.y + b.h), b.w, b.h, fill, 2f, alpha);
        frame(b.x, b.y, b.w, b.h, b.checked ? GOLD : DIM, 1f, alpha * 0.65f);
    }
}
