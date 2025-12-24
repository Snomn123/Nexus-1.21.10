package nexus.hud.elements;

import io.wispforest.owo.ui.core.OwoUIDrawContext;
import net.minecraft.text.Text;
import nexus.config.Feature;
import nexus.config.SettingBool;
import nexus.hud.SimpleTextElement;
import nexus.hud.clickgui.Settings;
import nexus.misc.Utils;

import java.util.ArrayList;
import java.util.List;

public class TPS extends SimpleTextElement {
    public final SettingBool average = new SettingBool(false, "average", instance.key());
    public int clientTicks = 20;
    public int serverTicks = 0;
    public List<Integer> tpsList = new ArrayList<>();

    public TPS(String text) {
        super(Text.literal(text), new Feature("tpsElement"), "TPS Element");
        this.options = this.getBaseSettings(List.of(
                new Settings.Toggle("Average", average, "Tracks and adds the average TPS to the element.")
        ));
    }

    @Override
    public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
        if (this.shouldRender()) {
            super.draw(context, mouseX, mouseY, partialTicks, delta);
        }
    }

    public void setTps(int tps) {
        if (average.value()) {
            if (this.tpsList.size() > 30) {
                this.tpsList.removeFirst();
            }
            this.tpsList.add(Math.clamp(tps, 0, 20));
            int avg = 0;
            for (int previous : this.tpsList) {
                avg += previous;
            }
            this.setText(Utils.format("TPS: §f{} §7{}", tps, Utils.formatDecimal(avg / (double) tpsList.size())));
        } else {
            this.setText(Utils.format("TPS: §f{}", tps));
        }
    }

    public void reset() {
        this.clientTicks = 20;
        this.serverTicks = 0;
        this.tpsList.clear();
        this.setText("TPS: §f0");
    }
}
