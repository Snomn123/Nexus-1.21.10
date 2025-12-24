package nexus;

import com.mojang.brigadier.CommandDispatcher;
import io.wispforest.owo.config.ui.ConfigScreenProviders;
import meteordevelopment.orbit.EventBus;
import meteordevelopment.orbit.IEventBus;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.util.Util;
import nexus.commands.NexusCommand;
import nexus.config.Config;
import nexus.features.general.*;
import nexus.features.misc.*;
import nexus.features.render.*;
import nexus.hud.HudManager;
import nexus.hud.clickgui.ClickGui;
import nexus.misc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

public class Main implements ModInitializer {
    public static final String MOD_ID = "nexus";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final IEventBus eventBus = new EventBus();

    public static MinecraftClient mc;

    public static void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess access) {
        NexusCommand.init(dispatcher);
    }

    public static void injectRenderDoc() {
        try {
            String path = System.getProperty("nexus.renderdoc.library_path");
            if (path != null) {
                System.load(path);
                LOGGER.info("Loaded RenderDoc lib: {}", path);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onInitialize() {
        long start = Util.getMeasuringTimeMs();

        mc = MinecraftClient.getInstance();

        injectRenderDoc();

        Config.load();

        ConfigScreenProviders.register(MOD_ID, screen -> new ClickGui());

        ClientCommandRegistrationCallback.EVENT.register(Main::registerCommands);

        eventBus.registerLambdaFactory(MOD_ID, (lookupInMethod, glass) -> (MethodHandles.Lookup) lookupInMethod.invoke(null, glass, MethodHandles.lookup()));

        eventBus.subscribe(SkyblockData.class);
        eventBus.subscribe(SlotOptions.class);
        eventBus.subscribe(Utils.class);
        eventBus.subscribe(HudManager.class);
        eventBus.subscribe(UpdateChecker.class);
        eventBus.subscribe(AutoSprint.class);
        eventBus.subscribe(Fullbright.class);
        eventBus.subscribe(ClickGUI.class);
        eventBus.subscribe(CustomKeybinds.class);
        LOGGER.info("Nexus mod initialized in {}ms.", Util.getMeasuringTimeMs() - start);
    }
}