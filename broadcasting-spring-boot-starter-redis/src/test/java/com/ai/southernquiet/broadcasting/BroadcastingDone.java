package com.ai.southernquiet.broadcasting;

import java.io.Serializable;
import java.util.UUID;

@ShouldBroadcast
public class BroadcastingDone implements SerializableEvent {
    private UUID id = UUID.randomUUID();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }
}

