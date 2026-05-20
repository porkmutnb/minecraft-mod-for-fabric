package org.chermew.grapandgo.client.mixin;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.chermew.grapandgo.client.interfaces.ICarryState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public class CarryStateMixin implements ICarryState {
    // สร้างช่องเก็บ Entity สำรองไว้ใน State
    @Unique
    public Entity carriedEntity;

    @Override
    public void setCarriedEntity(Entity entity) {
        this.carriedEntity = entity;
    }

    @Override
    public Entity getCarriedEntity() {
        return this.carriedEntity;
    }
}
