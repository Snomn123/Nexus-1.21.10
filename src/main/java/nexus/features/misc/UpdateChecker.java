package nexus.features.misc;

import meteordevelopment.orbit.EventHandler;
import nexus.config.Feature;
import nexus.events.ServerJoinEvent;
import nexus.misc.Utils;

import static nexus.Main.eventBus;

public class UpdateChecker {
    public static final Feature instance = new Feature("updateChecker");

    @EventHandler
    private static void onJoin(ServerJoinEvent event) {
        if (instance.isActive()) {
            Utils.checkUpdate(false);
            eventBus.unsubscribe(UpdateChecker.class);
        }
    }
}
