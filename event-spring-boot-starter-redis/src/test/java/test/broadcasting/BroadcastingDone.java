package test.broadcasting;

import com.ai.southernquiet.event.CustomApplicationEvent;

import java.io.Serializable;
import java.util.UUID;

@CustomApplicationEvent
public class BroadcastingDone implements Serializable {
    private UUID id = UUID.randomUUID();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }
}

