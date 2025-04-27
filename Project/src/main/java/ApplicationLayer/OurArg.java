package ApplicationLayer;

import java.util.Arrays;

public class OurArg extends IllegalArgumentException {
    public OurArg(String message, Object...objects) {
        super("IssacTheDebugException thrown! mesage: " + message +" objects involved: " +Arrays.toString(objects));
    }
    
    public OurArg(String message, Throwable cause) {
        super("IssacTheDebugException thrown! message: " + message, cause);
    }

    public OurArg(Throwable cause) {
        super("IssacTheDebugException caused by: " + cause.getClass().getSimpleName() + " " + cause.getMessage(), cause);
    }
    
}
