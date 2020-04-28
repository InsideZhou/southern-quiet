package test.notification;

import java.util.UUID;

public class StandardNotification {
    private UUID id = UUID.randomUUID();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }
}
