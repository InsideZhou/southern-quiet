package test.broadcasting;

import me.insidezhou.southernquiet.event.ShouldBroadcast;

import java.io.Serializable;

@ShouldBroadcast(channels = "haha", typeId = "haha")
public class HahaEvent implements Serializable {
    private String guid;

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }
}
