package achievementsrenewed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;

/**
 * What this mod keeps in a save: plain maps and lists of strings and numbers in the sector's persistent
 * data, never a class of this mod, so a save still loads after the mod is removed.
 */
public final class SaveData {

    private SaveData() {}

    private static Map<String, Object> data() {
        return Global.getSector() == null ? null : Global.getSector().getPersistentData();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(String key, boolean create) {
        Map<String, Object> data = data();
        if (data == null) return null;
        Object value = data.get(key);
        if (value instanceof Map) return (Map<String, Object>) value;
        if (!create) return null;
        Map<String, Object> made = new HashMap<>();
        data.put(key, made);
        return made;
    }

    public static int get(String mapKey, String id) {
        Map<String, Object> map = map(mapKey, false);
        Object value = map == null ? null : map.get(id);
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    /** Adds to a count and returns the new value. */
    public static int add(String mapKey, String id, int amount) {
        Map<String, Object> map = map(mapKey, true);
        if (map == null) return 0;
        Object value = map.get(id);
        long next = (value instanceof Number ? ((Number) value).longValue() : 0L) + amount;
        int clamped = (int) Math.max(0L, Math.min(Integer.MAX_VALUE, next));
        map.put(id, clamped);
        return clamped;
    }

    /** Entries "timestamp|id" of achievements done in this save that still have an intel entry. */
    @SuppressWarnings("unchecked")
    public static List<String> notices() {
        Map<String, Object> data = data();
        if (data == null) return new ArrayList<>();
        Object value = data.get(Ids.DATA_NOTICES);
        if (value instanceof List) return (List<String>) value;
        List<String> made = new ArrayList<>();
        data.put(Ids.DATA_NOTICES, made);
        return made;
    }
}
