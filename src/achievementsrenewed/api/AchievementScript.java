package achievementsrenewed.api;

/**
 * Optional per-achievement logic, named in the script column of achievements.csv. One instance per
 * achievement, made once at startup with the class's no-argument constructor. Nothing of it is saved.
 */
public interface AchievementScript {

    /**
     * Current count in this save; the achievement is done when it reaches the spec's goal. For a one-time
     * achievement return 1 when it is done and 0 when it is not. Polled daily, after battles, on game load
     * and when the list opens.
     */
    int count(AchievementSpec spec);

    /** Called on every game load, before the first check. */
    default void onGameLoad(AchievementSpec spec) {}
}
