package org.chermew.lushstack;

import net.fabricmc.api.ModInitializer;
import org.chermew.lushstack.components.ItemStackComponents;

public class Lushstack implements ModInitializer {

    @Override
    public void onInitialize() {
        ItemStackComponents.register();
    }
}
