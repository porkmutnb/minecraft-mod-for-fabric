package org.chermew.quantum_mob_stacker.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.chermew.quantum_mob_stacker.interfaces.IStackableMob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Animal.class)
public abstract class AnimalMixin {

    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void quantum_mob_stacker$onMobInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Animal self = (Animal) (Object) this;
        ItemStack itemStack = player.getItemInHand(hand);

        if (self.isFood(itemStack)) {
            int age = self.getAge();
            if (!self.level().isClientSide() && age == 0 && self.canFallInLove()) {
                IStackableMob stackable = (IStackableMob) self;
                if (stackable.getStackCount() > 1) {
                    // Decrement stack count of the source stack
                    stackable.setStackCount(stackable.getStackCount() - 1);

                    // Create the new individual entity that will breed
                    Animal detachedMob = (Animal) self.getType().create(self.level(), EntitySpawnReason.MOB_SUMMONED); // (Animal) self.getType().create(self.level());
                    if (detachedMob != null) {
                        // Position it at the stack's current location
                        // detachedMob.moveTo(self.getX(), self.getY(), self.getZ(), self.getYRot(), self.getXRot());
                        detachedMob.setPos(self.getX()+1, self.getY(), self.getZ());
                        detachedMob.setYRot(self.getYRot());
                        detachedMob.setXRot(self.getXRot());

                        // Configure stack count and breeding state
                        IStackableMob detachedStackable = (IStackableMob) detachedMob;
                        detachedStackable.setStackCount(1);
                        detachedStackable.quantum_mob_stacker$setBreeding(true);

                        // Put the detached mob in the "InLove" state
                        detachedMob.setInLove(player);

                        // Consume the player's item
                        if (!player.isCreative()) {
                            itemStack.shrink(1);
                        }

                        // Spawn the detached mob into the world
                        self.level().addFreshEntity(detachedMob);

                        // Cancel default interaction and return SUCCESS
                        cir.setReturnValue(self.level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME);
                    }
                }
            }
        }
    }
}
