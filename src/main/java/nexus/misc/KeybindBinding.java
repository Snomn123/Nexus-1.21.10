package nexus.misc;

import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Component;
import nexus.hud.clickgui.components.KeybindButton;

import java.util.List;

/**
 * Centralized helper to forward key/mouse inputs to any KeybindButton
 * currently awaiting a binding within OWO UI layouts.
 */
public final class KeybindBinding {
    private KeybindBinding() {}

    /**
     * Forward a key or mouse button to the first active KeybindButton
     * within the given layout tree.
     */
    public static boolean forwardInLayout(FlowLayout root, int keyOrButton) {
        if (root == null) return false;
        return bindInLayout(root, keyOrButton);
    }

    /**
     * Forward a key or mouse button to the first active KeybindButton
     * within the provided list of settings rows.
     */
    public static boolean forwardInChildren(List<FlowLayout> settings, int keyOrButton) {
        if (settings == null) return false;
        for (FlowLayout setting : settings) {
            if (bindInLayout(setting, keyOrButton)) {
                return true;
            }
        }
        return false;
    }

    // Recursive search for a KeybindButton awaiting input
    private static boolean bindInLayout(FlowLayout layout, int keyOrButton) {
        for (Component child : layout.children()) {
            if (child instanceof KeybindButton keybind) {
                if (keybind.isBinding) {
                    keybind.bind(keyOrButton);
                    return true;
                }
            } else if (child instanceof FlowLayout flow) {
                if (bindInLayout(flow, keyOrButton)) {
                    return true;
                }
            }
        }
        return false;
    }
}
