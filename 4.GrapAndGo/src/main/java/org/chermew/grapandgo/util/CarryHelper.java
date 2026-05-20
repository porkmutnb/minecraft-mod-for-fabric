package org.chermew.grapandgo.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import org.chermew.grapandgo.common.GrapData;
import org.chermew.grapandgo.network.CarrySyncPayload;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CarryHelper {

    // ⭐ กระเป๋าโดเรม่อนชั่วคราวของน้องแป้ง!
    // เอาไว้เก็บข้อมูล GrapData แยกตามตัวผู้เล่น (ใช้ UUID) จะได้เทสระบบได้ทันทีค่ะ
    private static final Map<UUID, GrapData> CARRYING_DATA = new HashMap<>();

    /**
     * เมธอดสำหรับเริ่มการอุ้ม (Grab)
     */
    public static void tryGrabEntity(ServerPlayer player, int entityId) {
        // หาตัว Entity จาก ID ที่ส่งมาจาก Client
        Entity target = player.level().getEntity(entityId);

        if (target != null && target != player) {
            // 1. สร้าง Data เก็บ NBT ของม็อบไว้
            GrapData data = new GrapData(target);

            // ⭐ เก็บข้อมูลลง Map ชั่วคราวแทนการใช้ Attachment
            CARRYING_DATA.put(player.getUUID(), data);

            // 3. ลบ Entity ตัวจริงออกจากโลก (หายตัวไปแล้ว!)
            target.discard();

            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("พี่ปออุ้ม " + target.getName().getString() + " เข้ากระเป๋าแล้วจ้า!"));

            // 4. ส่ง Packet บอก Client ให้วาด Entity และเปลี่ยนท่าทางแขน
            sendSyncPacket(player, true);
        }
    }

    /**
     * เมธอดสำหรับปล่อย (Drop)
     */
    public static void tryDropEntity(ServerPlayer player) {
        // 1. ดึงข้อมูล GrapData ออกมาจากกระเป๋า
        GrapData data = CARRYING_DATA.get(player.getUUID());

        // พอเราเปิดใช้ data แล้ว ตรงนี้ก็จะไม่ Error แล้วค่ะ!
        if (data != null && data.getEntityData() != null) {
            CompoundTag nbt = data.getEntityData();

            double x = player.getX() + player.getLookAngle().x * 1.5;
            double y = player.getY();
            double z = player.getZ() + player.getLookAngle().z * 1.5;

            // 2. ใช้ player.level() แทน และระบุชนิด (Entity entity) ให้ชัดเจน ป้องกัน Java งง
            Entity restoredEntity = EntityType.loadEntityRecursive(nbt, player.level(), EntitySpawnReason.LOAD, (entity) -> {
                entity.absSnapTo(x, y, z, player.getYRot(), player.getXRot());
                return entity;
            });

            if (restoredEntity != null) {
                player.level().addFreshEntity(restoredEntity);

                // 3. วางแล้วก็ต้องเคลียร์กระเป๋าด้วยน้า
                CARRYING_DATA.remove(player.getUUID());

                player.sendSystemMessage(Component.literal("พี่ปอวาง " + restoredEntity.getName().getString() + " ลงพื้นแล้วค่ะ!"));

                // ส่ง Packet บอก Client ว่าเราไม่ได้อุ้มอะไรแล้ว
                sendSyncPacket(player, false);
            }
        } else {
            player.sendSystemMessage(Component.literal("ในมือพี่ปอว่างเปล่า... จะวางอะไรเอ่ย?"));
        }
    }

    public static boolean isCarrying(ServerPlayer player) {
        return CARRYING_DATA.containsKey(player.getUUID());
    }

    public static void sendSyncPacket(ServerPlayer player, boolean isCarrying) {
        CarrySyncPayload payload = new CarrySyncPayload(player.getUUID(), isCarrying);
        // ส่งให้คนรอบข้างเห็น
        net.fabricmc.fabric.api.networking.v1.PlayerLookup.tracking(player).forEach(p ->
                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(p, payload)
        );
        // ส่งให้ตัวเองเห็นด้วย (สำคัญมาก!)
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, payload);
    }
}
