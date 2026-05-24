package org.chermew.quantum_mob_stacker.interfaces;

public interface IStackableMob {
    long getStackCount();
    void setStackCount(long count);
    void addStack(long amount);
    boolean quantum_mob_stacker$isBreeding();
    void quantum_mob_stacker$setBreeding(boolean breeding);
}

