package achievementsrenewed.ui;

import java.util.Map;

import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;

import achievementsrenewed.Tracker;

/**
 * Opens the achievements screen. When the screen closes, this dialog closes itself on the next frame
 * rather than from inside the screen's dismissal callback. A plain Close option stays underneath in case
 * the screen is ever closed some other way.
 */
public final class AchievementsDialog implements InteractionDialogPlugin {

    private static final String OPTION_CLOSE = "acr_close";
    private static final String OPTION_OPEN = "acr_open";

    private InteractionDialogAPI dialog;
    private boolean closeRequested;
    private boolean dismissed;

    @Override
    public void init(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        dialog.getOptionPanel().addOption("Open the achievements", OPTION_OPEN);
        dialog.getOptionPanel().addOption("Close", OPTION_CLOSE);
        dialog.getOptionPanel().setShortcut(OPTION_CLOSE, Keyboard.KEY_ESCAPE, false, false, false, true);
        showList();
    }

    private void showList() {
        // Fresh counts for the progress shown in the list.
        Tracker.check();
        AchievementsPanel list = new AchievementsPanel();
        dialog.showCustomVisualDialog(list.width(), list.height(),
                new AchievementsDelegate(list, () -> closeRequested = true));
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        if (OPTION_CLOSE.equals(optionData)) dismiss();
        else if (OPTION_OPEN.equals(optionData)) showList();
    }

    @Override
    public void optionMousedOver(String optionText, Object optionData) {
    }

    @Override
    public void advance(float amount) {
        if (closeRequested) {
            closeRequested = false;
            dismiss();
        }
    }

    private void dismiss() {
        if (dismissed || dialog == null) return;
        dismissed = true;
        dialog.dismiss();
    }

    @Override
    public void backFromEngagement(EngagementResultAPI battleResult) {
    }

    @Override
    public Object getContext() {
        return null;
    }

    @Override
    public Map<String, MemoryAPI> getMemoryMap() {
        return null;
    }
}
