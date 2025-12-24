package nexus.features.misc;

import meteordevelopment.orbit.EventHandler;
import nexus.config.Feature;
import nexus.config.SettingBool;
import nexus.events.ServerJoinEvent;
import nexus.misc.Utils;

import static nexus.Main.eventBus;

public class UpdateChecker {
    public static final Feature instance = new Feature("updateChecker");
    public static SettingBool notifyOnJoin = new SettingBool(false, "notifyOnJoin", instance.key());

    @EventHandler
    private static void onJoin(ServerJoinEvent event) {
        if (instance.isActive()) {
            Utils.checkUpdate(notifyOnJoin.value());
            eventBus.unsubscribe(UpdateChecker.class);
        }
    }
}
