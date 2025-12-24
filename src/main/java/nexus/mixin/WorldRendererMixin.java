package nexus.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.client.util.math.MatrixStack;
import nexus.events.WorldRenderEvent;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static nexus.Main.eventBus;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {

    @Unique
    @Final
    private VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(new BufferAllocator(1536 * 20));

    @SuppressWarnings("mapping")
    // the compiler sometimes claims that the inject target wasn't found, but it works fine regardless
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4fStack;popMatrix()Lorg/joml/Matrix4fStack;"))
    private void onRenderWorld(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, Matrix4f positionMatrix, Matrix4f matrix4f, Matrix4f projectionMatrix, GpuBufferSlice fogBuffer, Vector4f fogColor, boolean renderSky, CallbackInfo ci) {
        eventBus.post(new WorldRenderEvent(immediate, tickCounter, camera, new MatrixStack()));
        immediate.draw();
    }
}
