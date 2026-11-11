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
import achievementsrenewed.RewardLines;
import achievementsrenewed.Settings;
import achievementsrenewed.api.AchievementSpec;
import achievementsrenewed.api.Achievements;
import achievementsrenewed.compat.ExternalAchievement;
import achievementsrenewed.compat.ExternalSources;

/**
 * The achievements screen: a column of tab buttons at the left, the list of the chosen tab at the right.
 *
 * The first tab, "Achievements", holds base game achievements only. Every other tab is named by the
 * achievements in it (their tab column, or the name of the mod that defined them) and the tabs come in
 * alphabetical order, so no mod is put before another. What MagicLib and the milestone skills show goes in
 * the tab named after the mod it comes from, read-only, after this system's own rows of that name and
 * looking the same (no heading for the other system). A tab only exists when something is in it. Inside a
 * tab the rows are grouped by category, then chain; a chain sits in a block with space around it.
 *
 * A row that is not done is greyed out and shows its name and hint (the exact condition when that LunaLib
 * setting is on), or "Secret Achievement". A done row shows the name and the description. Both show the
 * reward lines (RewardLines). Opened from an intel entry, the list starts on that achievement.
 *
 * Every tab's list is one container panel built once; choosing a tab moves the other containers off screen
 * (a scrolling TooltipMakerAPI cannot be taken out of a panel again). A list is a scrolling TooltipMakerAPI
 * holding a content panel with the labels, and so is the tab column. Backgrounds, icons, bars and buttons
 * are drawn and hit-tested by this panel (as in Rule Book - Renewed and Gambling Den), which reads back
 * each frame where the scrolled content really is on screen. Nothing changes while the screen is open, so
 * every text is laid out once.
 */
public final class AchievementsPanel extends BaseCustomUIPanelPlugin {

    private static final float PAD = 16f, TITLE_H = 34f, HEADER_H = 30f, FOOT_H = 36f;
    private static final float TAB_W = 250f, GUTTER = 10f, TAB_BTN_H = 30f, TAB_BTN_GAP = 4f, COUNT_W = 64f;
    private static final float SCROLLBAR_W = 22f, HIDDEN_X = -20000f;
    private static final float CATEGORY_H = 26f, CHAIN_H = 22f, GAP = 4f;
    private static final float ROW_X = 10f, ROW_PAD = 8f, ICON = 48f, TEXT_GAP = 12f, LINE_GAP = 3f;
    private static final float STATUS_W = 150f, CHAIN_INDENT = 22f, BAR_W = 110f, BAR_H = 6f;
    /** Space around a chain's block, and inside it below its last step. */
    private static final float CHAIN_GAP = 16f, CHAIN_PAD = 10f;
    private static final String SECRET_TITLE = "Secret Achievement";

    private static final Color BG = new Color(10, 13, 19);
    private static final Color PANEL = new Color(20, 25, 34);
    private static final Color ROW_FILL = new Color(24, 30, 40);
    private static final Color BAR_BACK = new Color(48, 56, 68);
    private static final Color BUTTON = new Color(32, 40, 50);
    private static final Color BUTTON_HOVER = new Color(60, 67, 78);
    private static final Color BUTTON_ON = new Color(98, 76, 38);
    private static final Color GOLD = new Color(235, 185, 100);
    private static final Color GREEN = new Color(120, 210, 130);
    private static final Color DIM = new Color(150, 157, 173);
    private static final Color FAINT = new Color(105, 110, 121);
    private static final Color TEXT = new Color(220, 224, 232);
    private static final Color CHAIN_FILL = new Color(14, 18, 25);

    private static final int CATEGORY = 1, CHAIN = 2, ROW = 3;

    /** One line of a list: a heading or an achievement. The texts first, then where it sits. */
    private static final class Item {
        final int kind;
        String title = "", line2 = "", line3 = "", status = "", status2 = "", icon;
        Color titleColor = TEXT, line2Color = DIM, statusColor = DIM;
        Color line3Color = GOLD;
        boolean done, hidden, chained, chainStart, chainEnd, focus;
        int progress, goal;
        float y, h, titleH, line2H, line3H;
        /** A chain heading: the bottom of its block, below its last step. */
        float blockBottom;
        /** A row: the id of its achievement, when it is one of this system's. */
        String id;

        Item(int kind) {
            this.kind = kind;
        }
    }

    /** One tab: its button in the column and its list. */
    private static final class Tab {
        final String name;
        final boolean vanilla;
        final List<AchievementSpec> specs = new ArrayList<>();
        final List<ExternalAchievement> externals = new ArrayList<>();
        final List<Item> items = new ArrayList<>();
        int done, total;
        CustomPanelAPI container, content;
        TooltipMakerAPI scroller;
        float contentH;
        float contentLeft, contentTop, buttonY;
        boolean hover;

        Tab(String name, boolean vanilla) {
            this.name = name;
            this.vanilla = vanilla;
        }
    }

    private static final class Btn {
        final String action;
        final float x, y, w, h;
        boolean hover;

        Btn(String action, float x, float y, float w, float h) {
            this.action = action;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
    }

    private final float panelW, panelH, colTop, colH, listX, listTop, listW, listH;
    private final List<Btn> buttons = new ArrayList<>();
    private final List<Tab> tabs = new ArrayList<>();
    private final Map<String, SpriteAPI> sprites = new HashMap<>();
    private CustomPanelAPI panel, scratch, tabsContent;
    private DialogCallbacks callbacks;
    private PositionAPI position;
    private LabelAPI listTitle, listCount;
    private float tabsLeft, tabsTop, tabButtonW;
    private int tab;
    private boolean dismissed, wasLeftDown;
    /** The achievement to show first: its tab is chosen and the list scrolled to it once. */
    private final String focusId;
    private Item focusItem;
    private boolean focusScrolled;
    private float focusGlow;

    public AchievementsPanel() {
        this(null);
    }

    public AchievementsPanel(String focusId) {
        this.focusId = focusId;
        float screenW = Global.getSettings().getScreenWidth();
        float screenH = Global.getSettings().getScreenHeight();
        panelW = Math.max(Math.min(1240f, screenW - 80f), Math.min(900f, screenW - 10f));
        panelH = Math.max(Math.min(940f, screenH - 80f), Math.min(600f, screenH - 10f));
        colTop = PAD + TITLE_H;
        colH = panelH - colTop - PAD - FOOT_H - 8f;
        listX = PAD + TAB_W + GUTTER;
        listTop = colTop + HEADER_H;
        listW = panelW - PAD - listX;
        listH = colH - HEADER_H;
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

        label(panel, "ACHIEVEMENTS", GOLD, PAD, PAD + 2f, 320f, 26f, Fonts.ORBITRON_20AA, Alignment.LMID);
        listTitle = label(panel, "", GOLD, listX + 12f, colTop + 6f, listW - COUNT_W - 36f, 18f, Fonts.ORBITRON_12, Alignment.LMID);
        listCount = label(panel, "", TEXT, listX + listW - COUNT_W - 62f, colTop + 6f, COUNT_W + 50f, 18f,
                Fonts.DEFAULT_SMALL, Alignment.RMID);

        collectTabs();
        buildTabColumn();
        for (Tab t : tabs) {
            fill(t);
            build(t);
        }

        float footY = panelH - PAD - FOOT_H;
        if (!Settings.showHowTo()) {
            label(panel, "Exact conditions: LunaLib setting \"Show how to get achievements\".", FAINT,
                    PAD, footY + 9f, panelW - PAD * 2f - 140f, 18f, Fonts.DEFAULT_SMALL, Alignment.LMID);
        }
        buttons.add(new Btn("close", panelW - PAD - 120f, footY, 120f, FOOT_H));
        label(panel, "Close", Color.WHITE, panelW - PAD - 120f, footY + (FOOT_H - 18f) / 2f, 120f, 18f,
                Fonts.DEFAULT_SMALL, Alignment.MID);
        int first = 0;
        for (int i = 0; i < tabs.size() && focusId != null; i++) {
            for (Item item : tabs.get(i).items) {
                if (focusId.equals(item.id)) {
                    first = i;
                    focusItem = item;
                    item.focus = true;
                }
            }
        }
        showTab(first);
    }

    // ---- which tabs there are ----

    /**
     * The base game tab first, then every other tab by title. Other systems' achievements join the tab
     * with the name of their mod, or make one.
     */
    private void collectTabs() {
        Map<String, Tab> byKey = new HashMap<>();
        for (AchievementSpec spec : Achievements.all()) {
            tabFor(byKey, spec.tabName(), spec.isVanilla()).specs.add(spec);
        }
        for (ExternalAchievement a : ExternalSources.readAll()) {
            tabFor(byKey, a.group, false).externals.add(a);
        }
        tabs.sort((a, b) -> {
            if (a.vanilla != b.vanilla) return a.vanilla ? -1 : 1;
            int byName = a.name.compareToIgnoreCase(b.name);
            return byName != 0 ? byName : a.name.compareTo(b.name);
        });
        for (Tab t : tabs) {
            t.total = t.specs.size() + t.externals.size();
            for (AchievementSpec spec : t.specs) if (spec.isDone()) t.done++;
            for (ExternalAchievement a : t.externals) if (a.done) t.done++;
        }
    }

    private Tab tabFor(Map<String, Tab> byKey, String name, boolean vanilla) {
        String key = Catalog.tabKey(name);
        Tab found = byKey.get(key);
        if (found == null) {
            found = new Tab(name.trim(), vanilla);
            byKey.put(key, found);
            tabs.add(found);
        }
        return found;
    }

    // ---- what a list says ----

    /** This system's rows by category and chain, then what other systems show for the same mod. */
    private void fill(Tab t) {
        boolean howTo = Settings.showHowTo();
        String category = null, chain = null;
        Item lastChained = null;
        for (AchievementSpec spec : t.specs) {
            boolean newCategory = !spec.category.equals(category);
            boolean newChain = newCategory || !spec.chain.equals(chain);
            if (newChain && lastChained != null) {
                lastChained.chainEnd = true;
                lastChained = null;
            }
            if (newCategory) {
                category = spec.category;
                if (!category.isEmpty()) t.items.add(heading(CATEGORY, category));
            }
            if (newChain) {
                chain = spec.chain;
                if (!chain.isEmpty()) t.items.add(heading(CHAIN, chain));
            }

            Item row = new Item(ROW);
            row.id = spec.id;
            row.icon = spec.icon;
            row.done = spec.isDone();
            String rewards = RewardLines.text(spec);
            if (row.done) {
                row.title = spec.name;
                row.titleColor = Color.WHITE;
                row.line2 = AchievementNoticeIntel.body(spec);
                row.line3 = rewards;
                row.status = "Done";
                row.statusColor = GREEN;
                String date = Achievements.doneDate(spec.id);
                row.status2 = date == null ? "" : date;
            } else {
                boolean revealed = spec.isRevealed();
                row.hidden = !revealed && !howTo;
                row.titleColor = DIM;
                if (row.hidden) {
                    // A secret one, or a step of a chain whose step before is not done yet.
                    row.title = spec.secret ? SECRET_TITLE : "Next step";
                    if (!spec.secret) {
                        row.line2 = "Shows once the step before it is done.";
                        row.line2Color = FAINT;
                    }
                } else {
                    row.title = spec.name;
                    row.line2 = howTo ? spec.howTo : spec.hint;
                    row.line3 = rewards;
                    row.line3Color = FAINT;
                }
                if (spec.isCounted() && !row.hidden) {
                    row.goal = spec.goal;
                    row.progress = Math.max(0, Math.min(spec.goal, spec.progress()));
                    row.status = row.progress + " / " + row.goal;
                    row.statusColor = TEXT;
                }
            }
            if (!spec.chain.isEmpty()) {
                row.chained = true;
                row.chainStart = lastChained == null;
                lastChained = row;
            }
            t.items.add(row);
        }
        if (lastChained != null) lastChained.chainEnd = true;

        // What other systems keep (MagicLib, milestones) follows as plain rows of the same tab, no heading.
        for (ExternalAchievement a : t.externals) {
            Item row = new Item(ROW);
            row.icon = a.icon.isEmpty() ? Ids.ICON : a.icon;
            row.done = a.done;
            row.hidden = a.hidden && !a.done;
            row.title = row.hidden || a.name.isEmpty() ? SECRET_TITLE : a.name;
            row.titleColor = a.done ? Color.WHITE : DIM;
            row.line2 = row.hidden ? "" : a.description;
            row.line2Color = a.done ? DIM : FAINT;
            if (a.done) {
                row.status = "Done";
                row.statusColor = GREEN;
            } else if (a.goal > 1 && !a.hidden) {
                row.goal = a.goal;
                row.progress = Math.max(0, Math.min(a.goal, a.progress));
                row.status = row.progress + " / " + row.goal;
                row.statusColor = TEXT;
            }
            t.items.add(row);
        }
    }

    private static Item heading(int kind, String title) {
        Item item = new Item(kind);
        item.title = title;
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

    /** A text cut to fit a width on one line, with "..." when something was cut. */
    private static String fit(String text, String font, float width) {
        try {
            if (Global.getSettings().computeStringWidth(text, font) <= width) return text;
            String cut = text;
            while (cut.length() > 1 && Global.getSettings().computeStringWidth(cut + "...", font) > width) {
                cut = cut.substring(0, cut.length() - 1);
            }
            return cut.trim() + "...";
        } catch (Throwable t) {
            // No width to be had: cut by length.
            return text.length() <= 26 ? text : text.substring(0, 24).trim() + "...";
        }
    }

    /** The column of tab buttons: one scrolling list of names and counts. */
    private void buildTabColumn() {
        float contentH = Math.max(colH, tabs.size() * (TAB_BTN_H + TAB_BTN_GAP) + TAB_BTN_GAP);
        boolean scrolls = contentH > colH;
        tabButtonW = TAB_W - (scrolls ? SCROLLBAR_W : 0f);

        CustomPanelAPI container = panel.createCustomPanel(TAB_W, colH, null);
        panel.addComponent(container).inTL(PAD, colTop);
        tabsLeft = PAD;
        tabsTop = colTop;
        TooltipMakerAPI scroller = container.createUIElement(TAB_W, colH, true);
        tabsContent = container.createCustomPanel(TAB_W, contentH, null);
        float y = TAB_BTN_GAP;
        for (Tab t : tabs) {
            t.buttonY = y;
            float textY = y + (TAB_BTN_H - 18f) / 2f;
            label(tabsContent, fit(t.name, Fonts.DEFAULT_SMALL, tabButtonW - COUNT_W - 26f), Color.WHITE, 12f,
                    textY, tabButtonW - COUNT_W - 20f, 18f, Fonts.DEFAULT_SMALL, Alignment.LMID);
            label(tabsContent, t.done + " / " + t.total, t.done >= t.total ? GREEN : DIM, tabButtonW - COUNT_W - 10f,
                    textY, COUNT_W, 18f, Fonts.DEFAULT_SMALL, Alignment.RMID);
            y += TAB_BTN_H + TAB_BTN_GAP;
        }
        scroller.addCustom(tabsContent, 0f);
        scroller.setHeightSoFar(contentH);
        container.addUIElement(scroller).inTL(0f, 0f);
    }

    /** Lays the items out top to bottom, then builds the container, the scroller and every label. */
    private void build(Tab t) {
        float y = GAP;
        Item chainHeading = null;
        for (Item item : t.items) {
            if (item.kind == CATEGORY) {
                if (y > GAP) y += 6f;
                item.h = CATEGORY_H;
            } else if (item.kind == CHAIN) {
                if (y > GAP) y += CHAIN_GAP;
                y += 4f;
                item.h = CHAIN_H;
                chainHeading = item;
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
            if (item.kind == ROW && item.chainEnd && chainHeading != null) {
                chainHeading.blockBottom = item.y + item.h + CHAIN_PAD;
                y += CHAIN_PAD + CHAIN_GAP;
                chainHeading = null;
            }
        }
        float contentH = Math.max(listH, y + GAP);
        t.contentH = contentH;

        t.container = panel.createCustomPanel(listW, listH, null);
        panel.addComponent(t.container).inTL(HIDDEN_X, listTop);
        t.contentLeft = listX;
        t.contentTop = listTop;

        TooltipMakerAPI scroller = t.container.createUIElement(listW, listH, true);
        t.scroller = scroller;
        t.content = t.container.createCustomPanel(listW, contentH, null);
        for (Item item : t.items) addLabels(t.content, item);
        scroller.addCustom(t.content, 0f);
        scroller.setHeightSoFar(contentH);
        t.container.addUIElement(scroller).inTL(0f, 0f);
    }

    private void addLabels(CustomPanelAPI content, Item item) {
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
        if (!item.line3.isEmpty()) wrapped(content, item.line3, item.line3Color, Fonts.DEFAULT_SMALL, x, y, w);

        // The status sits right of the text: one line centred, or two lines (or a line and a bar).
        float statusX = contentW() - STATUS_W - TEXT_GAP;
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

    // ---- tabs and input ----

    private void showTab(int next) {
        if (tabs.isEmpty()) return;
        tab = next;
        for (int i = 0; i < tabs.size(); i++) {
            tabs.get(i).container.getPosition().inTL(i == tab ? listX : HIDDEN_X, listTop);
        }
        Tab t = tabs.get(tab);
        listTitle.setText(fit(t.name.toUpperCase(), Fonts.ORBITRON_12, listW - COUNT_W - 80f));
        listCount.setText(t.done + " / " + t.total + " done");
    }

    private void close() {
        if (dismissed) return;
        dismissed = true;
        if (callbacks != null) callbacks.dismissDialog();
    }

    /** Reads where the scrolled contents sit now, in this panel's top-down coordinates. */
    private void updateContent() {
        if (position == null) return;
        if (tabsContent != null && tabsContent.getPosition() != null) {
            PositionAPI p = tabsContent.getPosition();
            tabsLeft = p.getX() - position.getX();
            tabsTop = (position.getY() + panelH) - (p.getY() + p.getHeight());
        }
        if (tabs.isEmpty()) return;
        Tab t = tabs.get(tab);
        PositionAPI p = t.content == null ? null : t.content.getPosition();
        if (p == null) return;
        t.contentLeft = p.getX() - position.getX();
        t.contentTop = (position.getY() + panelH) - (p.getY() + p.getHeight());
    }

    private static boolean inBox(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    /** Scrolls the shown list so the focused row sits near the top, once the scroller exists. */
    private void scrollToFocus() {
        if (focusScrolled || focusItem == null || tabs.isEmpty()) return;
        Tab t = tabs.get(tab);
        if (t.scroller == null || t.scroller.getExternalScroller() == null) return;
        focusScrolled = true;
        float offset = Math.max(0f, Math.min(t.contentH - listH, focusItem.y - 40f));
        t.scroller.getExternalScroller().setYOffset(offset);
        focusGlow = 3f;
    }

    @Override
    public void advance(float amount) {
        scrollToFocus();
        if (focusGlow > 0f) focusGlow = Math.max(0f, focusGlow - amount);
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
        // A tab button reacts only where it shows: inside the column's box.
        boolean inColumn = inBox(mx, my, PAD, colTop, tabButtonW, colH);
        for (Tab t : tabs) {
            t.hover = inColumn && inBox(mx, my, tabsLeft, tabsTop + t.buttonY, tabButtonW, TAB_BTN_H);
        }
        if (!clicked) return;
        for (Btn b : buttons) {
            if (b.hover && b.action.equals("close")) {
                close();
                return;
            }
        }
        for (int i = 0; i < tabs.size(); i++) {
            if (tabs.get(i).hover && i != tab) {
                Global.getSoundPlayer().playUISound("ui_button_pressed", 1f, 0.6f);
                showTab(i);
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

    private void clip(float left, float top, float w, float h) {
        float scale = Global.getSettings().getScreenScaleMult();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int) (x(left) * scale), (int) (y(top + h) * scale),
                (int) Math.ceil(w * scale), (int) Math.ceil(h * scale));
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
        if (position == null) return;
        updateContent();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT
                | GL11.GL_TEXTURE_BIT | GL11.GL_LINE_BIT | GL11.GL_SCISSOR_BIT);
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            rect(0f, 0f, panelW, panelH, BG, alpha);
            frame(0f, 0f, panelW, panelH, GOLD, 2f, alpha * 0.6f);

            drawTabColumn(alpha);
            if (!tabs.isEmpty()) drawList(tabs.get(tab), alpha);
            for (Btn b : buttons) {
                GLDraw.bevelledPanel(x(b.x), y(b.y + b.h), b.w, b.h, b.hover ? BUTTON_HOVER : BUTTON, 2f, alpha);
                frame(b.x, b.y, b.w, b.h, DIM, 1f, alpha * 0.65f);
            }
        } finally {
            GL11.glPopAttrib();
        }
    }

    private void drawTabColumn(float alpha) {
        rect(PAD, colTop, TAB_W, colH, PANEL, alpha * 0.6f);
        frame(PAD, colTop, TAB_W, colH, DIM, 1f, alpha * 0.25f);
        clip(PAD, colTop, TAB_W, colH);
        for (int i = 0; i < tabs.size(); i++) {
            Tab t = tabs.get(i);
            float top = tabsTop + t.buttonY;
            if (top + TAB_BTN_H < colTop || top > colTop + colH) continue;
            boolean on = i == tab;
            float left = tabsLeft + 4f, w = tabButtonW - 8f;
            GLDraw.bevelledPanel(x(left), y(top + TAB_BTN_H), w, TAB_BTN_H, on ? BUTTON_ON : t.hover ? BUTTON_HOVER : BUTTON,
                    2f, alpha);
            frame(left, top, w, TAB_BTN_H, on ? GOLD : DIM, 1f, alpha * (on ? 0.8f : 0.4f));
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private void drawList(Tab t, float alpha) {
        // The header bar with the tab's title and count, then the list box.
        rect(listX, colTop, listW, HEADER_H - 2f, PANEL, alpha);
        rect(listX, colTop + HEADER_H - 3f, listW, 1f, GOLD, alpha * 0.45f);
        rect(listX, listTop, listW, listH, PANEL, alpha * 0.6f);
        frame(listX, listTop, listW, listH, DIM, 1f, alpha * 0.25f);

        clip(listX, listTop, listW, listH);
        float left = t.contentLeft, w = contentW();
        for (Item item : t.items) {
            if (item.kind != CHAIN || item.blockBottom <= 0f) continue;
            float top = t.contentTop + item.y - 6f, h = item.blockBottom - item.y + 6f;
            if (top + h < listTop || top > listTop + listH) continue;
            rect(left + 2f, top, w - 4f, h, CHAIN_FILL, alpha * 0.8f);
            frame(left + 2f, top, w - 4f, h, DIM, 1f, alpha * 0.22f);
        }
        for (Item item : t.items) {
            float top = t.contentTop + item.y;
            if (top + item.h + GAP < listTop || top - GAP > listTop + listH) continue;
            if (item.kind == CATEGORY) {
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

        if (item.focus) {
            // The achievement an intel entry pointed at: a gold frame that pulses, then stays.
            float pulse = focusGlow > 0f ? 0.55f + 0.45f * (float) Math.abs(Math.sin(focusGlow * 3.0)) : 0.6f;
            frame(left + indent, top, w - indent, item.h, GOLD, 2f, alpha * pulse);
        }

        if (item.goal > 1 && !item.done) {
            float barX = left + w - TEXT_GAP - BAR_W, barY = top + item.h / 2f + 6f;
            rect(barX, barY, BAR_W, BAR_H, BAR_BACK, alpha);
            rect(barX, barY, BAR_W * item.progress / (float) item.goal, BAR_H, GOLD, alpha * 0.9f);
        }
    }
}
