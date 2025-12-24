package nexus.features.render;

import meteordevelopment.orbit.EventHandler;
import nexus.config.Feature;
import nexus.config.SettingBool;
import nexus.config.SettingColor;
import nexus.config.SettingInt;
import nexus.config.SettingKeybind;
import nexus.events.InputEvent;
import nexus.hud.clickgui.ClickGui;
import nexus.misc.RenderColor;
import nexus.misc.Utils;
import org.lwjgl.glfw.GLFW;

import static nexus.Main.mc;

public class ClickGUI {
    public static final Feature instance = new Feature("clickGUI");
    public static final SettingKeybind openKey = new SettingKeybind(GLFW.GLFW_KEY_UNKNOWN, "openKey", instance.key());

    // Background Effects
    public static final SettingBool backgroundBlur = new SettingBool(true, "backgroundBlur", instance.key());
    public static final SettingInt darkeningOpacity = new SettingInt(130, "darkeningOpacity", instance.key());

    // Module Colors
    public static final SettingColor activeModuleTextColor = new SettingColor(RenderColor.fromHex(0x88aaff), "activeModuleTextColor", instance.key());
    public static final SettingColor inactiveModuleTextColor = new SettingColor(RenderColor.fromHex(0xaaaacc), "inactiveModuleTextColor", instance.key());
    public static final SettingColor activeModuleBackground = new SettingColor(RenderColor.fromArgb(0xaa1a1a3a), "activeModuleBackground", instance.key());
    public static final SettingColor inactiveModuleBackground = new SettingColor(RenderColor.fromArgb(0xaa0f0f1f), "inactiveModuleBackground", instance.key());
    public static final SettingColor activeModuleBorder = new SettingColor(RenderColor.fromHex(0x7289da), "activeModuleBorder", instance.key());

    // Category Colors
    public static final SettingColor categoryHeaderBackground = new SettingColor(RenderColor.fromHex(0x5865f2), "categoryHeaderBackground", instance.key());
    public static final SettingColor categoryHeaderText = new SettingColor(RenderColor.fromHex(0xffffff), "categoryHeaderText", instance.key());
    public static final SettingColor categoryScrollbar = new SettingColor(RenderColor.fromHex(0x7289da), "categoryScrollbar", instance.key());

    // Main GUI Colors
    public static final SettingColor mainScrollbar = new SettingColor(RenderColor.fromHex(0x88aaff), "mainScrollbar", instance.key());
    public static final SettingColor buttonBorder = new SettingColor(RenderColor.fromHex(0x5865f2), "buttonBorder", instance.key());
    public static final SettingColor buttonBackground = new SettingColor(RenderColor.fromHex(0x0f0f1f), "buttonBackground", instance.key());

    // Customization
    public static final SettingInt categoryWidth = new SettingInt(150, "categoryWidth", instance.key());
    public static final SettingInt scrollbarThickness = new SettingInt(2, "scrollbarThickness", instance.key());
    // HUD Editor Colors
    public static final SettingColor hudEditorMainBackground = new SettingColor(RenderColor.fromHex(0x1a1a3e), "hudEditorMainBackground", instance.key());
    public static final SettingColor hudEditorMainBorder = new SettingColor(RenderColor.fromHex(0x4a4aff), "hudEditorMainBorder", instance.key());
    public static final SettingColor hudEditorTabActiveBackground = new SettingColor(RenderColor.fromHex(0x1A1D24), "hudEditorTabActiveBackground", instance.key());
    public static final SettingColor hudEditorTabActiveText = new SettingColor(RenderColor.fromHex(0xE6E8EE), "hudEditorTabActiveText", instance.key());
    public static final SettingColor hudEditorTabActiveAccent = new SettingColor(RenderColor.fromHex(0x4F7CFF), "hudEditorTabActiveAccent", instance.key());
    public static final SettingColor hudEditorTabInactiveText = new SettingColor(RenderColor.fromHex(0x9AA0B2), "hudEditorTabInactiveText", instance.key());
    public static final SettingColor hudEditorSidebarBackground = new SettingColor(RenderColor.fromHex(0x0F1117), "hudEditorSidebarBackground", instance.key());
    public static final SettingColor hudEditorSidebarText = new SettingColor(RenderColor.fromHex(0x9AA0B2), "hudEditorSidebarText", instance.key());
    public static final SettingColor hudEditorElementEnabledBackground = new SettingColor(RenderColor.fromHex(0x171A21), "hudEditorElementEnabledBackground", instance.key());
    public static final SettingColor hudEditorElementDisabledBackground = new SettingColor(RenderColor.fromHex(0x12141A), "hudEditorElementDisabledBackground", instance.key());
    public static final SettingColor hudEditorElementEnabledText = new SettingColor(RenderColor.fromHex(0xE6E8EE), "hudEditorElementEnabledText", instance.key());
    public static final SettingColor hudEditorElementDisabledText = new SettingColor(RenderColor.fromHex(0x9AA0B2), "hudEditorElementDisabledText", instance.key());
    public static final SettingColor hudEditorElementBorder = new SettingColor(RenderColor.fromHex(0x4F7CFF), "hudEditorElementBorder", instance.key());
    public static final SettingColor hudEditorScrollbar = new SettingColor(RenderColor.fromHex(0x5a5aaa), "hudEditorScrollbar", instance.key());
    public static final SettingColor hudEditorGridColor = new SettingColor(RenderColor.fromArgb(0x08ffffff), "hudEditorGridColor", instance.key());
    public static final SettingColor hudEditorCenterlineColor = new SettingColor(RenderColor.fromHex(0x4F7CFF), "hudEditorCenterlineColor", instance.key());

    @EventHandler
    private static void onInput(InputEvent event) {
        if (mc.currentScreen == null && openKey.key() == event.key) {
            if (event.action == GLFW.GLFW_PRESS) {
                Utils.setScreen(new ClickGui());
            }
            event.cancel();
        }
    }
}