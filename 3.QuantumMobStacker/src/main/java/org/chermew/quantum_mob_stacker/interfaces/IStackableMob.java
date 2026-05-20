package org.chermew.quantum_mob_stacker.interfaces;

public interface IStackableMob {
    long getStackCount();
    void setStackCount(long count);
    void addStack(long amount);
}
