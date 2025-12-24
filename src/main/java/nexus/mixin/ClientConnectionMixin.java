package nexus.mixin;

import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import nexus.events.ReceivePacketEvent;
import nexus.events.SendPacketEvent;
import nexus.events.ServerTickEvent;
import nexus.misc.SkyblockData;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static nexus.Main.eventBus;

@Mixin(ClientConnection.class)
public abstract class ClientConnectionMixin {
    @Inject(method = "handlePacket", at = @At("HEAD"), cancellable = true)
    private static void onPacketReceive(Packet<?> packet, PacketListener listener, CallbackInfo ci) {
        if (packet instanceof CommonPingS2CPacket) {
            eventBus.post(new ServerTickEvent());
        }
        if (packet instanceof PlayerListS2CPacket listPacket) {
            SkyblockData.updateTabList(listPacket, listPacket.getEntries());
        }
        if (eventBus.post(new ReceivePacketEvent(packet)).isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;Lio/netty/channel/ChannelFutureListener;Z)V", at = @At("HEAD"), cancellable = true)
    private void onPacketSend(Packet<?> packet, @Nullable ChannelFutureListener channelFutureListener, boolean flush, CallbackInfo ci) {
        if (eventBus.post(new SendPacketEvent(packet)).isCancelled()) {
            ci.cancel();
        }
    }
}