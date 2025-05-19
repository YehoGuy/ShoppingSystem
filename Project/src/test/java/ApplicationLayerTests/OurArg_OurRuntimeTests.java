package ApplicationLayerTests;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import com.example.app.ApplicationLayer.OurArg;
import com.example.app.ApplicationLayer.OurRuntime;



class OurArg_OurRuntimeTests {

    // ----- OurArg & OurRuntime -----

    @Test
    void testOurArg_Constructors() {
        OurArg a1 = new OurArg("m");
        assertTrue(a1.getMessage().contains("IssacTheDebugException thrown! mesage: m"));

        OurArg a2 = new OurArg("m2", new RuntimeException("c"));
        assertTrue(a2.getMessage().contains("IssacTheDebugException thrown! message: m2"));

        OurArg a3 = new OurArg(new IllegalStateException("err"));
        assertTrue(a3.getMessage().contains("IssacTheDebugException caused by: IllegalStateException"));
    }

    @Test
    void testOurRuntime_Constructors() {
        OurRuntime r1 = new OurRuntime("msg");
        assertTrue(r1.getMessage().contains("MosheTheDebugException thrown! mesage: msg"));

        OurRuntime r2 = new OurRuntime("msg2", new IllegalArgumentException("cause"));
        assertTrue(r2.getMessage().contains("MosheTheDebugException thrown! message: msg2"));

        OurRuntime r3 = new OurRuntime(new NullPointerException());
        assertTrue(r3.getMessage().contains("MosheTheDebugException caused by: NullPointerException"));
    }
}
