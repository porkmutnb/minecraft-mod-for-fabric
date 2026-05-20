package org.chermew.grapandgo.client.mixin;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.chermew.grapandgo.client.interfaces.ICarryState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class CarryRendererMixin {

    // จุดที่ 1: ส่งข้อมูลจาก Entity จริงเข้าสู่ State
    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void onExtractState(Entity entity, EntityRenderState state, float f, CallbackInfo ci) {
        // เช็คว่าผู้เล่นอุ้มอะไรอยู่ (ตัวอย่าง: ตัวแรกที่ขี่)
        Entity passenger = entity.getFirstPassenger();
        ((ICarryState) state).setCarriedEntity(passenger);
    }

    // จุดที่ 2: วาด Entity ออกมาบนจอ
    @Inject(method = "shouldRender", at = @At("TAIL"))
    private void shouldRender(Entity entity, Frustum frustum, double d, double e, double f, CallbackInfoReturnable<Boolean> cir) {
        // สมมติว่าพี่มีวิธีเช็คว่า Entity ตัวนี้ "กำลังถูกอุ้ม" อยู่หรือไม่
        // เช่น เช็คจาก Tag หรือเช็คว่ามันเป็น Passenger ของผู้เล่นไหม
        if (entity.getVehicle() instanceof net.minecraft.world.entity.player.Player) {
            // บังคับให้คืนค่า true ทันที เพื่อไม่ให้เกม "ซ่อน" (Culling) ตัวละครที่ถูกอุ้ม
            cir.setReturnValue(true);
        }
    }
}
