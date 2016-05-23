package black.door.angstrom;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import static java.util.function.Function.identity;

/**
 * Created by nfischer on 5/16/2016.
 */
public class Server implements Runnable{
	//Function<Request<ByteBuf>, CompletionStage<Response<ByteBuf>>>

	private int port;
	private boolean ssl = true;
	private Router router = new Router();

	public Server(int port){
		this.port = port;
		router = new Router();
	}

	public Server addRoute(
			HttpMethod method,
			String pattern,
			Function<Request<ByteBuf>, CompletionStage<Response>> handler){
		router.addRoute(method, Pattern.compile(pattern), handler);
		return this;
	}

	public <C extends Controller> Server addRoute(
			HttpMethod method,
			String pattern,
			Supplier<C> controllerFactory,
			Function<C, Function<Request<ByteBuf>, CompletionStage<Response>>> caller){
		Function<Request<ByteBuf>, CompletionStage<Response>> h = r -> {
			C c = controllerFactory.get();
			return caller
					.apply(c)
					.apply(r)
					.applyToEither(c._short(), identity());
		};
		addRoute(method, pattern, h);

		return this;
	}

	public Server get(String pattern, Function<Request<ByteBuf>, CompletionStage<Response>> handler){
		return addRoute(HttpMethod.GET, pattern, handler);
	}

	public Server post(String pattern, Function<Request<ByteBuf>, CompletionStage<Response>> handler){
		return addRoute(HttpMethod.POST, pattern, handler);
	}

	public Server patch(String pattern, Function<Request<ByteBuf>, CompletionStage<Response>> handler){
		return addRoute(HttpMethod.PATCH, pattern, handler);
	}

	public Server put(String pattern, Function<Request<ByteBuf>, CompletionStage<Response>> handler){
		return addRoute(HttpMethod.PUT, pattern, handler);
	}

	public Server delete(String pattern, Function<Request<ByteBuf>, CompletionStage<Response>> handler){
		return addRoute(HttpMethod.DELETE, pattern, handler);
	}

	public void run(){
		try {
			start();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public Server start() throws InterruptedException {
		EventLoopGroup bossGroup = new NioEventLoopGroup(1);
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		try {
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup)
					.channel(NioServerSocketChannel.class)
					.handler(new LoggingHandler(LogLevel.INFO))
					.childHandler(new Initializer(router));

			Channel ch = b.bind(port).sync().channel();

			System.err.println("Open your web browser and navigate to " +
					(ssl ? "https" : "http") + "://127.0.0.1:" + port + '/');

			ch.closeFuture().sync();
		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}

		return this;
	}
}
