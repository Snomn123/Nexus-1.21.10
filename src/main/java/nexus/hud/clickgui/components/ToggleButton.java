package nexus.hud.clickgui.components;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.util.EventSource;
import io.wispforest.owo.util.EventStream;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import nexus.misc.Rendering;

import static nexus.Main.mc;

public class ToggleButton extends ButtonComponent {
    public static final MutableText enabledText = Text.literal("ON").withColor(0x00ddff);
    public static final MutableText disabledText = Text.literal("OFF").withColor(0x888888);
    private final EventStream<ToggleChanged> changedEvents = ToggleChanged.newStream();
    private boolean toggle;

    public ToggleButton(boolean toggled) {
        super(disabledText, buttonComponent -> {
        });
        this.renderer(this::renderPillToggle);
        this.toggle = toggled;
        this.setMessage(this.toggle ? enabledText : disabledText);
        this.onPress(button -> this.setToggle());
    }
    
    private void renderPillToggle(DrawContext context, ButtonComponent button, float delta) {
        boolean isHovered = button.isInBoundingBox((double)mc.mouse.getX() * mc.getWindow().getScaledWidth() / mc.getWindow().getWidth(),
            (double)mc.mouse.getY() * mc.getWindow().getScaledHeight() / mc.getWindow().getHeight());
        
        // Modern switch design - visual sliding indicator
        int trackColor;
        int thumbColor;
        int thumbX;
        
        if (this.toggle) {
            // Enabled - accent blue
            trackColor = isHovered ? 0xFF5A8FFF : 0xFF4F7CFF;
            thumbColor = 0xFFFFFFFF;
            thumbX = button.x() + button.width() - button.height() + 2; // Right side
        } else {
            // Disabled - neutral gray
            trackColor = isHovered ? 0xFF2A2D35 : 0xFF21242B;
            thumbColor = 0xFF9AA0B2;
            thumbX = button.x() + 2; // Left side
        }
        
        // Switch track (rounded rect)
        int trackRadius = button.height() / 2;
        Rendering.drawRoundedRect(context, button.x(), button.y(), button.width(), button.height(), trackRadius, trackColor);
        
        // Switch thumb (circular indicator)
        int thumbSize = button.height() - 4;
        int thumbY = button.y() + 2;
        Rendering.drawRoundedRect(context, thumbX, thumbY, thumbSize, thumbSize, thumbSize / 2, thumbColor);
    }

    public void setToggle() {
        this.setToggle(!this.toggle);
    }

    public void setToggle(boolean toggle) {
        this.toggle = toggle;
        this.setMessage(this.toggle ? enabledText : disabledText);
        changedEvents.sink().onToggle(this.toggle);
    }

    public EventSource<ToggleChanged> onToggled() {
        return changedEvents.source();
    }

    public interface ToggleChanged {
        static EventStream<ToggleChanged> newStream() {
            return new EventStream<>(subscribers -> (toggle) -> {
                for (var subscriber : subscribers) {
                    subscriber.onToggle(toggle);
                }
            });
        }

        void onToggle(boolean toggle);
    }
}
