package gdn.hypercube.epsilon.core.command;

import gdn.hypercube.epsilon.core.util.EngineCommand;

@SuppressWarnings("unused")
public class Debug {
    EngineCommand NOP = new EngineCommand(EngineCommand.Type.PLAFORM_SPECIFIC, 0x00, (engine, argv) -> {
        // This is a non-op just in case anything ever tries running [00 00].
        // I guess you could also use it to busy-wait for a single cycle...?
    });
}
