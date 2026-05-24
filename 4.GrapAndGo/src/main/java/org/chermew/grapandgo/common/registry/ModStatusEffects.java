package org.chermew.grapandgo.common.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import org.chermew.grapandgo.Grapandgo;
import org.chermew.grapandgo.common.effect.GrabStatusEffect;

public class ModStatusEffects {
    public static final MobEffect GRAB_EFFECT = new GrabStatusEffect(MobEffectCategory.NEUTRAL, 0x98D7C2);

    public static void registerAll() {
        Registry.register(
                BuiltInRegistries.MOB_EFFECT,
                Identifier.fromNamespaceAndPath(Grapandgo.MOD_ID, "grab"),
                GRAB_EFFECT
        );
    }
}
