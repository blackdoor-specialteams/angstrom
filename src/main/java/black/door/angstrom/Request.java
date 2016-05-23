package black.door.angstrom;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.*;

import java.net.URI;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableMap;

/**
 * Created by nfischer on 5/17/2016.
 */
public class Request<B> {

	private static <T> T lazy(T value, Supplier<T> s){
		return value == null ? s.get() : value;
	}

	static Request<ByteBuf> fromNettyRequest(FullHttpRequest request,
	                                         Map<String, String> params){
		Map<String, String> headers = new HashMap<>();
		request.headers()
				.iteratorAsString()
				.forEachRemaining(e -> headers.put(e.getKey(), e.getValue()));

		Map<String, List<String>> paramz = new HashMap<>();

		params.entrySet().forEach(e -> paramz.put(e.getKey(), asList(e.getValue())));

		return new Request<>(
				request.method(),
				URI.create(request.uri()),
				headers,
				request.content(),
				paramz);
	}

	private final Map<String, String> headers;
	private final HttpMethod method;
	private final URI uri;
	private final Map<String, List<String>> params;
	private final B body;

	public Request(HttpMethod method, URI uri, Map<String, String> headers, B body){
		this(method, uri, headers, body, new QueryStringDecoder(uri).parameters());
	}

	protected Request(HttpMethod method, URI uri, Map<String, String> headers, B body, Map<String, List<String>> params){
		this.method = method;
		this.uri = uri;
		this.headers = unmodifiableMap(new HashMap<>(headers));
		this.params = unmodifiableMap(params);
		this.body = body;
	}

	public String path(){
		return uri.getPath();
	}

	public Map<String, String> headers(){
		return headers;
	}

	public Optional<String> header(String name){
		return Optional.ofNullable(headers.get(name));
	}

	public Map<String,List<String>> parameters(){
		return params;
	}

	public Optional<String> parameter(String key){
		return Optional.ofNullable(params.get(key))
				.filter(l -> l.size() > 0)
				.map(l -> l.get(0));
	}

	public HttpMethod method(){
		return method;
	}

	public URI uri(){
		return uri;
	}

	public B body(){
		return body;
	}

	public <T> Request<T> mapBody(Function<B, T> bodyMapper){
		return withBody(bodyMapper.apply(body));
	}

	public <T> Request<T> withBody(T body){
		return new Request<>(method, uri, headers, body, params);
	}

	public Request<B> withHeader(String headerName, String headerValue){
		Map<String, String> headers = new HashMap<>(this.headers);
		headers.put(headerName, headerValue);
		return new Request<>(method, uri, headers, body, params);
	}

	public Request<B> withHeaders(Map<String, String> headers){
		return new Request<>(method, uri, headers, body, params);
	}

	public Request<B> withMethod(HttpMethod method){
		return new Request<>(method, uri, headers, body, params);
	}

	public <T> T map(Function<Request<B>, T> fn){
		return fn.apply(this);
	}
}
