package achievementsrenewed;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;

import achievementsrenewed.scripts.BuiltIn;

/**
 * Counts the battles the player's side wins, in all and per enemy faction, for the "wins" scripts, and
 * checks achievements after every battle the player is in. Transient.
 *
 * Also counts wins against Omega, Threat or Dweller fleets and wins against an enemy with at least twice
 * the fleet points of the player's side, for the rbr_abyss and rbr_giant achievements (Feats).
 */
public final class BattleListener extends BaseCampaignEventListener {

    /** Key in the battle counts: won against an Omega, Threat or Dweller fleet. */
    public static final String WINS_ABYSS = "abyss";
    /** Key in the battle counts: won against at least twice the player side's fleet points. */
    public static final String WINS_GIANT = "giant";

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
        float enemyPoints = 0f, ownPoints = 0f;
        for (CampaignFleetAPI fleet : enemies) {
            if (fleet == null) continue;
            enemyPoints += fleet.getFleetPoints();
            if (fleet.getFaction() != null) factions.add(fleet.getFaction().getId());
            if (fleet.isStationMode()) station = true;
        }
        List<CampaignFleetAPI> own = battle.getPlayerSideSnapshot();
        if (own == null || own.isEmpty()) own = battle.getPlayerSide();
        if (own != null) for (CampaignFleetAPI fleet : own) if (fleet != null) ownPoints += fleet.getFleetPoints();
        boolean abyss = factions.contains(Factions.OMEGA) || factions.contains(Factions.THREAT)
                || factions.contains(Factions.DWELLER);

        SaveData.add(Ids.DATA_WINS, BuiltIn.WINS_ALL, 1);
        if (station) SaveData.add(Ids.DATA_WINS, BuiltIn.WINS_STATION, 1);
        for (String faction : factions) SaveData.add(Ids.DATA_WINS, BuiltIn.WINS_FACTION + faction, 1);
        if (abyss) SaveData.add(Ids.DATA_WINS, WINS_ABYSS, 1);
        if (ownPoints > 0f && enemyPoints >= ownPoints * 2f) SaveData.add(Ids.DATA_WINS, WINS_GIANT, 1);
    }
}
