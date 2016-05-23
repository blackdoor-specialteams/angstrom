package black.door.angstrom;

import io.netty.buffer.ByteBuf;

/**
 * Created by nfischer on 5/22/2016.
 */
public class BodyNotSerializedException extends RuntimeException {
	BodyNotSerializedException(Class bodyType){
		super("Final response body was of type " + bodyType.getCanonicalName()
		+ " expected " + ByteBuf.class);
	}
}
