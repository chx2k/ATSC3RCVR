package com.sony.tv.app.atsc3receiver1_0.app.events;

import com.sony.tv.app.atsc3receiver1_0.app.AdContent;

/**
 * Created by valokafor on 4/14/17.
 */

public class OnNewAdCreatedEvent {
    private final AdContent createdAdContent;

    public OnNewAdCreatedEvent(AdContent createdAdContent) {
        this.createdAdContent = createdAdContent;
    }

    public AdContent getCreatedAd() {
        return createdAdContent;
    }
}
