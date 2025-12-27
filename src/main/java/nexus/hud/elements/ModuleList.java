package nexus.hud.elements;

import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.text.Text;
import nexus.config.Feature;
import nexus.config.SettingBool;
import nexus.config.SettingInt;
import nexus.features.combat.*;
import nexus.features.general.*;
import nexus.features.misc.*;
import nexus.features.render.*;
import nexus.hud.HudElement;
import nexus.hud.clickgui.Settings;
import nexus.hud.clickgui.components.PlainLabel;

import java.util.*;

import static nexus.Main.mc;

public class ModuleList extends HudElement {
    private final SettingInt gap = new SettingInt(5, "gap", this.instance);
    private final SettingBool shadow = new SettingBool(true, "shadow", this.instance);
    private final SettingBool line = new SettingBool(true, "line", this.instance);
    private final SettingBool lowercase = new SettingBool(false, "lowercase", this.instance);
    private final FlowLayout content;
    private final List<Feature> allFeatures = new ArrayList<>();

    public ModuleList() {
        super(new Feature("moduleList"), "Module List");
        this.content = Containers.verticalFlow(Sizing.content(), Sizing.content());
        this.content.gap(this.gap.value());
        this.content.horizontalAlignment(HorizontalAlignment.RIGHT);
        this.layout.child(this.content);

        // Collect all features dynamically
        collectFeatures();

        this.options = this.getBaseSettings(List.of(
            new Settings.Separator("Module Display"),
            new Settings.SliderInt("Module Gap", 1, 20, 1, gap, "Gap between modules."),
            new Settings.Toggle("Text Shadow", shadow, "Add shadow to module text."),
            new Settings.Toggle("Line Indicator", line, "Show a line before each module name."),
            new Settings.Toggle("Lowercase", lowercase, "Display module names in lowercase.")
        ));
    }

    @Override
    public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
        if (this.shouldRender()) {
            super.draw(context, mouseX, mouseY, partialTicks, delta);
        }
    }

    private void collectFeatures() {
        // Manually register all features
        // Combat features
        allFeatures.add(KillAura.instance);
        // General features
        allFeatures.add(AutoSprint.instance);
        allFeatures.add(CustomKeybinds.instance);
        
        // Misc features
        allFeatures.add(AutoSave.instance);
        allFeatures.add(UpdateChecker.instance);
        allFeatures.add(namesOnly.instance);

        // Render features
        allFeatures.add(ClickGUI.instance);
        allFeatures.add(ESP.instance);
        allFeatures.add(Fullbright.instance);
        allFeatures.add(PestESP.instance);
    }

    private String formatModuleName(String key) {
        String formatted = key.replaceAll("([A-Z]+)([A-Z][a-z])", "$1 $2")
                            .replaceAll("([a-z])([A-Z])", "$1 $2");
        
        // Capitalize first letter of each word
        String[] words = formatted.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                if (!result.isEmpty()) {
                    result.append(" ");
                }
            result.append(this.lowercase.value() 
                    ? word.toLowerCase() 
                    : Character.toUpperCase(word.charAt(0)) + word.substring(1));
            }
        }
        
        return result.toString();
    }

    public void updateModuleList() {
        this.content.clearChildren();
        this.content.gap(this.gap.value());

        // Get active features and sort them by rendered width (longest to shortest)
        List<Feature> activeFeatures = allFeatures.stream()
                .filter(Feature::isActive)
                .sorted(Comparator.comparingInt((Feature f) -> {
                    String name = formatModuleName(f.key());
                    return mc.textRenderer.getWidth(name);
                }).reversed())
                .toList();

        for (Feature feature : activeFeatures) {
            FlowLayout moduleRow = Containers.horizontalFlow(Sizing.content(), Sizing.content());
            
            // Add line indicator if enabled
            if (this.line.value()) {
                LabelComponent lineLabel = new PlainLabel(Text.literal("│").withColor(0x5865f2));
                lineLabel.shadow(false);
                lineLabel.margins(Insets.right(3));
                moduleRow.child(lineLabel);
            }

            // Add module name
            String moduleName = formatModuleName(feature.key());
            LabelComponent nameLabel = new PlainLabel(Text.literal(moduleName).withColor(0x88aaff));
            nameLabel.shadow(this.shadow.value());
            moduleRow.child(nameLabel);

            this.content.child(moduleRow);
        }

        if (activeFeatures.isEmpty()) {
            LabelComponent emptyLabel = new PlainLabel(Text.literal("No active modules").withColor(0x888888));
            emptyLabel.shadow(this.shadow.value());
            this.content.child(emptyLabel);
        }
    }
}
