package achievementsrenewed.ui;

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CustomVisualDialogDelegate;
import com.fs.starfarer.api.ui.CustomPanelAPI;

/** Hosts the achievements panel inside an interaction dialog. */
public final class AchievementsDelegate implements CustomVisualDialogDelegate {

    private final AchievementsPanel list;
    private final Runnable onClose;
    private boolean reported;

    public AchievementsDelegate(AchievementsPanel list, Runnable onClose) {
        this.list = list;
        this.onClose = onClose;
    }

    @Override
    public CustomUIPanelPlugin getCustomPanelPlugin() {
        return list;
    }

    @Override
    public void init(CustomPanelAPI panel, DialogCallbacks callbacks) {
        callbacks.getPanelFader().setDurationOut(0.3f);
        list.init(panel, callbacks);
    }

    @Override
    public float getNoiseAlpha() {
        return 0.05f;
    }

    @Override
    public void advance(float amount) {
    }

    @Override
    public void reportDismissed(int option) {
        if (reported) return;
        reported = true;
        if (onClose != null) onClose.run();
    }
}
