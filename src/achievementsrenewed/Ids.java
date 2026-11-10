package achievementsrenewed;

/** Ids and paths used in more than one place. */
public final class Ids {

    private Ids() {}

    public static final String MOD_ID = "achievements_renewed";
    public static final String CSV = "data/config/achievements_renewed/achievements.csv";
    /** Done achievements, in saves/common. */
    public static final String COMMON_FILE = "achievements_renewed.json";
    public static final String COMMON_BACKUP = "achievements_renewed_unreadable.json";
    public static final String ICON = "graphics/achievements_renewed/icon.png";

    /** Sector persistent data: counts of achievements driven from code (id to count). */
    public static final String DATA_COUNTS = "acr_counts";
    /** Sector persistent data: battles won (all, station, faction:id). */
    public static final String DATA_WINS = "acr_wins";
    /** Sector persistent data: achievements done in this save that still have an intel entry. */
    public static final String DATA_NOTICES = "acr_notices";

    /** Sector memory, for other mods and rules.csv. */
    public static final String MEMORY_DONE = "$acr_doneCount";
    public static final String MEMORY_TOTAL = "$acr_totalCount";

    public static final String INTEL_TAG = "Achievements";
    public static final String MAGICLIB = "MagicLib";
}
