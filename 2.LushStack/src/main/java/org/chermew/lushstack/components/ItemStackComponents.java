package org.chermew.lushstack.components;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.Identifier;

// สร้าง Class นี้ไว้ใน Package หลักของพี่ปอนะคะ
public class ItemStackComponents {
    // ลงทะเบียน Component สำหรับเก็บจำนวนแบบ Long
    public static final DataComponentType<Long> LONG_COUNT = DataComponentType.<Long>builder()
            .persistent(Codec.LONG) // ใช้ persistent แทน codec ในบางเวอร์ชัน
            .build();

    public static void register() {
        // ใช้ Identifier.of ตามมาตรฐาน Fabric ล่าสุดปี 2026 ค่ะ
        Registry.register(
                BuiltInRegistries.DATA_COMPONENT_TYPE,
                Identifier.fromNamespaceAndPath ("lushstack", "long_count"),
                LONG_COUNT
        );
    }
}
