package achievementsrenewed.campaign;

import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.impl.campaign.abilities.BaseDurationAbility;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import achievementsrenewed.Settings;
import achievementsrenewed.api.Achievements;

/**
 * The Achievements button on the ability bar: opens the list. Never saved: AbilityKeeper takes it off the
 * player before a save and gives it back after.
 */
public final class AchievementsAbility extends BaseDurationAbility {

    @Override
    protected void activateImpl() {
        Achievements.openList();
    }

    @Override
    protected void applyEffect(float amount, float level) {
    }

    @Override
    protected void deactivateImpl() {
    }

    @Override
    protected void cleanupImpl() {
    }

    @Override
    public boolean isUsable() {
        return true;
    }

    @Override
    public boolean hasTooltip() {
        return true;
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded) {
        tooltip.addTitle("Achievements");
        tooltip.addPara("Opens the list of achievements. %s of %s done.", 10f, Misc.getHighlightColor(),
                "" + Achievements.doneCount(), "" + Achievements.totalCount());
        int key = Settings.hotkey();
        String name = key > 0 ? Keyboard.getKeyName(key) : null;
        if (name != null) {
            tooltip.addPara("Hotkey: %s (LunaLib settings).", 10f, Misc.getGrayColor(), Misc.getHighlightColor(), name);
        }
    }
}
