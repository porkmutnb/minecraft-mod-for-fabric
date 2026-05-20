package org.chermew.grapandgo.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import org.chermew.grapandgo.util.CarryHelper;

public class PacketHandler {
    public static void registerServerReceiver() {
        // 1. ถ้า playC2S() ไม่เจอ ลองใช้ playC2S() (ตัวเล็กตัวใหญ่มีผลนะคะ)
        // หรือถ้ายังไม่เจอ ให้ลองกด Alt+Enter ที่ PayloadTypeRegistry เพื่อดู method ที่มีค่ะ
        PayloadTypeRegistry.serverboundPlay().register(GrabPayload.ID, GrabPayload.CODEC);

        // 2. รับข้อมูล
        ServerPlayNetworking.registerGlobalReceiver(GrabPayload.ID, (payload, context) -> {
            int entityId = payload.entityId();

            context.server().execute(() -> {
                var player = context.player();

                // --- Logic ใหม่: เช็คก่อนว่าอุ้มอะไรอยู่ไหม ---
                if (CarryHelper.isCarrying(player)) {
                    // ถ้ามีอะไรขี่หัวอยู่ -> ให้วางลงค่ะ
                    CarryHelper.tryDropEntity(player);
                    CarryHelper.sendSyncPacket(player, false);
                } else {
                    // ถ้ามือว่าง -> ให้ไปอุ้มตัวที่เราเล็งไว้ค่ะ
                    CarryHelper.tryGrabEntity(player, entityId);
                    CarryHelper.sendSyncPacket(player, true);
                }
            });

            context.player().sendSystemMessage(
                    Component.literal("GrapAndGo: Received Entity ID " + entityId)
            );
        });

        // เปลี่ยนจาก playS2C() เป็น clientboundPlay() นะคะพี่ปอ
        PayloadTypeRegistry.clientboundPlay().register(CarrySyncPayload.ID, CarrySyncPayload.CODEC);
    }
}
