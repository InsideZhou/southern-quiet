package test.job;

import java.io.Serializable;
import java.util.UUID;

public class NoProcessorJob implements Serializable {
    private UUID id = UUID.randomUUID();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }
}
