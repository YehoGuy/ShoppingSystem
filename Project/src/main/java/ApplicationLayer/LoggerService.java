package ApplicationLayer;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggerService {
    private static final Logger logger = LoggerFactory.getLogger(LoggerService.class);


    public static void logMethodExecution(String methodName, Object... args) {
        String argString = Arrays.toString(args);
        logger.info("Executing method: " + methodName + " with arguments: " + argString);
    }
    
    public static void logError(String methodName, Exception e,  Object... args) {
        String argString = Arrays.toString(args);
        logger.error("Error in method: " + methodName + " - " +" with arguments: " + argString + e.getMessage(), e);
    }

    public static void logMethodExecutionEnd(String methodName, Object returnValue) {
        logger.info("Method: " + methodName + " executed successfully. Return value: " + returnValue);
    }

    public static void logMethodExecutionEndVoid(String methodName) {
        logger.info("Method: " + methodName + " executed successfully.");
    }    

    public static void logDebug(String methodName, OurRuntime e){
        logger.debug("Debugging method: " + methodName + " - Exception: " + e.getMessage(), e);
    }

}
