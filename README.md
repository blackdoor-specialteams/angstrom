# Angstrom

Angstrom is a small, simple async HTTP server library built on netty.
It is unopinionated and picks no libraries for you. The library focuses on the
functional and async paradigm with `CompletionStage` (a promise or future in some languages).
This encourages *DRY* principals like re-use of components which are often run
before or after many routes, such as loggers, parsers, serializers or authentication.

The entire library can be summed up by the method `get(String, Function<Request,
CompletionStage<Response>>)` used to handle a `GET` request.
Let's try this with a hello world example:

```java
Server server = new Server(80) // create a server on port 80
	.get("^/?$", // respond to GET requests for the path `/` or no path
		r -> completedFuture("Hello world.") // respond with Hello world.
		.thenApply(s -> new Response<>(OK, s)) // wrap our "Hello world." string in an HTTP response object.
		.thenApply(Serializers::serializeStringResponse) // serialize the body of our response and set the content headers
	)
	.start();
```
Currently the path is specified using regex, in the future it will use the URI template standard.
Specify parameters in the path with named capture groups (`(?<name)`).
An example with path parameters `/{a}/{b}`:
```java
server.get("^/(?<a>\\w+)/(?<b>\\w+)$",
		r -> completedFuture(r.parameter("a") + " " + r.parameter("b"))
		.thenApply(s -> new Response<>(OK, s))
		.thenApply(Serializers::serializeStringResponse)
);
```

An example passing in a JSON array in the body, and returning the sum:
```java
server.post("^/json$",
		request -> completedFuture(request.mapBody(Serializers.jsonDeserializer.apply(new ObjectMapper())))
		.thenApply(jsonNodeRequest -> {
			JsonNode body = jsonNodeRequest.body();
			int sum = 0;
			for(int i = 0; i < body.size(); i++){
				sum += body.get(i).asInt();
			}
			return sum;
		})
		.thenApply(sum -> new Response(OK, String.valueOf(sum)))
		.thenApply(Serializers::serializeStringResponse)
);
```

Those would all look a bit messy, we want our route definitions to be clean.
Move the logic into separate methods so the route declarations can look like this:

```java
server
	.get( "^/?$",                     this::helloWorld)
	.get( "^/(?<a>\\w+)/(?<b>\\w+)$", this::pathParams)
	.post("^/json$",                  this::json)
	.start();
```

Those `CompletionStage`s can be tricky. What if we want to return right away and
not do the rest of the stages? The `Controller` class has a helper method `immediately`.
`GET /continue` will return `continue continuing`, but `GET /abort` will return `aborted` and
skip adding `continuing`.

```java
class SwitchController extends Controller{
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
```

We just need to define how to create our controller and invoke our `doIt` method.
```java
server.addRoute(HttpMethod.GET,
	"^/(?<action>\\w+)$",
	SwitchController::new,
	controller -> request -> controller.doIt(request.parameter("action"))
);
```

Since we control creation of the `Controller` at the top level, we can control how dependency injection is done.

Do it ourselves:
```java
server.addRoute(HttpMethod.GET,
	"^/(?<action>\\w+)$",
	() -> new SwitchController(new Dependency()),
	controller -> request -> controller.doIt(request.parameter("action"))
);
```

Create instances of dependencies:

```java
server.addRoute(HttpMethod.GET,
	"^/(?<action>\\w+)$",
	() -> new SwitchController(injector.getInstance(Dependency.class)),
	controller -> request -> controller.doIt(request.parameter("action"))
);
```

Let the DI create the whole controller:

```java
server.addRoute(HttpMethod.GET,
	"^/(?<action>\\w+)$",
	() -> injector.getInstance(SwitchController.class),
	controller -> request -> controller.doIt(request.parameter("action"))
);
```




