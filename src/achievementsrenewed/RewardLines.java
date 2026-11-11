package achievementsrenewed;

import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.Global;

import achievementsrenewed.api.AchievementSpec;
import achievementsrenewed.api.RewardText;

/** The reward line of the achievement itself, then the lines other mods add for it. */
public final class RewardLines {

    private static final List<RewardText> SOURCES = new ArrayList<>();

    private RewardLines() {}

    public static void add(RewardText source) {
        if (source == null) return;
        SOURCES.removeIf(known -> known == source || known.getClass() == source.getClass());
        SOURCES.add(source);
    }

    public static void remove(RewardText source) {
        SOURCES.remove(source);
    }

    /** Every line, in order, without empty ones; joined with line breaks by {@link #text}. */
    public static List<String> of(AchievementSpec spec) {
        List<String> lines = new ArrayList<>();
        if (spec == null) return lines;
        if (!spec.reward.isEmpty()) lines.add(spec.reward);
        for (RewardText source : new ArrayList<>(SOURCES)) {
            try {
                String line = source.rewardFor(spec);
                if (line != null && !line.trim().isEmpty() && !lines.contains(line.trim())) lines.add(line.trim());
            } catch (Throwable t) {
                Global.getLogger(RewardLines.class).error("Achievements: a reward line failed on " + spec.id, t);
            }
        }
        return lines;
    }

    public static String text(AchievementSpec spec) {
        return String.join("\n", of(spec));
    }
}
