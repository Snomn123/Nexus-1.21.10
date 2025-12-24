package nexus.mixin;

import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DrawContext.class)
public abstract class DrawContextMixin {
    @Shadow
    @Final
    private Matrix3x2fStack matrices;
}
