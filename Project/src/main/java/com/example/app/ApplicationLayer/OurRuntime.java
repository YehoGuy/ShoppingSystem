package com.example.app.ApplicationLayer;

import java.util.Arrays;

public class OurRuntime extends RuntimeException {
    public OurRuntime(String message, Object...objects) {
        super("MosheTheDebugException thrown! mesage: " + message +" objects involved: " +Arrays.toString(objects));
    }
    
    public OurRuntime(String message, Throwable cause) {
        super("MosheTheDebugException thrown! message: " + message, cause);
    }

    public OurRuntime(Throwable cause) {
        super("MosheTheDebugException caused by: " + cause.getClass().getSimpleName(), cause);
    }
}
