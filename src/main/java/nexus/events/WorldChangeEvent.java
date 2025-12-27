package nexus.events;

import net.minecraft.client.world.ClientWorld;

public class WorldChangeEvent {
    public final ClientWorld world;

    public WorldChangeEvent(ClientWorld world) {
        this.world = world;
    }
}

