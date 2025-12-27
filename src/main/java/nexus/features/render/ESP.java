package nexus.features.render;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import nexus.config.*;
import nexus.events.WorldRenderEvent;
import nexus.misc.RenderColor;
import nexus.misc.Rendering;

import java.util.ArrayList;
import java.util.List;

import static nexus.Main.mc;

public class ESP {
    public static final Feature instance = new Feature("ESP");

    // Render Modes
    public static final SettingEnum<RenderMode> renderMode = new SettingEnum<>(RenderMode.Both, RenderMode.class, "renderMode", instance.key());
    public static final SettingBool throughWalls = new SettingBool(true, "throughWalls", instance.key());

    // Box Customization
    public static final SettingDouble boxExpandX = new SettingDouble(0.0, "boxExpandX", instance.key());
    public static final SettingDouble boxExpandY = new SettingDouble(0.0, "boxExpandY", instance.key());
    public static final SettingDouble boxExpandZ = new SettingDouble(0.0, "boxExpandZ", instance.key());
    public static final SettingDouble outlineWidth = new SettingDouble(3.0, "outlineWidth", instance.key());
    public static final SettingDouble fillOpacity = new SettingDouble(0.3, "fillOpacity", instance.key());

    // Entity Type Toggles
    public static final SettingBool showPlayers = new SettingBool(true, "showPlayers", instance.key());
    public static final SettingBool showHostile = new SettingBool(true, "showHostile", instance.key());
    public static final SettingBool showPassive = new SettingBool(false, "showPassive", instance.key());
    public static final SettingBool showArmorStands = new SettingBool(false, "showArmorStands", instance.key());
    public static final SettingBool showOther = new SettingBool(false, "showOther", instance.key());

    // Filtering
    public static final SettingString entityTypeFilter = new SettingString("", "entityTypeFilter", instance.key());
    public static final SettingString entityNameFilter = new SettingString("", "entityNameFilter", instance.key());
    public static final SettingBool filterMode = new SettingBool(false, "filterMode", instance.key()); // false = whitelist, true = blacklist
    public static final SettingDouble maxRange = new SettingDouble(64.0, "maxRange", instance.key());
    public static final SettingBool excludePests = new SettingBool(true, "excludePests", instance.key()); // Don't render pests if PestESP is handling them

    // Colors
    public static final SettingEnum<ColorMode> colorMode = new SettingEnum<>(ColorMode.EntityType, ColorMode.class, "colorMode", instance.key());
    public static final SettingColor playerColor = new SettingColor(RenderColor.fromHex(0x00FF00), "playerColor", instance.key());
    public static final SettingColor hostileColor = new SettingColor(RenderColor.fromHex(0xFF0000), "hostileColor", instance.key());
    public static final SettingColor passiveColor = new SettingColor(RenderColor.fromHex(0x00FFFF), "passiveColor", instance.key());
    public static final SettingColor armorStandColor = new SettingColor(RenderColor.fromHex(0xFFFF00), "armorStandColor", instance.key());
    public static final SettingColor otherColor = new SettingColor(RenderColor.fromHex(0xFFFFFF), "otherColor", instance.key());
    public static final SettingColor healthBasedLowColor = new SettingColor(RenderColor.fromHex(0xFF0000), "healthBasedLowColor", instance.key());
    public static final SettingColor healthBasedHighColor = new SettingColor(RenderColor.fromHex(0x00FF00), "healthBasedHighColor", instance.key());

    // Extras
    public static final SettingBool showTracers = new SettingBool(false, "showTracers", instance.key());
    public static final SettingColor tracerColor = new SettingColor(RenderColor.fromHex(0xFFFFFF), "tracerColor", instance.key());
    public static final SettingBool showLabels = new SettingBool(false, "showLabels", instance.key());
    public static final SettingDouble labelScale = new SettingDouble(0.025, "labelScale", instance.key());
    public static final SettingBool showDistance = new SettingBool(true, "showDistance", instance.key());
    public static final SettingBool showHealth = new SettingBool(true, "showHealth", instance.key());
    public static final SettingBool labelBackground = new SettingBool(true, "labelBackground", instance.key());
    public static final SettingDouble labelBackgroundOpacity = new SettingDouble(0.75, "labelBackgroundOpacity", instance.key());
    public static final SettingBool coloredLabels = new SettingBool(true, "coloredLabels", instance.key());
    public static final SettingBool showBeam = new SettingBool(false, "showBeam", instance.key());
    public static final SettingInt beamHeight = new SettingInt(256, "beamHeight", instance.key());

    private static final List<String> entityTypeFilterList = new ArrayList<>();
    private static final List<String> entityNameFilterList = new ArrayList<>();

    @EventHandler
    private static void onRender(WorldRenderEvent event) {
        if (!instance.isActive() || mc.player == null || mc.world == null) return;

        updateFilterLists();
        float tickDelta = event.tickCounter.getTickProgress(true);

        for (Entity entity : mc.world.getEntities()) {
            if (!shouldRenderEntity(entity)) continue;

            double distance = getDistanceToEntity(entity);
            if (distance > maxRange.value()) continue;

            Box box = getEntityBox(entity, tickDelta);
            RenderColor color = getColorForEntity(entity);

            // Apply fill opacity
            RenderColor fillColor = RenderColor.fromFloat(
                color.r, color.g, color.b, (float) fillOpacity.value()
            );

            // Render based on mode
            switch (renderMode.value()) {
                case Outline -> Rendering.drawOutline(event.matrices, event.consumer, event.camera, box, throughWalls.value(), color);
                case Fill -> Rendering.drawFilled(event.matrices, event.consumer, event.camera, box, throughWalls.value(), fillColor);
                case Both -> {
                    Rendering.drawFilled(event.matrices, event.consumer, event.camera, box, throughWalls.value(), fillColor);
                    Rendering.drawOutline(event.matrices, event.consumer, event.camera, box, throughWalls.value(), color);
                }
            }

            // Tracers
            if (showTracers.value()) {
                Vec3d entityCenter = box.getCenter();
                Rendering.drawTracer(event.matrices, event.consumer, event.camera, entityCenter, tracerColor.value());
            }

            // Beams
            if (showBeam.value()) {
                Vec3d lerpedPos = entity.getLerpedPos(tickDelta);
                Rendering.drawBeam(event.matrices, event.consumer, event.camera, lerpedPos, beamHeight.value(), throughWalls.value(), color);
            }

            // Labels
            if (showLabels.value()) {
                Vec3d lerpedPos = entity.getLerpedPos(tickDelta);
                Vec3d labelPos = new Vec3d(
                    lerpedPos.x,
                    lerpedPos.y + entity.getHeight() + 0.3,
                    lerpedPos.z
                );

                renderLabel(event, entity, labelPos, distance, color);
            }
        }
    }

    private static void updateFilterLists() {
        entityTypeFilterList.clear();
        String types = entityTypeFilter.value();
        if (!types.isEmpty()) {
            for (String type : types.split(",")) {
                String trimmed = type.trim();
                if (!trimmed.isEmpty()) {
                    entityTypeFilterList.add(trimmed.toLowerCase());
                }
            }
        }

        entityNameFilterList.clear();
        String names = entityNameFilter.value();
        if (!names.isEmpty()) {
            for (String name : names.split(",")) {
                String trimmed = name.trim();
                if (!trimmed.isEmpty()) {
                    entityNameFilterList.add(trimmed.toLowerCase());
                }
            }
        }
    }

    private static boolean shouldRenderEntity(Entity entity) {
        if (entity == null || entity == mc.player) return false;
        if (entity.isRemoved()) return false;

        // Exclude pests if PestESP is active and exclude option is enabled
        if (excludePests.value() && PestESP.instance.isActive() && PestESP.isPestEntity(entity)) {
            return false;
        }

        // Check entity type toggles
        if (entity instanceof PlayerEntity && !showPlayers.value()) return false;
        if (entity instanceof ArmorStandEntity && !showArmorStands.value()) return false;
        if (entity instanceof HostileEntity && !showHostile.value()) return false;
        if (entity instanceof PassiveEntity && !showPassive.value()) return false;

        // Check for other living entities
        if (entity instanceof LivingEntity) {
            if (!(entity instanceof PlayerEntity) &&
                !(entity instanceof ArmorStandEntity) &&
                !(entity instanceof HostileEntity) &&
                !(entity instanceof PassiveEntity)) {
                if (!showOther.value()) return false;
            }
        } else {
            // Non-living entities
            if (!showOther.value()) return false;
        }

        // Apply filters
        if (!entityTypeFilterList.isEmpty() || !entityNameFilterList.isEmpty()) {
            boolean typeMatch = checkTypeFilter(entity);
            boolean nameMatch = checkNameFilter(entity);

            if (filterMode.value()) {
                // Blacklist mode: hide if matches
                if (typeMatch || nameMatch) return false;
            } else {
                // Whitelist mode: show only if matches
                if (!typeMatch && !nameMatch) return false;
            }
        }

        return true;
    }

    private static boolean checkTypeFilter(Entity entity) {
        if (entityTypeFilterList.isEmpty()) return false;
        String entityTypeId = entity.getType().toString().toLowerCase();
        return entityTypeFilterList.stream().anyMatch(entityTypeId::contains);
    }

    private static boolean checkNameFilter(Entity entity) {
        if (entityNameFilterList.isEmpty()) return false;
        String entityName = entity.getName().getString().toLowerCase();
        return entityNameFilterList.stream().anyMatch(entityName::contains);
    }

    private static Box getEntityBox(Entity entity, float tickDelta) {
        // Use lerped position for smooth rendering
        Vec3d lerpedPos = entity.getLerpedPos(tickDelta);
        Box box = entity.getDimensions(entity.getPose()).getBoxAt(lerpedPos);
        return box.expand(
            boxExpandX.value(),
            boxExpandY.value(),
            boxExpandZ.value()
        );
    }

    private static RenderColor getColorForEntity(Entity entity) {
        switch (colorMode.value()) {
            case EntityType -> {
                if (entity instanceof PlayerEntity) return playerColor.value();
                if (entity instanceof ArmorStandEntity) return armorStandColor.value();
                if (entity instanceof HostileEntity) return hostileColor.value();
                if (entity instanceof PassiveEntity) return passiveColor.value();
                return otherColor.value();
            }
            case HealthBased -> {
                if (entity instanceof LivingEntity living) {
                    float health = living.getHealth();
                    float maxHealth = living.getMaxHealth();
                    float ratio = Math.clamp(health / maxHealth, 0.0f, 1.0f);

                    // Interpolate between low and high color
                    RenderColor low = healthBasedLowColor.value();
                    RenderColor high = healthBasedHighColor.value();

                    return RenderColor.fromFloat(
                        low.r + (high.r - low.r) * ratio,
                        low.g + (high.g - low.g) * ratio,
                        low.b + (high.b - low.b) * ratio,
                        1.0f
                    );
                }
                return otherColor.value();
            }
            case Distance -> {
                double distance = getDistanceToEntity(entity);
                double maxDist = maxRange.value();
                float ratio = (float) Math.clamp(1.0 - (distance / maxDist), 0.0, 1.0);

                // Close = green, far = red
                return RenderColor.fromFloat(1.0f - ratio, ratio, 0.0f, 1.0f);
            }
            case Single -> {
                return playerColor.value(); // Use player color as the single color
            }
        }
        return RenderColor.white;
    }

    private static double getDistanceToEntity(Entity entity) {
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d entityCenter = entity.getBoundingBox().getCenter();
        return eyePos.distanceTo(entityCenter);
    }

    private static void renderLabel(WorldRenderEvent event, Entity entity, Vec3d pos, double distance, RenderColor entityColor) {
        Matrix4f matrix = new Matrix4f();
        Vec3d camPos = event.camera.getPos();

        float x = (float) (pos.getX() - camPos.getX());
        float y = (float) (pos.getY() - camPos.getY());
        float z = (float) (pos.getZ() - camPos.getZ());

        matrix.translate(x, y, z);
        matrix.rotate(event.camera.getRotation());

        float scale = (float) labelScale.value();
        matrix.scale(scale, -scale, scale);

        // Build label lines
        List<String> lines = buildLabelLines(entity, distance);

        // Calculate dimensions
        int maxWidth = 0;
        for (String line : lines) {
            int width = mc.textRenderer.getWidth(line);
            if (width > maxWidth) maxWidth = width;
        }

        int lineHeight = 11;
        int totalHeight = lines.size() * lineHeight;
        int padding = 4;

        // Draw background if enabled
        if (labelBackground.value()) {
            int bgWidth = maxWidth + padding * 2;
            int bgHeight = totalHeight + padding * 2;
            int bgX = -bgWidth / 2;
            int bgY = -padding;

            net.minecraft.client.render.VertexConsumer buffer = throughWalls.value()
                ? event.consumer.getBuffer(Rendering.Layers.BoxFilledNoCull)
                : event.consumer.getBuffer(Rendering.Layers.BoxFilled);

            // Modern dark background with slight transparency
            int bgAlpha = (int) (labelBackgroundOpacity.value() * 255);
            int backgroundColor = (bgAlpha << 24) | 0x000000;

            // Main background
            Rendering.drawRect(
                matrix,
                buffer,
                bgX, bgY, bgX + bgWidth, bgY + bgHeight,
                backgroundColor
            );

            // Modern accent bar on left side (thin colored line)
            int accentColor = entityColor.argb | 0xFF000000; // Full alpha
            Rendering.drawRect(
                matrix,
                buffer,
                bgX, bgY, bgX + 2, bgY + bgHeight,
                accentColor
            );

            // Subtle outline for depth (semi-transparent white)
            int outlineColor = 0x30FFFFFF; // 19% white

            // Top
            Rendering.drawRect(
                matrix,
                buffer,
                bgX, bgY, bgX + bgWidth, bgY + 1,
                outlineColor
            );
            // Bottom
            Rendering.drawRect(
                matrix,
                buffer,
                bgX, bgY + bgHeight - 1, bgX + bgWidth, bgY + bgHeight,
                outlineColor
            );
            // Right
            Rendering.drawRect(
                matrix,
                buffer,
                bgX + bgWidth - 1, bgY, bgX + bgWidth, bgY + bgHeight,
                outlineColor
            );
        }

        // Draw text lines with modern styling
        int yOffset = 1; // Small offset from top
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int textWidth = mc.textRenderer.getWidth(line);
            float textX = labelBackground.value() ? -textWidth / 2f + 3 : -textWidth / 2f; // Offset if background (for accent bar)

            // Determine text color with modern palette
            int textColor = 0xFFFFFFFF; // Default bright white
            if (coloredLabels.value()) {
                if (i == 0) {
                    // First line (name) - bright and prominent
                    textColor = 0xFFFFFFFF;
                } else if (i == 1 && showDistance.value()) {
                    // Distance in subtle gray
                    textColor = 0xFFB0B0B0;
                } else if (showHealth.value() && entity instanceof LivingEntity) {
                    // Health color based on percentage - vibrant colors
                    LivingEntity living = (LivingEntity) entity;
                    float healthPercent = living.getHealth() / living.getMaxHealth();
                    if (healthPercent > 0.75f) {
                        textColor = 0xFF4ADE80; // Modern green
                    } else if (healthPercent > 0.5f) {
                        textColor = 0xFFFACC15; // Modern yellow
                    } else if (healthPercent > 0.25f) {
                        textColor = 0xFFFB923C; // Modern orange
                    } else {
                        textColor = 0xFFF87171; // Modern red
                    }
                }
            }

            // Draw text with shadow
            mc.textRenderer.draw(
                line,
                textX,
                yOffset,
                textColor,
                true,
                matrix,
                event.consumer,
                throughWalls.value() ? net.minecraft.client.font.TextRenderer.TextLayerType.SEE_THROUGH : net.minecraft.client.font.TextRenderer.TextLayerType.NORMAL,
                0,
                net.minecraft.client.render.LightmapTextureManager.MAX_LIGHT_COORDINATE
            );

            yOffset += lineHeight;
        }
    }

    private static List<String> buildLabelLines(Entity entity, double distance) {
        List<String> lines = new ArrayList<>();

        // Line 1: Entity name
        lines.add(entity.getName().getString());

        // Line 2: Distance
        if (showDistance.value()) {
            lines.add(String.format("%.1fm", distance));
        }

        // Line 3: Health
        if (showHealth.value() && entity instanceof LivingEntity living) {
            float health = living.getHealth();
            float maxHealth = living.getMaxHealth();
            lines.add(String.format("%.1f / %.1f HP", health, maxHealth));
        }

        return lines;
    }

    public enum RenderMode {
        Outline,
        Fill,
        Both
    }

    public enum ColorMode {
        EntityType,
        HealthBased,
        Distance,
        Single
    }
}
