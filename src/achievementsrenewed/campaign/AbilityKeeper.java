package achievementsrenewed.campaign;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.PersistentUIDataAPI.AbilitySlotAPI;
import com.fs.starfarer.api.campaign.PersistentUIDataAPI.AbilitySlotsAPI;

/**
 * Gives the player the Achievements ability and keeps it out of the save file, as IntelKeeper does with
 * the intel entries: before a save the ability is taken off the bar and off the player, and the slots it
 * sat in are kept in the save as plain text; after the save (and on load) it goes back to the same slots.
 * The first time a save sees this mod the ability goes into the first free slot; once the player takes
 * it off the bar it stays off.
 */
public final class AbilityKeeper {

    public static final String ABILITY_ID = "acr_achievements";
    /** Sector persistent data: slots the ability sat in, as "bar:slot:n" (normal) or "bar:slot:h" (hyperspace). */
    private static final String DATA_SLOTS = "acr_abilitySlots";
    /** Sector persistent data: the ability has been put on the bar once in this save. */
    private static final String DATA_PLACED = "acr_abilityPlaced";
    private static final int BARS = 5;

    private AbilityKeeper() {}

    /** On game load and after a save: the ability, back in its slots. */
    public static void give() {
        try {
            CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
            if (!Global.getSector().getCharacterData().getAbilities().contains(ABILITY_ID)) {
                Global.getSector().getCharacterData().addAbility(ABILITY_ID);
            }
            if (fleet != null && !fleet.hasAbility(ABILITY_ID)) fleet.addAbility(ABILITY_ID);

            Map<String, Object> data = Global.getSector().getPersistentData();
            if (data.get(DATA_PLACED) == null) {
                data.put(DATA_PLACED, "true");
                if (findSlots().isEmpty()) placeInFreeSlot();
            } else {
                restoreSlots();
            }
        } catch (Throwable t) {
            Global.getLogger(AbilityKeeper.class).error("Achievements: could not add the ability", t);
        }
    }

    /** Before a save: remember the slots, clear them and take the ability off the player. */
    public static void take() {
        try {
            List<String> slots = findSlots();
            Global.getSector().getPersistentData().put(DATA_SLOTS, new ArrayList<>(slots));
            forEachSlot((bar, slot) -> {
                if (ABILITY_ID.equals(slot.getAbilityId())) slot.setAbilityId(null);
                if (ABILITY_ID.equals(slot.getInHyperAbilityId())) slot.setInHyperAbilityId(null);
                return false;
            });
            CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
            if (fleet != null && fleet.hasAbility(ABILITY_ID)) fleet.removeAbility(ABILITY_ID);
            Global.getSector().getCharacterData().removeAbility(ABILITY_ID);
        } catch (Throwable t) {
            Global.getLogger(AbilityKeeper.class).error("Achievements: could not take the ability out before the save", t);
        }
    }

    private interface SlotVisitor {
        /** True to stop. */
        boolean visit(int bar, AbilitySlotAPI slot);
    }

    private static void forEachSlot(SlotVisitor visitor) {
        AbilitySlotsAPI slots = Global.getSector().getUIData().getAbilitySlotsAPI();
        int current = slots.getCurrBarIndex();
        try {
            // The current bar first, so a new ability lands where the player looks.
            List<Integer> order = new ArrayList<>();
            order.add(current);
            for (int bar = 0; bar < BARS; bar++) if (bar != current) order.add(bar);
            // The game has 5 bars of 10 slots (0.98a); the slots in the copied list are the live ones.
            for (int bar : order) {
                List<AbilitySlotAPI> barSlots;
                try {
                    slots.setCurrBarIndex(bar);
                    barSlots = slots.getCurrSlotsCopy();
                } catch (Throwable noSuchBar) {
                    continue;
                }
                for (AbilitySlotAPI slot : barSlots) {
                    if (visitor.visit(bar, slot)) return;
                }
            }
        } finally {
            slots.setCurrBarIndex(current);
        }
    }

    private static List<String> findSlots() {
        List<String> found = new ArrayList<>();
        forEachSlot((bar, slot) -> {
            if (ABILITY_ID.equals(slot.getAbilityId())) found.add(bar + ":" + slot.getSlotId() + ":n");
            if (ABILITY_ID.equals(slot.getInHyperAbilityId())) found.add(bar + ":" + slot.getSlotId() + ":h");
            return false;
        });
        return found;
    }

    private static void placeInFreeSlot() {
        forEachSlot((bar, slot) -> {
            if (slot.getAbilityId() != null || slot.getInHyperAbilityId() != null) return false;
            slot.setAbilityId(ABILITY_ID);
            return true;
        });
    }

    private static void restoreSlots() {
        Object stored = Global.getSector().getPersistentData().get(DATA_SLOTS);
        if (!(stored instanceof List) || ((List<?>) stored).isEmpty()) return;
        List<?> wanted = (List<?>) stored;
        forEachSlot((bar, slot) -> {
            String normal = bar + ":" + slot.getSlotId() + ":n", hyper = bar + ":" + slot.getSlotId() + ":h";
            if (wanted.contains(normal) && slot.getAbilityId() == null) slot.setAbilityId(ABILITY_ID);
            if (wanted.contains(hyper) && slot.getInHyperAbilityId() == null) slot.setInHyperAbilityId(ABILITY_ID);
            return false;
        });
    }
}
