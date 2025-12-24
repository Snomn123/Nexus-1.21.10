package nexus.hud;

import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.DraggableContainer;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.gui.Click;
import net.minecraft.client.util.Window;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import nexus.config.Feature;
import nexus.config.SettingBool;
import nexus.config.SettingColor;
import nexus.config.SettingDouble;
import nexus.config.SettingInt;
import nexus.hud.clickgui.Settings;
import nexus.misc.RenderColor;
import nexus.misc.Utils;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

import static nexus.Main.mc;

public class HudElement extends DraggableContainer<FlowLayout> {
    public final MutableText elementLabel;
    public final Feature instance;
    public final SettingDouble xPos;
    public final SettingDouble yPos;
    public final SettingBool useBackground;
    public final SettingColor background;
    public final SettingBool useBorder;
    public final SettingColor borderColor;
    public final SettingInt borderWidth;
    public final SettingInt paddingHorizontal;
    public final SettingInt paddingVertical;
    public final SettingInt opacity;
    public final Identifier identifier;
    public final Surface disabledSurface = Surface.flat(0x55ff0000);
    public FlowLayout layout;
    public HudSettings options;
    public boolean toggling = false;

    public HudElement(FlowLayout layout, Feature instance, String label) {
        super(Sizing.content(), Sizing.content(), layout);
        this.elementLabel = Text.literal(label);
        this.instance = instance;
        this.xPos = new SettingDouble(0.5, "x", instance);
        this.yPos = new SettingDouble(0.5, "y", instance);
        this.useBackground = new SettingBool(false, "useBackground", instance);
        this.background = new SettingColor(RenderColor.fromArgb(0x40000000), "background", instance);
        this.useBorder = new SettingBool(false, "useBorder", instance);
        this.borderColor = new SettingColor(RenderColor.fromHex(0x5865f2), "borderColor", instance);
        this.borderWidth = new SettingInt(1, "borderWidth", instance);
        this.paddingHorizontal = new SettingInt(2, "paddingHorizontal", instance);
        this.paddingVertical = new SettingInt(2, "paddingVertical", instance);
        this.opacity = new SettingInt(100, "opacity", instance);
        this.identifier = Identifier.of("nexus", Utils.toLower(label.replaceAll(" ", "-")));
        this.positioning(Positioning.absolute(0, 0));
        this.layout = layout;
        this.layout.sizing(Sizing.content(), Sizing.content());
        this.layout.allowOverflow(true);
        this.foreheadSize(0);
        this.allowOverflow(true);
        this.child(this.layout);
        HudManager.addNew(this);
    }

    public HudElement(Feature instance, String label) {
        this(Containers.horizontalFlow(Sizing.content(), Sizing.content()), instance, label);
    }

    @Override
    protected void drawChildren(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta, List<? extends Component> children) {
        // Apply padding before drawing children
        this.layout.padding(Insets.of(paddingVertical.value(), paddingHorizontal.value(), paddingVertical.value(), paddingHorizontal.value()));

        // Draw border if enabled
        if (this.useBorder.value()) {
            int borderWidth = this.borderWidth.value();
            int borderColor = this.borderColor.value().argb;
            nexus.misc.Rendering.drawBorder(context, this.x, this.y, this.width, this.height, borderColor, borderWidth);
        }

        try {
            super.drawChildren(context, mouseX, mouseY, partialTicks, delta, children);
        } catch (Exception ignored) {
        }
    }

    @Override
    public boolean onMouseDown(Click click, boolean doubled) {
        if (this.instance.isActive()) {
            if (click.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                mc.setScreen(this.options);
                return true;
            }
        }
        return super.onMouseDown(click, doubled);
    }

    @Override
    public boolean onMouseDrag(Click click, double deltaX, double deltaY) {
        if (this.instance.isActive()) {
            boolean result = super.onMouseDrag(click, deltaX, deltaY);
            Window window = mc.getWindow();
            this.savePosition(this.xOffset / window.getScaledWidth(), this.yOffset / window.getScaledHeight());
            return result;
        }
        return false;
    }

    @Override
    public Component childAt(int x, int y) {
        if (this.isInBoundingBox(x, y)) { // gets rid of the forehead
            return this;
        }
        return super.childAt(x, y);
    }

    public boolean isActive() {
        return this.instance.isActive();
    }

    public Surface getBackground() {
        if (this.useBackground.value()) {
            RenderColor bgColor = this.background.value();
            float opacityMultiplier = this.opacity.value() / 100.0f;
            RenderColor adjustedColor = RenderColor.fromFloat(bgColor.r, bgColor.g, bgColor.b, bgColor.a * opacityMultiplier);
            return Surface.flat(adjustedColor.argb);
        }
        return Surface.BLANK;
    }

    public boolean shouldRender() {
        if (!this.instance.isActive()) {
            return false;
        }
        boolean active = this.instance.isActive();
        this.layout.surface(active ? this.getBackground() : this.disabledSurface);
        return active || HudManager.isEditingHud();
    }

    public HudSettings getBaseSettings() {
        return this.getBaseSettings(new ArrayList<>());
    }

    public HudSettings getBaseSettings(List<FlowLayout> extra) {
        List<FlowLayout> list = new ArrayList<>(extra);
        list.add(new Settings.Toggle("Use Background", this.useBackground, "Draw a background for this element."));
        list.add(new Settings.SliderInt("Opacity", 0, 100, 5, this.opacity, "The opacity of the background."));
        list.add(new Settings.ColorPicker("Background Color", true, this.background, "The color of the background."));
        list.add(new Settings.Toggle("Use Border", this.useBorder, "Draw a border around this element."));
        list.add(new Settings.ColorPicker("Border Color", false, this.borderColor, "The color of the border."));
        list.add(new Settings.SliderInt("Border Width", 1, 10, 1, this.borderWidth, "The width of the border in pixels."));
        list.add(new Settings.SliderInt("Horizontal Padding", 0, 20, 1, this.paddingHorizontal, "The horizontal padding inside the element."));
        list.add(new Settings.SliderInt("Vertical Padding", 0, 20, 1, this.paddingVertical, "The vertical padding inside the element."));
        HudSettings settings = new HudSettings(list);
        settings.setTitle(this.elementLabel);
        return settings;
    }

    public boolean isEditingHud() {
        return HudManager.isEditingHud();
    }

    public void updatePosition() {
        Window window = mc.getWindow();
        int width = window.getScaledWidth(), height = window.getScaledHeight();
        this.xOffset = Math.clamp(this.xPos.value() * width, 0, Math.clamp(width - this.width, 0, width));
        this.yOffset = Math.clamp(this.yPos.value() * height, 0, Math.clamp(height - this.height, 0, height));
        this.updateX(0);
        this.updateY(0);
    }

    public void savePosition(double x, double y) {
        this.xPos.set(x);
        this.yPos.set(y);
    }

    public void toggle() {
        this.instance.setActive(!this.instance.isActive());
    }

    public Identifier getIdentifier() {
        return this.identifier;
    }
}