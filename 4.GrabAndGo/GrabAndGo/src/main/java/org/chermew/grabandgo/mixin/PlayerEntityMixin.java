package org.chermew.grabandgo.mixin;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.chermew.grabandgo.duck.GrabCarrier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerEntityMixin implements GrabCarrier {

    @Unique
    private static final EntityDataAccessor<Boolean> IS_CARRYING = SynchedEntityData.defineId(Player.class, EntityDataSerializers.BOOLEAN);
    
    @Unique
    private static final EntityDataAccessor<String> CARRIED_DATA = SynchedEntityData.defineId(Player.class, EntityDataSerializers.STRING);

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void grabandgo$defineSynchedData(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(IS_CARRYING, false);
        builder.define(CARRIED_DATA, new String());
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void grabandgo$addAdditionalSaveData(ValueOutput output, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        output.putBoolean("GrabAndGo_IsCarrying", self.getEntityData().get(IS_CARRYING));
        // 1. ดึง String ออกมา
        String nbtString = self.getEntityData().get(CARRIED_DATA);
        try {
            // 2. แปลง String กลับเป็น CompoundTag ก่อนส่งให้ CustomData
            CompoundTag tag = TagParser.parseCompoundFully(nbtString);
            output.store("GrabAndGo_CarriedData", CustomData.CODEC, CustomData.of(tag));
        } catch (CommandSyntaxException e) {
            // ถ้า String พัง ให้ส่งเป็น CustomData ว่างๆ แทนค่ะ
            output.store("GrabAndGo_CarriedData", CustomData.CODEC, CustomData.EMPTY);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void grabandgo$readAdditionalSaveData(ValueInput input, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        // ใช้ input ในการดึงข้อมูล
        if (input.contains("GrabAndGo_IsCarrying")) {
            self.getEntityData().set(IS_CARRYING, input.getBooleanOr("GrabAndGo_IsCarrying", false));
        }
        // การอ่านกลับมา
        String nbtString = input.getStringOr("GrabAndGo_CarriedData", "{}");
        self.getEntityData().set(CARRIED_DATA, nbtString);
    }

    @Override
    public boolean grabandgo$isCarrying() {
        return ((Player) (Object) this).getEntityData().get(IS_CARRYING);
    }

    @Override
    public void grabandgo$setCarrying(boolean carrying) {
        ((Player) (Object) this).getEntityData().set(IS_CARRYING, carrying);
    }

    @Override
    public CompoundTag grabandgo$getCarriedData() {
        String nbtString = ((Player) (Object) this).getEntityData().get(CARRIED_DATA);
        try {
            return TagParser.parseCompoundFully(nbtString);
        } catch (CommandSyntaxException e) {
            return new CompoundTag(); // ถ้าพาร์สพลาด ให้คืนค่าว่างไปค่ะ
        }
    }

    @Override
    public void grabandgo$setCarriedData(CompoundTag tag) {
        ((Player) (Object) this).getEntityData().set(CARRIED_DATA, tag.toString());
    }

    @Override
    public void grabandgo$clearCarried() {
        ((Player) (Object) this).getEntityData().set(IS_CARRYING, false);
        ((Player) (Object) this).getEntityData().set(CARRIED_DATA, new String());
    }
}
