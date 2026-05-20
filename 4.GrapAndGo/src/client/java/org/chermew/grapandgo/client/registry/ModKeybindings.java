package org.chermew.grapandgo.client.registry;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class ModKeybindings {
    // ประกาศตัวแปร KeyMapping สำหรับการอุ้ม/วาง (Grap/Drop)
    public static KeyMapping grapKey;

    public static void register() {
        // ลงทะเบียนโดยใช้ KeyMapping
        grapKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.grapandgo.grap",
                InputConstants.Type.MOUSE,
                GLFW.GLFW_MOUSE_BUTTON_RIGHT,
                KeyMapping.Category.GAMEPLAY
        ));
    }
}
