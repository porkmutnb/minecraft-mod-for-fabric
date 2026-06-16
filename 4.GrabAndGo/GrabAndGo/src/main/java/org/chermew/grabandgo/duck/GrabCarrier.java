package org.chermew.grabandgo.duck;

import net.minecraft.nbt.CompoundTag;

public interface GrabCarrier {
    boolean grabandgo$isCarrying();
    void grabandgo$setCarrying(boolean carrying);
    
    CompoundTag grabandgo$getCarriedData();
    void grabandgo$setCarriedData(CompoundTag tag);
    
    void grabandgo$clearCarried();
}
