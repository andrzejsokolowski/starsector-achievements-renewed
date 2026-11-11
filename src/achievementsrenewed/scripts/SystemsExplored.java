package achievementsrenewed.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;

import achievementsrenewed.api.AchievementScript;
import achievementsrenewed.api.AchievementSpec;

/**
 * The share of the Sector's star systems the player has entered in this save, in whole percent (rounded
 * down). Hidden systems (theme_hidden, the abyssal ones, systems kept off the map) count for neither side.
 */
public final class SystemsExplored implements AchievementScript {

    @Override
    public int count(AchievementSpec spec) {
        int all = 0, entered = 0;
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            if (system.hasTag(Tags.THEME_HIDDEN) || system.hasTag(Tags.SYSTEM_ABYSSAL)
                    || system.hasTag(Tags.STAR_HIDDEN_ON_MAP)) continue;
            all++;
            if (system.isEnteredByPlayer()) entered++;
        }
        return all == 0 ? 0 : (int) Math.floor(entered * 100.0 / all);
    }
}
