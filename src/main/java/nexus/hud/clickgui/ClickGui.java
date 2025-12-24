package nexus.hud.clickgui;

import com.google.common.collect.Lists;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import nexus.features.general.*;
import nexus.features.misc.*;
import nexus.features.render.*;
import nexus.hud.HudEditorScreen;
import nexus.hud.clickgui.components.PlainLabel;
import nexus.misc.BlurEffect;
import nexus.misc.KeybindBinding;
import nexus.misc.Rendering;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.List;

import static nexus.Main.mc;

public class ClickGui extends BaseOwoScreen<FlowLayout> {
    public List<Category> categories;
    public ScrollContainer<FlowLayout> mainScroll;
    public int mouseX = 0;
    public int mouseY = 0;
    public Category selectedCategory;
    public FlowLayout mainContentArea;
    public int contentAreaWidth; // Explicit width for content area
    private String activeTab = "NEXUS";

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        ClickGUI.instance.setActive(true);
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        if (ClickGUI.backgroundBlur.value()) {
            // Apply darkening overlay with configurable opacity
            int opacity = ClickGUI.darkeningOpacity.value();
            BlurEffect.applyBlur(context, this.width, this.height, opacity);
        } else {
            super.renderBackground(context, mouseX, mouseY, delta);
        }
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        // If any KeybindButton is waiting for input, bind it first
        if (forwardBinding(input.key())) {
            return true;
        }
        if (input.key() != GLFW.GLFW_KEY_LEFT && input.key() != GLFW.GLFW_KEY_RIGHT && input.key() != GLFW.GLFW_KEY_PAGE_DOWN && input.key() != GLFW.GLFW_KEY_PAGE_UP) {
            return super.keyPressed(input);
        } else {
            return this.mainScroll.onMouseScroll(0, 0, input.key() == GLFW.GLFW_KEY_PAGE_UP ? 4 : -4);
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
        // Forward mouse buttons to any waiting KeybindButton
        if (forwardBinding(click.button())) {
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // Always try to scroll the main container first, regardless of mouse position
        // This ensures scrolling works even when hovering over collapsed modules
        boolean handled = this.mainScroll.onMouseScroll(mouseX, mouseY, verticalAmount * 2);
        if (handled) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }

    private void switchCategory(Category category) {
        this.selectedCategory = category;
        this.mainContentArea.clearChildren();
        
        // Update category header
        FlowLayout categoryHeader = this.uiAdapter.rootComponent.childById(FlowLayout.class, "category-header");
        if (categoryHeader != null) {
            categoryHeader.clearChildren();
            
            // Clean minimalist title - left-aligned with content
            FlowLayout titleRow = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
            titleRow.gap(6);
            
            // Large bold title - left-aligned, not floating
            PlainLabel categoryTitle = new PlainLabel(Text.literal(category.categoryName.toUpperCase()));
            categoryTitle.color(Color.ofRgb(0xE6E8EE)); // Brighter text
            categoryTitle.shadow(true);
            // No centering - left-aligned with content below
            titleRow.child(categoryTitle);
            
            categoryHeader.child(titleRow);
            
            // Minimal separator with subtle section background
            FlowLayout separator = Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(1));
            separator.margins(Insets.top(10));
            separator.surface((context, component) -> {
                // Single subtle line - no gradient
                context.fill(component.x(), component.y(), component.x() + component.width(), component.y() + component.height(), 0x20ffffff);
            });
            categoryHeader.child(separator);
        }
        
        // Add modules
        for (Module module : category.features) {
            module.horizontalSizing(Sizing.fixed(this.contentAreaWidth)); // Explicit width constraint
            this.mainContentArea.child(module);
        }
    }

    // Attempt to bind inside the currently selected category first, then fall back to all categories
    private boolean forwardBinding(int keyOrButton) {
        // Prefer currently selected category if present
        if (this.selectedCategory != null) {
            for (Module module : this.selectedCategory.features) {
                if (module.settingsContainer != null && KeybindBinding.forwardInLayout(module.settingsContainer, keyOrButton)) {
                    return true;
                }
            }
        }
        // Fallback: scan all categories
        if (this.categories != null) {
            for (Category category : this.categories) {
                for (Module module : category.features) {
                    if (module.settingsContainer != null && KeybindBinding.forwardInLayout(module.settingsContainer, keyOrButton)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    protected void build(FlowLayout root) {
        root.surface(Surface.VANILLA_TRANSLUCENT);
        
        // Initialize categories
        this.categories = Lists.newArrayList(
                new Category("General", List.of(
                        new Module("Auto Sprint", AutoSprint.instance, new Settings(List.of(
                                new Settings.Toggle("Water Check", AutoSprint.waterCheck, "Prevents Auto Sprint from working while you are in water.")
                        ))),
                        new Module("Custom Keybinds", CustomKeybinds.instance, CustomKeybinds.buildSettings())
                )),
                new Category("Misc", List.of(
                        new Module("Update Checker", UpdateChecker.instance, new Settings(List.of(
                                new Settings.Toggle("Notify on Join", UpdateChecker.notifyOnJoin, "Notifies you in chat when joining a server if there is an update available.")
                        ))),
                        new Module("Auto Save", AutoSave.instance)
                )),
                new Category("Render", List.of(
                        new Module("Click GUI", ClickGUI.instance, new Settings(List.of(
                                new Settings.Keybind("Open Keybind", ClickGUI.openKey, "The keybind used to open the Click GUI."),
                                new Settings.Separator("Background Effects"),
                                new Settings.Toggle("Background Blur", ClickGUI.backgroundBlur, "Enables a darkening effect on the background of the Click GUI."),
                                new Settings.SliderInt("Darkening Opacity", 0, 255, 5, ClickGUI.darkeningOpacity, "The opacity of the darkening overlay (0=transparent, 255=fully opaque)."),
                                new Settings.Separator("HUD Editor Colors"),
                                new Settings.ColorPicker("Main Background", false, ClickGUI.hudEditorMainBackground, "HUD Editor main container background."),
                                new Settings.ColorPicker("Main Border", false, ClickGUI.hudEditorMainBorder, "HUD Editor main container border."),
                                new Settings.ColorPicker("Active Tab Background", false, ClickGUI.hudEditorTabActiveBackground, "Active tab background color."),
                                new Settings.ColorPicker("Active Tab Text", false, ClickGUI.hudEditorTabActiveText, "Active tab text color."),
                                new Settings.ColorPicker("Active Tab Accent", false, ClickGUI.hudEditorTabActiveAccent, "Active tab accent/underline color."),
                                new Settings.ColorPicker("Inactive Tab Text", false, ClickGUI.hudEditorTabInactiveText, "Inactive tab text color."),
                                new Settings.ColorPicker("Sidebar Background", false, ClickGUI.hudEditorSidebarBackground, "Element list sidebar background."),
                                new Settings.ColorPicker("Sidebar Text", false, ClickGUI.hudEditorSidebarText, "Sidebar label text color."),
                                new Settings.ColorPicker("Element Enabled Background", false, ClickGUI.hudEditorElementEnabledBackground, "Enabled element row background."),
                                new Settings.ColorPicker("Element Disabled Background", false, ClickGUI.hudEditorElementDisabledBackground, "Disabled element row background."),
                                new Settings.ColorPicker("Element Enabled Text", false, ClickGUI.hudEditorElementEnabledText, "Enabled element text color."),
                                new Settings.ColorPicker("Element Disabled Text", false, ClickGUI.hudEditorElementDisabledText, "Disabled element text color."),
                                new Settings.ColorPicker("Element Border", false, ClickGUI.hudEditorElementBorder, "Enabled element border accent."),
                                new Settings.ColorPicker("Scrollbar", false, ClickGUI.hudEditorScrollbar, "Scrollbar color."),
                                new Settings.ColorPicker("Grid Color", true, ClickGUI.hudEditorGridColor, "Background grid line color."),
                                new Settings.ColorPicker("Centerline Color", false, ClickGUI.hudEditorCenterlineColor, "Center guide line color.")
                        ))),
                        new Module("Fullbright", Fullbright.instance, new Settings(List.of(
                                new Settings.Dropdown<>("Mode", Fullbright.mode, "The lighting mode used by fullbright."),
                                new Settings.Toggle("No Effect", Fullbright.noEffect, "Removes the Night Vision effect while active. Ignored if you use the Potion mode.")
                        )))
                ))
        );
        
        // Calculate responsive container size - smaller, more compact
        int containerWidth = (int)(this.width * 0.70);
        int containerHeight = (int)(this.height * 0.70);
        containerWidth = Math.min(containerWidth, 1100); // Reduced max width
        containerHeight = Math.min(containerHeight, 700); // Reduced max height
        
        // Calculate explicit content area width to constrain modules
        int sidebarWidth = 160; // Reduced from 200px
        int horizontalPadding = 32; // Reduced from 40px
        this.contentAreaWidth = containerWidth - sidebarWidth - horizontalPadding;
        
        // Main container - dark blue background
        FlowLayout mainContainer = Containers.verticalFlow(Sizing.fixed(containerWidth), Sizing.fixed(containerHeight));
        mainContainer.surface((context, component) -> {
            // Modern neutral background
            Rendering.drawRoundedRect(context, component.x(), component.y(), component.width(), component.height(), 8, 0xFF12141A);
            // Very subtle border
            Rendering.drawRoundedBorder(context, component.x(), component.y(), component.width(), component.height(), 8, 0x30ffffff, 1);
        });
        mainContainer.positioning(Positioning.relative(50, 50));
        mainContainer.gap(0);
        
        // Top header with NEXUS / CLIENT tabs
        FlowLayout topHeader = Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(50));
        topHeader.surface((context, component) -> {
            // Gradient background
            int startColor = 0xFF0d0d28;
            int endColor = 0xFF1a1a3e;
            for (int i = 0; i < component.height(); i++) {
                float ratio = (float) i / component.height();
                int color = interpolateColor(startColor, endColor, ratio);
                context.fill(component.x(), component.y() + i, component.x() + component.width(), component.y() + i + 1, color);
            }
            // Bottom border
            context.fill(component.x(), component.y() + component.height() - 1, component.x() + component.width(), component.y() + component.height(), 0xFF2a2a5e);
        });
        topHeader.padding(Insets.of(10, 15, 10, 15));
        topHeader.gap(0);
        
        // Nexus tab
        ButtonComponent nexusTab = Components.button(Text.empty(), btn -> {
            // Already on Nexus, do nothing
        });
        nexusTab.horizontalSizing(Sizing.fixed(110));
        nexusTab.verticalSizing(Sizing.fill(100));
        nexusTab.renderer((context, button, delta) -> {
            boolean isHovered = button.isInBoundingBox(this.mouseX, this.mouseY);
            
            // Active tab styling (always active when on this screen)
            Rendering.drawRoundedRect(context, button.x(), button.y() + 5, button.width(), button.height() - 5, 4, 0xFF2a2a5e);
            Rendering.drawRoundedBorder(context, button.x(), button.y() + 5, button.width(), button.height() - 5, 4, 0xFF4a4aff, 1);
            context.fill(button.x() + 2, button.y() + button.height() - 3, button.x() + button.width() - 2, button.y() + button.height(), 0xFF5a5aff);
            
            // Text with shadow for depth
            int textX = button.x() + (button.width() - mc.textRenderer.getWidth("Nexus")) / 2;
            int textY = button.y() + (button.height() - 8) / 2;
            context.drawText(mc.textRenderer, "Nexus", textX + 1, textY + 1, 0x88000000, false);
            context.drawText(mc.textRenderer, "Nexus", textX, textY, 0xFFffffff, false);
        });
        topHeader.child(nexusTab);
        
        // HUD Editor tab
        ButtonComponent hudEditorTab = Components.button(Text.empty(), btn -> {
            mc.setScreen(new HudEditorScreen());
        });
        hudEditorTab.horizontalSizing(Sizing.fixed(110));
        hudEditorTab.verticalSizing(Sizing.fill(100));
        hudEditorTab.margins(Insets.left(5));
        hudEditorTab.renderer((context, button, delta) -> {
            boolean isHovered = button.isInBoundingBox(this.mouseX, this.mouseY);
            
            if (isHovered) {
                Rendering.drawRoundedRect(context, button.x(), button.y() + 5, button.width(), button.height() - 5, 4, 0xFF1a1a3e);
                Rendering.drawRoundedBorder(context, button.x(), button.y() + 5, button.width(), button.height() - 5, 4, 0xFF3a3a6a, 1);
            }
            
            int textX = button.x() + (button.width() - mc.textRenderer.getWidth("HUD Editor")) / 2;
            int textY = button.y() + (button.height() - 8) / 2;
            int textColor = isHovered ? 0xFFffffff : 0xFF8888aa;
            
            if (isHovered) {
                context.drawText(mc.textRenderer, "HUD Editor", textX + 1, textY + 1, 0x44000000, false);
            }
            context.drawText(mc.textRenderer, "HUD Editor", textX, textY, textColor, false);
        });
        topHeader.child(hudEditorTab);
        
        mainContainer.child(topHeader);
        
        // Main content area - horizontal split
        FlowLayout mainContent = Containers.horizontalFlow(Sizing.fill(100), Sizing.fill(100));
        mainContent.id("main-content");
        mainContent.gap(0);
        
        // Left sidebar - slim modern design
        FlowLayout sidebar = Containers.verticalFlow(Sizing.fixed(sidebarWidth), Sizing.fill(100));
        sidebar.surface((context, component) -> {
            // Slightly darker than main content
            context.fill(component.x(), component.y(), component.x() + component.width(), component.y() + component.height(), 0xFF0F1117);
            // No visible border - use spacing for separation
        });
        sidebar.padding(Insets.of(16, 10, 16, 10)); // More compact padding
        sidebar.gap(10); // Tighter spacing
        
        // "CATEGORIES" label with enhanced styling - larger and brighter
        // Simplified "CATEGORIES" label - subtle and clean
        FlowLayout categoryLabelContainer = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        categoryLabelContainer.surface((context, component) -> {
            // Single clean text - no triple draw
            String text = "CATEGORIES";
            int x = component.x() + 5;
            int y = component.y() + 8;
            context.drawText(mc.textRenderer, text, x, y, 0x809AA0B2, false); // Muted secondary color
        });
        categoryLabelContainer.margins(Insets.of(8, 5, 15, 5));
        categoryLabelContainer.verticalSizing(Sizing.fixed(18));
        sidebar.child(categoryLabelContainer);
        
        // Remove heavy separator - let spacing breathe
        
        // Category buttons
        for (Category category : this.categories) {
            ButtonComponent categoryBtn = Components.button(Text.empty(), btn -> {
                switchCategory(category);
            });
            categoryBtn.horizontalSizing(Sizing.fill(100));
            categoryBtn.verticalSizing(Sizing.fixed(36)); // Reduced from 40px
            categoryBtn.renderer((context, button, delta) -> {
                boolean isSelected = this.selectedCategory == category;
                boolean isHovered = button.isInBoundingBox(this.mouseX, this.mouseY);
                
                if (isSelected) {
                    // Selected: left accent bar + subtle background tint
                    // Clean 3px accent bar
                    context.fill(button.x(), button.y() + 8, button.x() + 3, button.y() + button.height() - 8, 0xFF4F7CFF);
                    
                    // Subtle background tint - no borders
                    Rendering.drawRoundedRect(context, button.x(), button.y(), button.width(), button.height(), 6, 0x20ffffff);
                    
                    
                    // Icon and text with improved shadow
                    String icon = getCategoryIcon(category.categoryName);
                    String fullText = icon + " " + category.categoryName;
                    int textX = button.x() + 15;
                    int textY = button.y() + (button.height() - 8) / 2;
                    
                    // Text shadow for depth
                    context.drawText(mc.textRenderer, fullText, textX + 1, textY + 1, 0x88000000, false);
                    context.drawText(mc.textRenderer, fullText, textX, textY, 0xFFffffff, false);
                    
                    // Right arrow indicator with glow
                    int arrowX = button.x() + button.width() - 20;
                    int arrowY = button.y() + (button.height() - 8) / 2;
                    context.drawText(mc.textRenderer, "▶", arrowX + 1, arrowY + 1, 0x88000000, false);
                    context.drawText(mc.textRenderer, "▶", arrowX, arrowY, 0xFFaaaaff, false);
                } else {
                    // Unselected category
                    if (isHovered) {
                        // Very subtle background on hover - no borders
                        Rendering.drawRoundedRect(context, button.x(), button.y(), button.width(), button.height(), 6, 0x15ffffff);
                    }
                    
                    String icon = getCategoryIcon(category.categoryName);
                    String fullText = icon + " " + category.categoryName;
                    int textX = button.x() + 15;
                    int textY = button.y() + (button.height() - 8) / 2;
                    int textColor = isHovered ? 0xFFdddddd : 0xFF8888aa;
                    
                    if (isHovered) {
                        context.drawText(mc.textRenderer, fullText, textX + 1, textY + 1, 0x44000000, false);
                    }
                    context.drawText(mc.textRenderer, fullText, textX, textY, textColor, false);
                }
            });
            sidebar.child(categoryBtn);
        }
        
        mainContent.child(sidebar);
        
        // Right side - modules list with explicit width constraint
        FlowLayout rightSide = Containers.verticalFlow(Sizing.fixed(this.contentAreaWidth + horizontalPadding), Sizing.fill(100));
        rightSide.gap(0);
        rightSide.padding(Insets.of(20, 16, 20, 16)); // More compact padding
        
        // Category header in main area
        FlowLayout categoryHeader = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        categoryHeader.margins(Insets.bottom(16)); // Reduced from 24px
        categoryHeader.id("category-header");
        rightSide.child(categoryHeader);
        
        // Module content area with card separation
        this.mainContentArea = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        this.mainContentArea.gap(12); // Reduced from 16px
        this.mainContentArea.padding(Insets.bottom(100)); // Add bottom padding to ensure scrolling works
        
        this.mainScroll = Containers.verticalScroll(Sizing.fill(100), Sizing.fill(100), this.mainContentArea);
        this.mainScroll.scrollbarThiccness(3).scrollbar(ScrollContainer.Scrollbar.flat(Color.ofArgb(0xFF5a5aaa)));
        
        rightSide.child(this.mainScroll);
        mainContent.child(rightSide);
        
        mainContainer.child(mainContent);
        root.child(mainContainer);
        
        // Select first category by default
        if (!this.categories.isEmpty()) {
            switchCategory(this.categories.get(0));
        }
    }
    
    private String getCategoryIcon(String categoryName) {
        return switch (categoryName) {
            case "General" -> "✦";
            case "Misc" -> "▶";
            case "Render" -> "◈";
            default -> "●";
        };
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
    public void close() {
        if (AutoSave.instance.isActive()) AutoSave.save();
        if (this.uiAdapter != null) {
            this.uiAdapter.dispose();
        }
        BlurEffect.cleanup(); // Clean up blur framebuffers
        ClickGUI.instance.setActive(false);
        super.close();
    }
}
