package achievementsrenewed;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.fs.starfarer.api.Global;

/**
 * Achievements done in any save, with the real-world date, kept in saves/common so they last across
 * campaigns. SettingsAPI's common files are the only file access mods have (java.io is refused), so this
 * is read and written through it. Ids of achievements whose mod is off stay in the file.
 */
public final class Store {

    private static final Logger LOG = Global.getLogger(Store.class);

    /** Id to date (yyyy-MM-dd, or empty when the date is unknown). */
    private static Map<String, String> done;

    private Store() {}

    private static Map<String, String> done() {
        if (done == null) done = read();
        return done;
    }

    private static Map<String, String> read() {
        Map<String, String> out = new LinkedHashMap<>();
        String text = null;
        try {
            if (!Global.getSettings().fileExistsInCommon(Ids.COMMON_FILE)) return out;
            text = Global.getSettings().readTextFileFromCommon(Ids.COMMON_FILE);
            if (text == null || text.trim().isEmpty()) return out;
            JSONObject entries = new JSONObject(text).optJSONObject("done");
            if (entries == null) return out;
            Iterator<?> keys = entries.keys();
            while (keys.hasNext()) {
                String id = String.valueOf(keys.next()).trim();
                if (id.isEmpty() || entries.isNull(id)) continue;
                JSONObject entry = entries.optJSONObject(id);
                String date = entry != null ? entry.optString("date", "") : entries.optString(id, "");
                out.put(id, date == null || date.equals("null") ? "" : date.trim());
            }
        } catch (Throwable t) {
            LOG.error("Achievements: could not read " + Ids.COMMON_FILE + ", starting with none done", t);
            out.clear();
            keepUnreadable(text);
        }
        return out;
    }

    /** The next write replaces the file, so a file that could not be read is kept under another name. */
    private static void keepUnreadable(String text) {
        if (text == null || text.trim().isEmpty()) return;
        try {
            Global.getSettings().writeTextFileToCommon(Ids.COMMON_BACKUP, text);
        } catch (Throwable t) {
            LOG.error("Achievements: could not keep a copy of the unreadable " + Ids.COMMON_FILE, t);
        }
    }

    private static void write() {
        try {
            JSONObject entries = new JSONObject();
            for (Map.Entry<String, String> e : done().entrySet()) {
                JSONObject entry = new JSONObject();
                entry.put("date", e.getValue());
                entries.put(e.getKey(), entry);
            }
            JSONObject root = new JSONObject();
            root.put("version", 1);
            root.put("done", entries);
            Global.getSettings().writeTextFileToCommon(Ids.COMMON_FILE, root.toString(2));
        } catch (Throwable t) {
            LOG.error("Achievements: could not save " + Ids.COMMON_FILE, t);
        }
    }

    public static boolean isDone(String id) {
        return done().containsKey(id);
    }

    /** The date it was done (yyyy-MM-dd, possibly empty), or null when it is not done. */
    public static String date(String id) {
        return done().get(id);
    }

    /** Records an achievement as done today. Returns true when it is new. */
    public static boolean markDone(String id) {
        if (done().containsKey(id)) return false;
        String today;
        try {
            today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        } catch (Throwable t) {
            today = "";
        }
        done().put(id, today);
        write();
        return true;
    }
}
