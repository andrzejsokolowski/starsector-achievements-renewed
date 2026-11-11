package achievementsrenewed.api;

/**
 * A line another mod adds under an achievement in the list and in its intel entry, such as what that mod
 * pays or unlocks for it ("+5 career points"). Kept for the session: add it in onApplicationLoad. One per
 * class, as with listeners. Return null or "" for an achievement your mod gives nothing for.
 */
public interface RewardText {
    String rewardFor(AchievementSpec spec);
}
