package org.chermew.grapandgo.client.mixin;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.chermew.grapandgo.client.interfaces.IEntityState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.UUID;

@Mixin(EntityRenderState.class)
public class EntityRenderStateMixin implements IEntityState {
    @Unique
    private UUID grapAndGo$uuid;

    @Override
    public void grapAndGo$setUuid(UUID uuid) { this.grapAndGo$uuid = uuid; }

    @Override
    public UUID grapAndGo$getUuid() { return this.grapAndGo$uuid; }
}
