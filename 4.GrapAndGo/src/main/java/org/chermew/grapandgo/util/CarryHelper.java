package org.chermew.grapandgo.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.chermew.grapandgo.common.registry.ModStatusEffects;
import org.chermew.grapandgo.network.CarrySyncPayload;

public class CarryHelper {

    /**
     * เมธอดสำหรับเริ่มการอุ้ม (Grab)
     */
    public static void tryGrabEntity(ServerPlayer player, int entityId) {
        Entity target = player.level().getEntity(entityId);

        if (target != null && target != player && target instanceof LivingEntity livingTarget) {
            // 1. ใส่ MobEffect "Grab/Lift" ให้ Player แบบถาวร (ไม่มีวันหมดอายุจนกว่าจะเอาลง)
            player.addEffect(new MobEffectInstance(
                    BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModStatusEffects.GRAB_EFFECT),
                    MobEffectInstance.INFINITE_DURATION
            ));

            // 2. ให้ม็อบเป้าหมายมาขี่ผู้เล่น (ขึ้นมือ/ขึ้นหัว)
            livingTarget.startRiding(player);

            player.sendSystemMessage(Component.literal("พี่ปออุ้ม " + livingTarget.getName().getString() + " ขึ้นมาแล้วจ้า!"));

            // 3. ส่ง Packet บอก Client ให้วาด Entity และเปลี่ยนท่าทางแขน
            sendSyncPacket(player, true);
        }
    }

    /**
     * เมธอดสำหรับปล่อย (Drop)
     */
    public static void tryDropEntity(ServerPlayer player) {
        if (player.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModStatusEffects.GRAB_EFFECT))) {
            // 1. เคลียร์สถานะยกของออก
            player.removeEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModStatusEffects.GRAB_EFFECT));

            boolean droppedAny = false;
            // 2. ปลดผู้โดยสารทุกคนลง (ปกติมีตัวเดียว)
            for (Entity passenger : player.getPassengers()) {
                passenger.stopRiding();

                // คำนวณตำแหน่งสำหรับวางข้างหน้าผู้เล่น
                double yawRad = Math.toRadians(player.getYRot());
                double x = player.getX() - Math.sin(yawRad) * 1.5;
                double y = player.getY();
                double z = player.getZ() + Math.cos(yawRad) * 1.5;

                // วางบนพื้นในทิศที่มอง
                passenger.absSnapTo(x, y, z, player.getYRot(), player.getXRot());
                
                player.sendSystemMessage(Component.literal("พี่ปอวาง " + passenger.getName().getString() + " ลงพื้นแล้วค่ะ!"));
                droppedAny = true;
            }

            if (!droppedAny) {
                player.sendSystemMessage(Component.literal("พี่ปอวางเสร็จสิ้นแล้วค่ะ!"));
            }

            // 3. ส่ง Packet บอก Client ว่าเราไม่ได้อุ้มอะไรแล้ว
            sendSyncPacket(player, false);
        } else {
            player.sendSystemMessage(Component.literal("ในมือพี่ปอว่างเปล่า... จะวางอะไรเอ่ย?"));
        }
    }

    public static boolean isCarrying(ServerPlayer player) {
        return player.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModStatusEffects.GRAB_EFFECT));
    }

    public static void sendSyncPacket(ServerPlayer player, boolean isCarrying) {
        CarrySyncPayload payload = new CarrySyncPayload(player.getUUID(), isCarrying);
        // ส่งให้คนรอบข้างเห็น
        net.fabricmc.fabric.api.networking.v1.PlayerLookup.tracking(player).forEach(p ->
                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(p, payload)
        );
        // ส่งให้ตัวเองเห็นด้วย
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, payload);
    }
}
