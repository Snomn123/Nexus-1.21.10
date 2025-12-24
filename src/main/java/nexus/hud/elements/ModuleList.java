package nexus.hud.elements;

import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.text.Text;
import nexus.config.Feature;
import nexus.config.SettingBool;
import nexus.config.SettingInt;
import nexus.hud.HudElement;
import nexus.hud.clickgui.Settings;
import nexus.hud.clickgui.components.PlainLabel;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

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
        // Get all features from their packages
        String[] packages = {"nexus.features.general", "nexus.features.misc", "nexus.features.render"};
        
        for (String packageName : packages) {
            try {
                Class<?>[] classes = getClasses(packageName);
                for (Class<?> clazz : classes) {
                    try {
                        Field instanceField = clazz.getDeclaredField("instance");
                        if (java.lang.reflect.Modifier.isStatic(instanceField.getModifiers()) && 
                            Feature.class.isAssignableFrom(instanceField.getType())) {
                            instanceField.setAccessible(true);
                            Feature feature = (Feature) instanceField.get(null);
                            if (feature != null) {
                                allFeatures.add(feature);
                            }
                        }
                    } catch (Exception e) {
                        // No instance field or not accessible
                    }
                }
            } catch (Exception e) {
                // Package not found or not accessible
            }
        }
    }

    private Class<?>[] getClasses(String packageName) throws Exception {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String path = packageName.replace('.', '/');
        URL resource = classLoader.getResource(path);
        if (resource == null) {
            return new Class<?>[0];
        }
        
        File directory = new File(resource.getFile());
        if (!directory.exists()) {
            return new Class<?>[0];
        }
        
        File[] files = directory.listFiles();
        if (files == null) {
            return new Class<?>[0];
        }
        
        List<Class<?>> classes = new ArrayList<>();
        for (File file : files) {
            if (file.getName().endsWith(".class")) {
                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                try {
                    classes.add(Class.forName(className));
                } catch (ClassNotFoundException e) {
                    // Skip
                }
            }
        }
        return classes.toArray(new Class<?>[0]);
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
