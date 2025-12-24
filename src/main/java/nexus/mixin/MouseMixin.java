package nexus.mixin;

import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import nexus.events.InputEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static nexus.Main.eventBus;

@Mixin(Mouse.class)
public abstract class MouseMixin {

    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void onMouseButton(long window, MouseInput input, int action, CallbackInfo ci) {
        if (eventBus.post(new InputEvent(input, action)).isCancelled()) {
            ci.cancel();
        }
    }
}
