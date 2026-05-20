package org.chermew.essential_hud.client.overlay;

import net.minecraft.client.Minecraft;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public class HUDInfoOverlay implements HudElement {
    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphicsExtractor, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;

        // --- [ ส่วนที่ 1: เตรียมพิกัดและสี ] ---
        int width = minecraft.getWindow().getGuiScaledWidth()-50;
        int height = minecraft.getWindow().getGuiScaledHeight()-5;
        int centerX = width / 2;

        boolean isCreative = minecraft.player.isCreative();
        int dynamicYOffset = isCreative ? 30 : 55;
        int infoY = height - dynamicYOffset;

        int labelColor = 0xFFFFAA00; // สีส้มทอง (Label)
        int valueColor = 0xFFFFFFFF; // สีขาว (Value)

        // 2.1 XYZ (ใช้ทศนิยม 3 ตำแหน่งแบบ F3 ตามที่พี่ปออยากได้)
        String coordVal = String.format("%.2f %.2f %.2f", minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ());
        guiGraphicsExtractor.text(minecraft.font, "XYZ: ", centerX - 120, infoY, labelColor, true);
        guiGraphicsExtractor.text(minecraft.font, coordVal, centerX - 95, infoY, valueColor, true);

        // 2.2 Direction
        String dirVal = minecraft.player.getDirection().getSerializedName().toUpperCase().substring(0, 1);
        guiGraphicsExtractor.text(minecraft.font, "DIR: ", centerX + 30, infoY, labelColor, true);
        guiGraphicsExtractor.text(minecraft.font, dirVal, centerX + 55, infoY, valueColor, true);

        // 2.3 Day & Time (ใช้ getDayTime เพื่อเลขวันที่นิ่ง ๆ ค่ะ)
        long totalTicks = minecraft.level.getGameTime();
        long dayCount = totalTicks / 24000;
        long time = (totalTicks + 6000) % 24000;
        String timeVal = String.format("DAY %d | %02d:%02d", dayCount, time / 1000, (time % 1000) * 60 / 1000);
        guiGraphicsExtractor.text(minecraft.font, timeVal, centerX + 80, infoY, valueColor, true);

        if (!isCreative) {
            // --- [ ส่วนที่ 3: วาดสถานะเกราะและของในมือ (ข้าง Hotbar) ] ---
            // ปรับให้ลอยขึ้นพ้นดินและขยับไปทางซ้ายไม่ให้ทับ XYZ
            int armorX = centerX - 195;
            int armorY = height - 100;

            EquipmentSlot[] slots = {
                    EquipmentSlot.HEAD,
                    EquipmentSlot.CHEST,
                    EquipmentSlot.LEGS,
                    EquipmentSlot.FEET,
                    EquipmentSlot.OFFHAND
            };

            for (EquipmentSlot slot : slots) {
                ItemStack stack = minecraft.player.getItemBySlot(slot);
                if (!stack.isEmpty()) {
                    // วาดไอคอนไอเทม
                    guiGraphicsExtractor.item(stack, armorX, armorY);
                    // เช็กความพัง (Durability)
                    if (stack.isDamageableItem()) {
                        int maxDur = stack.getMaxDamage();
                        int currentDur = maxDur - stack.getDamageValue();
                        double durPercent = (double) currentDur / maxDur;

                        // เปลี่ยนสีตาม Damage (เขียว > เหลือง > แดง)
                        int color = (durPercent > 0.5) ? 0xFF55FF55 : (durPercent > 0.25 ? 0xFFFFFF55 : 0xFFFF5555);

                        guiGraphicsExtractor.text(minecraft.font, currentDur + "/" + maxDur, armorX + 20, armorY + 4, color, true);
                    } else if (stack.getCount() > 1) {
                        // ถ้าถือของอื่นในมือซ้าย ให้โชว์จำนวนแทน
                        guiGraphicsExtractor.text(minecraft.font, "x" + stack.getCount(), armorX + 20, armorY + 4, 0xFFFFFFFF, true);
                    }
                    // ขยับ Y ลงเพื่อวาดชิ้นถัดไป
                    armorY += 18;
                }
            }
        }
    }
}
