package org.chermew.grabandgo;

import net.fabricmc.api.ModInitializer;
import org.chermew.grabandgo.event.GrabHandler;

public class Grabandgo implements ModInitializer {

    @Override
    public void onInitialize() {
        GrabHandler.register();
    }
}
