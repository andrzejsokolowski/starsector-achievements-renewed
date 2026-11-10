package achievementsrenewed;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;

/**
 * Checks achievements on the first frame after a game load (so listeners other mods add in their
 * onGameLoad hear about it) and then once a day of game time. Added as a transient script on every load.
 */
public final class CheckScript implements EveryFrameScript {

    private static final float CHECK_DAYS = 1f;

    private float days;
    private boolean checkedOnce;

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }

    @Override
    public void advance(float amount) {
        if (Global.getCurrentState() != GameState.CAMPAIGN) return;
        if (!checkedOnce) {
            checkedOnce = true;
            Tracker.check();
        }
        if (Global.getSector().isPaused()) return;
        days += Global.getSector().getClock().convertToDays(amount);
        if (days < CHECK_DAYS) return;
        days = 0f;
        Tracker.check();
        IntelKeeper.prune();
    }
}
