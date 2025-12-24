package nexus.features.misc;

import nexus.config.Config;
import nexus.config.Feature;

public class AutoSave {
    public static final Feature instance = new Feature("autoSave");

    private static int hash = 0;

    public static void save() {
        if (hash != Config.getHash()) {
            Config.saveAsync();
            hash = Config.getHash();
        }
    }
}
