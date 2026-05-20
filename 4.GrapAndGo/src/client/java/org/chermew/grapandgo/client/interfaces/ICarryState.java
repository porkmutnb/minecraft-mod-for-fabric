package org.chermew.grapandgo.client.interfaces;

import net.minecraft.world.entity.Entity;

public interface ICarryState {
    void setCarriedEntity(Entity entity);
    Entity getCarriedEntity();
}
