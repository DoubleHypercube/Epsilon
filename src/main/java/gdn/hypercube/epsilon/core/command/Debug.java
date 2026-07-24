package gdn.hypercube.epsilon.core.command;

import gdn.hypercube.epsilon.core.util.Argument;
import gdn.hypercube.epsilon.core.util.EngineCommand;
import gdn.hypercube.epsilon.core.util.MemoryHelper;
import gdn.hypercube.epsilon.core.util.VarargsCommand;

@SuppressWarnings("unused")
public class Debug {
    EngineCommand NOP = new EngineCommand(EngineCommand.Type.PLAFORM_SPECIFIC, 0x00, (engine, argv) -> {
        // This is a non-op just in case anything ever tries running [00 00].
        // I guess you could also use it to busy-wait for a single cycle...?
    });

    EngineCommand DEBUG = new VarargsCommand(EngineCommand.Type.PLAFORM_SPECIFIC, 0xFF, (engine, argv) -> {
        switch ((int) argv[0].value) {
            case 0 -> engine.dumping = !engine.dumping;
            case 1 -> MemoryHelper.debug = !MemoryHelper.debug;
        }
    }, chars -> switch (chars[0]) {
        default -> 1;
    }, new Argument(Argument.Type.BYTE));
}
