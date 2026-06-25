package gdn.hypercube.epsilon.core.command;

import gdn.hypercube.epsilon.core.EpsilonEngine;
import gdn.hypercube.epsilon.core.util.Argument;
import gdn.hypercube.epsilon.core.util.EngineCommand;
import gdn.hypercube.epsilon.core.util.MemoryHelper;
import gdn.hypercube.epsilon.core.util.Pair;
import gdn.hypercube.epsilon.core.util.VarargsCommand;

@SuppressWarnings("unused")
public class ControlFlow {
    private static long regValue(EpsilonEngine engine, long register) {
        return MemoryHelper.readLong(engine.memory, MemoryHelper.registerBase((int) register));
    }

    EngineCommand SetDecisionIndex = new EngineCommand(
        EngineCommand.Type.CONTROL_FLOW, 0x11,
        (engine, argv) -> engine.decindex = (int) argv[0].value,
        new Argument(Argument.Type.INT)
    );

    EngineCommand SetDecisionText = new VarargsCommand(
        EngineCommand.Type.CONTROL_FLOW, 0x0F,
        (engine, argv) -> {
            char[] raw = new char[argv.length];
            for (int index = 1; index < argv.length; index++) {
                raw[index] = (char) argv[index].value;
            }
            String text = new String(raw);
            Pair<String, Integer> existing = engine.decisions.get(engine.decindex);
            if (existing == null) existing = new Pair<>("", 0);
            Pair<String, Integer> entry = new Pair<>(text, existing.right());
            engine.decisions.put(engine.decindex, entry);
        }, chars -> chars[0] + 1,
        new Argument(Argument.Type.BYTE)
    );

    EngineCommand SetDecisionJumpTarget = new EngineCommand(
        EngineCommand.Type.CONTROL_FLOW, 0x10,
        (engine, argv) -> {
            int address = (int) argv[0].value;
            Pair<String, Integer> existing = engine.decisions.get(engine.decindex);
            if (existing == null) existing = new Pair<>("", 0);
            Pair<String, Integer> entry = new Pair<>(existing.left(), address);
            engine.decisions.put(engine.decindex, entry);
        },
        new Argument(Argument.Type.INT)
    );

    /** Push Decision To Register */
    EngineCommand PDTR = new EngineCommand(
        EngineCommand.Type.CONTROL_FLOW,
        0x12,
        (engine, argv) -> {
            Pair<String, Integer> existing = engine.decisions.get(engine.decindex);
            if (existing == null) existing = new Pair<>("", 0);
            MemoryHelper.writeIntAt(engine.memory, MemoryHelper.registerBase((int) argv[0].value), (int) argv[1].value, existing.right());
        },
        new Argument(Argument.Type.BYTE)
    );

    EngineCommand Jump = new EngineCommand(
        EngineCommand.Type.CONTROL_FLOW, 0x00,
        (engine, argv) -> engine.jump((int) argv[0].value),
        new Argument(Argument.Type.INT)
    );

    EngineCommand JumpSubroutine = new EngineCommand(
        EngineCommand.Type.CONTROL_FLOW, 0x05,
        (engine, argv) -> engine.jsr((int) argv[0].value),
        new Argument(Argument.Type.INT)
    );

    EngineCommand ReturnSubroutine = new EngineCommand(
        EngineCommand.Type.CONTROL_FLOW, 0x0A,
        (engine, argv) -> engine.ret()
    );

    EngineCommand JumpEqual = new EngineCommand(
        EngineCommand.Type.CONTROL_FLOW, 0x01,
        (engine, argv) -> {
            if (regValue(engine, argv[0].value) == argv[1].value)
                engine.jump((int) argv[2].value);
        },
        new Argument(Argument.Type.BYTE),
        new Argument(Argument.Type.LONG),
        new Argument(Argument.Type.INT)
    );

    EngineCommand JumpNotEqual = new EngineCommand(
        EngineCommand.Type.CONTROL_FLOW, 0x02,
        (engine, argv) -> {
            if (regValue(engine, argv[0].value) != argv[1].value)
                engine.jump((int) argv[2].value);
        },
        new Argument(Argument.Type.BYTE),
        new Argument(Argument.Type.LONG),
        new Argument(Argument.Type.INT)
    );

    EngineCommand JumpLessThan = new EngineCommand(
        EngineCommand.Type.CONTROL_FLOW, 0x03,
        (engine, argv) -> {
            if (regValue(engine, argv[0].value) < argv[1].value)
                engine.jump((int) argv[2].value);
        },
        new Argument(Argument.Type.BYTE),
        new Argument(Argument.Type.LONG),
        new Argument(Argument.Type.INT)
    );

    EngineCommand JumpGreaterThan = new EngineCommand(
        EngineCommand.Type.CONTROL_FLOW, 0x04,
        (engine, argv) -> {
            if (regValue(engine, argv[0].value) > argv[1].value)
                engine.jump((int) argv[2].value);
        },
        new Argument(Argument.Type.BYTE),
        new Argument(Argument.Type.LONG),
        new Argument(Argument.Type.INT)
    );

    EngineCommand JumpLessOrEqual = new EngineCommand(
        EngineCommand.Type.CONTROL_FLOW, 0x0B,
        (engine, argv) -> {
            if (regValue(engine, argv[0].value) <= argv[1].value)
                engine.jump((int) argv[2].value);
        },
        new Argument(Argument.Type.BYTE),
        new Argument(Argument.Type.LONG),
        new Argument(Argument.Type.INT)
    );

    EngineCommand JumpGreaterOrEqual = new EngineCommand(
        EngineCommand.Type.CONTROL_FLOW, 0x0C,
        (engine, argv) -> {
            if (regValue(engine, argv[0].value) >= argv[1].value)
                engine.jump((int) argv[2].value);
        },
        new Argument(Argument.Type.BYTE),
        new Argument(Argument.Type.LONG),
        new Argument(Argument.Type.INT)
    );

    EngineCommand JumpSubroutineEqual = new EngineCommand(
        EngineCommand.Type.CONTROL_FLOW, 0x06,
        (engine, argv) -> {
            if (regValue(engine, argv[0].value) == argv[1].value)
                engine.jsr((int) argv[2].value);
        },
        new Argument(Argument.Type.BYTE),
        new Argument(Argument.Type.LONG),
        new Argument(Argument.Type.INT)
    );

    EngineCommand JumpSubroutineNotEqual = new EngineCommand(
        EngineCommand.Type.CONTROL_FLOW, 0x07,
        (engine, argv) -> {
            if (regValue(engine, argv[0].value) != argv[1].value)
                engine.jsr((int) argv[2].value);
        },
        new Argument(Argument.Type.BYTE),
        new Argument(Argument.Type.LONG),
        new Argument(Argument.Type.INT)
    );

    EngineCommand JumpSubroutineLessThan = new EngineCommand(
        EngineCommand.Type.CONTROL_FLOW, 0x08,
        (engine, argv) -> {
            if (regValue(engine, argv[0].value) < argv[1].value)
                engine.jsr((int) argv[2].value);
        },
        new Argument(Argument.Type.BYTE),
        new Argument(Argument.Type.LONG),
        new Argument(Argument.Type.INT)
    );

    EngineCommand JumpSubroutineGreaterThan = new EngineCommand(
        EngineCommand.Type.CONTROL_FLOW, 0x09,
        (engine, argv) -> {
            if (regValue(engine, argv[0].value) > argv[1].value)
                engine.jsr((int) argv[2].value);
        },
        new Argument(Argument.Type.BYTE),
        new Argument(Argument.Type.LONG),
        new Argument(Argument.Type.INT)
    );

    EngineCommand JumpSubroutineLessOrEqual = new EngineCommand(
        EngineCommand.Type.CONTROL_FLOW, 0x0D,
        (engine, argv) -> {
            if (regValue(engine, argv[0].value) <= argv[1].value)
                engine.jsr((int) argv[2].value);
        },
        new Argument(Argument.Type.BYTE),
        new Argument(Argument.Type.LONG),
        new Argument(Argument.Type.INT)
    );

    EngineCommand JumpSubroutineGreaterOrEqual = new EngineCommand(
        EngineCommand.Type.CONTROL_FLOW, 0x0E,
        (engine, argv) -> {
            if (regValue(engine, argv[0].value) >= argv[1].value)
                engine.jsr((int) argv[2].value);
        },
        new Argument(Argument.Type.BYTE),
        new Argument(Argument.Type.LONG),
        new Argument(Argument.Type.INT)
    );
}