package gdn.hypercube.epsilon.core.command;

import gdn.hypercube.epsilon.core.util.Argument;
import gdn.hypercube.epsilon.core.util.EngineCommand;
import gdn.hypercube.epsilon.core.util.VarargsCommand;

@SuppressWarnings("unused")
public class Debug {
    private enum Flags {
        BYTECODE_DUMPING,
        MEMORY_INTROSPECTION,
        JUMP_INTROSPECTION,
    }

    EngineCommand NOP = new EngineCommand(EngineCommand.Type.PLAFORM_SPECIFIC, 0x00, (engine, argv) -> {
        // This is a non-op just in case anything ever tries running [00 00].
        // I guess you could also use it to busy-wait for a single cycle...?
    });

    EngineCommand DEBUG = new VarargsCommand(EngineCommand.Type.PLAFORM_SPECIFIC, 0xFF, (engine, argv) -> {
        int flag = (int) argv[0].value;
        switch (flag) {
            default -> {
                boolean toggled = !engine.debug[flag];
                engine.debug[flag] = toggled;
                System.out.println("Set debug flag " + Flags.values()[flag].name() + " " + toggled);
            }
        }
    }, chars -> switch (chars[0]) {
        default -> 1;
    }, new Argument(Argument.Type.BYTE));
}
