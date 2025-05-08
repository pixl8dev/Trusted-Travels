package me.lukasabbe.trustedtravelplugin;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MinecraftServerPinger {
    private static final int PROTOCOL_VERSION = 578; // 1.15.2 protocol version

    public static CompletableFuture<Boolean> pingServer(String host, int port) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        EventLoopGroup group = new NioEventLoopGroup();

        try {
            Bootstrap bootstrap = new Bootstrap()
                .group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ch.pipeline().addLast(
                            new VarIntFrameDecoder(),
                            new MinecraftStatusHandler(future)
                        );
                    }
                });

            ChannelFuture channelFuture = bootstrap.connect(new InetSocketAddress(host, port));
            channelFuture.addListener(f -> {
                if (!f.isSuccess()) {
                    future.complete(false);
                    group.shutdownGracefully();
                }
            });
        } catch (Exception e) {
            future.complete(false);
            group.shutdownGracefully();
        }

        return future;
    }

    private static byte[] createHandshakePacket(String host, int port) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // Packet ID 0x00 (Handshake)
        dos.write(writeVarInt(0x00));
        dos.write(writeVarInt(PROTOCOL_VERSION));
        dos.write(writeVarInt(host.length()));
        dos.write(host.getBytes(StandardCharsets.UTF_8));
        dos.writeShort(port);
        dos.write(writeVarInt(1)); // Next state: Status

        byte[] handshake = baos.toByteArray();
        byte[] length = writeVarInt(handshake.length);

        ByteArrayOutputStream finalPacket = new ByteArrayOutputStream();
        finalPacket.write(length);
        finalPacket.write(handshake);
        return finalPacket.toByteArray();
    }

    private static byte[] writeVarInt(int value) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while ((value & ~0x7F) != 0) {
            baos.write((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        baos.write(value);
        return baos.toByteArray();
    }

    private static class VarIntFrameDecoder extends ByteToMessageDecoder {
        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
            if (!in.isReadable()) return;

            int readerIndex = in.readerIndex();
            byte[] lengthBytes = new byte[5];
            for (int i = 0; i < lengthBytes.length; i++) {
                if (!in.isReadable()) return;
                byte b = in.readByte();
                lengthBytes[i] = b;
                if ((b & 0x80) == 0) break;
            }

            ByteBuf buf = Unpooled.wrappedBuffer(lengthBytes);
            int length = readVarInt(buf);
            if (in.readableBytes() < length) {
                in.readerIndex(readerIndex);
                return;
            }

            ByteBuf packet = in.readSlice(length);
            out.add(packet.retain());
        }

        private int readVarInt(ByteBuf buf) {
            int value = 0;
            int position = 0;
            byte currentByte;

            while (true) {
                currentByte = buf.readByte();
                value |= (currentByte & 0x7F) << position;
                position += 7;

                if ((currentByte & 0x80) == 0) break;
            }

            return value;
        }
    }

    private static class MinecraftStatusHandler extends SimpleChannelInboundHandler<ByteBuf> {
        private final CompletableFuture<Boolean> future;

        public MinecraftStatusHandler(CompletableFuture<Boolean> future) {
            this.future = future;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            ByteBuf handshake = Unpooled.wrappedBuffer(createHandshakePacket(
                ctx.channel().remoteAddress().toString().split(":")[0].substring(1), 
                ((InetSocketAddress)ctx.channel().remoteAddress()).getPort()
            ));
            ctx.writeAndFlush(handshake);

            // Send status request
            ByteBuf statusRequest = Unpooled.buffer();
            statusRequest.writeByte(0x01); // Packet length
            statusRequest.writeByte(0x00); // Packet ID for status request
            ctx.writeAndFlush(statusRequest);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
            try {
                // Successfully read a packet, server is online
                future.complete(true);
                ctx.close();
            } catch (Exception e) {
                future.complete(false);
                ctx.close();
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            future.complete(false);
            ctx.close();
        }
    }
}
