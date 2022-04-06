package netty.springboot.multipersonchatroom.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import netty.springboot.multipersonchatroom.pojo.WsMessage;

import com.google.gson.Gson;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketHandler extends SimpleChannelInboundHandler<Object> {

    private WebSocketServerHandshaker handshaker;
    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);
    private final Gson gson = new Gson();

    //ws.onopen in chat.js
    //Invoked when a channel is active; the channel is connected/bound and ready.
    //When the connection is open, this means that there is data coming in
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        NettyConfig.group.add(ctx.channel());
    }

    //ws.onclose in chat.js
    //Invoked when a channel leaves active state and is no longer connected to its remote peer.
    //When the connection is about to be closed
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        broadcastWsMessage(ctx, new WsMessage(-11000, ctx.channel().id().toString()));
        NettyConfig.group.remove(ctx.channel());
    }

    //ws.onmessage in chat.js
    //Invoked when a signal comes in
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            handHttpRequest(ctx, (FullHttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            handWsMessage(ctx, (WebSocketFrame) msg);
        }
    }

    //Invoked when a read operation on the channel has been completed.
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    //ws.onerror in chat.js
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    //Deal with http request. WebSocket initial handshake (opening handshake) starts with http request.
    private void handHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) {
        if(!req.decoderResult().isSuccess() || !("websocket".equals(req.headers().get("Upgrade")))){
            sendHttpResponse(ctx, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }
        WebSocketServerHandshakerFactory factory = new WebSocketServerHandshakerFactory("ws://" + NettyConfig.WS_HOST + NettyConfig.WS_PORT, null, false);
        handshaker = factory.newHandshaker(req);
        if(handshaker == null){
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            handshaker.handshake(ctx.channel(), req);
        }
    }

    //Respond to non-WebSocket initial handshake request.
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

    private void handWsMessage(ChannelHandlerContext ctx, WebSocketFrame msg) {
        if (msg instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) msg.retain());
        }
        if (msg instanceof PingWebSocketFrame) {
            ctx.channel().write(new PongWebSocketFrame(msg.content().retain()));
        } else if (msg instanceof TextWebSocketFrame) {
            TextWebSocketFrame message = (TextWebSocketFrame) msg;
            WsMessage wsMessage = gson.fromJson(message.text(), WsMessage.class);
            logger.info("Message received：" + wsMessage);
            switch (wsMessage.getT()) {
                case 1:
                    broadcastWsMessage( ctx, new WsMessage(-10001, wsMessage.getN()) );
                    AttributeKey<String> name = AttributeKey.newInstance(wsMessage.getN());
                    ctx.channel().attr(name);
                    ctx.channel().writeAndFlush( new TextWebSocketFrame(gson.toJson(new WsMessage(-1, wsMessage.getN()))));
                    break;
                case 2:
                    broadcastWsMessage( ctx, new WsMessage(-2, wsMessage.getN(), wsMessage.getBody()));
                    break;
                case 3:
                    broadcastWsMessage( ctx, new WsMessage(-11000, wsMessage.getN()));
                    break;
            }
//            NettyConfig.group.writeAndFlush(new TextWebSocketFrame(new Date().toString()));
        }else {
            // donothing
        }
    }

    private void broadcastWsMessage(ChannelHandlerContext ctx, WsMessage msg) {
        NettyConfig.group.stream()
                .filter(channel -> channel.id() != ctx.channel().id())
                .forEach(channel -> {
                    channel.writeAndFlush(new TextWebSocketFrame(gson.toJson(msg)));
                });
    }

}
