package nexus.hud.clickgui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Category {
    public List<Module> features;
    public String categoryName;

    protected Category(String title, List<Module> children) {
        this.categoryName = title;
        this.features = new ArrayList<>(children);
        this.features.sort(Comparator.comparing(module -> module.name));
    }
}
