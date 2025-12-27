package nexus.features.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.RaycastContext;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import nexus.config.*;
import nexus.events.InputEvent;
import nexus.events.WorldChangeEvent;
import nexus.events.WorldTickEvent;
import nexus.events.WorldRenderEvent;
import nexus.features.misc.namesOnly;
import nexus.misc.Utils;
import nexus.misc.RenderColor;
import nexus.misc.Rendering;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static nexus.Main.mc;

public class KillAura {
    public static final Feature instance = new Feature("Kill Aura");

    public static final SettingKeybind toggleKey = new SettingKeybind(GLFW.GLFW_KEY_UNKNOWN, "toggleKey", instance.key());
    public static final SettingBool RightClick = new SettingBool(false, "RightClick", instance);
    public static final SettingDouble Range = new SettingDouble(3.0, "Range", instance);
    public static final SettingString TargetTypes = new SettingString("", "TargetTypes", instance.key());
    public static final SettingDouble MinCPS = new SettingDouble(9.0, "MinCPS", instance);
    public static final SettingDouble MaxCPS = new SettingDouble(12.0, "MaxCPS", instance);
    public static final SettingEnum<SortingMode> Sorting = new SettingEnum<>(SortingMode.Distance, SortingMode.class, "Sorting", instance.key());
    public static final SettingEnum<TargetPoint> TargetPointMode = new SettingEnum<>(TargetPoint.Closest, TargetPoint.class, "TargetPoint", instance.key());
    public static final SettingEnum<TargetingMode> Targeting = new SettingEnum<>(TargetingMode.Single, TargetingMode.class, "Targeting", instance.key());
    public static final SettingDouble SwitchDelay = new SettingDouble(150.0, "SwitchDelay", instance);
    public static final SettingDouble FOV = new SettingDouble(360.0, "FOV", instance);
    public static final SettingBool ThroughWalls = new SettingBool(true, "ThroughWalls", instance);
    public static final SettingBool DrawRange = new SettingBool(false, "DrawRange", instance);
    public static final SettingBool DisableOnWorldChange = new SettingBool(true, "DisableOnWorldChange", instance);
    public static final SettingBool shouldLogFeature = new SettingBool(false, "shouldLogFeature", instance);

    private static final List<String> targetTypesList = new ArrayList<>();
    private static long nextInteractTime = 0;
    private static Entity currentTarget = null;
    private static long lastSwitchTime = 0;

    @EventHandler
    private static void onInput(InputEvent event) {
        if (toggleKey.bound() && toggleKey.key() == event.key && event.action == GLFW.GLFW_PRESS) {
            instance.setActive(!instance.isActive());
            Utils.info("Kill Aura " + (instance.isActive() ? "§aenabled" : "§cdisabled"));
            event.cancel();
        }
    }

    @EventHandler
    private static void onWorldChange(WorldChangeEvent event) {
        if (DisableOnWorldChange.value() && instance.isActive()) {
            instance.setActive(false);
            Utils.info("Kill Aura §cdisabled §7(world change)");
        }
    }

    @EventHandler
    private static void onTick(WorldTickEvent event) {
        if (!canOperate()) return;

        updateTargetTypesList();

        List<Entity> validTargets = collectValidTargets();
        if (validTargets.isEmpty()) {
            currentTarget = null;
            return;
        }

        sortTargets(validTargets);
        Entity target = selectTarget(validTargets);

        if (target != null) {
            interact(target);
        }
    }

    private static boolean canOperate() {
        return instance.isActive() && mc.player != null && mc.world != null;
    }

    private static List<Entity> collectValidTargets() {
        List<Entity> validTargets = new ArrayList<>();
        double maxRange = Range.value();

        for (Entity entity : mc.world.getEntities()) {
            if (!isValidTarget(entity)) continue;

            double distance = getDistanceToEntity(entity);
            // Be strict: only include entities that are definitely within range
            if (distance >= maxRange) continue;

            if (!isInFOV(entity)) continue;

            validTargets.add(entity);
        }

        return validTargets;
    }

    private static double getDistanceToEntity(Entity entity) {
        // Calculate distance from player's eye position to entity's center
        // This is more accurate for range checking than bounding box edges
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d entityCenter = entity.getBoundingBox().getCenter();

        return eyePos.distanceTo(entityCenter);
    }

    private static void updateTargetTypesList() {
        targetTypesList.clear();

        String types = TargetTypes.value();
        if (types.isEmpty()) return;

        for (String type : types.split(",")) {
            String trimmed = type.trim();
            if (!trimmed.isEmpty()) {
                targetTypesList.add(trimmed);
            }
        }
    }

    private static boolean isValidTarget(Entity entity) {
        if (entity == null || entity == mc.player) return false;

        if (!isEntityAlive(entity)) return false;

        if (!namesOnly.instance.isActive()) return true;

        // Check based on the target mode
        if (namesOnly.targetMode.value() == namesOnly.TargetMode.Type) {
            return isInTargetTypesList(entity);
        } else {
            return isInTargetNamesList(entity);
        }
    }

    private static boolean isInTargetNamesList(Entity entity) {
        String targetNames = namesOnly.TargetNames.value();
        if (targetNames.isEmpty()) return false;

        String entityName = entity.getName().getString();
        for (String name : targetNames.split(",")) {
            String trimmed = name.trim();
            if (!trimmed.isEmpty() && entityName.equalsIgnoreCase(trimmed)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isEntityAlive(Entity entity) {
        if (entity instanceof LivingEntity living) {
            return living.isAlive() && !living.isDead() && !living.isRemoved();
        }
        return !entity.isRemoved();
    }

    private static boolean isInTargetTypesList(Entity entity) {
        if (targetTypesList.isEmpty()) return false;
        String entityTypeId = entity.getType().toString();
        return targetTypesList.stream().anyMatch(entityTypeId::equalsIgnoreCase);
    }


    private static boolean checkCPSDelay() {
        long currentTime = System.currentTimeMillis();
        if (currentTime < nextInteractTime) return false;

        nextInteractTime = currentTime + calculateDelayFromCPS();
        return true;
    }

    private static long calculateDelayFromCPS() {
        double minCPS = Math.min(MinCPS.value(), MaxCPS.value());
        double maxCPS = Math.max(MinCPS.value(), MaxCPS.value());
        double avgCPS = (minCPS + maxCPS) / 2.0;
        double randomVariation = (Math.random() - 0.5) * (maxCPS - minCPS);
        double actualCPS = avgCPS + randomVariation;

        actualCPS = Math.max(minCPS, Math.min(maxCPS, actualCPS));

        return (long) (1000.0 / actualCPS);
    }

    private static void interact(Entity entity) {
        if (!canInteract()) return;
        if (!checkCPSDelay()) return;
        if (!ThroughWalls.value() && !canSeeEntity(entity)) return;

        if (RightClick.value()) {
            performRightClick(entity);
        } else {
            performLeftClick(entity);
        }
    }

    private static boolean canInteract() {
        if (mc.player == null || mc.interactionManager == null) return false;
        if (!mc.player.isAlive()) return false;
        if (!(mc.currentScreen == null)) return false;
        if (mc.interactionManager.isBreakingBlock() || mc.player.isUsingItem()) return false;
        if (mc.options.attackKey.isPressed() || mc.options.useKey.isPressed()) return false;
        return true;
    }

    private static void performRightClick(Entity entity) {
        mc.interactionManager.interactEntity(mc.player, entity, Hand.MAIN_HAND);
        mc.player.swingHand(Hand.MAIN_HAND);
        logInteraction("Interacted", entity);
    }

    private static void performLeftClick(Entity entity) {
        mc.interactionManager.attackEntity(mc.player, entity);
        mc.player.swingHand(Hand.MAIN_HAND);
        logInteraction("Attacking", entity);
    }

    private static void logInteraction(String action, Entity entity) {
        if (!shouldLogFeature.value()) return;

        String message = String.format("%s entity: %s at distance: %.2f",
                action,
                entity.getName().getString(),
                getDistanceToEntity(entity));
        Utils.info(message);
    }

    public static void addTargetType(String type) {
        String currentTypes = TargetTypes.value();
        if (currentTypes.isEmpty()) {
            TargetTypes.set(type);
            return;
        }
        if (currentTypes.toLowerCase().contains(type.toLowerCase())) return;
        TargetTypes.set(currentTypes + ", " + type);
    }

    public static void removeTargetType(String type) {
        String currentTypes = TargetTypes.value();
        if (currentTypes.isEmpty()) return;

        StringBuilder newTypes = new StringBuilder();
        for (String t : currentTypes.split(",")) {
            String trimmed = t.trim();
            if (trimmed.isEmpty() || trimmed.equalsIgnoreCase(type)) continue;
            if (!newTypes.isEmpty()) newTypes.append(", ");
            newTypes.append(trimmed);
        }

        TargetTypes.set(newTypes.toString());
    }

    public static void clearTargetTypes() {
        TargetTypes.set("");
    }

    private static boolean isInFOV(Entity entity) {
        if (FOV.value() >= 360.0) return true;

        Vec3d playerPos = mc.player.getEyePos();
        Vec3d entityPos = getTargetPoint(entity);
        Vec3d toEntity = entityPos.subtract(playerPos).normalize();
        Vec3d playerLook = mc.player.getRotationVec(1.0f);

        double dot = playerLook.dotProduct(toEntity);
        double angle = Math.toDegrees(Math.acos(dot));

        return angle <= FOV.value() / 2.0;
    }

    private static boolean canSeeEntity(Entity entity) {
        Vec3d playerEye = mc.player.getEyePos();
        Vec3d entityCenter = getTargetPoint(entity);

        RaycastContext context = new RaycastContext(
            playerEye,
            entityCenter,
            RaycastContext.ShapeType.COLLIDER,
            RaycastContext.FluidHandling.NONE,
            mc.player
        );

        HitResult result = mc.world.raycast(context);
        return result.getType() == HitResult.Type.MISS;
    }

    private static Vec3d getTargetPoint(Entity entity) {
        Vec3d entityPos = entity.getEntityPos();
        double eyeHeight = entity.getStandingEyeHeight();

        return switch (TargetPointMode.value()) {
            case Closest -> {
                Vec3d center = entity.getBoundingBox().getCenter();
                double offsetX = (Math.random() - 0.5) * 0.2; // ±0.1 blocks
                double offsetY = (Math.random() - 0.5) * 0.2;
                double offsetZ = (Math.random() - 0.5) * 0.2;
                yield center.add(offsetX, offsetY, offsetZ);
            }
            case Middle -> entityPos.add(0, eyeHeight / 2.0, 0);
        };
    }

    private static void sortTargets(List<Entity> targets) {
        switch (Sorting.value()) {
            case Distance -> targets.sort(Comparator.comparingDouble(KillAura::getDistanceToEntity));
            case Health -> targets.sort((e1, e2) -> {
                float health1 = e1 instanceof LivingEntity l1 ? l1.getHealth() : Float.MAX_VALUE;
                float health2 = e2 instanceof LivingEntity l2 ? l2.getHealth() : Float.MAX_VALUE;
                return Float.compare(health1, health2);
            });
        }
    }

    private static Entity selectTarget(List<Entity> validTargets) {
        switch (Targeting.value()) {
            case Single -> {
                if (currentTarget != null && validTargets.contains(currentTarget)) {
                    return currentTarget;
                }
                currentTarget = validTargets.getFirst();
                return currentTarget;
            }
            case Switch -> {
                long currentTime = System.currentTimeMillis();
                if (currentTarget == null || !validTargets.contains(currentTarget) ||
                    currentTime - lastSwitchTime >= SwitchDelay.value()) {

                    int currentIndex = validTargets.indexOf(currentTarget);
                    if (currentIndex == -1 || currentIndex >= validTargets.size() - 1) {
                        currentTarget = validTargets.getFirst();
                    } else {
                        currentTarget = validTargets.get(currentIndex + 1);
                    }
                    lastSwitchTime = currentTime;
                }
                return currentTarget;
            }
        }
        return null;
    }

    @EventHandler
    private static void onRender(WorldRenderEvent event) {
        if (!instance.isActive() || !DrawRange.value() || mc.player == null) return;

        double range = Range.value();
        Vec3d camPos = event.camera.getPos();

        float tickDelta = mc.getRenderTickCounter().getTickProgress(true);
        Vec3d playerPos = mc.player.getLerpedPos(tickDelta);

        double playerX = playerPos.x;
        double playerY = playerPos.y + 0.8;
        double playerZ = playerPos.z;

        // Very smooth circle with many segments
        int segments = 128;
        double angleStep = 2 * Math.PI / segments;

        event.matrices.push();
        event.matrices.translate(-camPos.getX(), -camPos.getY(), -camPos.getZ());

        VertexConsumer buffer = event.consumer.getBuffer(Rendering.Layers.LineDepthTest);
        MatrixStack.Entry entry = event.matrices.peek();

        // Draw single ring with depth-based shading for 3D effect
        for (int i = 0; i < segments; i++) {
            double angle1 = i * angleStep;
            double angle2 = (i + 1) * angleStep;

            float x1 = (float) (playerX + range * Math.cos(angle1));
            float z1 = (float) (playerZ + range * Math.sin(angle1));
            float x2 = (float) (playerX + range * Math.cos(angle2));
            float z2 = (float) (playerZ + range * Math.sin(angle2));

            buffer.vertex(entry, x1, (float) playerY, z1).color(255, 255, 255, 255).normal(entry, 0, 1, 0);
            buffer.vertex(entry, x2, (float) playerY, z2).color(255, 255, 255, 255).normal(entry, 0, 1, 0);
        }

        event.matrices.pop();
    }

    public enum SortingMode { Distance, Health }
    public enum TargetPoint { Closest, Middle }
    public enum TargetingMode { Single, Switch }
}
