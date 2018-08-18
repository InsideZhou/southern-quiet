package test.broadcasting;

import com.ai.southernquiet.broadcasting.ShouldBroadcastCustomApplicationEvent;

import java.util.UUID;

@ShouldBroadcastCustomApplicationEvent
public class NonSerializableEvent {
    private UUID id = UUID.randomUUID();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }
}
