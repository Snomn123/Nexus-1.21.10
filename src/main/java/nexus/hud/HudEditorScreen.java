package nexus.hud;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import nexus.features.misc.AutoSave;
import nexus.features.render.ClickGUI;
import nexus.hud.clickgui.ClickGui;
import nexus.hud.clickgui.components.PlainLabel;
import nexus.hud.clickgui.components.ToggleButton;
import nexus.misc.BlurEffect;
import nexus.misc.RenderColor;
import nexus.misc.Rendering;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

import static nexus.Main.mc;

public class HudEditorScreen extends BaseOwoScreen<FlowLayout> {
    public int mouseX = 0;
    public int mouseY = 0;
    
    public HudEditorScreen() {
        super(Text.of(""));
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout root) {
        root.surface(Surface.VANILLA_TRANSLUCENT);
        root.allowOverflow(false);
        
        // Calculate responsive container size - match ClickGui (smaller)
        int containerWidth = (int)(this.width * 0.70);
        int containerHeight = (int)(this.height * 0.70);
        containerWidth = Math.min(containerWidth, 1100); // Reduced max width
        containerHeight = Math.min(containerHeight, 700); // Reduced max height
        
        // Calculate explicit content area width to constrain elements
        int sidebarWidth = 180; // Reduced from 220px
        int horizontalPadding = 40; // Match ClickGui pattern
        int contentAreaWidth = containerWidth - sidebarWidth - horizontalPadding;
        
        // Main container with modern design
        FlowLayout mainContainer = Containers.verticalFlow(Sizing.fixed(containerWidth), Sizing.fixed(containerHeight));
        mainContainer.surface((context, component) -> {
            Rendering.drawRoundedRect(context, component.x(), component.y(), component.width(), component.height(), 8, ClickGUI.hudEditorMainBackground.value().argb);
            Rendering.drawRoundedBorder(context, component.x(), component.y(), component.width(), component.height(), 8, ClickGUI.hudEditorMainBorder.value().argb, 2);
            // Reduced outer glow
            RenderColor borderColor = ClickGUI.hudEditorMainBorder.value();
            int glowColor = RenderColor.fromFloat(borderColor.r, borderColor.g, borderColor.b, 0.17f).argb;
            Rendering.drawRoundedBorder(context, component.x() - 1, component.y() - 1, component.width() + 2, component.height() + 2, 9, glowColor, 1);
        });
        mainContainer.positioning(Positioning.relative(50, 50));
        mainContainer.gap(0);
        
        // Top header with tabs
        FlowLayout topHeader = Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(50));
        topHeader.surface((context, component) -> {
            int startColor = 0xFF0d0d28;
            int endColor = 0xFF1a1a3e;
            for (int i = 0; i < component.height(); i++) {
                float ratio = (float) i / component.height();
                int color = interpolateColor(startColor, endColor, ratio);
                context.fill(component.x(), component.y() + i, component.x() + component.width(), component.y() + i + 1, color);
            }
            context.fill(component.x(), component.y() + component.height() - 1, component.x() + component.width(), component.y() + component.height(), 0xFF2a2a5e);
        });
        topHeader.padding(Insets.of(10, 15, 10, 15));
        topHeader.gap(0);
        
        // Nexus tab (inactive)
        ButtonComponent nexusTab = Components.button(Text.empty(), btn -> {
            mc.setScreen(new ClickGui());
        });
        nexusTab.horizontalSizing(Sizing.fixed(110));
        nexusTab.verticalSizing(Sizing.fill(100));
        nexusTab.renderer((context, button, delta) -> {
            boolean isHovered = button.isInBoundingBox(this.mouseX, this.mouseY);
            
            if (isHovered) {
                Rendering.drawRoundedRect(context, button.x(), button.y() + 5, button.width(), button.height() - 5, 4, 0xFF171A21);
                Rendering.drawRoundedBorder(context, button.x(), button.y() + 5, button.width(), button.height() - 5, 4, 0x30ffffff, 1);
            }
            
            int textX = button.x() + (button.width() - mc.textRenderer.getWidth("Nexus")) / 2;
            int textY = button.y() + (button.height() - 8) / 2;
            int textColor = isHovered ? ClickGUI.hudEditorTabActiveText.value().argb : ClickGUI.hudEditorTabInactiveText.value().argb;
            
            if (isHovered) {
                context.drawText(mc.textRenderer, "Nexus", textX + 1, textY + 1, 0x44000000, false);
            }
            context.drawText(mc.textRenderer, "Nexus", textX, textY, textColor, false);
        });
        topHeader.child(nexusTab);
        
        // HUD Editor tab (active)
        ButtonComponent hudEditorTab = Components.button(Text.empty(), btn -> {});
        hudEditorTab.horizontalSizing(Sizing.fixed(110));
        hudEditorTab.verticalSizing(Sizing.fill(100));
        hudEditorTab.margins(Insets.left(5));
        hudEditorTab.renderer((context, button, delta) -> {
            Rendering.drawRoundedRect(context, button.x(), button.y() + 5, button.width(), button.height() - 5, 4, ClickGUI.hudEditorTabActiveBackground.value().argb);
            context.fill(button.x(), button.y() + button.height() - 3, button.x() + button.width(), button.y() + button.height(), ClickGUI.hudEditorTabActiveAccent.value().argb);
            
            int textX = button.x() + (button.width() - mc.textRenderer.getWidth("HUD Editor")) / 2;
            int textY = button.y() + (button.height() - 8) / 2;
            context.drawText(mc.textRenderer, "HUD Editor", textX + 1, textY + 1, 0x88000000, false);
            context.drawText(mc.textRenderer, "HUD Editor", textX, textY, ClickGUI.hudEditorTabActiveText.value().argb, false);
        });
        topHeader.child(hudEditorTab);
        
        mainContainer.child(topHeader);
        
        // Sidebar with element list
        FlowLayout contentArea = Containers.horizontalFlow(Sizing.fill(100), Sizing.fill(100));
        contentArea.gap(0);
        
        // Left sidebar - element toggle list with reduced size
        FlowLayout elementListSidebar = Containers.verticalFlow(Sizing.fixed(sidebarWidth), Sizing.fill(100));
        elementListSidebar.surface((context, component) -> {
            // Sidebar background - matches ClickGui
            context.fill(component.x(), component.y(), component.x() + component.width(), component.y() + component.height(), ClickGUI.hudEditorSidebarBackground.value().argb);
        });
        elementListSidebar.padding(Insets.of(12, 10, 12, 10)); // Compact padding
        elementListSidebar.gap(8); // Tighter spacing
        
        // "HUD ELEMENTS" label - larger and brighter for hierarchy
        FlowLayout elementsLabelContainer = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        elementsLabelContainer.surface((context, component) -> {
            String text = "HUD ELEMENTS";
            int x = component.x() + 5;
            int y = component.y() + 8;
            // Single clean text - no triple draw
            RenderColor labelColor = ClickGUI.hudEditorSidebarText.value();
            int textColor = RenderColor.fromFloat(labelColor.r, labelColor.g, labelColor.b, 0.5f).argb;
            context.drawText(mc.textRenderer, text, x, y, textColor, false);
        });
        elementsLabelContainer.margins(Insets.of(8, 5, 15, 5));
        elementsLabelContainer.verticalSizing(Sizing.fixed(18));
        elementListSidebar.child(elementsLabelContainer);
        
        // Separator line under label
        FlowLayout separator = Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(1));
        separator.surface((context, component) -> {
            // Subtle separator
            context.fill(component.x(), component.y(), component.x() + component.width(), component.y() + component.height(), 0x20ffffff);
        });
        separator.margins(Insets.bottom(8));
        elementListSidebar.child(separator);
        
        // Create toggle buttons for each HUD element
        FlowLayout elementScrollContent = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        elementScrollContent.gap(6);
        
        for (HudElement element : HudManager.getElements()) {
            // Element label
            PlainLabel label = new PlainLabel(element.elementLabel);
            int labelColor = element.isActive() ? ClickGUI.hudEditorElementEnabledText.value().argb : ClickGUI.hudEditorElementDisabledText.value().argb;
            label.color(Color.ofRgb(labelColor & 0xFFFFFF));
            label.verticalTextAlignment(VerticalAlignment.CENTER);
            label.horizontalSizing(Sizing.fill(100));
            
            // Settings button (added before toggle, conditionally shown)
            ButtonComponent settingsBtn = Components.button(Text.empty(), btn -> {
                mc.setScreen(element.getBaseSettings());
            });
            settingsBtn.horizontalSizing(Sizing.fixed(24));
            settingsBtn.verticalSizing(Sizing.fixed(24));
            settingsBtn.margins(Insets.left(6));
            settingsBtn.tooltip(Text.literal("Settings for " + element.elementLabel.getString()));
            settingsBtn.renderer((context, btn, delta) -> {
                if (btn.isHovered()) {
                    RenderColor accentColor = ClickGUI.hudEditorTabActiveAccent.value();
                    int hoverBg = RenderColor.fromFloat(accentColor.r, accentColor.g, accentColor.b, 0.125f).argb;
                    Rendering.drawRoundedRect(context, btn.getX(), btn.getY(), btn.getWidth(), btn.getHeight(), 4, hoverBg);
                }
                // Draw "S" icon
                int textX = btn.getX() + (btn.getWidth() - mc.textRenderer.getWidth("S")) / 2;
                int textY = btn.getY() + (btn.getHeight() - mc.textRenderer.fontHeight) / 2;
                context.drawText(mc.textRenderer, "S", textX, textY, ClickGUI.hudEditorTabInactiveText.value().argb, false);
            });
            
            // Modern toggle switch (visual indicator only, row handles clicks)
            ToggleButton toggle = new ToggleButton(element.isActive());
            toggle.horizontalSizing(Sizing.fixed(40));
            toggle.verticalSizing(Sizing.fixed(20));
            
            // Create element row with toggle switch - make it a clickable container
            FlowLayout elementItem = new FlowLayout(Sizing.fill(100), Sizing.fixed(32), FlowLayout.Algorithm.HORIZONTAL) {
                @Override
                public boolean onMouseDown(Click click, boolean doubled) {
                    if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                        // Toggle the element
                        boolean newState = !element.instance.isActive();
                        element.instance.setActive(newState);
                        int newLabelColor = newState ? ClickGUI.hudEditorElementEnabledText.value().argb : ClickGUI.hudEditorElementDisabledText.value().argb;
                        label.color(Color.ofRgb(newLabelColor & 0xFFFFFF));
                        
                        // Add or remove element from canvas
                        if (newState) {
                            if (!root.children().contains(element)) {
                                root.child(element);
                            }
                            // Add settings button when enabled
                            if (!this.children().contains(settingsBtn)) {
                                this.child(1, settingsBtn); // Insert at position 1 (after label)
                            }
                        } else {
                            root.removeChild(element);
                            // Remove settings button when disabled
                            this.removeChild(settingsBtn);
                        }
                        
                        // Update toggle button visual state
                        toggle.setToggle(newState);
                        return true;
                    }
                    return super.onMouseDown(click, doubled);
                }
            };
            elementItem.padding(Insets.of(6, 10, 6, 10));
            elementItem.gap(10);
            elementItem.verticalAlignment(VerticalAlignment.CENTER);
            
            elementItem.child(label);
            
            // Only add settings button if element is enabled
            if (element.isActive()) {
                elementItem.child(settingsBtn);
            }
            
            elementItem.child(toggle);
            
            elementItem.surface((context, component) -> {
                boolean isHovered = component.isInBoundingBox(this.mouseX, this.mouseY);
                boolean isVisible = element.isActive();
                
                int bgColor;
                RenderColor enabledBg = ClickGUI.hudEditorElementEnabledBackground.value();
                RenderColor disabledBg = ClickGUI.hudEditorElementDisabledBackground.value();
                
                if (isVisible) {
                    bgColor = isHovered ? ClickGUI.hudEditorTabActiveBackground.value().argb : enabledBg.argb;
                } else {
                    bgColor = isHovered ? enabledBg.argb : disabledBg.argb;
                }
                
                Rendering.drawRoundedRect(context, component.x(), component.y(), component.width(), component.height(), 4, bgColor);
                
                if (isVisible) {
                    // Subtle border for enabled state
                    RenderColor borderColor = ClickGUI.hudEditorElementBorder.value();
                    int subtleBorder = RenderColor.fromFloat(borderColor.r, borderColor.g, borderColor.b, 0.19f).argb;
                    Rendering.drawRoundedBorder(context, component.x(), component.y(), component.width(), component.height(), 4, subtleBorder, 1);
                }
            });
            
            elementScrollContent.child(elementItem);
        }
        
        ScrollContainer<FlowLayout> elementScroll = Containers.verticalScroll(Sizing.fill(100), Sizing.fill(100), elementScrollContent);
        elementScroll.scrollbarThiccness(4).scrollbar(ScrollContainer.Scrollbar.flat(Color.ofArgb(ClickGUI.hudEditorScrollbar.value().argb)));
        elementListSidebar.child(elementScroll);
        
        contentArea.child(elementListSidebar);
        
        // Right side - instructions and preview hint with explicit width constraint
        FlowLayout instructionsArea = Containers.verticalFlow(Sizing.fixed(contentAreaWidth + horizontalPadding), Sizing.fill(100));
        instructionsArea.allowOverflow(false);
        instructionsArea.padding(Insets.of(24, 24, 24, 24)); // More compact padding
        instructionsArea.surface((context, component) -> {
            // Draw subtle grid lines with reduced opacity
            int gridSpacing = 40;
            int gridColor = ClickGUI.hudEditorGridColor.value().argb;
            
            // Vertical lines
            for (int x = component.x(); x < component.x() + component.width(); x += gridSpacing) {
                context.fill(x, component.y(), x + 1, component.y() + component.height(), gridColor);
            }
            
            // Horizontal lines
            for (int y = component.y(); y < component.y() + component.height(); y += gridSpacing) {
                context.fill(component.x(), y, component.x() + component.width(), y + 1, gridColor);
            }
                        // CENTER GUIDES - More visible than grid
            int centerX = component.x() + component.width() / 2;
            int centerY = component.y() + component.height() / 2;
            RenderColor accentColor = ClickGUI.hudEditorCenterlineColor.value();
            int centerColor = RenderColor.fromFloat(accentColor.r, accentColor.g, accentColor.b, 0.19f).argb;
            
            // Vertical centerline
            context.fill(centerX, component.y(), centerX + 1, component.y() + component.height(), centerColor);
            
            // Horizontal centerline
            context.fill(component.x(), centerY, component.x() + component.width(), centerY + 1, centerColor);
                        // Safe-zone outline (10% margin)
            int safeMargin = (int)(component.width() * 0.1);
            int safeX1 = component.x() + safeMargin;
            int safeY1 = component.y() + safeMargin;
            int safeX2 = component.x() + component.width() - safeMargin;
            int safeY2 = component.y() + component.height() - safeMargin;
            
            // Draw safe zone border with dashed effect
            int dashLength = 8;
            int gapLength = 4;
            int safeColor = 0x405555ff;
            
            // Top border
            for (int x = safeX1; x < safeX2; x += dashLength + gapLength) {
                int endX = Math.min(x + dashLength, safeX2);
                context.fill(x, safeY1, endX, safeY1 + 1, safeColor);
            }
            // Bottom border
            for (int x = safeX1; x < safeX2; x += dashLength + gapLength) {
                int endX = Math.min(x + dashLength, safeX2);
                context.fill(x, safeY2, endX, safeY2 + 1, safeColor);
            }
            // Left border
            for (int y = safeY1; y < safeY2; y += dashLength + gapLength) {
                int endY = Math.min(y + dashLength, safeY2);
                context.fill(safeX1, y, safeX1 + 1, endY, safeColor);
            }
            // Right border
            for (int y = safeY1; y < safeY2; y += dashLength + gapLength) {
                int endY = Math.min(y + dashLength, safeY2);
                context.fill(safeX2, y, safeX2 + 1, endY, safeColor);
            }
        });
        instructionsArea.padding(Insets.of(30, 20, 30, 20));
        instructionsArea.gap(15);
        instructionsArea.verticalAlignment(VerticalAlignment.CENTER);
        instructionsArea.horizontalAlignment(HorizontalAlignment.CENTER);
        
        // Contextual title based on whether elements are enabled
        int enabledCount = (int) HudManager.getElements().stream().filter(HudElement::isActive).count();
        String contextTitle = enabledCount == 0 ? "Enable elements to begin" : 
                             "Drag elements to position (" + enabledCount + " enabled)";
        
        PlainLabel title = new PlainLabel(Text.literal(contextTitle));
        title.color(Color.ofRgb(ClickGUI.hudEditorTabActiveText.value().argb & 0xFFFFFF));
        title.horizontalTextAlignment(HorizontalAlignment.CENTER);
        title.horizontalSizing(Sizing.fill(100));
        title.margins(Insets.bottom(8));
        instructionsArea.child(title);
        
        // Only show instruction box if no elements are enabled
        if (enabledCount == 0) {
            FlowLayout instructionBox = Containers.verticalFlow(Sizing.fixed(350), Sizing.content());
            instructionBox.surface((context, component) -> {
                Rendering.drawRoundedRect(context, component.x(), component.y(), component.width(), component.height(), 8, ClickGUI.hudEditorElementEnabledBackground.value().argb);
                Rendering.drawRoundedBorder(context, component.x(), component.y(), component.width(), component.height(), 8, 0x30ffffff, 1);
            });
            instructionBox.padding(Insets.of(24));
            instructionBox.gap(12);
            
            // Step-by-step instructions
            String[][] instructions = {
                {"1.", "Enable elements using toggles →"},
                {"2.", "Drag them into position"},
                {"3.", "Click S for element settings"}
            };
            
            for (String[] instruction : instructions) {
                FlowLayout instrRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
                instrRow.gap(12);
                instrRow.verticalAlignment(VerticalAlignment.CENTER);
                
                // Number/icon with accent color
                PlainLabel iconLabel = new PlainLabel(Text.literal(instruction[0]));
                iconLabel.color(Color.ofRgb(ClickGUI.hudEditorTabActiveAccent.value().argb & 0xFFFFFF));
                iconLabel.horizontalSizing(Sizing.fixed(24));
                instrRow.child(iconLabel);
                
                // Instruction text
                PlainLabel instrLabel = new PlainLabel(Text.literal(instruction[1]));
                instrLabel.color(Color.ofRgb(ClickGUI.hudEditorTabInactiveText.value().argb & 0xFFFFFF));
                instrRow.child(instrLabel);
                
                instructionBox.child(instrRow);
            }
            
            instructionsArea.child(instructionBox);
        } else {
            // Show helpful tip when elements are enabled
            FlowLayout tipBox = Containers.verticalFlow(Sizing.fixed(300), Sizing.content());
            tipBox.surface((context, component) -> {
                RenderColor accentColor = ClickGUI.hudEditorTabActiveAccent.value();
                int tipBg = RenderColor.fromFloat(accentColor.r, accentColor.g, accentColor.b, 0.125f).argb;
                Rendering.drawRoundedRect(context, component.x(), component.y(), component.width(), component.height(), 6, tipBg);
            });
            tipBox.padding(Insets.of(12));
            tipBox.gap(6);
            
            PlainLabel tip = new PlainLabel(Text.literal("💡 Tip: No"));
            tip.color(Color.ofRgb(ClickGUI.hudEditorTabActiveText.value().argb & 0xFFFFFF));
            tip.horizontalTextAlignment(HorizontalAlignment.CENTER);
            tip.horizontalSizing(Sizing.fill(100));
            tipBox.child(tip);
            
            instructionsArea.child(tipBox);
        }
        
        contentArea.child(instructionsArea);
        
        mainContainer.child(contentArea);
        root.child(mainContainer);
        
        // Add HUD elements directly to root for dragging
        for (HudElement element : HudManager.getElements()) {
            root.child(element);
        }
        
        HudManager.armorElement.updateArmor();
    }
    
    private int interpolateColor(int color1, int color2, float ratio) {
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        
        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        
        int a = (int) (a1 + (a2 - a1) * ratio);
        int r = (int) (r1 + (r2 - r1) * ratio);
        int g = (int) (g1 + (g2 - g1) * ratio);
        int b = (int) (b1 + (b2 - b1) * ratio);
        
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        if (ClickGUI.backgroundBlur.value()) {
            int opacity = ClickGUI.darkeningOpacity.value();
            BlurEffect.applyBlur(context, this.width, this.height, opacity);
        } else {
            super.renderBackground(context, mouseX, mouseY, delta);
        }
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        for (HudElement element : HudManager.getElements()) {
            if (element.isActive()) element.updatePosition();
        }
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (this.uiAdapter == null) {
            return false;
        }
        // Remove right-click menu - all toggles are in sidebar now
        return this.uiAdapter.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            for (HudElement element : HudManager.getElements()) {
                if (element.toggling && element.isActive()) {
                    element.toggling = false;
                    element.toggle();
                    return true;
                }
            }
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        for (HudElement element : HudManager.getElements()) {
            if (element.toggling && element.isActive()) {
                element.toggling = false;
            }
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public void close() {
        if (AutoSave.instance.isActive()) AutoSave.save();
        if (this.uiAdapter != null) {
            this.uiAdapter.dispose();
        }
        BlurEffect.cleanup();
        super.close();
    }
}