package black.door.angstrom;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import org.javatuples.Pair;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;

/**
 * Created by nfischer on 5/17/2016.
 */
public class RequestHandler extends SimpleChannelInboundHandler<Object> {

	private final Router router;
	private final Map<Class, Function<?, ByteBuf>> serializers;

	public RequestHandler(Router router){
		this.router = router;
		serializers = new ConcurrentHashMap<>();
		serializers.put(String.class, Serializers.stringSerializer);
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object o) throws Exception {
		if(o instanceof FullHttpRequest){
			FullHttpRequest request = (FullHttpRequest) o;
			Optional<Pair<Function<Request<ByteBuf>, CompletionStage<Response>>, Map<String, String>>> handler = router.route(request.method(),
					URI.create(request.uri()).getPath());

			handler
					.map(pair -> pair.getValue0().apply(Request.fromNettyRequest(request, pair.getValue1())))
					.orElseGet(() -> completedFuture(new Response(NOT_FOUND)))
					//.thenApply(this::autoOK)
					.thenApply(this::serializeBody)
					.exceptionally(this::handleException)
					.thenAccept(result -> sendResponse(ctx, result));
		}else{
			//freak out
		}
	}

	private <T> Response<T> autoOK(T o){
		if(o instanceof Response)
			return (Response) o;
		return new Response<>(OK, o);
	}

	private Response<ByteBuf> serializeBody(Response response){
		Object body = response.body();
		if(body != null && !(body instanceof ByteBuf)){
			return serializers.entrySet().stream()
					.filter(e -> e.getKey().isInstance(body))
					.findAny()
					.map(s -> response.mapBody(s.getValue()))
					.orElseThrow(() -> new BodyNotSerializedException(body.getClass()));
		}
		return response;
	}

	private Response handleException(Throwable t){
		return new Response(INTERNAL_SERVER_ERROR);
	}

	private void sendResponse(ChannelHandlerContext ctx, Response<ByteBuf> responseObject){
		FullHttpResponse response = responseObject.toNettyResponse();

		// Add 'Content-Length' header only for a keep-alive connection.
		response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
		// Add keep alive header as per:
		// - http://www.w3.org/Protocols/HTTP/1.1/draft-ietf-http-v11-spec-01.html#Connection
		response.headers().set(CONNECTION, KEEP_ALIVE);
		ctx.writeAndFlush(response);
	}
}
