package nexus.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import nexus.events.BlockUpdateEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static nexus.Main.eventBus;

@Mixin(World.class)
public abstract class WorldMixin {
    @Inject(method = "onBlockStateChanged", at = @At(value = "TAIL"))
    private void onBlockChanged(BlockPos pos, BlockState oldBlock, BlockState newBlock, CallbackInfo ci) {
        eventBus.post(new BlockUpdateEvent(pos, oldBlock, newBlock));
    }
}