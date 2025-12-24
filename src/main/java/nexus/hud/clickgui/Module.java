package nexus.hud.clickgui;

import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import nexus.config.Feature;
import nexus.features.render.ClickGUI;
import nexus.misc.Rendering;
import org.lwjgl.glfw.GLFW;

import static nexus.Main.mc;

public class Module extends FlowLayout {
    public Feature feature;
    public String name;
    public Settings settings;
    public boolean expanded = false;
    public FlowLayout settingsContainer;
    public FlowLayout headerContainer;

    public Module(String name, Feature feature) {
        this(name, feature, null);
    }

    public Module(String name, Feature feature, Settings settings) {
        super(Sizing.fill(100), Sizing.content(), Algorithm.VERTICAL);
        this.name = name;
        this.feature = feature;
        this.settings = settings;
        this.gap(0);
        this.allowOverflow(true); // Allow overflow so scroll container works properly
        
        // Header container - card design with click handling
        this.headerContainer = new FlowLayout(Sizing.fill(100), Sizing.fixed(44), Algorithm.HORIZONTAL) {
            @Override
            public boolean onMouseDown(Click click, boolean doubled) {
                // Header handles its own clicks
                if (click.button() == GLFW.GLFW_MOUSE_BUTTON_1) {
                    Module.this.feature.setActive(!Module.this.feature.isActive());
                    return true;
                } else if (click.button() == GLFW.GLFW_MOUSE_BUTTON_2 && Module.this.settings != null && !Module.this.settings.settings.isEmpty()) {
                    Module.this.toggleExpanded();
                    return true;
                }
                return super.onMouseDown(click, doubled);
            }
        };
        this.headerContainer.surface((context, component) -> {
            boolean isHovered = component.isInBoundingBox(
                ((ClickGui) mc.currentScreen).mouseX,
                ((ClickGui) mc.currentScreen).mouseY
            );
            boolean isActive = this.feature.isActive();
            
            int bgColor;
            int borderColor;
            int glowColor;
            int textColor;
            
            if (isActive) {
                bgColor = 0xFF171A21;
                borderColor = 0x004F7CFF;
                glowColor = 0;
                textColor = 0xFFE6E8EE;
            } else if (isHovered) {
                // Hover: slight background lift
                bgColor = 0xFF1A1D24;
                borderColor = 0;
                glowColor = 0;
                textColor = 0xFFE6E8EE;
            } else {
                bgColor = 0xFF171A21;
                borderColor = 0;
                glowColor = 0;
                textColor = 0xFF9AA0B2;
            }
            
            // Card background with softer corners (8px)
            if (this.expanded) {
                // Only round top corners
                Rendering.drawRoundedRect(context, component.x(), component.y(), component.width(), component.height(), 8, bgColor);
            } else {
                // Round all corners - floating card feel
                Rendering.drawRoundedRect(context, component.x(), component.y(), component.width(), component.height(), 8, bgColor);
            }
            
            // Border only for active state
            if (borderColor != 0) {
                Rendering.drawRoundedBorder(context, component.x(), component.y(), component.width(), component.height(), 8, borderColor, 1);
            }
            
            // No glow effects - clean modern look
            
            // Left accent bar for active modules (thinner and more elegant)
            if (isActive) {
                Rendering.drawRoundedRect(context, component.x() + 8, component.y() + 12, 3, component.height() - 24, 2, 0xFF6a6aff);
            }
            
            // Draw module name with shadow for depth
            int nameX = component.x() + (isActive ? 24 : 18);
            int nameY = component.y() + (component.height() - 8) / 2 - 6;
            
            if (isActive || isHovered) {
                context.drawText(mc.textRenderer, this.name, nameX + 1, nameY + 1, 0x66000000, false);
            }
            context.drawText(mc.textRenderer, this.name, nameX, nameY, textColor, false);
            
            // Status badge (pill style) - below module name
            String statusText = isActive ? "ENABLED" : "";
            int badgeColor = isActive ? 0x80007766 : 0x60333333;
            int badgeTextColor = isActive ? 0xFF00ffaa : 0xFF888888;
            
            if (!statusText.isEmpty() || isHovered) {
                int badgeY = nameY + 12;
                int badgeWidth = mc.textRenderer.getWidth(statusText) + 16;
                
                if (isActive) {
                    // Enabled badge
                    Rendering.drawRoundedRect(context, nameX, badgeY, badgeWidth, 14, 7, badgeColor);
                    Rendering.drawRoundedBorder(context, nameX, badgeY, badgeWidth, 14, 7, 0x6000ffaa, 1);
                    context.drawText(mc.textRenderer, statusText, nameX + 8, badgeY + 3, badgeTextColor, false);
                }
            }
            
            // Draw arrow on the right if has settings
            if (this.settings != null && !this.settings.settings.isEmpty()) {
                String arrow = this.expanded ? "▼" : "›";
                int arrowX = component.x() + component.width() - 20;
                int arrowY = component.y() + (component.height() - 8) / 2;
                
                if (isActive || isHovered) {
                    context.drawText(mc.textRenderer, arrow, arrowX + 1, arrowY + 1, 0x44000000, false);
                }
                context.drawText(mc.textRenderer, arrow, arrowX, arrowY, textColor, false);
            }
        });
        
        this.child(this.headerContainer);
        
        // Settings container (hidden by default) - card continuation
        if (this.settings != null && !this.settings.settings.isEmpty()) {
            this.settingsContainer = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
            this.settingsContainer.allowOverflow(true); // Allow overflow for proper scrolling
            this.settingsContainer.surface((context, component) -> {
                // Slightly darker continuation - no borders
                context.fill(component.x(), component.y(), component.x() + component.width(), component.y() + component.height(), 0xFF0F1117);
                
                // Round bottom corners (8px to match)
                Rendering.drawRoundedRect(context, component.x(), component.y() + component.height() - 8, component.width(), 8, 8, 0xFF0F1117);
            });
            this.settingsContainer.padding(Insets.of(20)); // More spacious padding
            this.settingsContainer.gap(12);
            
            for (FlowLayout setting : this.settings.settings) {
                setting.horizontalSizing(Sizing.fill(100));
                this.settingsContainer.child(setting);
            }
        }
    }
    
    public void toggleExpanded() {
        this.expanded = !this.expanded;
        
        if (this.expanded && this.settingsContainer != null) {
            if (!this.children().contains(this.settingsContainer)) {
                this.child(this.settingsContainer);
            }
        } else if (this.settingsContainer != null) {
            this.removeChild(this.settingsContainer);
        }
    }
}
