package org.chermew.grapandgo.network;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;

// ใช้ record เพื่อความง่ายในการเก็บข้อมูลค่ะ
public record GrabPayload(int entityId) implements CustomPacketPayload {

    // 1. แก้ Private Access: ใช้ Identifier.of แทนการ new ค่ะ
    public static final CustomPacketPayload.Type<GrabPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("grapandgo", "grab_packet"));

    // 2. แก้ Functional Interface: ใช้ ByteBufCodecs.VAR_INT จะเขียนง่ายและไม่ Error ค่ะ
    public static final StreamCodec<RegistryFriendlyByteBuf, GrabPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, GrabPayload::entityId,
                    GrabPayload::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
