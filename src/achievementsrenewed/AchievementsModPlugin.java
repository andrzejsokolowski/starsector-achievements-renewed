package achievementsrenewed;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;

/** Reads the achievement list at startup and wires the checks and the intel entries into every save. */
public final class AchievementsModPlugin extends BaseModPlugin {

    @Override
    public void onApplicationLoad() {
        try {
            Global.getSettings().loadTexture(Ids.ICON);
        } catch (Exception ignored) {
            // A missing icon only shows as blank; the build checks the path.
        }
        Catalog.ensureLoaded();
    }

    @Override
    public void onGameLoad(boolean newGame) {
        Global.getSector().addTransientScript(new CheckScript());
        Global.getSector().addTransientListener(new BattleListener());
        try {
            IntelKeeper.restoreOnLoad();
        } catch (Throwable t) {
            Global.getLogger(AchievementsModPlugin.class).error("Achievements: could not set up the intel entries", t);
        }
        Tracker.onGameLoad();
    }

    @Override
    public void beforeGameSave() {
        IntelKeeper.stash();
    }

    @Override
    public void afterGameSave() {
        IntelKeeper.unstash();
    }

    @Override
    public void onGameSaveFailed() {
        IntelKeeper.unstash();
    }
}
