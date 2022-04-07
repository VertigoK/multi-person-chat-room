package netty.springboot.multipersonchatroom;

import io.netty.channel.ChannelFuture;
import netty.springboot.multipersonchatroom.netty.NettyServerConfig;
import netty.springboot.multipersonchatroom.netty.ChatServerBootStrap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.InetSocketAddress;

@SpringBootApplication
public class MultiPersonChatroomApplication implements CommandLineRunner {

	private static final Logger logger = LoggerFactory.getLogger(MultiPersonChatroomApplication.class);

	@Autowired
	private ChatServerBootStrap ws;

	public static void main(String[] args) throws Exception {
		SpringApplication.run(MultiPersonChatroomApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		logger.info("Netty's websocket server is listening: " + NettyServerConfig.WS_PORT);
		InetSocketAddress address = new InetSocketAddress(NettyServerConfig.WS_HOST, NettyServerConfig.WS_PORT);
		ChannelFuture future = ws.start(address);
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				ws.destroy();
			}
		});
		future.channel().closeFuture().syncUninterruptibly();
	}

}
