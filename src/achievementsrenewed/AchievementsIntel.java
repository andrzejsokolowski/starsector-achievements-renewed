package achievementsrenewed;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.IntelUIAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.util.Misc;

import achievementsrenewed.api.AchievementSpec;
import achievementsrenewed.api.Achievements;

/**
 * The permanent Achievements entry on the intel screen: the counts per mod and a button that opens the
 * list. Never saved: IntelKeeper takes it out before a save and puts it back after.
 */
public final class AchievementsIntel extends BaseIntelPlugin {

    private static final String BUTTON_OPEN = "acr_open";

    @Override
    public String getName() {
        return "Achievements";
    }

    @Override
    public String getIcon() {
        return Ids.ICON;
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        tags.add(Ids.INTEL_TAG);
        return tags;
    }

    @Override
    public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
        info.addPara(getName(), getTitleColor(mode), 0f);
        info.addPara(Achievements.doneCount() + " / " + Achievements.totalCount() + " done", Misc.getGrayColor(), 3f);
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        float pad = 10f;
        info.addPara(Achievements.doneCount() + " / " + Achievements.totalCount() + " done", Misc.getHighlightColor(), pad);

        // Done and total per mod, in list order.
        Map<String, int[]> perMod = new LinkedHashMap<>();
        for (AchievementSpec spec : Achievements.all()) {
            int[] counts = perMod.computeIfAbsent(spec.modId, id -> new int[2]);
            if (spec.isDone()) counts[0]++;
            counts[1]++;
        }
        if (perMod.size() > 1) {
            info.addSectionHeading("By mod", Alignment.MID, pad);
            for (Map.Entry<String, int[]> e : perMod.entrySet()) {
                int[] counts = e.getValue();
                info.addPara(Catalog.modName(e.getKey()) + "   " + counts[0] + " / " + counts[1],
                        counts[0] >= counts[1] ? Misc.getPositiveHighlightColor() : Misc.getTextColor(), 5f);
            }
        }

        info.addButton("Open the list", BUTTON_OPEN, width, 24f, pad * 2f);
    }

    @Override
    public void buttonPressConfirmed(Object buttonId, IntelUIAPI ui) {
        if (BUTTON_OPEN.equals(buttonId)) Achievements.openList(ui);
    }

    @Override
    public boolean shouldRemoveIntel() {
        return false;
    }

    @Override
    public boolean isHidden() {
        return false;
    }
}
