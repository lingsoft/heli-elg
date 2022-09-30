package eu.elg.heli;

import eu.elg.heli.impl.HeLI;
import io.micronaut.runtime.Micronaut;

public class Application {

    static {
        // set up HeLI in a static initializer so it can be initialized at compile time by GraalVM
        HeLI.setup();
    }

    public static void main(String[] args) {
        Micronaut.run(Application.class, args);
    }
}
