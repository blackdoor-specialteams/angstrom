package black.door.angstrom;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static java.util.Collections.unmodifiableMap;

/**
 * Created by nfischer on 5/19/2016.
 */
public class Response<B> {

	private final HttpResponseStatus status;
	private final Map<String, String> headers;

	private final B body;

	public Response(HttpResponseStatus status){
		this(status, new HashMap<>(), null);
	}

	public Response(HttpResponseStatus status, Map<String, String> headers){
		this(status, headers, null);
	}

	public Response(HttpResponseStatus status, B body){
		this(status, new HashMap<>(), body);
	}

	public Response(HttpResponseStatus status, Map<String, String> headers, B body){
		this.status = status;

		this.headers = unmodifiableMap(new HashMap<>(headers));
		this.body = body;
	}

	public B body(){
		return body;
	}

	public Response<B> withStatus(HttpResponseStatus status){
		return new Response<>(status, headers, body);
	}

	public Response<B> withHeader(CharSequence headerName, CharSequence headerValue){
		Map<String, String> headers = new HashMap<>(this.headers);
		headers.put(headerName.toString(), headerValue.toString());
		return new Response<>(status, headers, body);
	}

	public Response<B> withHeaders(Map<String, String> headers){
		return new Response<>(status, headers, body);
	}

	public <T> Response<T> mapBody(Function<B, T> bodyMapper){
		return this.withBody(bodyMapper.apply(body));
	}

	public <T> Response<T> withBody(T body){
		return new Response<>(status, headers, body);
	}

	FullHttpResponse toNettyResponse(){
		DefaultFullHttpResponse response;
		if(body == null){
			response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status);
		}else{
			if(body instanceof ByteBuf){
				ByteBuf body = (ByteBuf) this.body;
				response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, body);
			}else
				throw new BodyNotSerializedException(body.getClass());
		}

		HttpHeaders headers = response.headers();
		this.headers.forEach(headers::add);

		return response;
	}

}
