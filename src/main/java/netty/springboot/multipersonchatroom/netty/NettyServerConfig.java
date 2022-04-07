package netty.springboot.multipersonchatroom.netty;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NettyServerConfig {

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
