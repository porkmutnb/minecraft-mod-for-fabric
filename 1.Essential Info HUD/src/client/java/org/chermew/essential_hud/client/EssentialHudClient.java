package org.chermew.essential_hud.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;
import org.chermew.essential_hud.client.overlay.HUDInfoOverlay;

public class EssentialHudClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        System.out.println(">>> [TestProject] Client Initialized! <<<");
        HudElementRegistry.addFirst(
                Identifier.fromNamespaceAndPath("testproject", "hud_overlay"),
                new HUDInfoOverlay()
        );
    }
}
