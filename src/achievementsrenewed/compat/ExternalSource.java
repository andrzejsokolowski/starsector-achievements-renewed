package achievementsrenewed.compat;

import java.util.List;

/** Reads the achievements of another system, as they stand right now. */
public interface ExternalSource {
    List<ExternalAchievement> read();
}
