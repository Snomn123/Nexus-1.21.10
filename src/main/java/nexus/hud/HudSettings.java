package nexus.hud;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.text.Text;
import nexus.hud.clickgui.Settings;
import nexus.misc.Rendering;

import java.util.List;

import static nexus.Main.mc;

public class HudSettings extends Settings {
    public HudSettings(List<FlowLayout> settings) {
        super(settings);
    }

    @Override
    protected void build(FlowLayout root) {
        super.build(root);
        
        // Add back button breadcrumb at the top
        ButtonComponent backButton = Components.button(Text.empty(), btn -> {
            mc.setScreen(new HudEditorScreen());
        });
        backButton.horizontalSizing(Sizing.fixed(180));
        backButton.verticalSizing(Sizing.fixed(28));
        backButton.positioning(Positioning.absolute(10, 10));
        backButton.renderer((context, btn, delta) -> {
            int bgColor = btn.isHovered() ? 0x404F7CFF : 0x20ffffff;
            int borderColor = btn.isHovered() ? 0x604F7CFF : 0x30ffffff;
            
            Rendering.drawRoundedRect(context, btn.getX(), btn.getY(), btn.getWidth(), btn.getHeight(), 4, bgColor);
            Rendering.drawRoundedBorder(context, btn.getX(), btn.getY(), btn.getWidth(), btn.getHeight(), 4, borderColor, 1);
            
            int textColor = btn.isHovered() ? 0xFFFFFFFF : 0xFF9AA0B2;
            int textX = btn.getX() + 10;
            int textY = btn.getY() + (btn.getHeight() - mc.textRenderer.fontHeight) / 2;
            context.drawText(mc.textRenderer, "← Back to HUD Editor", textX, textY, textColor, true);
        });
        
        root.child(backButton);
    }

    @Override
    public void close() {
        mc.setScreen(new HudEditorScreen());
    }
}
