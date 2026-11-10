package achievementsrenewed.compat.magiclib;

import java.util.ArrayList;
import java.util.List;

import org.magiclib.achievements.MagicAchievement;
import org.magiclib.achievements.MagicAchievementManager;
import org.magiclib.achievements.MagicAchievementSpoilerLevel;

import achievementsrenewed.compat.ExternalAchievement;
import achievementsrenewed.compat.ExternalSource;

/**
 * Reads the achievements MagicLib keeps (its own, Nexerelin's and those of every other mod that defines
 * them through MagicLib). Read-only: nothing is completed or changed.
 *
 * The only class in this mod that names MagicLib. ExternalSources makes it by name, and only when the mod
 * MagicLib is enabled; the build fails if any other class refers to MagicLib or to this package.
 */
public final class MagicLibReader implements ExternalSource {

    @Override
    public List<ExternalAchievement> read() {
        List<ExternalAchievement> out = new ArrayList<>();
        MagicAchievementManager manager = MagicAchievementManager.getInstance();
        if (manager == null || manager.getAchievements() == null) return out;
        for (MagicAchievement achievement : manager.getAchievements().values()) {
            if (achievement == null) continue;
            try {
                boolean done = achievement.isComplete();
                MagicAchievementSpoilerLevel spoiler = achievement.getSpoilerLevel();
                // MagicLib does not list a hidden achievement until it is done; neither does this list.
                if (!done && spoiler == MagicAchievementSpoilerLevel.Hidden) continue;
                boolean hidden = !done && spoiler == MagicAchievementSpoilerLevel.Spoiler;

                int progress = 0, goal = 0;
                if (achievement.getHasProgressBar() && achievement.getMaxProgress() != null) {
                    goal = Math.round(achievement.getMaxProgress());
                    Float now = achievement.getProgress();
                    progress = now == null ? 0 : Math.round(now);
                }
                out.add(new ExternalAchievement(achievement.getModName(), achievement.getName(),
                        achievement.getDescription(), achievement.getImage(), done, hidden, progress, goal));
            } catch (Throwable ignored) {
                // One achievement with a broken script does not hide the rest.
            }
        }
        return out;
    }
}
