package achievementsrenewed;

import lunalib.lunaSettings.LunaSettings;

/** LunaLib values, read live. A missing value falls back to the default in LunaSettings.csv. */
public final class Settings {

    private Settings() {}

    public static boolean showHowTo() {
        return flag("acr_showHowTo", false);
    }

    public static boolean showMessage() {
        return flag("acr_showMessage", true);
    }

    /** Key code of the hotkey that opens the list; 0 or less when there is none. */
    public static int hotkey() {
        try {
            Integer value = LunaSettings.getInt(Ids.MOD_ID, "acr_hotkey");
            return value == null ? 37 : value;
        } catch (Throwable t) {
            return 37;
        }
    }

    private static boolean flag(String key, boolean fallback) {
        try {
            Boolean value = LunaSettings.getBoolean(Ids.MOD_ID, key);
            return value == null ? fallback : value;
        } catch (Throwable t) {
            return fallback;
        }
    }
}
