package nexus.features.render;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import nexus.config.*;
import nexus.events.WorldRenderEvent;
import nexus.misc.RenderColor;
import nexus.misc.Rendering;

import java.util.Set;

import static nexus.Main.mc;

public class PestESP {
    public static final Feature instance = new Feature("PestESP");
    
    // All pest names
    private static final Set<String> PEST_NAMES = Set.of(
        "Fly", "Cricket", "Locust", "Rat", "Mosquito", 
        "Earthworm", "Mite", "Moth", "Slug", "Beetle",
        "Praying Mantis", "Firefly", "Field Mouse",
        "Dragonfly"
    );
    
    // Render Settings
    public static final SettingEnum<RenderMode> renderMode = new SettingEnum<>(RenderMode.Both, RenderMode.class, "renderMode", instance.key());
    public static final SettingBool throughWalls = new SettingBool(true, "throughWalls", instance.key());
    
    // Box Customization
    public static final SettingDouble boxExpandX = new SettingDouble(0.0, "boxExpandX", instance.key());
    public static final SettingDouble boxExpandY = new SettingDouble(0.0, "boxExpandY", instance.key());
    public static final SettingDouble boxExpandZ = new SettingDouble(0.0, "boxExpandZ", instance.key());
    public static final SettingDouble outlineWidth = new SettingDouble(3.0, "outlineWidth", instance.key());
    public static final SettingDouble fillOpacity = new SettingDouble(0.3, "fillOpacity", instance.key());
    
    // Color Settings
    public static final SettingColor pestColor = new SettingColor(RenderColor.fromHex(0xFF6B00), "pestColor", instance.key());
    
    // Range
    public static final SettingDouble maxRange = new SettingDouble(64.0, "maxRange", instance.key());
    
    // Extras
    public static final SettingBool showBeam = new SettingBool(true, "showBeam", instance.key());
    public static final SettingInt beamHeight = new SettingInt(100, "beamHeight", instance.key());
    public static final SettingBool showLabel = new SettingBool(true, "showLabel", instance.key());
    public static final SettingDouble labelScale = new SettingDouble(0.025, "labelScale", instance.key());
    public static final SettingBool showDistance = new SettingBool(true, "showDistance", instance.key());
    public static final SettingBool showTracers = new SettingBool(false, "showTracers", instance.key());
    
    @EventHandler
    private static void onRender(WorldRenderEvent event) {
        if (!instance.isActive() || mc.player == null || mc.world == null) return;
        
        float tickDelta = event.tickCounter.getTickProgress(true);
        
        for (Entity entity : mc.world.getEntities()) {
            // Only check armor stands
            if (!(entity instanceof ArmorStandEntity)) continue;
            if (entity.isRemoved()) continue;
            
            // Check if the armor stand's name matches any pest name
            String entityName = entity.getName().getString();
            if (!isPest(entityName)) continue;
            
            // Get entity position and distance
            double distance = getDistanceToEntity(entity);
            if (distance > maxRange.value()) continue;
            
            Box box = getEntityBox(entity, tickDelta);
            RenderColor color = pestColor.value();
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
            
            // Render beacon beam
            if (showBeam.value()) {
                Vec3d lerpedPos = entity.getLerpedPos(tickDelta);
                Rendering.drawBeam(event.matrices, event.consumer, event.camera, lerpedPos, beamHeight.value(), throughWalls.value(), color);
            }
            
            // Render tracers
            if (showTracers.value()) {
                Vec3d entityCenter = box.getCenter();
                Rendering.drawTracer(event.matrices, event.consumer, event.camera, entityCenter, color);
            }
            
            // Render label with distance
            if (showLabel.value()) {
                Vec3d lerpedPos = entity.getLerpedPos(tickDelta);
                Vec3d labelPos = new Vec3d(
                    lerpedPos.x,
                    lerpedPos.y + entity.getHeight() + 0.3,
                    lerpedPos.z
                );
                
                String label = entityName;
                if (showDistance.value()) {
                    label = String.format("%s (%.1fm)", entityName, distance);
                }
                
                Rendering.drawTextWithBackground(event.consumer, event.camera, labelPos, 
                    net.minecraft.text.Text.literal(label), (float) labelScale.value(), throughWalls.value(), color);
            }
        }
    }
    
    private static boolean isPest(String entityName) {
        // Check if any pest name is contained in the entity name
        // This handles cases where the name might have extra formatting or prefixes
        for (String pestName : PEST_NAMES) {
            if (entityName.contains(pestName)) {
                return true;
            }
        }
        return false;
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
    
    private static double getDistanceToEntity(Entity entity) {
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d entityCenter = entity.getBoundingBox().getCenter();
        return eyePos.distanceTo(entityCenter);
    }
    
    /**
     * Helper method to check if an entity is a pest (can be used by ESP to filter out pests)
     */
    public static boolean isPestEntity(Entity entity) {
        if (!(entity instanceof ArmorStandEntity)) return false;
        return isPest(entity.getName().getString());
    }
    
    public enum RenderMode {
        Outline,
        Fill,
        Both
    }
}
