package achievementsrenewed.scripts;

import achievementsrenewed.api.AchievementScript;
import achievementsrenewed.api.AchievementSpec;

/**
 * The script of every rbr_* row in achievements.csv: reports the count of that achievement's check in
 * Feats, found by the row's id.
 */
public final class FeatScript implements AchievementScript {

    @Override
    public int count(AchievementSpec spec) {
        return Feats.count(spec.id);
    }
}
