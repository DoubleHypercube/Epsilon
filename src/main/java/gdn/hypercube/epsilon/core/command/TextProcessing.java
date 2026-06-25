package gdn.hypercube.epsilon.core.command;

import gdn.hypercube.epsilon.core.EpsilonEngine;
import gdn.hypercube.epsilon.core.util.Argument;
import gdn.hypercube.epsilon.core.util.EngineCommand;

@SuppressWarnings("unused")
public class TextProcessing {
    EngineCommand NewLine = new EngineCommand(EngineCommand.Type.TEXT_PROCESSING, 0, (engine, argv) -> {
        engine.newline();
    });

    EngineCommand Halt = new EngineCommand(EngineCommand.Type.TEXT_PROCESSING, 1, (engine, argv) -> {
       engine.status = EpsilonEngine.Status.HALTED;
    });

    EngineCommand WaitForInput = new EngineCommand(EngineCommand.Type.TEXT_PROCESSING, 2, (engine, argv) -> {
        engine.status = EpsilonEngine.Status.WAITING;
    });

    EngineCommand DelayFrames = new EngineCommand(EngineCommand.Type.TEXT_PROCESSING, 3, (engine, argv) -> {
        engine.pauseTarget = argv[0].value;
        engine.pauseTime = 0;
        engine.status = EpsilonEngine.Status.PAUSED;
    }, new Argument(Argument.Type.BYTE));

    EngineCommand Colourize = new EngineCommand(EngineCommand.Type.TEXT_PROCESSING, 4, (engine, argv) -> {
        int min = (int) argv[0].value;
        long max = argv[1].value;
        int colour = (int) argv[2].value;
        for (int index = 0; index < max; index++) {
            engine.colours.put(min + index, colour);
        }
    }, new Argument(Argument.Type.BYTE), new Argument(Argument.Type.BYTE), new Argument(Argument.Type.INT));
}
