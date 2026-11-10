package achievementsrenewed;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;

import achievementsrenewed.scripts.BuiltIn;

/**
 * Counts the battles the player's side wins, in all and per enemy faction, for the "wins" scripts, and
 * checks achievements after every battle the player is in. Transient.
 */
public final class BattleListener extends BaseCampaignEventListener {

    public BattleListener() {
        super(false);
    }

    @Override
    public void reportBattleFinished(CampaignFleetAPI primaryWinner, BattleAPI battle) {
        try {
            if (battle == null || !battle.isPlayerInvolved()) return;
            recordWin(primaryWinner, battle);
            Tracker.check();
        } catch (Throwable t) {
            Global.getLogger(BattleListener.class).error("Achievements: could not record a battle", t);
        }
    }

    private static void recordWin(CampaignFleetAPI primaryWinner, BattleAPI battle) {
        if (primaryWinner == null || !battle.onPlayerSide(primaryWinner)) return;
        List<CampaignFleetAPI> enemies = battle.getNonPlayerSideSnapshot();
        if (enemies == null || enemies.isEmpty()) enemies = battle.getNonPlayerSide();
        if (enemies == null || enemies.isEmpty()) return;

        Set<String> factions = new HashSet<>();
        boolean station = false;
        for (CampaignFleetAPI fleet : enemies) {
            if (fleet == null) continue;
            if (fleet.getFaction() != null) factions.add(fleet.getFaction().getId());
            if (fleet.isStationMode()) station = true;
        }
        SaveData.add(Ids.DATA_WINS, BuiltIn.WINS_ALL, 1);
        if (station) SaveData.add(Ids.DATA_WINS, BuiltIn.WINS_STATION, 1);
        for (String faction : factions) SaveData.add(Ids.DATA_WINS, BuiltIn.WINS_FACTION + faction, 1);
    }
}
