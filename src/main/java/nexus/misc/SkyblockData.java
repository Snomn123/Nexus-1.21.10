package nexus.misc;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.ScoreboardObjectiveUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.TeamS2CPacket;
import net.minecraft.network.packet.s2c.query.PingResultS2CPacket;
import net.minecraft.scoreboard.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import nexus.events.ReceivePacketEvent;
import nexus.events.ServerJoinEvent;

import java.util.ArrayList;
import java.util.List;

import static nexus.Main.mc;

public class SkyblockData {
    private static String location = "";
    private static String area = "";
    private static boolean inSkyblock = false;
    private static boolean instanceOver = false;
    private static List<String> lines = new ArrayList<>();
    private static boolean showPing = false;

    /**
     * Returns the current location from the scoreboard, such as "⏣ Your Island". The location prefix is not omitted.
     */
    public static String getLocation() {
        return location;
    }

    /**
     * Returns the current area from the tab list, such as "Area: Private Island". The area/dungeon prefix is omitted.
     */
    public static String getArea() {
        return area;
    }

    public static boolean isInSkyblock() {
        return inSkyblock;
    }

    public static boolean isInstanceOver() {
        return instanceOver;
    }

    /**
     * Returns a list with every line that is currently displayed on the scoreboard.
     */
    public static List<String> getLines() {
        return new ArrayList<>(lines); // return a copy to avoid a potential concurrent modification exception
    }

    public static void showPing() {
        showPing = true;
        Utils.sendPingPacket();
    }

    public static void updateTabList(PlayerListS2CPacket packet, List<PlayerListS2CPacket.Entry> entries) {
        for (PlayerListS2CPacket.Entry entry : entries) {
            if (entry.displayName() == null) continue;
            String name = Utils.toPlain(entry.displayName()).trim();
            if (name.startsWith("Area:") || name.startsWith("Dungeon:")) {
                area = name.split(":", 2)[1].trim();
                break;
            }
        }
    }

    public static void updateObjective(ScoreboardObjectiveUpdateS2CPacket packet) {
        if (mc.player != null) {
            Scoreboard scoreboard = mc.player.networkHandler.getScoreboard();
            ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.FROM_ID.apply(1));
            if (objective != null) {
                inSkyblock = Utils.toPlain(objective.getDisplayName()).contains("SKYBLOCK");
            }
        }
    }

    public static void updateScoreboard(TeamS2CPacket packet) {
        if (mc.player != null) {
            List<String> currentLines = new ArrayList<>();
            Scoreboard scoreboard = mc.player.networkHandler.getScoreboard();
            ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.FROM_ID.apply(1));
            for (ScoreHolder scoreHolder : scoreboard.getKnownScoreHolders()) {
                if (scoreboard.getScoreHolderObjectives(scoreHolder).containsKey(objective)) {
                    Team team = scoreboard.getScoreHolderTeam(scoreHolder.getNameForScoreboard());
                    if (team != null) {
                        String line = Formatting.strip(team.getPrefix().getString() + team.getSuffix().getString()).trim();
                        if (!line.isEmpty()) {
                            if (line.startsWith(Utils.Symbols.zone) || line.startsWith(Utils.Symbols.zoneRift)) {
                                location = line;
                            }
                            if (Utils.isInKuudra() && !instanceOver) {
                                instanceOver = line.startsWith("Instance Shutdown");
                            }
                            currentLines.add(line);
                        }
                    }
                }
            }
            lines = currentLines;
        }
    }

    @EventHandler
    private static void onJoinServer(ServerJoinEvent event) {
        instanceOver = false;
        inSkyblock = false;
        location = "";
        area = "";
        lines.clear();
    }

    @EventHandler
    private static void onPing(ReceivePacketEvent event) {
        if (showPing && event.packet instanceof PingResultS2CPacket pingPacket) {
            Utils.infoFormat("§aPing: §f{}ms", Util.getMeasuringTimeMs() - pingPacket.startTime());
            showPing = false;
        }
    }
}
