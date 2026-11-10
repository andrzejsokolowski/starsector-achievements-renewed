package achievementsrenewed.compat;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;

import achievementsrenewed.Ids;

/**
 * Achievements kept by other systems: MagicLib's (Nexerelin and others define theirs through it) and the
 * milestone skills of Quality Perks and Quality Captains. The list shows each in the tab named after the
 * mod it comes from.
 *
 * The MagicLib reader is the only class that names MagicLib. It is made by name, and only when the mod
 * MagicLib is enabled, so nothing here needs MagicLib to load.
 */
public final class ExternalSources {

    private static final Logger LOG = Global.getLogger(ExternalSources.class);
    private static final String MAGICLIB_READER = "achievementsrenewed.compat.magiclib.MagicLibReader";

    private static ExternalSource magicLib;
    private static boolean magicLibTried;

    private ExternalSources() {}

    private static ExternalSource magicLib() {
        if (magicLibTried) return magicLib;
        magicLibTried = true;
        try {
            if (Global.getSettings().getModManager().isModEnabled(Ids.MAGICLIB)) {
                Object made = Global.getSettings().getInstanceOfScript(MAGICLIB_READER);
                if (made instanceof ExternalSource) magicLib = (ExternalSource) made;
            }
        } catch (Throwable t) {
            LOG.error("Achievements: could not set up the MagicLib reader", t);
        }
        return magicLib;
    }

    /** Everything the other systems show, each system in its own order. */
    public static List<ExternalAchievement> readAll() {
        List<ExternalAchievement> out = new ArrayList<>();
        read(magicLib(), out, "MagicLib");
        read(new MilestoneSkillReader(), out, "milestone skills");
        return out;
    }

    private static void read(ExternalSource source, List<ExternalAchievement> out, String what) {
        if (source == null) return;
        try {
            out.addAll(source.read());
        } catch (Throwable t) {
            LOG.error("Achievements: could not read the achievements of " + what, t);
        }
    }
}
