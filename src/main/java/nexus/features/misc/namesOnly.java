package nexus.features.misc;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import nexus.config.Feature;
import nexus.config.SettingBool;
import nexus.config.SettingEnum;
import nexus.config.SettingString;
import nexus.events.WorldTickEvent;
import nexus.features.combat.KillAura;
import nexus.misc.Utils;
import org.lwjgl.glfw.GLFW;

import static nexus.Main.mc;

public class namesOnly {
    public static final Feature instance = new Feature("namesOnly");

    public static final SettingBool onMiddleClick = new SettingBool(true, "onMiddleClick", instance);
    public static final SettingBool debug = new SettingBool(false, "debug", instance);
    public static final SettingEnum<TargetMode> targetMode = new SettingEnum<>(TargetMode.Type, TargetMode.class, "targetMode", instance.key());
    public static final SettingString TargetNames = new SettingString("", "TargetNames", instance.key());

    private static boolean wasMiddlePressed = false;

    @EventHandler
    private static void onTick(WorldTickEvent event) {
        if (!instance.isActive() || mc.player == null) return;

        // Check if middle mouse button is pressed
        boolean isMiddlePressed = GLFW.glfwGetMouseButton(
                mc.getWindow().getHandle(),
                GLFW.GLFW_MOUSE_BUTTON_MIDDLE
        ) == GLFW.GLFW_PRESS;

        // Detect middle-click (pressed now, wasn't pressed before)
        if (isMiddlePressed && !wasMiddlePressed) {
            onMiddleClick();
        }

        wasMiddlePressed = isMiddlePressed;
    }

    private static void onMiddleClick() {
        if (!onMiddleClick.value()) return;

        // Get the entity the player is looking at
        HitResult hitResult = mc.crosshairTarget;

        if (hitResult != null && hitResult.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHit = (EntityHitResult) hitResult;
            Entity entity = entityHit.getEntity();

            if (entity != null && entity != mc.player) {
                String entityName = entity.getName().getString();
                String entityType = entity.getType().getName().getString();
                String entityTypeId = entity.getType().toString();

                if (targetMode.value() == TargetMode.Type) {
                    // Target by entity type
                    String currentTypes = KillAura.TargetTypes.value();
                    boolean isInList = currentTypes.toLowerCase().contains(entityTypeId.toLowerCase());

                    if (isInList) {
                        KillAura.removeTargetType(entityTypeId);
                        Utils.info("§cRemoved type §f" + entityType + "§c from target list!");
                    } else {
                        KillAura.addTargetType(entityTypeId);
                        Utils.info("§aAdded type §f" + entityType + "§a to target list!");
                    }

                    if (debug.value()) {
                        Utils.info("§7Entity Name: " + entityName);
                        Utils.info("§7Entity Type: " + entityType);
                        Utils.info("§7Type ID: " + entityTypeId);
                        Utils.info("§7Action: " + (isInList ? "Removed" : "Added"));
                    }
                } else {
                    // Target by entity name
                    String currentNames = TargetNames.value();
                    boolean isInList = currentNames.toLowerCase().contains(entityName.toLowerCase());

                    if (isInList) {
                        removeTargetName(entityName);
                        Utils.info("§cRemoved name §f" + entityName + "§c from target list!");
                    } else {
                        addTargetName(entityName);
                        Utils.info("§aAdded name §f" + entityName + "§a to target list!");
                    }

                    if (debug.value()) {
                        Utils.info("§7Entity Name: " + entityName);
                        Utils.info("§7Entity Type: " + entityType);
                        Utils.info("§7Action: " + (isInList ? "Removed" : "Added"));
                    }
                }
            }
        }
    }

    public static void addTargetName(String name) {
        String currentNames = TargetNames.value();
        if (currentNames.isEmpty()) {
            TargetNames.set(name);
            return;
        }
        if (currentNames.toLowerCase().contains(name.toLowerCase())) return;
        TargetNames.set(currentNames + ", " + name);
    }

    public static void removeTargetName(String name) {
        String currentNames = TargetNames.value();
        if (currentNames.isEmpty()) return;

        StringBuilder newNames = new StringBuilder();
        for (String n : currentNames.split(",")) {
            String trimmed = n.trim();
            if (trimmed.isEmpty() || trimmed.equalsIgnoreCase(name)) continue;
            if (!newNames.isEmpty()) newNames.append(", ");
            newNames.append(trimmed);
        }

        TargetNames.set(newNames.toString());
    }

    public enum TargetMode { Type, Name }
}

