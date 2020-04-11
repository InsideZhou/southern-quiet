package test.broadcasting;

import me.insidezhou.southernquiet.event.ShouldBroadcast;

import java.io.Serializable;
import java.util.UUID;

@ShouldBroadcast("TEST.CHANNEL")
public class BroadcastingCustomChannel implements Serializable {
    private UUID id = UUID.randomUUID();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }
}
