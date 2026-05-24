package org.chermew.grapandgo.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.chermew.grapandgo.common.registry.ModStatusEffects;
import org.chermew.grapandgo.util.CarryHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Player.class, LivingEntity.class})
public abstract class PlayerMixin extends Entity {
    protected PlayerMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }
    /**
     * กำหนดตำแหน่งผู้โดยสาร (ม็อบที่โดนอุ้ม) ให้อยู่ที่ตำแหน่งมือของ Player ทุกๆ เฟรม ทั้ง Server และ Client
     */
    @Inject(method = "rideTick", at = @At("HEAD")) // 💡 ปลดลบล็อค cancellable = true ออก เพื่อให้ระบบเกมหลักทำงานต่อค๊าาา!
    private void adjustCarriedMobPosition(CallbackInfo ci) {
        // ✨ [จุดแก้ที่ 1]: บังคับเช็กตรงๆ ฝั่ง Client เลยว่า ตัวเรา (this) มีใครขี่หลังอยู่ไหม?
        // ถ้าตัวพี่ปอเริ่มมีน้องม็อบขี่หลัง (getPassengers ไม่ว่าง) สั่งดูดวาร์ปม็อบมาที่มือทันทีค๊าาา!
        if (this.isVehicle() && !this.getPassengers().isEmpty()) {
            Entity passenger = this.getPassengers().get(0);

            double yawRad = Math.toRadians(this.getYRot());
            double distance = 0.8; // ยื่นน้องม็อบไปด้านหน้าพี่ปอ 0.8 บล็อก ไม่ให้จมในตัว
            double xOffset = -Math.sin(yawRad) * distance;
            double zOffset = Math.cos(yawRad) * distance;
            double yOffset = 0.4;  // ระดับความสูงอยู่ที่อ้อมอก/มืออุ้มพอดีสวยๆ ค๊าาา

            // สั่งย้ายตำแหน่งม็อบมาล็อกไว้ที่มือเราฝั่งหน้าจอ Client ทุกเฟรมค๊าาา!
            passenger.setPos(this.getX() + xOffset, this.getY() + yOffset, this.getZ() + zOffset);
        }
    }
    /**
     * อัปเดตสถานะความปลอดภัยทุกๆ Tick ของเกม
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void syncGrabState(CallbackInfo ci) {
        if ((Object)this instanceof Player player) {
            if (!player.level().isClientSide()) {
                boolean hasGrab = player.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModStatusEffects.GRAB_EFFECT));
                boolean hasPassengers = player.isVehicle();

                if (hasGrab && !hasPassengers) {
                    player.removeEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModStatusEffects.GRAB_EFFECT));
                    CarryHelper.sendSyncPacket((ServerPlayer) player, false);
                }

                if (!hasGrab && hasPassengers) {
                    player.ejectPassengers();
                    CarryHelper.sendSyncPacket((ServerPlayer) player, false);
                }
            }
        }
    }
    /**
     * ล็อกไม่ให้ Player โยนไอเทมในมือทิ้งขณะอุ้มม็อบอยู่
     */
    @Inject(method = "drop", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void cancelDrop(CallbackInfoReturnable<ItemEntity> cir) {
        if ((Object)this instanceof Player player) {
            if (player.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModStatusEffects.GRAB_EFFECT))) {
                cir.setReturnValue(null);
            }
        }
    }
}
