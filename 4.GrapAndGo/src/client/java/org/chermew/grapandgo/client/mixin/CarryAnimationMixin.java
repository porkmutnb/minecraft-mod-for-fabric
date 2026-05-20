package org.chermew.grapandgo.client.mixin;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.chermew.grapandgo.client.common.ClientCarryState;
import org.chermew.grapandgo.client.interfaces.IEntityState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public class CarryAnimationMixin<S extends HumanoidRenderState> {

    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void injectCarryAnimation(S state, CallbackInfo ci) {
        IO.println("intitialized CarryAnimationMixin.injectCarryAnimation: "+state);
        // ดึง UUID ผ่าน Interface ที่เราสร้างไว้
        if (state instanceof IEntityState access) {
            var uuid = access.grapAndGo$getUuid();

            if (uuid != null && ClientCarryState.CARRYING_PLAYERS.contains(uuid)) {
                HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;

                // ท่าอุ้มไม้ตายของพี่ปอ!
                model.rightArm.xRot = -1.0F;
                model.leftArm.xRot = -1.0F;
                model.rightArm.yRot = -0.1F;
                model.leftArm.yRot = 0.1F;
            }
        }
    }
}
