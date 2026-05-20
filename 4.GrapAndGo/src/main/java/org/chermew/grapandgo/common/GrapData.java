package org.chermew.grapandgo.common;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.TagValueOutput;

public class GrapData {
    private CompoundTag entityData;

    public GrapData(Entity entity) {
        // 1. ดึง Registry Access มาเตรียมไว้เหมือนเดิม
        HolderLookup.Provider registries = entity.level().registryAccess();

        // 2. สร้าง TagValueOutput ขึ้นมา (เป็นตัวแทนของ ValueOutput)
        // ทริคคือใช้ ProblemReporter.DISCARDING เพื่อบอกว่าถ้ามี error ยิบย่อยตอนเซฟเราขอข้ามไปก่อนค่ะ
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);

        // 3. ใช้ท่า saveAsPassenger โดยโยน output ลงไปรับค่าแทน
        if (entity.saveAsPassenger(output)) {
            // 4. ทีเด็ดอยู่ตรงนี้! สั่ง buildResult() เพื่อแปลงร่างมันกลับมาเป็น CompoundTag ปกติ!
            this.entityData = (CompoundTag) output.buildResult();
        } else {
            System.err.println("น้องแป้งเตือน: ม็อบตัวนี้ดื้อ ไม่ยอมให้อุ้ม (เซฟไม่ได้) ค่ะ!");
        }
    }

    public CompoundTag getEntityData() {
        return this.entityData;
    }
}
