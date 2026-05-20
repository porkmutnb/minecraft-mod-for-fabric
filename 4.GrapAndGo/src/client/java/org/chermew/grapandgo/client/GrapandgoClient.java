package org.chermew.grapandgo.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import org.chermew.grapandgo.client.common.ClientCarryState;
import org.chermew.grapandgo.client.registry.ModKeybindings;
import org.chermew.grapandgo.network.CarrySyncPayload;
import org.chermew.grapandgo.network.GrabPayload;
import org.lwjgl.glfw.GLFW;

public class GrapandgoClient implements ClientModInitializer {

    private boolean wasPressedLastTick = false;

    @Override
    public void onInitializeClient() {
        // ลงทะเบียนปุ่มกดที่เราสร้างไว้ในไฟล์แรกสุดไงคะพี่ปอ
        ModKeybindings.register();

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            // ทำงานเฉพาะมือหลัก และต้องเป็นฝั่ง Client (เพราะเราอยู่ใน ClientInitializer)
            if (world.isClientSide() && hand == InteractionHand.MAIN_HAND) {

                // ถ้าอุ้มของอยู่ -> กดคลิกขวาเพื่อวาง (PASS ไปให้ Server จัดการเอาลง)
                if (player.isPassenger()) {
                    return InteractionResult.PASS;
                }

                // ถ้าไม่ได้อุ้ม และกด Shift อยู่ -> "อุ้ม"
                // ใช้ isSecondaryUseActive() เพื่อเช็คการกด Shift
                if (player.isSecondaryUseActive()) {
                    // ตรงนี้ถ้าพี่มีระบบ Networking สำหรับส่งไปบอก Server ให้ "อุ้ม"
                    // พี่อาจจะต้องส่ง Payload ไปค่ะ เช่น:
                    // ClientPlayNetworking.send(new CarryActionPayload(entity.getId()));

                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.PASS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(this::handleInput);

        ClientPlayNetworking.registerGlobalReceiver(CarrySyncPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                if (payload.isCarrying()) {
                    ClientCarryState.CARRYING_PLAYERS.add(payload.playerUuid());
                } else {
                    ClientCarryState.CARRYING_PLAYERS.remove(payload.playerUuid());
                }
            });
        });
    }

    private void handleInput(Minecraft client) {
        if (client.player == null) return;

        // 1. ดึงข้อมูลปุ่มที่เราจองไว้
        var keyMapping = ModKeybindings.grapKey;
        long windowHandle = client.getWindow().handle();

        // 2. เช็คสถานะ "ดิบ" จากเมาส์ (ใช้เลข 1 สำหรับคลิกขวา หรือดึงจาก mapping)
        // น้องแป้งแนะนำให้ลองเช็คด้วย GLFW โดยตรงแบบนี้เพื่อพิสูจน์ค่ะ
        boolean isRightClickDown = GLFW.glfwGetMouseButton(windowHandle, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

        // 3. ใส่ Log ดูความจริงกันอีกรอบ!
        if (isRightClickDown) {
            System.out.println("DEBUG: เจอแล้ว! GLFW บอกว่าพี่ปอกดคลิกขวาอยู่จ้าาา!");
        }

        if (isRightClickDown && !wasPressedLastTick) {
            var targetEntity = client.crosshairPickEntity;
            if (targetEntity != null) {
                ClientPlayNetworking.send(new GrabPayload(targetEntity.getId()));
            } else {
                ClientPlayNetworking.send(new GrabPayload(-1));
            }
        }

        wasPressedLastTick = isRightClickDown;

        if (client.player == null) return;
        while (ModKeybindings.grapKey.consumeClick()) {
            System.out.println("DEBUG: พี่ปอกดปุ่ม Action แล้วนะจ๊ะ!");
            // ในเวอร์ชันนี้ crosshairPickEntity คือตัว Entity ที่เราชี้อยู่ตรงๆ เลยค่ะ
            var targetEntity = client.crosshairPickEntity;

            if (targetEntity != null) {
                // ถ้าเจอ Entity (targetEntity ไม่เป็น null) ก็ดึง ID มาส่งได้เลยค่ะ
                int entityId = targetEntity.getId();

                // ส่งจดหมายไปอุ้มตัวนี้แหละ!
                ClientPlayNetworking.send(new GrabPayload(entityId));
            } else {
                // ถ้าไม่เจอตัวอะไร (มองอากาศว่างเปล่า) ส่ง -1 ไปเพื่อบอกว่า "วางของ" ค่ะ
                ClientPlayNetworking.send(new GrabPayload(-1));
            }
        }
    }

}
