package nexus.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.World;
import nexus.features.render.Fullbright;
import nexus.misc.Utils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static nexus.Main.mc;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @ModifyReturnValue(method = "hasStatusEffect", at = @At("RETURN"))
    private boolean hasNightVision(boolean original, RegistryEntry<StatusEffect> effect) {
        if (Fullbright.instance.isActive() && Utils.isSelf(this) && effect == StatusEffects.NIGHT_VISION) {
            if (Fullbright.noEffect.value() && !Fullbright.mode.value().equals(Fullbright.modes.Potion)) {
                return false;
            }
        }
        return original;
    }
}