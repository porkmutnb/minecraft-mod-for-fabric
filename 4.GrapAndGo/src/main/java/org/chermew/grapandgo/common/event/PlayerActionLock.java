package org.chermew.grapandgo.common.event;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionResult;
import org.chermew.grapandgo.common.registry.ModStatusEffects;

public class PlayerActionLock {

    public static void register() {
        // 1. ล็อกการขุด/ตีบล็อก (Left-Click บล็อก)
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            if (player.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModStatusEffects.GRAB_EFFECT))) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });

        // 2. ล็อกการโจมตีม็อบ/ผู้เล่นอื่น (Left-Click ม็อบ)
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (player.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModStatusEffects.GRAB_EFFECT))) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });

        // 3. ล็อกการคลิกขวาที่บล็อก (เช่น เปิดกล่อง, กดปุ่ม)
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (player.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModStatusEffects.GRAB_EFFECT))) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });

        // 4. ล็อกการคลิกขวาที่ม็อบตัวอื่น
        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (player.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModStatusEffects.GRAB_EFFECT))) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });

        // 5. ล็อกการใช้งานไอเทมในมือ (เช่น กินอาหาร, กินยา, ถือดาบตั้งป้องกัน)
        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (player.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModStatusEffects.GRAB_EFFECT))) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });
    }
}
