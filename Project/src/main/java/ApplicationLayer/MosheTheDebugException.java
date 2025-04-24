package ApplicationLayer;

import java.util.Arrays;

public class MosheTheDebugException extends RuntimeException {
    public MosheTheDebugException(String message, Object...objects) {
        super("MosheTheDebugException thrown! mesage: " + message +" objects involved: " +Arrays.toString(objects));
    }
    
    public MosheTheDebugException(String message, Throwable cause) {
        super("MosheTheDebugException thrown! message: " + message, cause);
    }

    public MosheTheDebugException(Throwable cause) {
        super("MosheTheDebugException caused by: " + cause.getClass().getSimpleName(), cause);
    }
}
