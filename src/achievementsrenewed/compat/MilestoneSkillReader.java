package achievementsrenewed.compat;

import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;

/**
 * The milestones of Quality Perks and Quality Captains (by Dal). They are skills under the aptitude
 * "aptitude_milestones" that the player gets when a milestone is reached, so the game's own skill list is
 * all this reads: no class of those mods is named. While a milestone is not reached, those mods rename
 * the skill themselves, and that name and line are what is shown.
 */
public final class MilestoneSkillReader implements ExternalSource {

    private static final String APTITUDE = "aptitude_milestones";

    @Override
    public List<ExternalAchievement> read() {
        List<ExternalAchievement> out = new ArrayList<>();
        if (Global.getSector() == null || !Global.getSettings().getAptitudeIds().contains(APTITUDE)) return out;
        MutableCharacterStatsAPI player = Global.getSector().getPlayerStats();
        if (player == null) return out;
        for (String id : Global.getSettings().getSkillIds()) {
            SkillSpecAPI skill = Global.getSettings().getSkillSpec(id);
            if (skill == null || skill.isAptitudeEffect() || !APTITUDE.equals(skill.getGoverningAptitudeId())) continue;
            ModSpecAPI mod = skill.getSourceMod();
            String group = mod != null && mod.getName() != null ? mod.getName().trim() : "Milestones";
            out.add(new ExternalAchievement(group, skill.getName(), skill.getDescription(), skill.getSpriteName(),
                    player.getSkillLevel(id) > 0f, false, 0, 0));
        }
        return out;
    }
}
