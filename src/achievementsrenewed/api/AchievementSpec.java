package achievementsrenewed.api;

/**
 * One achievement. Immutable. Read from data/config/achievements_renewed/achievements.csv, or built in code
 * with {@link #builder} and handed to {@link Achievements#register}.
 */
public final class AchievementSpec {

    /** The icon used when a row names none. */
    public static final String DEFAULT_ICON = "graphics/achievements_renewed/icon.png";

    public final String id;
    /** The mod whose CSV defined it (or the mod named when it was built in code). */
    public final String modId;
    /** Shown once done: what the player did, as a short plain line. */
    public final String name;
    /** Shown before it is done: one short line that points at it without naming it. */
    public final String hint;
    /** The exact condition. Shown instead of the hint when the player turns that setting on. */
    public final String howTo;
    /** Shown under the name once done. May be empty; the list then shows howTo. */
    public final String description;
    /** Sprite path. */
    public final String icon;
    /** Heading inside the mod's group. May be empty. */
    public final String category;
    /** Count needed, at least 1. Above 1 the list shows progress, for example "3 / 10". */
    public final int goal;
    /** Name and hint show as ??? until done. */
    public final boolean secret;
    /** What it gives, for example "Unlocks the rule Imperial Charter". May be empty. */
    public final String reward;
    /** Achievements with the same chain are shown as one group, under this name. May be empty. */
    public final String chain;
    /** Order inside the chain. */
    public final int step;
    /** Id of the achievement that must be done before this one is shown. May be empty. */
    public final String after;
    /** Null when the achievement is driven from code with complete or addProgress. */
    public final AchievementScript script;
    /** Sort order inside the mod's group; lower comes first. */
    public final float order;

    private AchievementSpec(Builder b) {
        id = b.id;
        modId = b.modId;
        name = b.name;
        hint = b.hint.isEmpty() ? b.name : b.hint;
        description = b.description;
        howTo = !b.howTo.isEmpty() ? b.howTo : !b.description.isEmpty() ? b.description : b.name;
        icon = b.icon.isEmpty() ? DEFAULT_ICON : b.icon;
        category = b.category;
        goal = Math.max(1, b.goal);
        secret = b.secret;
        reward = b.reward;
        chain = b.chain;
        step = b.step;
        after = b.after;
        script = b.script;
        order = b.order;
    }

    /** Done in any save. */
    public boolean isDone() {
        return Achievements.isDone(id);
    }

    /** Current count in this save. */
    public int progress() {
        return Achievements.progress(id);
    }

    /** False while it is secret, or while the achievement named in after is not done. Always true once done. */
    public boolean isRevealed() {
        if (isDone()) return true;
        if (secret) return false;
        return after.isEmpty() || Achievements.get(after) == null || Achievements.isDone(after);
    }

    /** True when the goal is above 1, so the list shows progress. */
    public boolean isCounted() {
        return goal > 1;
    }

    @Override
    public String toString() {
        return id;
    }

    /** Starts a spec built in code. modId is the id of the mod it belongs to. */
    public static Builder builder(String id, String modId, String name) {
        return new Builder(id, modId, name);
    }

    public static final class Builder {
        private final String id, modId, name;
        private String hint = "", howTo = "", description = "", icon = "", category = "", reward = "", chain = "", after = "";
        private int goal = 1, step;
        private boolean secret;
        private AchievementScript script;
        private float order = 1000f;

        private Builder(String id, String modId, String name) {
            if (id == null || id.trim().isEmpty()) throw new IllegalArgumentException("an achievement needs an id");
            if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("achievement " + id + " needs a name");
            this.id = id.trim();
            this.modId = modId == null ? "" : modId.trim();
            this.name = name.trim();
        }

        private static String clean(String text) {
            return text == null ? "" : text.trim();
        }

        public Builder hint(String hint) { this.hint = clean(hint); return this; }
        public Builder howTo(String howTo) { this.howTo = clean(howTo); return this; }
        public Builder description(String description) { this.description = clean(description); return this; }
        public Builder icon(String icon) { this.icon = clean(icon); return this; }
        public Builder category(String category) { this.category = clean(category); return this; }
        public Builder goal(int goal) { this.goal = goal; return this; }
        public Builder secret(boolean secret) { this.secret = secret; return this; }
        public Builder reward(String reward) { this.reward = clean(reward); return this; }
        public Builder chain(String chain, int step) { this.chain = clean(chain); this.step = step; return this; }
        public Builder after(String after) { this.after = clean(after); return this; }
        public Builder script(AchievementScript script) { this.script = script; return this; }
        public Builder order(float order) { this.order = order; return this; }

        public AchievementSpec build() {
            return new AchievementSpec(this);
        }
    }
}
