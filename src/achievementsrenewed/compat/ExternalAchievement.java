package achievementsrenewed.compat;

/** An achievement another system keeps, shown read-only in the tab named after its mod. */
public final class ExternalAchievement {

    /** The title of the tab it is listed in: the name of the mod it comes from. */
    public final String group;
    /** The system that keeps it ("MagicLib", "Milestones"), shown as a heading when a tab mixes systems. */
    public final String source;
    public final String name;
    public final String description;
    /** Sprite path, or empty for the default trophy. */
    public final String icon;
    public final boolean done;
    /** Its own system hides the name and description until it is done. */
    public final boolean hidden;
    /** Progress toward goal; goal is 0 when it has no count. */
    public final int progress, goal;

    public ExternalAchievement(String group, String name, String description, String icon, boolean done,
                               boolean hidden, int progress, int goal, String source) {
        this.group = group == null || group.trim().isEmpty() ? "Other" : group.trim();
        this.name = name == null ? "" : name.trim();
        this.description = description == null ? "" : description.trim();
        this.icon = icon == null ? "" : icon.trim();
        this.done = done;
        this.hidden = hidden;
        this.progress = progress;
        this.goal = goal;
        this.source = source == null ? "" : source.trim();
    }
}
