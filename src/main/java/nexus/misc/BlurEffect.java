package nexus.misc;

import net.minecraft.client.gui.DrawContext;

/**
 * Utility class for applying darkening effects to the screen background.
 */
public class BlurEffect {

    /**
     * Applies a darkening overlay to the background.
     *
     * @param context DrawContext for rendering
     * @param width Screen width
     * @param height Screen height
     * @param darkeningOpacity Darkening overlay opacity (0-255)
     */
    public static void applyBlur(DrawContext context, int width, int height, int darkeningOpacity) {
        // Apply darkening overlay
        if (darkeningOpacity > 0) {
            int darkenColor = (darkeningOpacity << 24); // Alpha channel only (black)
            context.fill(0, 0, width, height, darkenColor);
        }
    }

    /**
     * Simple cleanup method (no cleanup since we're not using framebuffers).
     * Kept for API compatibility.
     */
    public static void cleanup() {
        // No resources to clean up with this simplified implementation
    }
}

