package black.door.angstrom;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Created by nfischer on 5/18/2016.
 */
public abstract class Controller {
	private CompletableFuture<Response> _short = new CompletableFuture<>();

	protected CompletionStage<Response> _short(){
		return _short;
	}

	public CompletionStage<Response> immediately(Response result){
		_short.complete(result);
		return new CompletableFuture<>();
	}
}
