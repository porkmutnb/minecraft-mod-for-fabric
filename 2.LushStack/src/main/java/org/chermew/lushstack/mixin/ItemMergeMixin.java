package org.chermew.lushstack.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.chermew.lushstack.components.ItemStackComponents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

// คลาสสำหรับ Mixin เข้าไปที่ ItemEntity
@Mixin(ItemEntity.class)
public abstract class ItemMergeMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (self.level().isClientSide() || !self.isAlive()) return;

        if (self.getAge() % 5 == 0) {
            double radius = 8.0; // รัศมี 8 บล็อกตามที่พี่ปอขอเลย
            List<ItemEntity> nearbyItems = self.level().getEntitiesOfClass(
                    ItemEntity.class,
                    self.getBoundingBox().inflate(radius),
                    entity -> entity != self && entity.isAlive() && entity.getItem().getItem() == self.getItem().getItem()
            );

            for (ItemEntity other : nearbyItems) {
                applyGravity(self, other);
                if (self.distanceTo(other) < 0.5) {
                    combineStacks(self, other);
                }
            }
        }
    }

    private void applyGravity(ItemEntity primary, ItemEntity secondary) {
        double dx = primary.getX() - secondary.getX();
        double dy = primary.getY() - secondary.getY();
        double dz = primary.getZ() - secondary.getZ();
        secondary.setDeltaMovement(dx * 0.15, dy * 0.15, dz * 0.15);
    }

    private void combineStacks(ItemEntity primary, ItemEntity secondary) {
        ItemStack pStack = primary.getItem();
        ItemStack sStack = secondary.getItem();

        long current = pStack.getOrDefault(ItemStackComponents.LONG_COUNT, (long) pStack.getCount());
        long added = sStack.getOrDefault(ItemStackComponents.LONG_COUNT, (long) sStack.getCount());

        pStack.set(ItemStackComponents.LONG_COUNT, current + added);
        pStack.setCount(1); // ในเชิง Entity ให้เห็นแค่ 1 ชิ้นเพื่อลดแลค

        secondary.discard();
        updateItemLabel(primary);
    }

    private void updateItemLabel(ItemEntity entity) {
        ItemStack stack = entity.getItem();
        String itemName = stack.getHoverName().getString();
        long count = stack.getOrDefault(ItemStackComponents.LONG_COUNT, (long) stack.getCount());
        String label = "§6" + itemName + " §ex" + count;
        entity.setCustomName(Component.literal(label));
        entity.setCustomNameVisible(true);
    }

}
