package net.minecraft.client.multiplayer;

import com.google.common.base.Splitter;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.util.List;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.server.network.LegacyProtocolUtils;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LegacyServerPinger extends SimpleChannelInboundHandler<ByteBuf> {
    private static final Splitter SPLITTER = Splitter.on('\u0000').limit(6);
    private final ServerAddress address;
    private final LegacyServerPinger.Output output;

    public LegacyServerPinger(ServerAddress p_295697_, LegacyServerPinger.Output p_295291_) {
        this.address = p_295697_;
        this.output = p_295291_;
    }

    @Override
    public void channelActive(ChannelHandlerContext p_294106_) throws Exception {
        super.channelActive(p_294106_);
        ByteBuf bytebuf = p_294106_.alloc().buffer();

        try {
            bytebuf.writeByte(254);
            bytebuf.writeByte(1);
            bytebuf.writeByte(250);
            LegacyProtocolUtils.writeLegacyString(bytebuf, "MC|PingHost");
            int i = bytebuf.writerIndex();
            bytebuf.writeShort(0);
            int j = bytebuf.writerIndex();
            bytebuf.writeByte(127);
            LegacyProtocolUtils.writeLegacyString(bytebuf, this.address.getHost());
            bytebuf.writeInt(this.address.getPort());
            int k = bytebuf.writerIndex() - j;
            bytebuf.setShort(i, k);
            p_294106_.channel().writeAndFlush(bytebuf).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
        } catch (Exception exception) {
            bytebuf.release();
            throw exception;
        }
    }

    protected void channelRead0(ChannelHandlerContext p_295830_, ByteBuf p_294393_) {
        short short1 = p_294393_.readUnsignedByte();
        if (short1 == 255) {
            String s = LegacyProtocolUtils.readLegacyString(p_294393_);
            List<String> list = SPLITTER.splitToList(s);
            if ("\u00a71".equals(list.get(0))) {
                int i = Mth.getInt(list.get(1), 0);
                String s1 = list.get(2);
                String s2 = list.get(3);
                int j = Mth.getInt(list.get(4), -1);
                int k = Mth.getInt(list.get(5), -1);
                this.output.handleResponse(i, s1, s2, j, k);
            }
        }

        p_295830_.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext p_296319_, Throwable p_296239_) {
        p_296319_.close();
    }

    @FunctionalInterface
    @OnlyIn(Dist.CLIENT)
    public interface Output {
        void handleResponse(int p_295657_, String p_296381_, String p_295397_, int p_295673_, int p_295810_);
    }
}
