package org.chermew.quantum_mob_stacker.mixin;

import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.chermew.quantum_mob_stacker.interfaces.IStackableMob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(LivingEntity.class)
public abstract class MobEntityMixin implements IStackableMob {

    @Unique
    private long stackCount = 1;

    @Unique
    private boolean quantum_mob_stacker$isBreeding = false;

    /**
     * Creates an invoker to call the protected method dropCustomDeathLoot.
     */
    @Invoker("dropCustomDeathLoot")
    protected abstract void invokeDropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer);

    /**
     * Creates an invoker to call the protected method dropFromLootTable.
     */
    @Invoker("dropFromLootTable")
    protected abstract void invokeDropFromLootTable(ServerLevel level, DamageSource source, boolean playerKilled);


    @Unique
    private LivingEntity self() {
        return (LivingEntity)(Object)this;
    }

    @Unique
    private void quantum_mob_stacker$updateCustomName() {
        LivingEntity self = self();
        if (this.getStackCount() > 1) {
            Component baseName = self.getType().getDescription().copy().withStyle(ChatFormatting.GRAY);
            Component stackComponent = Component.literal(" x" + this.getStackCount()).withStyle(ChatFormatting.GOLD);
            self.setCustomName(Component.empty().append(baseName).append(stackComponent));
            self.setCustomNameVisible(true);
        } else {
            self.setCustomName(null);
            self.setCustomNameVisible(false);
        }
    }

    @Override
    public long getStackCount() {
        return this.stackCount;
    }

    @Override
    public void setStackCount(long count) {
        this.stackCount = count > 0 ? count : 1;
        quantum_mob_stacker$updateCustomName();
    }

    @Override
    public void addStack(long amount) {
        this.stackCount += amount;
        quantum_mob_stacker$updateCustomName();
    }

    @Override
    public boolean quantum_mob_stacker$isBreeding() {
        return this.quantum_mob_stacker$isBreeding;
    }

    @Override
    public void quantum_mob_stacker$setBreeding(boolean breeding) {
        this.quantum_mob_stacker$isBreeding = breeding;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void quantum_mob_stacker$doStacking(CallbackInfo ci) {
        LivingEntity self = self();
        if (self instanceof Mob && !self.level().isClientSide() && self.tickCount % 10 == 0) {
            if (((IStackableMob) self).quantum_mob_stacker$isBreeding() || self.isBaby() || (self instanceof Animal animal && animal.isInLove())) {
                return;
            }
            double radius = 8.0;
            List<Mob> nearby = self.level().getEntitiesOfClass(Mob.class,
                    self.getBoundingBox().inflate(radius),
                    (Mob e) -> { // ระบุ Type ให้ชัดในวงเล็บแบบนี้ค่ะ
                        return e.getType() == self.getType()
                                && e != self
                                && e.isAlive()
                                && ((IStackableMob)e).getStackCount() > 0
                                && !((IStackableMob)e).quantum_mob_stacker$isBreeding()
                                && (e instanceof net.minecraft.world.entity.animal.Animal animal && !animal.isBaby())
                                ;
                    });

            if (!nearby.isEmpty()) {
                for (Mob other : nearby) {
                    IStackableMob otherStack = (IStackableMob) other;
                    this.addStack(otherStack.getStackCount());
                    other.discard();
                }
            }
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void quantum_mob_stacker$checkBreedingStatus(CallbackInfo ci) {
        LivingEntity self = self();
        if (!self.level().isClientSide() && self instanceof Animal animal && self.tickCount % 20 == 0) {
            if (this.quantum_mob_stacker$isBreeding()) {
                if (animal.isInLove()) {
                    // สถานะ: กำลังลั้นลา (ห้ามรวม Stack)
                    this.quantum_mob_stacker$setBreeding(true);
                } else if (animal.isBaby()) {
                    // สถานะ: เป็นเด็ก (ห้ามรวม Stack)
                    this.quantum_mob_stacker$setBreeding(true);
                } else {
                    // สถานะ: พร้อมกลับเข้ากลุ่ม!
                    this.quantum_mob_stacker$setBreeding(false);
                }
            }
        }
    }

    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void quantum_mob_stacker$preventTrueDeath(DamageSource source, CallbackInfo ci) {
        LivingEntity self = self();
        if (self.level() instanceof ServerLevel serverLevel && self instanceof Mob mobSelf && this.getStackCount() > 1) {
            // Drop loot before doing anything else
            if (!mobSelf.isBaby() && serverLevel.getGameRules().get(GameRules.MOB_DROPS)) {
                // Call the protected methods using our invokers
                this.invokeDropFromLootTable(serverLevel, source, true);
                this.invokeDropCustomDeathLoot(serverLevel, source, true);
            }

            // Now, decrease the stack and prevent death
            this.setStackCount(this.getStackCount() - 1);
            
            self.level().broadcastEntityEvent(self, (byte)3);
            self.setHealth(self.getMaxHealth());
            self.deathTime = 0;
            
            ci.cancel();
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void quantum_mob_stacker$writeStackData(ValueOutput output, CallbackInfo ci) {
        if((Object)this instanceof Mob) {
            output.putLong("StackCount", this.getStackCount());
            output.putBoolean("IsBreeding", this.quantum_mob_stacker$isBreeding());
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void quantum_mob_stacker$readStackNbt(ValueInput input, CallbackInfo ci) {
        if((Object)this instanceof Mob) {
            input.read("StackCount", Codec.LONG).ifPresent(this::setStackCount);
            input.read("IsBreeding", Codec.BOOL).ifPresent(this::quantum_mob_stacker$setBreeding);
        }
    }
}