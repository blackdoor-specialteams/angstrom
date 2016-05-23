package black.door.angstrom;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import static io.netty.handler.codec.http.HttpResponseStatus.*;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpMethod;
import org.junit.Test;

import java.util.concurrent.CompletionStage;

import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * Created by nfischer on 5/22/2016.
 */
public class ServerTest {

	@Test
	public void test() throws InterruptedException {
		Server server = new Server(80);

		server.addRoute(HttpMethod.GET,
				"^/(?<action>\\w+)$",
				SwitchController::new,
				controller -> request -> controller.doIt(request.parameter("action").get())
		);

		server.get( "^/?$",                     this::helloWorld);
		server.get( "^/(?<a>\\w+)/(?<b>\\w+)$", this::pathParams);
		server.post("^/json$",                  this::json);

		server.start();
	}

	CompletionStage<Response> json(Request<ByteBuf> request){
		return completedFuture(request.mapBody(Serializers.jsonDeserializer.apply(new ObjectMapper())))
				.thenApply(jsonNodeRequest -> {
					JsonNode body = jsonNodeRequest.body();
					int sum = 0;
					for(int i = 0; i < body.size(); i++){
						sum += body.get(i).asInt();
					}
					return sum;
				})
				.thenApply(sum -> new Response<>(OK, String.valueOf(sum)))
				.thenApply(Serializers::serializeStringResponse);
	}

	CompletionStage<Response> pathParams(Request r){
		return completedFuture(r.parameter("a") + " " + r.parameter("b"))
			.thenApply(s -> new Response<>(OK, s))
			.thenApply(Serializers::serializeStringResponse);
	}

	CompletionStage<Response> helloWorld(Request r){
		return completedFuture("Hello world.")
			.thenApply(s -> new Response<>(OK, s))
			.thenApply(Serializers::serializeStringResponse);
	}


	static class SwitchController extends Controller{

		public CompletionStage<Response> doIt(String action){

			CompletionStage<Response> result;

			switch (action){
				case "continue":
					result = completedFuture(new Response(OK, "continue"));
					break;
				case "abort":
					result = immediately(new Response(OK, "aborted"));
					break;
				default:
					return completedFuture(new Response(BAD_REQUEST));
			}

			return result.thenApply(response -> response.mapBody(s -> s+" continuing"));
		}
	}
}