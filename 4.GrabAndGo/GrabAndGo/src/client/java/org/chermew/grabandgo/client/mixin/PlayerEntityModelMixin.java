package org.chermew.grabandgo.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.Entity;
import org.chermew.grabandgo.duck.GrabCarrier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class PlayerEntityModelMixin extends HumanoidModel<HumanoidRenderState> {

    public PlayerEntityModelMixin(ModelPart root) {
        super(root);
    }

    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void grabandgo$applyCarryingAnimation(AvatarRenderState state, CallbackInfo ci) {
        // ดึง Entity จากโลกโดยใช้ ID ที่ติดมากับ state
        Entity entity = Minecraft.getInstance().level.getEntity(state.id);
        if (entity instanceof GrabCarrier carrier && carrier.grabandgo$isCarrying()) {
            this.leftArm.xRot = -1.2F;
            this.leftArm.yRot = 0.15F;
            this.rightArm.xRot = -1.2F;
            this.rightArm.yRot = -0.15F;
        }
    }
}
