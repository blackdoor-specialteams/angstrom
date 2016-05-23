package black.door.angstrom;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

/**
 * Created by nfischer on 5/17/2016.
 */
public class Initializer extends ChannelInitializer<SocketChannel> {

	private Router router;

	Initializer(Router router){
		this.router = router;
	}

	@Override
	protected void initChannel(SocketChannel socketChannel) throws Exception {
		ChannelPipeline pipe = socketChannel.pipeline();
		pipe
				.addLast(new HttpRequestDecoder())
				.addLast(new HttpObjectAggregator(1048576))
				.addLast(new HttpResponseEncoder())
				.addLast(new RequestHandler(router));
	}
}
