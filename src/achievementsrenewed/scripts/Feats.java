package achievementsrenewed.scripts;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;

import achievementsrenewed.BattleListener;
import achievementsrenewed.Ids;
import achievementsrenewed.SaveData;

/**
 * The checks of the rbr_* achievements, which came from Rule Book - Renewed 0.10.3 (its Feats class)
 * with their ids unchanged: New Game Plus - Renewed pays career points once per id, and
 * Rule Book unlocks its rules from them. Never rename an id here or change what it asks for.
 *
 * Story checks read the game's own mission flags (the _missionCompleted keys the vanilla missions set
 * when they end). Battle checks read this mod's battle counts (BattleListener). Checks about Oddisz's
 * other mods read only state those mods leave in the save (memory keys, persistent data, industries,
 * hull mods, intel class names), never their classes; their rows' requiredMod column keeps them out
 * when the mod is off.
 */
public final class Feats {

    interface Count {
        int get();
    }

    private interface Flag {
        boolean done();
    }

    private static final Map<String, Count> COUNTS = new HashMap<>();

    static {
        // Vanilla story missions.
        flag("rbr_academy", () -> sector("$gaKA_missionCompleted"));
        flag("rbr_hamatsu", () -> sector("$gaRH_missionCompleted"));
        flag("rbr_coureuse", () -> sector("$gaFC_missionCompleted"));
        flag("rbr_project_ziggurat", () -> sector("$gaPZ_missionCompleted"));
        flag("rbr_ziggurat", () -> sector("$defeatedZiggurat"));
        flag("rbr_atg", () -> sector("$gaATG_missionCompleted"));
        flag("rbr_oddity", () -> sector("$gaDHO_missionCompleted"));
        flag("rbr_usurpers", () -> sector("$sdtu_missionCompleted"));
        flag("rbr_persea", () -> sector("$soe_completed"));
        flag("rbr_duel", () -> character("$soe_wonDuel"));
        flag("rbr_knight", () -> sector("$lke_missionCompleted"));
        flag("rbr_pilgrim", () -> sector("$lpp_missionCompleted"));
        flag("rbr_false_idols", () -> sector("$bffi_missionCompleted"));
        flag("rbr_stray_flock", () -> sector("$locrlf_completed"));
        flag("rbr_volturn", () -> sector("$anh_missionCompleted"));
        flag("rbr_planetkiller", () -> sector("$pk_recovered"));
        flag("rbr_kanta", () -> character("$everHadKantaProtection"));
        // The campaign at large.
        flag("rbr_colony", () -> colonies(1) >= 1);
        flag("rbr_colony6", () -> colonies(6) >= 1);
        flag("rbr_colonies5", () -> colonies(1) >= 5);
        flag("rbr_millionaire", () -> Global.getSector().getPlayerFleet() != null
                && Global.getSector().getPlayerFleet().getCargo().getCredits().get() >= 1000000f);
        // Battles, on the counts the wins scripts use too.
        COUNTS.put("rbr_wins", () -> wins(BuiltIn.WINS_ALL));
        COUNTS.put("rbr_remnant", () -> wins(BuiltIn.WINS_FACTION + "remnant"));
        COUNTS.put("rbr_abyss", () -> wins(BattleListener.WINS_ABYSS));
        COUNTS.put("rbr_station", () -> wins(BuiltIn.WINS_STATION));
        COUNTS.put("rbr_giant", () -> wins(BattleListener.WINS_GIANT));

        // Oddisz's other mods.
        flag("rbr_sol_reach", Feats::inSol);
        flag("rbr_sol_quest", () -> "DONE".equals(sectorString("$sr_quest_stage")));
        flag("rbr_sol_shroud", () -> sector("$sr_quest_shroud_killed"));
        flag("rbr_sol_crisis", () -> sector("$sr_crisis_deterred"));
        COUNTS.put("rbr_gd_wins", () -> persistentInt("gd_wins"));
        COUNTS.put("rbr_gd_pulls", () -> persistentInt("gd_pulls"));
        flag("rbr_cloning", () -> playerIndustry("oddisz_cloning_megafactory"));
        flag("rbr_cloning_supergrowth",
                () -> playerIndustryWith("oddisz_cloning_megafactory", "omega_core", "pristine_nanoforge"));
        flag("rbr_arsenal_weapons", () -> sectorSetSize("$RePA_ProducedWeapons") >= 10);
        flag("rbr_arsenal_ship", () -> sectorSetSize("$RePA_ProducedShips") >= 1);
        flag("rbr_starterpack", () -> Global.getSector().getPersistentData().get("$starterpack_applied") != null);
        flag("rbr_contract", () -> hasIntelOf("missionsrenewed.missions.ContractIntel"));
        flag("rbr_aar_ascended", () -> playerMarketMemory("$aar_ascended"));
        flag("rbr_aar_ecumenopolis", () -> playerCondition("aar_ecumenopolis"));
        flag("rbr_oce_tutelage", () -> officerMemory("$oce_omegaTutelage"));
        // The Everything Shop's nebula; Extra Submarkets, the mod it came from, called it the Gilgamesh Nebula.
        flag("rbr_great_mall", () -> enteredSystem("Gate of Babylon") || enteredSystem("Gilgamesh Nebula"));
        // New Game Plus - Renewed 0.7.2 sets $ngp_usedConsumable when any of its ship items is fitted; older
        // versions only leave the hull mod of the S-mod slot and Stat tuning items.
        flag("rbr_ngp_ledger", () -> sector("$ngp_usedConsumable") || fleetHasPermaMod("ngp_ledger_refit"));
    }

    private Feats() {}

    private static void flag(String id, Flag flag) {
        COUNTS.put(id, () -> flag.done() ? 1 : 0);
    }

    /** Whether this id has a check here. */
    static boolean has(String id) {
        return COUNTS.containsKey(id);
    }

    /** How far this achievement is in this save; 0 for an unknown id or when the check fails. */
    static int count(String id) {
        Count count = COUNTS.get(id);
        if (count == null || Global.getSector() == null) return 0;
        try {
            return Math.max(0, count.get());
        } catch (Throwable t) {
            return 0;
        }
    }

    // ---- reading the save ----

    private static int wins(String key) {
        return SaveData.get(Ids.DATA_WINS, key);
    }

    private static boolean sector(String key) {
        MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();
        return memory != null && memory.contains(key) && !Boolean.FALSE.equals(memory.get(key));
    }

    private static String sectorString(String key) {
        Object value = Global.getSector().getMemoryWithoutUpdate().get(key);
        return value == null ? null : value.toString();
    }

    private static boolean enteredSystem(String name) {
        StarSystemAPI system = Global.getSector().getStarSystem(name);
        return system != null && system.isEnteredByPlayer();
    }

    private static int sectorSetSize(String key) {
        Object value = Global.getSector().getMemoryWithoutUpdate().get(key);
        return value instanceof Collection ? ((Collection<?>) value).size() : 0;
    }

    private static int persistentInt(String key) {
        Object value = Global.getSector().getPersistentData().get(key);
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    private static boolean character(String key) {
        MemoryAPI memory = Global.getSector().getCharacterData().getMemoryWithoutUpdate();
        return memory != null && memory.contains(key);
    }

    private static int colonies(int minSize) {
        int count = 0;
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            if (market.isPlayerOwned() && market.getSize() >= minSize) count++;
        }
        return count;
    }

    private static boolean playerIndustry(String industryId) {
        for (MarketAPI market : Misc.getPlayerMarkets(false)) {
            if (market.hasIndustry(industryId)) return true;
        }
        return false;
    }

    private static boolean playerIndustryWith(String industryId, String coreId, String itemId) {
        for (MarketAPI market : Misc.getPlayerMarkets(false)) {
            Industry industry = market.getIndustry(industryId);
            if (industry == null || !coreId.equals(industry.getAICoreId())) continue;
            SpecialItemData item = industry.getSpecialItem();
            if (item != null && itemId.equals(item.getId())) return true;
        }
        return false;
    }

    private static boolean playerCondition(String conditionId) {
        for (MarketAPI market : Misc.getPlayerMarkets(false)) {
            if (market.hasCondition(conditionId)) return true;
        }
        return false;
    }

    private static boolean playerMarketMemory(String key) {
        for (MarketAPI market : Misc.getPlayerMarkets(false)) {
            if (market.getMemoryWithoutUpdate().contains(key)) return true;
        }
        return false;
    }

    private static boolean officerMemory(String key) {
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (fleet == null) return false;
        for (OfficerDataAPI officer : fleet.getFleetData().getOfficersCopy()) {
            if (officer.getPerson() != null && officer.getPerson().getMemoryWithoutUpdate().contains(key)) return true;
        }
        return false;
    }

    private static boolean fleetHasPermaMod(String hullModId) {
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (fleet == null) return false;
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            if (member.getVariant() != null && member.getVariant().getPermaMods().contains(hullModId)) return true;
        }
        return false;
    }

    private static boolean inSol() {
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        StarSystemAPI system = fleet == null ? null : fleet.getStarSystem();
        return system != null && system.getMemoryWithoutUpdate().contains("$sr_sol");
    }

    /** True when an intel of this class, or of a class built on it, is in the intel screen. */
    private static boolean hasIntelOf(String className) {
        for (IntelInfoPlugin intel : Global.getSector().getIntelManager().getIntel()) {
            for (Class<?> c = intel.getClass(); c != null; c = c.getSuperclass()) {
                if (c.getName().equals(className)) return true;
            }
        }
        return false;
    }
}
