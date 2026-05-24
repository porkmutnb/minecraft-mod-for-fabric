package org.chermew.grapandgo.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.chermew.grapandgo.client.interfaces.IEntityState;
import org.chermew.grapandgo.common.registry.ModStatusEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(HumanoidModel.class)
public class CarryAnimationMixin<S extends HumanoidRenderState> {
    // สั่งเกาะ setupAnim โดด ๆ ไม่ต้องใส่ Package ยาวให้งงค๊าาา!
    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void injectCarryAnimation(HumanoidRenderState state, CallbackInfo ci) {
        System.out.println("--- [Debug] แผงอินเจ็คอนิเมชันทำงานแล้วจ้าพี่ปอ! ---");
        HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;
        // 🎯 แผนผังทางลัด: เช็กตรง ๆ ผ่านตัวแปรระบบเรนเดอร์ของ Minecraft 1.26.2
        // เคสที่ 1: สำหรับพี่ปอ (ผู้เล่นที่อุ้มม็อบ)
        // ถ้าระบบ Server พี่ปอสั่งให้ม็อบขี่ตัวผู้เล่นอยู่ ตัวแปรดั้งเดิมของโมจังที่ชื่อ `state.isPassenger` จะต้องคุมสถานะได้ค๊าาา
        // หรือถ้าพี่ปอมีตัวแปร Custom สลักไว้ในระบบ รื้อเช็กตรงหน้างานแบบนี้เลยค๊าาา:
        if (state.isPassenger == false) {
            // 💡 ทริคเด็ด: ในเมื่อสืบค้น UUID ตรง ๆ ไม่ได้ เราเปลี่ยนมาสั่งให้แขนยกค้างไว้ "ตลอดเวลาที่กดอุ้ม"
            // โดยเช็กผ่านปุ่มคลิกขวา หรือสเตตัสที่ลิงก์กับผู้เล่นได้เลยค๊าาา
            // แต่ถ้าจะสั่งให้แขนยกชัวร์ ๆ ลองปลดล็อกลบ if เช็ก UUID ออก แล้วสั่งล็อกแขนยกตรงนี้ดูเลยค๊าาา:
            model.rightArm.xRot = -1.0F;
            model.leftArm.xRot = -1.0F;
            model.rightArm.yRot = -0.1F;
            model.leftArm.yRot = 0.1F;
            System.out.println("--- [Debug] สั่งหักกระดูกแขนพยุงของเรียบร้อยค๊าาา! ---");
        }
        // เคสที่ 2: สำหรับตัวม็อบที่โดนอุ้ม (ขากับแขนแกว่งดิ้น)
        if (state.isPassenger) {
            float age = state.ageInTicks;
            float armWiggle = (float) Math.sin(age * 0.25F) * 0.2F;
            float legWiggle = (float) Math.cos(age * 0.25F) * 0.2F;

            model.rightArm.xRot = -0.2F + armWiggle;
            model.leftArm.xRot = -0.2F - armWiggle;
            model.rightLeg.xRot = 0.2F + legWiggle;
            model.leftLeg.xRot = 0.2F - legWiggle;
            System.out.println("--- [Debug] ตัวม็อบขี่อยู่ สั่งขาดิ้นดุ๊กดิ๊กแล้วค๊าาา! ---");
        }
    }
}
