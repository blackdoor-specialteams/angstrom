package black.door.angstrom;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpHeaderValues.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

/**
 * Created by nfischer on 5/22/2016.
 */
public abstract class Serializers {

	public static final Function<ObjectMapper, Function<ByteBuf, JsonNode>>
			jsonDeserializer = mapper -> buf -> {
				try {
					return mapper.readTree(new ByteBufInputStream(buf));
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			};

	public static Request<JsonNode> deserializeJsonRequest(Request<ByteBuf> request, ObjectMapper mapper){
		return request.mapBody(jsonDeserializer.apply(mapper));
	}

	public static final Function<ByteBuf, String> stringDeserializer =
			b -> new String(b.array());

	public static final Function<String, ByteBuf> stringSerializer =
			s -> Unpooled.wrappedBuffer(s.getBytes(StandardCharsets.UTF_8));

	public static Response<ByteBuf> serializeStringResponse(Response<String> r){
		return r
				.mapBody(stringSerializer)
				.withHeader(CONTENT_TYPE, TEXT_PLAIN);
	}

}
