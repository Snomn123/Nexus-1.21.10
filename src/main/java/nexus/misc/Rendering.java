package nexus.misc;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.OptionalDouble;

import static nexus.Main.mc;

public final class Rendering {
    /**
     * Draws a filled box for the current frame. Automatically performs the required matrix stack translation.
     */
    public static void drawFilled(MatrixStack matrices, VertexConsumerProvider.Immediate consumer, Camera camera, Box box, boolean throughWalls, RenderColor color) {
        matrices.push();
        Vec3d camPos = camera.getPos().negate();
        matrices.translate(camPos.x, camPos.y, camPos.z);
        VertexConsumer buffer = throughWalls ? consumer.getBuffer(Layers.BoxFilledNoCull) : consumer.getBuffer(Layers.BoxFilled);
        VertexRendering.drawFilledBox(matrices, buffer, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, color.r, color.g, color.b, color.a);
        matrices.pop();
    }

    /**
     * Draws an outline box for the current frame. Automatically performs the required matrix stack translation.
     */
    public static void drawOutline(MatrixStack matrices, VertexConsumerProvider.Immediate consumer, Camera camera, Box box, boolean throughWalls, RenderColor color) {
        matrices.push();
        Vec3d camPos = camera.getPos().negate();
        matrices.translate(camPos.x, camPos.y, camPos.z);
        VertexConsumer buffer = throughWalls ? consumer.getBuffer(Layers.BoxOutlineNoCull) : consumer.getBuffer(Layers.BoxOutline);
        VertexRendering.drawBox(matrices.peek(), buffer, box, color.r, color.g, color.b, color.a);
        matrices.pop();
    }

    /**
     * Draws text within the world for the current frame. Automatically performs the required matrix stack translation.
     */
    public static void drawText(VertexConsumerProvider.Immediate consumer, Camera camera, Vec3d pos, Text text, float scale, boolean throughWalls, RenderColor color) {
        Matrix4f matrices = new Matrix4f();
        Vec3d camPos = camera.getPos();
        float textX = (float) (pos.getX() - camPos.getX());
        float textY = (float) (pos.getY() - camPos.getY());
        float textZ = (float) (pos.getZ() - camPos.getZ());
        matrices.translate(textX, textY, textZ);
        matrices.rotate(camera.getRotation());
        matrices.scale(scale, -scale, scale);
        mc.textRenderer.draw(text, -mc.textRenderer.getWidth(text) / 2f, 1.0f, color.argb, true, matrices, consumer, throughWalls ? TextRenderer.TextLayerType.SEE_THROUGH : TextRenderer.TextLayerType.NORMAL, 0, LightmapTextureManager.MAX_LIGHT_COORDINATE);
    }
    
    /**
     * Draws text with a background box and border for better readability.
     */
    public static void drawTextWithBackground(VertexConsumerProvider.Immediate consumer, Camera camera, Vec3d pos, Text text, float scale, boolean throughWalls, RenderColor color) {
        Matrix4f matrices = new Matrix4f();
        Vec3d camPos = camera.getPos();
        float textX = (float) (pos.getX() - camPos.getX());
        float textY = (float) (pos.getY() - camPos.getY());
        float textZ = (float) (pos.getZ() - camPos.getZ());
        matrices.translate(textX, textY, textZ);
        matrices.rotate(camera.getRotation());
        matrices.scale(scale, -scale, scale);
        
        int textWidth = mc.textRenderer.getWidth(text);
        int padding = 3;
        int bgWidth = textWidth + padding * 2;
        int bgHeight = 10 + padding * 2;
        int bgX = -bgWidth / 2;
        int bgY = -padding;
        
        // Draw background
        VertexConsumer buffer = throughWalls 
            ? consumer.getBuffer(Layers.BoxFilledNoCull)
            : consumer.getBuffer(Layers.BoxFilled);
        
        int bgAlpha = (int) (0.75 * 255);
        int backgroundColor = (bgAlpha << 24) | 0x000000;
        
        drawRect(matrices, buffer, bgX, bgY, bgX + bgWidth, bgY + bgHeight, backgroundColor);
        
        // Draw border (2px thick)
        int borderColor = color.argb;
        int borderAlpha = 255;
        int finalBorderColor = (borderAlpha << 24) | (borderColor & 0xFFFFFF);
        int borderThickness = 2;
        
        drawRect(matrices, buffer, bgX, bgY, bgX + bgWidth, bgY + borderThickness, finalBorderColor);
        drawRect(matrices, buffer, bgX, bgY + bgHeight - borderThickness, bgX + bgWidth, bgY + bgHeight, finalBorderColor);
        drawRect(matrices, buffer, bgX, bgY, bgX + borderThickness, bgY + bgHeight, finalBorderColor);
        drawRect(matrices, buffer, bgX + bgWidth - borderThickness, bgY, bgX + bgWidth, bgY + bgHeight, finalBorderColor);
        
        // Draw text
        mc.textRenderer.draw(text, -textWidth / 2f, 0, color.argb | 0xFF000000, true, matrices, consumer, throughWalls ? TextRenderer.TextLayerType.SEE_THROUGH : TextRenderer.TextLayerType.NORMAL, 0, LightmapTextureManager.MAX_LIGHT_COORDINATE);
    }

    /**
     * Draws a simulated beacon beam for the current frame. Automatically performs the required matrix stack translation.
     */
    public static void drawBeam(MatrixStack matrices, VertexConsumerProvider.Immediate consumer, Camera camera, Vec3d pos, int height, boolean throughWalls, RenderColor color) {
        drawFilled(matrices, consumer, camera, Box.of(pos, 0.5, 0, 0.5).stretch(0, height, 0), throughWalls, color);
    }

    /**
     * Draws a tracer going from the center of the screen to the provided coordinate. Automatically performs the required matrix stack translation.
     */
    public static void drawTracer(MatrixStack matrices, VertexConsumerProvider.Immediate consumer, Camera camera, Vec3d pos, RenderColor color) {
        Vec3d camPos = camera.getPos();
        matrices.push();
        matrices.translate(-camPos.getX(), -camPos.getY(), -camPos.getZ());
        MatrixStack.Entry entry = matrices.peek();
        VertexConsumer buffer = consumer.getBuffer(Layers.GuiLine);
        Vec3d point = camPos.add(Vec3d.fromPolar(camera.getPitch(), camera.getYaw())); // taken from Skyblocker's RenderHelper, my brain cannot handle OpenGL
        Vector3f normal = pos.toVector3f().sub((float) point.getX(), (float) point.getY(), (float) point.getZ()).normalize(new Vector3f(1.0f, 1.0f, 1.0f));
        buffer.vertex(entry, (float) point.getX(), (float) point.getY(), (float) point.getZ()).color(color.r, color.g, color.b, color.a).normal(entry, normal);
        buffer.vertex(entry, (float) pos.getX(), (float) pos.getY(), (float) pos.getZ()).color(color.r, color.g, color.b, color.a).normal(entry, normal);
        matrices.pop();
    }

    /**
     * Draws a filled 2D rectangle in world space (for labels, overlays, etc.)
     */
    public static void drawRect(Matrix4f matrix, VertexConsumer buffer, int x1, int y1, int x2, int y2, int color) {
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = ((color >> 24) & 0xFF) / 255.0f;

        // Draw two triangles to form a rectangle
        buffer.vertex(matrix, x1, y2, 0).color(r, g, b, a);
        buffer.vertex(matrix, x2, y2, 0).color(r, g, b, a);
        buffer.vertex(matrix, x2, y1, 0).color(r, g, b, a);
        buffer.vertex(matrix, x1, y1, 0).color(r, g, b, a);
    }

    public static void drawBorder(DrawContext context, int x, int y, int width, int height, RenderColor color) {
        drawBorder(context, x, y, width, height, color.argb);
    }

    public static void drawBorder(DrawContext context, int x, int y, int width, int height, int argb) {
        context.fill(x, y, x + width, y + 1, argb);
        context.fill(x, y + height - 1, x + width, y + height, argb);
        context.fill(x, y + 1, x + 1, y + height - 1, argb);
        context.fill(x + width - 1, y + 1, x + width, y + height - 1, argb);
    }

    public static void drawBorder(DrawContext context, int x, int y, int width, int height, int argb, int borderWidth) {
        for (int i = 0; i < borderWidth; i++) {
            context.fill(x + i, y + i, x + width - i, y + i + 1, argb);
            context.fill(x + i, y + height - i - 1, x + width - i, y + height - i, argb);
            context.fill(x + i, y + i + 1, x + i + 1, y + height - i - 1, argb);
            context.fill(x + width - i - 1, y + i + 1, x + width - i, y + height - i - 1, argb);
        }
    }

    /**
     * Draws a rounded rectangle
     */
    public static void drawRoundedRect(DrawContext context, int x, int y, int width, int height, int radius, int color) {
        // Main rectangle body
        context.fill(x + radius, y, x + width - radius, y + height, color);
        context.fill(x, y + radius, x + radius, y + height - radius, color);
        context.fill(x + width - radius, y + radius, x + width, y + height - radius, color);
        
        // Corners (simple approximation)
        drawCorner(context, x + radius, y + radius, radius, color, 0); // Top-left
        drawCorner(context, x + width - radius, y + radius, radius, color, 1); // Top-right
        drawCorner(context, x + radius, y + height - radius, radius, color, 2); // Bottom-left
        drawCorner(context, x + width - radius, y + height - radius, radius, color, 3); // Bottom-right
    }

    /**
     * Draws a rounded rectangle border
     */
    public static void drawRoundedBorder(DrawContext context, int x, int y, int width, int height, int radius, int color, int borderWidth) {
        // Top and bottom
        context.fill(x + radius, y, x + width - radius, y + borderWidth, color);
        context.fill(x + radius, y + height - borderWidth, x + width - radius, y + height, color);
        
        // Left and right
        context.fill(x, y + radius, x + borderWidth, y + height - radius, color);
        context.fill(x + width - borderWidth, y + radius, x + width, y + height - radius, color);
        
        // Corner outlines (simple)
        drawCornerBorder(context, x + radius, y + radius, radius, color, borderWidth, 0);
        drawCornerBorder(context, x + width - radius, y + radius, radius, color, borderWidth, 1);
        drawCornerBorder(context, x + radius, y + height - radius, radius, color, borderWidth, 2);
        drawCornerBorder(context, x + width - radius, y + height - radius, radius, color, borderWidth, 3);
    }

    private static void drawCorner(DrawContext context, int cx, int cy, int radius, int color, int corner) {
        for (int i = 0; i < radius; i++) {
            for (int j = 0; j < radius; j++) {
                if (i * i + j * j <= radius * radius) {
                    int px = cx + (corner == 1 || corner == 3 ? i : -i);
                    int py = cy + (corner == 2 || corner == 3 ? j : -j);
                    context.fill(px, py, px + 1, py + 1, color);
                }
            }
        }
    }

    private static void drawCornerBorder(DrawContext context, int cx, int cy, int radius, int color, int borderWidth, int corner) {
        for (int i = 0; i < radius; i++) {
            for (int j = 0; j < radius; j++) {
                double dist = Math.sqrt(i * i + j * j);
                if (dist <= radius && dist >= radius - borderWidth) {
                    int px = cx + (corner == 1 || corner == 3 ? i : -i);
                    int py = cy + (corner == 2 || corner == 3 ? j : -j);
                    context.fill(px, py, px + 1, py + 1, color);
                }
            }
        }
    }

    public static class Pipelines {
        public static final RenderPipeline.Snippet filledSnippet = RenderPipelines.POSITION_COLOR_SNIPPET;
        public static final RenderPipeline.Snippet outlineSnippet = RenderPipelines.RENDERTYPE_LINES_SNIPPET;

        public static final RenderPipeline filledNoCull = RenderPipelines.register(RenderPipeline.builder(filledSnippet)
                .withLocation(Identifier.of("nexus", "pipeline/nofrills_filled_no_cull"))
                .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.TRIANGLE_STRIP)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .build());
        public static final RenderPipeline filledCull = RenderPipelines.register(RenderPipeline.builder(filledSnippet)
                .withLocation(Identifier.of("nexus", "pipeline/nofrills_filled_cull"))
                .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.TRIANGLE_STRIP)
                .build());
        public static final RenderPipeline outlineNoCull = RenderPipelines.register(RenderPipeline.builder(outlineSnippet)
                .withLocation(Identifier.of("nexus", "pipeline/nofrills_outline_no_cull"))
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .build());
        public static final RenderPipeline outlineCull = RenderPipelines.register(RenderPipeline.builder(outlineSnippet)
                .withLocation(Identifier.of("nexus", "pipeline/nofrills_outline_cull"))
                .build());
        public static final RenderPipeline lineNoCull = RenderPipelines.register(RenderPipeline.builder(outlineSnippet)
                .withLocation(Identifier.of("nexus", "pipeline/nofrills_line_no_cull"))
                .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.DEBUG_LINE_STRIP)
                .withVertexShader("core/position_color")
                .withFragmentShader("core/position_color")
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .build());
        public static final RenderPipeline lineDepthTest = RenderPipelines.register(RenderPipeline.builder(outlineSnippet)
                .withLocation(Identifier.of("nexus", "pipeline/nofrills_line_depth_test"))
                .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.DEBUG_LINES)
                .withVertexShader("core/position_color")
                .withFragmentShader("core/position_color")
                .build());
    }

    public static class Parameters {
        public static final RenderLayer.MultiPhaseParameters.Builder filled = RenderLayer.MultiPhaseParameters.builder()
                .layering(RenderLayer.VIEW_OFFSET_Z_LAYERING);
        public static final RenderLayer.MultiPhaseParameters.Builder lines = RenderLayer.MultiPhaseParameters.builder()
                .layering(RenderLayer.VIEW_OFFSET_Z_LAYERING)
                .lineWidth(new RenderPhase.LineWidth(OptionalDouble.of(3.0)));
    }

    public static class Layers {
        public static final RenderLayer.MultiPhase BoxFilled = RenderLayer.of(
                "nofrills_box_filled",
                RenderLayer.DEFAULT_BUFFER_SIZE,
                false,
                true,
                Pipelines.filledCull,
                Parameters.filled.build(false)
        );
        public static final RenderLayer.MultiPhase BoxFilledNoCull = RenderLayer.of(
                "nofrills_box_filled_no_cull",
                RenderLayer.DEFAULT_BUFFER_SIZE,
                false,
                true,
                Pipelines.filledNoCull,
                Parameters.filled.build(false)
        );
        public static final RenderLayer.MultiPhase BoxOutline = RenderLayer.of(
                "nofrills_box_outline",
                RenderLayer.DEFAULT_BUFFER_SIZE,
                false,
                false,
                Pipelines.outlineCull,
                Parameters.lines.build(false)
        );
        public static final RenderLayer.MultiPhase BoxOutlineNoCull = RenderLayer.of(
                "nofrills_box_outline_no_cull",
                RenderLayer.DEFAULT_BUFFER_SIZE,
                false,
                false,
                Pipelines.outlineNoCull,
                Parameters.lines.build(false)
        );
        public static final RenderLayer.MultiPhase GuiLine = RenderLayer.of(
                "nofrills_gui_line",
                RenderLayer.DEFAULT_BUFFER_SIZE,
                false,
                false,
                Pipelines.lineNoCull,
                Parameters.lines.build(false)
        );
        public static final RenderLayer.MultiPhase LineDepthTest = RenderLayer.of(
                "nofrills_line_depth_test",
                RenderLayer.DEFAULT_BUFFER_SIZE,
                false,
                false,
                Pipelines.lineDepthTest,
                Parameters.lines.build(false)
        );
    }
}
