package nexus.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import nexus.misc.EntityRendering;
import nexus.misc.RenderColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public abstract class EntityMixin implements EntityRendering {
    @Shadow
    private Vec3d pos;
    @Unique
    private boolean glowRender = false;
    @Unique
    private RenderColor glowColor;

    @Shadow
    public abstract boolean isPlayer();

    @Override
    public void nexus_mod$setGlowingColored(boolean glowing, RenderColor color) {
        glowRender = glowing;
        glowColor = color;
    }

    @Override
    public boolean nexus_mod$getGlowing() {
        return glowRender;
    }

    @ModifyReturnValue(method = "isGlowing", at = @At("RETURN"))
    private boolean isGlowing(boolean original) {
        if (glowRender) {
            return true;
        }
        return original;
    }

    @ModifyReturnValue(method = "getTeamColorValue", at = @At("RETURN"))
    private int getTeamColorValue(int original) {
        if (glowRender) {
            return glowColor.hex;
        }
        return original;
    }
}
