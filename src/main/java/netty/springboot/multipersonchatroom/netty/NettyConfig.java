package netty.springboot.multipersonchatroom.netty;

import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NettyConfig {

    //Store all connected channels
    public static ChannelGroup group = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    //WebSocket host and port info
    public static String WS_HOST;
    public static int WS_PORT;

    @Value("${netty.host}")
    public void setWS_HOST(String host) {
        WS_HOST = host;
    }

    @Value("${netty.port}")
    public void setWS_PORT(int port) {
        WS_PORT = port;
    }

}
