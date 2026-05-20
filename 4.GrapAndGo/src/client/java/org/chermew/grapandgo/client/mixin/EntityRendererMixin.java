package org.chermew.grapandgo.client.mixin;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.chermew.grapandgo.client.interfaces.IEntityState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<E extends Entity, S extends EntityRenderState> {

    // น้องแป้งเปลี่ยนมาใช้ Entity และ EntityRenderState ตรงๆ ในพารามิเตอร์นะคะ จะได้ไม่แดง!
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void injectUUID(E entity, S state, float partialTick, CallbackInfo ci) {
        if (state instanceof IEntityState access) {
            access.grapAndGo$setUuid(entity.getUUID());
        }
    }
}
