# Minecraft Fabric Mods Workspace

โปรเจกต์นี้เป็น workspace ของ Minecraft Fabric mod ที่แยกเป็น 4 โฟลเดอร์ย่อย โดยแต่ละโฟลเดอร์เป็นโปรเจกต์ Fabric แยกกัน:

- `1.Essential Info HUD` — mod `essential_hud`
- `2.LushStack` — mod `lushstack`
- `3.QuantumMobStacker` — mod `quantum_mob_stacker`
- `4.GrapAndGo` — mod `grapandgo`

## โครงสร้างโปรเจกต์

แต่ละโฟลเดอร์จะมีโครงสร้างหลักเหมือนกัน:

- `build.gradle` — กำหนดการ build ด้วย Fabric Loom
- `gradle.properties` — กำหนดเวอร์ชันของ Minecraft, Fabric, Loader, mod ฯลฯ
- `src/main/java` — โค้ดหลักของม็อด
- `src/client/java` — โค้ดฝั่ง client
- `src/main/resources/fabric.mod.json` — ข้อมูล metadata ของม็อด

## ความต้องการพื้นฐาน

- Java 25
- Fabric Loader >= 0.19.2
- Fabric API
- Minecraft 26.1.2
- Gradle Wrapper ที่จัดให้ในแต่ละโปรเจกต์

## วิธี build และทดสอบ

1. เปิด terminal ไปที่โฟลเดอร์ของโปรเจกต์ที่ต้องการ เช่น:

   ```powershell
   cd "d:\cherMew\minecraft-mod-for-fabric\1.Essential Info HUD"
   ```

2. รันคำสั่ง build:

   ```powershell
   .\gradlew.bat build
   ```

3. หากต้องการรัน client ในสภาพแวดล้อม dev:

   ```powershell
   .\gradlew.bat runClient
   ```

> หมายเหตุ: ถ้าใช้งานบนระบบ Unix/Linux ให้ใช้ `./gradlew build` และ `./gradlew runClient` แทน

## การแก้ไขข้อมูลม็อด

- แก้ชื่อ และคำอธิบายใน `src/main/resources/fabric.mod.json`
- แก้เวอร์ชัน mod ใน `gradle.properties`
- ถ้าต้องการเพิ่ม dependencies ให้แก้ `build.gradle`

## ข้อมูลสำคัญ

- `fabric.mod.json` ของแต่ละโปรเจกต์ใช้ตัวแปร `${version}` และจะถูกแทนค่าจาก `project.version`
- `GrapAndGo` ใช้ไลเซนส์ `MIT`
- โปรเจกต์อื่นๆ ใน workspace ใช้ `All-Rights-Reserved`

## คำแนะนำเพิ่มเติม

- ถ้าเพิ่ม mod ใหม่ ให้สร้างโฟลเดอร์ใหม่พร้อมโครงสร้างเดียวกัน และตั้งค่า `fabric.mod.json` แบบเดียวกับโปรเจกต์อื่น
- ตรวจสอบ `gradle.properties` เสมอก่อน build เพื่อให้เวอร์ชัน Minecraft/Fabric ตรงกัน
- ใช้ `build/libs` ในแต่ละโฟลเดอร์เพื่อค้นหาไฟล์ JAR ที่ได้จากการ build
