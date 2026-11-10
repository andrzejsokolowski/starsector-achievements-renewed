package achievementsrenewed.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.StarSystemAPI;

import achievementsrenewed.api.AchievementScript;
import achievementsrenewed.api.AchievementSpec;

/** Star systems the player has entered in this save. */
public final class StarSystemsEntered implements AchievementScript {

    @Override
    public int count(AchievementSpec spec) {
        int found = 0;
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            if (system.isEnteredByPlayer()) found++;
        }
        return found;
    }
}
