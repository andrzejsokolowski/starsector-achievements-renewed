package achievementsrenewed.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

import achievementsrenewed.api.AchievementScript;
import achievementsrenewed.api.AchievementSpec;

/** Capital ships in the player's fleet. */
public final class CapitalShip implements AchievementScript {

    @Override
    public int count(AchievementSpec spec) {
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (fleet == null) return 0;
        int found = 0;
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            if (member.isCapital() && !member.isStation()) found++;
        }
        return found;
    }
}
