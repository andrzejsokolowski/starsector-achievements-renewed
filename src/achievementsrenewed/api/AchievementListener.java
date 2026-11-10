package achievementsrenewed.api;

/**
 * Told when an achievement is done for the first time. Listeners are not saved: add yours again on every
 * game load. One listener per class is kept, so adding a new instance of the same class replaces the old one.
 */
public interface AchievementListener {
    void achievementDone(AchievementSpec spec);
}
