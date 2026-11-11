package achievementsrenewed.campaign;

import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.listeners.CampaignInputListener;
import com.fs.starfarer.api.input.InputEventAPI;

import achievementsrenewed.Settings;
import achievementsrenewed.api.Achievements;

/** The LunaLib hotkey that opens the list from the campaign map. Added on every load, never saved. */
public final class HotkeyListener implements CampaignInputListener {

    @Override
    public int getListenerInputPriority() {
        return 0;
    }

    @Override
    public void processCampaignInputPreCore(List<InputEventAPI> events) {
        int key = Settings.hotkey();
        if (key <= 0 || events == null) return;
        CampaignUIAPI ui = Global.getSector().getCampaignUI();
        if (ui == null || ui.isShowingDialog() || ui.isShowingMenu() || ui.getCurrentCoreTab() != null) return;
        for (InputEventAPI event : events) {
            if (event.isConsumed() || !event.isKeyDownEvent() || event.getEventValue() != key) continue;
            if (event.isCtrlDown() || event.isAltDown() || event.isShiftDown()) continue;
            event.consume();
            Achievements.openList();
            return;
        }
    }

    @Override
    public void processCampaignInputPreFleetControl(List<InputEventAPI> events) {
    }

    @Override
    public void processCampaignInputPostCore(List<InputEventAPI> events) {
    }
}
