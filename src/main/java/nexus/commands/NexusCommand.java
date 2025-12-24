package nexus.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PlayerHeadItem;
import net.minecraft.util.math.Vec3d;
import nexus.config.Config;
import nexus.hud.HudEditorScreen;
import nexus.hud.clickgui.ClickGui;
import nexus.misc.SkyblockData;
import nexus.misc.Utils;

import java.util.List;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static nexus.Main.LOGGER;
import static nexus.Main.mc;

public class NexusCommand {
    private static final LiteralArgumentBuilder<FabricClientCommandSource> queueCommandBuilder = literal("queue").executes(context -> SINGLE_SUCCESS);

    public static final ModCommand[] commands = {
            new ModCommand("settings", "Opens the settings GUI.", literal("settings").executes(context -> {
                Utils.setScreen(new ClickGui());
                return SINGLE_SUCCESS;
            }).then(literal("load").executes(context -> {
                Config.load();
                Utils.info("§aLoaded latest settings from the configuration file.");
                return SINGLE_SUCCESS;
            })).then(literal("save").executes(context -> {
                Config.save();
                Utils.info("§aSaved your current settings to the configuration file.");
                return SINGLE_SUCCESS;
            }))),
            new ModCommand("checkUpdate", "Manually checks if a new release version of the mod is available.", literal("checkUpdate").executes(context -> {
                Utils.info("§7Checking for updates...");
                Utils.checkUpdate(true);
                return SINGLE_SUCCESS;
            })),
            new ModCommand("sendCoords", "Easily send your coordinates in the chat, with the option to choose the format. Uses Patcher format by default.", literal("sendCoords").executes(context -> {
                Utils.sendMessage(Utils.getCoordsFormatted("x: {}, y: {}, z: {}"));
                return SINGLE_SUCCESS;
            }).then(literal("patcher").executes(context -> {
                Utils.sendMessage(Utils.getCoordsFormatted("x: {}, y: {}, z: {}"));
                return SINGLE_SUCCESS;
            })).then(literal("simple").executes(context -> {
                Utils.sendMessage(Utils.getCoordsFormatted("{} {} {}"));
                return SINGLE_SUCCESS;
            })).then(literal("location").executes(context -> {
                Utils.sendMessage(Utils.format("{} [ {} ]", Utils.getCoordsFormatted("x: {}, y: {}, z: {}"), SkyblockData.getLocation()));
                return SINGLE_SUCCESS;
            }))),
            new ModCommand("copyCoords", "Alternative to the sendCoords command, which copies your coordinates to your clipboard instead of sending them in the chat.", literal("copyCoords").executes(context -> {
                mc.keyboard.setClipboard(Utils.getCoordsFormatted("x: {}, y: {}, z: {}"));
                return SINGLE_SUCCESS;
            }).then(literal("patcher").executes(context -> {
                mc.keyboard.setClipboard(Utils.getCoordsFormatted("x: {}, y: {}, z: {}"));
                return SINGLE_SUCCESS;
            })).then(literal("simple").executes(context -> {
                mc.keyboard.setClipboard(Utils.getCoordsFormatted("{} {} {}"));
                return SINGLE_SUCCESS;
            })).then(literal("location").executes(context -> {
                mc.keyboard.setClipboard(Utils.format("{} [ {} ]", Utils.getCoordsFormatted("x: {}, y: {}, z: {}"), SkyblockData.getLocation()));
                return SINGLE_SUCCESS;
            }))),
            new ModCommand("ping", "Checks your current ping.", literal("ping").executes(context -> {
                Utils.info("§7Pinging...");
                SkyblockData.showPing();
                return SINGLE_SUCCESS;
            })),
            new ModCommand("hudEditor", "Opens the NoFrills hud editor.", literal("hudEditor").executes(context -> {
                Utils.setScreen(new HudEditorScreen());
                return SINGLE_SUCCESS;
            })),
            new ModCommand("debug", "Random commands for logging, debugging, or testing.", literal("debug").executes(context -> {
                return SINGLE_SUCCESS;
            }).then(literal("dumpHeadTextures").executes(context -> {
                List<EquipmentSlot> searchedSlots = List.of(
                        EquipmentSlot.HEAD,
                        EquipmentSlot.MAINHAND,
                        EquipmentSlot.OFFHAND
                );
                for (Entity ent : Utils.getEntities()) {
                    if (ent instanceof LivingEntity living) {
                        for (EquipmentSlot slot : searchedSlots) {
                            ItemStack stack = living.getEquippedStack(slot);
                            GameProfile textures = Utils.getTextures(stack);
                            if (textures != null && stack.getItem() instanceof PlayerHeadItem) {
                                Vec3d pos = living.getEntityPos();
                                LOGGER.info(Utils.format("\n\tURL - {}\n\tSlot - {}\n\tEntity Name - {}\n\tHead Name - {}\n\tPosition - {} {} {}",
                                        Utils.getTextureUrl(textures),
                                        Utils.toUpper(slot.name()),
                                        living.getName().getString(),
                                        stack.getName().getString(),
                                        pos.getX(),
                                        pos.getY(),
                                        pos.getZ()
                                ));
                            }
                        }
                    }
                }
                Utils.info("Dumped head texture URL's to latest.log.");
                return SINGLE_SUCCESS;
            })).then(literal("dumpPlayerTextures").executes(context -> {
                MinecraftSessionService service = mc.getApiServices().sessionService();
                for (Entity ent : Utils.getEntities()) {
                    if (ent instanceof PlayerEntity player) {
                        if (player.getGameProfile() != null) {
                            MinecraftProfileTextures textures = service.getTextures(player.getGameProfile());
                            Vec3d pos = player.getEntityPos();
                            if (textures.skin() == null) {
                                continue;
                            }
                            LOGGER.info(Utils.format("\n\tURL - {}\n\tEntity Name - {}\n\tPosition - {} {} {}",
                                    textures.skin().getUrl(),
                                    player.getName().getString(),
                                    pos.getX(),
                                    pos.getY(),
                                    pos.getZ()
                            ));
                        }
                    }
                }
                Utils.info("Dumped player texture URL's to latest.log.");
                return SINGLE_SUCCESS;
            })))
    };

    public static void init(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        LiteralArgumentBuilder<FabricClientCommandSource> helpArg = literal("help").executes(context -> {
            Utils.info("§7Printing command list...");
            for (ModCommand command : commands) {
                Utils.info("§l" + command.command + "§r§7: " + command.description);
            }
            return SINGLE_SUCCESS;
        });
        LiteralArgumentBuilder<FabricClientCommandSource> commandMain = literal("nexus").executes(context -> {
            Utils.setScreen(new ClickGui());
            return SINGLE_SUCCESS;
        });
        LiteralArgumentBuilder<FabricClientCommandSource> commandShort = literal("n").executes(context -> {
            Utils.setScreen(new ClickGui());
            return SINGLE_SUCCESS;
        });
        commandMain.then(helpArg);
        commandShort.then(helpArg);
        for (ModCommand command : commands) {
            commandMain.then(command.builder);
            commandShort.then(command.builder);
        }
        dispatcher.register(commandMain);
        dispatcher.register(commandShort);
    }

    public static class ModCommand {
        public String command;
        public String description;
        public LiteralArgumentBuilder<FabricClientCommandSource> builder;

        public ModCommand(String command, String description, LiteralArgumentBuilder<FabricClientCommandSource> builder) {
            this.command = command;
            this.description = description;
            this.builder = builder;
        }
    }
}
