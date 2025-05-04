package ApplicationLayer;

import java.util.Arrays;

public class OurRuntime extends RuntimeException {
    public OurRuntime(String message, Object...objects) {
        super("RuntimeException! MosheTheDebugException thrown! mesage: " + message +" objects involved: " +Arrays.toString(objects));
    }
    
    public OurRuntime(String message, Throwable cause) {
        super("RuntimeException! MosheTheDebugException thrown! message: " + message, cause);
    }

    public OurRuntime(Throwable cause) {
        super("RuntimeException! MosheTheDebugException caused by: " + cause.getClass().getSimpleName(), cause);
    }
}
