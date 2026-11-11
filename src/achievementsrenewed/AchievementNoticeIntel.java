package achievementsrenewed;

import java.util.Set;

import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.IntelUIAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import achievementsrenewed.api.AchievementSpec;
import achievementsrenewed.api.Achievements;

/**
 * The intel log entry "Achievement: name" added when an achievement is done. Never saved: IntelKeeper
 * keeps the list of them in the save as plain text and rebuilds the entries on load.
 */
public final class AchievementNoticeIntel extends BaseIntelPlugin {

    private static final String BUTTON_OPEN = "acr_open";

    private final AchievementSpec spec;
    /** Campaign clock time it was done, so the entry can be dropped after a while. */
    private final long doneAt;

    public AchievementNoticeIntel(AchievementSpec spec, long doneAt) {
        this.spec = spec;
        this.doneAt = doneAt;
    }

    public AchievementSpec spec() {
        return spec;
    }

    public long doneAt() {
        return doneAt;
    }

    @Override
    public String getName() {
        return "Achievement: " + spec.name;
    }

    @Override
    public String getIcon() {
        return spec.icon;
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        tags.add(Ids.INTEL_TAG);
        return tags;
    }

    /** Under the name: the description, or the exact condition when there is none and it says more than the name. */
    public static String body(AchievementSpec spec) {
        if (!spec.description.isEmpty()) return spec.description;
        String plain = spec.howTo.endsWith(".") ? spec.howTo.substring(0, spec.howTo.length() - 1) : spec.howTo;
        return plain.equalsIgnoreCase(spec.name) ? "" : spec.howTo;
    }

    @Override
    public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
        info.addPara(getName(), getTitleColor(mode), 0f);
        for (String line : RewardLines.of(spec)) info.addPara(line, Misc.getGrayColor(), 3f);
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        float pad = 10f;
        String body = body(spec);
        if (!body.isEmpty()) info.addPara(body, pad);
        for (String line : RewardLines.of(spec)) info.addPara(line, Misc.getHighlightColor(), pad);
        info.addPara(spec.tabName(), Misc.getGrayColor(), pad);
        info.addButton("Show it in the list", BUTTON_OPEN, width, 24f, pad * 2f);
        addDeleteButton(info, width);
    }

    @Override
    public void buttonPressConfirmed(Object buttonId, IntelUIAPI ui) {
        if (BUTTON_OPEN.equals(buttonId)) {
            Achievements.openList(ui, spec.id);
            return;
        }
        super.buttonPressConfirmed(buttonId, ui);
    }

    @Override
    protected void notifyEnded() {
        super.notifyEnded();
        IntelKeeper.forget(this);
    }
}
