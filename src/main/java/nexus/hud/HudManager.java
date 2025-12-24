package nexus.hud;

import io.wispforest.owo.ui.hud.Hud;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.query.PingResultS2CPacket;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import nexus.events.*;
import nexus.hud.elements.*;
import nexus.misc.Utils;

import java.util.ArrayList;
import java.util.List;

import static nexus.Main.mc;

public class HudManager {
    public static final List<HudElement> elements = new ArrayList<>();
    public static final FPS fpsElement = new FPS("FPS: §f0");
    public static final TPS tpsElement = new TPS("TPS: §f20.00");
    public static final Ping pingElement = new Ping("Ping: §f0ms");
    public static final Armor armorElement = new Armor();
    public static final Inventory inventoryElement = new Inventory();
    public static final ModuleList moduleListElement = new ModuleList();

    public static boolean isEditingHud() {
        return mc.currentScreen instanceof HudEditorScreen;
    }

    public static List<HudElement> getElements() {
        return elements;
    }

    public static void addNew(HudElement element) {
        elements.add(element);
    }

    public static void registerElements() {
        for (HudElement element : elements) {
            Identifier identifier = element.getIdentifier();
            if (!Hud.hasComponent(identifier)) {
                Hud.add(identifier, () -> element);
            }
        }
    }

    @EventHandler
    private static void onRenderHud(HudRenderEvent event) {
        if (!isEditingHud()) {
            for (HudElement element : HudManager.elements) {
                if (element.isActive()) element.updatePosition();
            }
        }
    }

    @EventHandler
    private static void onJoinServer(ServerJoinEvent event) {
        pingElement.reset();
        tpsElement.reset();
        fpsElement.reset();
    }

    @EventHandler
    private static void onPing(ReceivePacketEvent event) {
        if (event.packet instanceof PingResultS2CPacket pingPacket) {
            if (pingElement.isActive()) {
                pingElement.setPing(Util.getMeasuringTimeMs() - pingPacket.startTime());
                pingElement.ticks = 20;
            }
        }
    }

    @EventHandler
    private static void onWorldTick(WorldTickEvent event) {
        if (pingElement.isActive()) { // pings every second when element is enabled, waits until ping result is received
            if (pingElement.ticks > 0) {
                pingElement.ticks -= 1;
                if (pingElement.ticks == 0) {
                    Utils.sendPingPacket();
                }
            }
        }
        if (tpsElement.isActive()) {
            if (tpsElement.clientTicks > 0) {
                tpsElement.clientTicks -= 1;
                if (tpsElement.clientTicks == 0) {
                    tpsElement.setTps(tpsElement.serverTicks);
                    tpsElement.clientTicks = 20;
                    tpsElement.serverTicks = 0;
                }
            }
        }
        if (fpsElement.isActive()) {
            if (fpsElement.ticks > 0) {
                fpsElement.ticks -= 1;
                if (fpsElement.ticks == 0) {
                    fpsElement.setFps(mc.getCurrentFps());
                    fpsElement.ticks = 20;
                }
            }
        }
        if (armorElement.isActive()) {
            armorElement.updateArmor();
        }
        if (inventoryElement.isActive()) {
            inventoryElement.updateInventory();
        }
        if (moduleListElement.isActive()) {
            moduleListElement.updateModuleList();
        }
    }

    @EventHandler
    private static void onServerTick(ServerTickEvent event) {
        if (tpsElement.isActive()) {
            tpsElement.serverTicks += 1;
        }
    }
}