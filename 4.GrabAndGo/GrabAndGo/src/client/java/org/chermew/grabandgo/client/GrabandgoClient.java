package org.chermew.grabandgo.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import org.chermew.grabandgo.client.render.CarriedObjectFeatureRenderer;

public class GrabandgoClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Register feature renderer on the player models (covers default and slim styles)
        LivingEntityRenderLayerRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
            if (entityRenderer instanceof AvatarRenderer avatarRenderer) {
                registrationHelper.register(new CarriedObjectFeatureRenderer(avatarRenderer));
            }
        });
    }
}
