package netty.springboot.multipersonchatroom.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.GlobalEventExecutor;
import netty.springboot.multipersonchatroom.domain.WsMessage;

import com.google.gson.Gson;
import io.netty.handler.codec.http.FullHttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ChannelHandler.Sharable
public class ChatServerHandler extends SimpleChannelInboundHandler<Object> {

    public static final ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private WebSocketServerHandshaker handshaker;
    private static final Logger logger = LoggerFactory.getLogger(ChatServerHandler.class);
    private final Gson gson = new Gson();

    //Invoked when a channel is active; the channel is connected / bound and ready.
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        channelGroup.add(ctx.channel());
    }

    //Invoked when a channel leaves active state and is no longer connected to its remote peer.
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        channelGroup.remove(ctx.channel());
    }

    //Invoked when a signal comes in.
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            handleWsMessage(ctx, (WebSocketFrame) msg);
        }
    }

    //Invoked when a read operation on the channel has been completed.
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    //Invoked when an exception occurs.
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    //Handle http requests: websocket initial handshake (opening handshake) starts with an http request.
    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) {
        if (!req.decoderResult().isSuccess() || !("websocket".equals(req.headers().get("Upgrade")))) {
            sendHttpResponse(ctx, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory("ws://"
                + NettyServerConfig.WS_HOST + NettyServerConfig.WS_PORT, null, false);
        handshaker = wsFactory.newHandshaker(req);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            handshaker.handshake(ctx.channel(), req);
        }
    }

    //Respond to non-websocket initial handshake request.
    private void sendHttpResponse(ChannelHandlerContext ctx,  DefaultFullHttpResponse res) {
        if(res.status().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(res.status().toString(), CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
        }
        ChannelFuture f = ctx.channel().writeAndFlush(res);
        if (res.status().code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }

    //Handle websocket messages coming in.
    private void handleWsMessage(ChannelHandlerContext ctx, WebSocketFrame msg) {
        if (msg instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) msg.retain());
        }
        if (msg instanceof PingWebSocketFrame) {
            //ping info
            ctx.channel().write(new PongWebSocketFrame(msg.content().retain()));
        } else if (msg instanceof TextWebSocketFrame) {
            TextWebSocketFrame message = (TextWebSocketFrame) msg;
            //text message
            WsMessage wsMessage = gson.fromJson(message.text(), WsMessage.class);
            logger.info("Message received：" + wsMessage);
            switch (wsMessage.getT()) {
                case 1: //When entering the chat room, (1) broadcast an incoming message to other users.
                    broadcastWsMessage(ctx, new WsMessage(-10001, wsMessage.getN()));
                    //(2) respond with a welcome message to a new user entering the chat room.
                    AttributeKey<String> name = AttributeKey.newInstance(wsMessage.getN());
                    ctx.channel().attr(name);
                    ctx.channel().writeAndFlush(new TextWebSocketFrame(
                            gson.toJson(new WsMessage(-1, wsMessage.getN()))));
                    break;
                case 2: //When sending a message, broadcast a message.
                    broadcastWsMessage(ctx, new WsMessage(-2, wsMessage.getN(), wsMessage.getBody()));
                    break;
                case 3: //When leaving the chat room, broadcast a message to other users.
                    broadcastWsMessage(ctx, new WsMessage(-11000, wsMessage.getN()));
                    break;
            }
        }
    }

    //Broadcast websocket messages to all users but yourself.
    private void broadcastWsMessage(ChannelHandlerContext ctx, WsMessage msg) {
        channelGroup.stream()
                .filter(channel -> channel.id() != ctx.channel().id())
                .forEach(channel -> {
                    channel.writeAndFlush(new TextWebSocketFrame(gson.toJson(msg)));
                });
    }

}
