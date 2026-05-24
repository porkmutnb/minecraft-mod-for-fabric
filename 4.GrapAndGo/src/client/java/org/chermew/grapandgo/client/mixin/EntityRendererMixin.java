package org.chermew.grapandgo.client.mixin;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.chermew.grapandgo.client.interfaces.IEntityState;
import org.chermew.grapandgo.common.registry.ModStatusEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<E extends Entity, S extends EntityRenderState> {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void injectUUID(Entity entity, EntityRenderState state, float partialTick, CallbackInfo ci) {
        if (state instanceof IEntityState access) {
            access.grapAndGo$setUuid(entity.getUUID());

            // 1. เช็คว่า Entity นี้อุ้มม็อบอยู่หรือไม่ (เช็คจาก MobEffect โดยหุ้มด้วย Holder)
            if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
                boolean isCarrying = living.hasEffect(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModStatusEffects.GRAB_EFFECT));
                access.grapAndGo$setCarrying(isCarrying);
            } else {
                access.grapAndGo$setCarrying(false);
            }

            // 2. เช็คว่า Entity นี้ถูกอุ้มอยู่หรือไม่ (เช็คว่าขี่ Player อยู่ไหม)
            boolean isCarried = entity.getVehicle() instanceof net.minecraft.world.entity.player.Player;
            access.grapAndGo$setCarriedByPlayer(isCarried);
        }
    }
}
