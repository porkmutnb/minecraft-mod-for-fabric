package org.chermew.grapandgo.client.interfaces;

import java.util.UUID;

public interface IEntityState {
    void grapAndGo$setUuid(UUID uuid);
    UUID grapAndGo$getUuid();

    boolean grapAndGo$isCarrying();
    void grapAndGo$setCarrying(boolean carrying);

    boolean grapAndGo$isCarriedByPlayer();
    void grapAndGo$setCarriedByPlayer(boolean carriedByPlayer);

}
