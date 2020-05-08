package test.notification;

import java.io.Serializable;
import java.util.UUID;

public class StandardNotification implements Serializable {
    private UUID id = UUID.randomUUID();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }
}
