package nexus.misc;

public interface TitleRendering {
    void nexus_mod$setRenderTitle(String title, int stayTicks, int yOffset, float scale, RenderColor color);

    boolean nexus_mod$isRenderingTitle();
}