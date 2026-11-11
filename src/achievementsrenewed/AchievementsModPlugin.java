package achievementsrenewed;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;

import achievementsrenewed.campaign.AbilityKeeper;
import achievementsrenewed.campaign.HotkeyListener;

/** Reads the achievement list at startup and wires the checks, the intel entries, the ability and the hotkey into every save. */
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
        Global.getSector().getListenerManager().addListener(new HotkeyListener(), true);
        AbilityKeeper.give();
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
        AbilityKeeper.take();
    }

    @Override
    public void afterGameSave() {
        IntelKeeper.unstash();
        AbilityKeeper.give();
    }

    @Override
    public void onGameSaveFailed() {
        IntelKeeper.unstash();
        AbilityKeeper.give();
    }
}
