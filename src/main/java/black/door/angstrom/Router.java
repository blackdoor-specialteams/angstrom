package black.door.angstrom;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpMethod;
import org.javatuples.Pair;

import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by nfischer on 5/17/2016.
 */
public class Router {
	Map<Pair<HttpMethod, Pattern>, Pair<Function<Request<ByteBuf>, CompletionStage<Response>>, Set<String>>> routes = new ConcurrentHashMap<>();

	static final Pattern groupRegex = Pattern.compile("\\(\\?\\<(?<name>\\w+)\\>");

	Optional<Pair<Function<Request<ByteBuf>, CompletionStage<Response>>, Map<String, String>>> route(final HttpMethod method, String path){
		for(Map.Entry<Pair<HttpMethod, Pattern>, Pair<Function<Request<ByteBuf>, CompletionStage<Response>>, Set<String>>> e
				: routes.entrySet()){
			if(e.getKey().getValue0().equals(method)){
				Matcher m = e.getKey().getValue1().matcher(path);
				if(m.matches()){
					Map<String, String> params = new HashMap<>();
					for(String groupName : e.getValue().getValue1()){
						params.put(groupName, m.group(groupName));
					}
					return Optional.of(e.getValue().setAt1(params));
				}
			}
		}

		return Optional.empty();
	}

	void addRoute(HttpMethod method, Pattern path, Function<Request<ByteBuf>, CompletionStage<Response>> handler){
		Matcher m = groupRegex.matcher(path.pattern());
		Set<String> groupNames = new HashSet<>();
		while (m.find()) {
			groupNames.add(m.group("name"));
		}
		routes.put(Pair.with(method, path), Pair.with(handler, groupNames));
	}

}
