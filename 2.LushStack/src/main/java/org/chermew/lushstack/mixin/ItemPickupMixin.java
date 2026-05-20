package org.chermew.lushstack.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.chermew.lushstack.components.ItemStackComponents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemPickupMixin {
    // 1. เปลี่ยนชื่อ method เป็น onPlayerTouch
    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void onPickup(Player player, CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (self.level().isClientSide() || !self.isAlive()) return;

        ItemStack stack = self.getItem();
        Long longCount = stack.get(ItemStackComponents.LONG_COUNT);

        if (longCount != null) {
            long totalToGive = longCount;

            while (totalToGive > 0) {
                int amountToGive = (int) Math.min(totalToGive, stack.getMaxStackSize());
                ItemStack copy = stack.copy();
                copy.set(ItemStackComponents.LONG_COUNT, null);
                copy.setCount(amountToGive);

                // 2. ลองใช้ .add(copy) หรือ .offerOrDrop(copy) ดูค่ะ
                if (player.getInventory().add(copy)) {
                    totalToGive -= (amountToGive - copy.getCount());
                    if (copy.getCount() > 0) break;
                } else {
                    break;
                }
            }

            if (totalToGive <= 0) {
                self.discard();
            } else {
                stack.set(ItemStackComponents.LONG_COUNT, totalToGive);
            }

            ci.cancel();
        }
    }
}
