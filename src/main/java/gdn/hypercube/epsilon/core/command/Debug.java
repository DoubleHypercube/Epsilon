package gdn.hypercube.epsilon.core.command;

import gdn.hypercube.epsilon.core.util.EngineCommand;

@SuppressWarnings("unused")
public class Debug {
    EngineCommand Debug = new EngineCommand(EngineCommand.Type.PLAFORM_SPECIFIC, 0x00, (engine, argv) -> {
        System.out.println("Dumping decision tree...");
        System.out.println();
        engine.decisions.forEach((index, entry) -> {
            System.out.println("    Entry index: " + index);
            System.out.println("    Entry string: " + entry.left());
            System.out.println("    Entry target: " + entry.right());
        });
    });
}
