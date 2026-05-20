package org.chermew.grapandgo;

import net.fabricmc.api.ModInitializer;
import org.chermew.grapandgo.network.PacketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Grapandgo implements ModInitializer {
    public static final String MOD_ID = "grapandgo";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // 1. ทักทายพี่ปอตอนเริ่มเกมหน่อยน้า
        LOGGER.info("GrapAndGo is Initializing...");

        // 2. เรียกใช้การลงทะเบียนตัวรับข้อมูลฝั่ง Server
        PacketHandler.registerServerReceiver();
    }
}
