package net.minecraft.network.protocol.common.custom;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record BrandPayload(String brand) implements CustomPacketPayload {
    public static final StreamCodec<FriendlyByteBuf, BrandPayload> STREAM_CODEC = CustomPacketPayload.codec(BrandPayload::write, BrandPayload::new);
    public static final CustomPacketPayload.Type<BrandPayload> TYPE = CustomPacketPayload.createType("brand");

    private BrandPayload(FriendlyByteBuf p_296001_) {
        this(p_296001_.readUtf());
    }

    private void write(FriendlyByteBuf p_294892_) {
        p_294892_.writeUtf(this.brand);
    }

    @Override
    public CustomPacketPayload.Type<BrandPayload> type() {
        return TYPE;
    }
}
