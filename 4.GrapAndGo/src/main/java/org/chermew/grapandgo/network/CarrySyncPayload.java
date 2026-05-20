package org.chermew.grapandgo.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record CarrySyncPayload(UUID playerUuid, boolean isCarrying) implements CustomPacketPayload {

    public static final Type<CarrySyncPayload> ID = new Type<>(Identifier.fromNamespaceAndPath("grapandgo", "carry_sync"));

    public static final StreamCodec<ByteBuf, CarrySyncPayload> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, CarrySyncPayload::playerUuid,
            ByteBufCodecs.BOOL, CarrySyncPayload::isCarrying,
            CarrySyncPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return ID; }
}
