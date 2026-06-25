package gdn.hypercube.epsilon.core.command;

import gdn.hypercube.epsilon.core.util.EngineCommand;
import gdn.hypercube.epsilon.core.util.Argument;
import gdn.hypercube.epsilon.core.util.MemoryHelper;

@SuppressWarnings("unused")
public class RegisterManipulation {
    EngineCommand ToggleRegisterRead = new EngineCommand(
        EngineCommand.Type.REGISTER_MANIPULATION, 0x00,
        (engine, argv) -> {
            int index = (int) argv[0].value;
            engine.reading[index] = !engine.reading[index];
        },
        new Argument(Argument.Type.BYTE)
    );

    EngineCommand Store8 = new EngineCommand(
        EngineCommand.Type.REGISTER_MANIPULATION, 0x07,
        (engine, argv) -> {
            int base = MemoryHelper.registerBase((int) argv[0].value);
            int offset = (int) argv[2].value;
            MemoryHelper.writeByteAt(engine.memory, base, offset, argv[1].value);
        },
        new Argument(Argument.Type.BYTE),
        new Argument(Argument.Type.BYTE),
        new Argument(Argument.Type.BYTE)
    );

    EngineCommand Store16 = new EngineCommand(
        EngineCommand.Type.REGISTER_MANIPULATION, 0x08,
        (engine, argv) -> {
            int base = MemoryHelper.registerBase((int) argv[0].value);
            int half = (int) argv[2].value;
            MemoryHelper.writeShortAt(engine.memory, base, half, argv[1].value);
        },
        new Argument(Argument.Type.BYTE),
        new Argument(Argument.Type.SHORT),
        new Argument(Argument.Type.BYTE)
    );

    EngineCommand Store32 = new EngineCommand(
        EngineCommand.Type.REGISTER_MANIPULATION, 0x09,
        (engine, argv) -> {
            int base = MemoryHelper.registerBase((int) argv[0].value);
            int word = (int) argv[2].value;
            MemoryHelper.writeIntAt(engine.memory, base, word, argv[1].value);
        },
        new Argument(Argument.Type.BYTE),
        new Argument(Argument.Type.INT),
        new Argument(Argument.Type.BYTE)
    );

    EngineCommand Store64 = new EngineCommand(
        EngineCommand.Type.REGISTER_MANIPULATION, 0x0E,
        (engine, argv) -> {
            int base = MemoryHelper.registerBase((int) argv[0].value);
            MemoryHelper.writeLong(engine.memory, base, argv[1].value);
        },
        new Argument(Argument.Type.BYTE),
        new Argument(Argument.Type.LONG)
    );

    EngineCommand Copy8 = new EngineCommand(
        EngineCommand.Type.REGISTER_MANIPULATION, 0x0A,
        (engine, argv) -> {
            int srcBase = MemoryHelper.registerBase((int) argv[0].value);
            int dstBase = MemoryHelper.registerBase((int) argv[2].value);
            MemoryHelper.writeByteAt(engine.memory, dstBase, (int) argv[3].value, MemoryHelper.readByteAt(engine.memory, srcBase, (int) argv[1].value));
        },
        new Argument(Argument.Type.BYTE),
        new Argument(Argument.Type.BYTE),
        new Argument(Argument.Type.BYTE),
        new Argument(Argument.Type.BYTE)
    );

    EngineCommand Copy16 = new EngineCommand(
        EngineCommand.Type.REGISTER_MANIPULATION, 0x0B,
        (engine, argv) -> {
            int srcBase = MemoryHelper.registerBase((int) argv[0].value);
            int dstBase = MemoryHelper.registerBase((int) argv[2].value);
            MemoryHelper.writeShortAt(engine.memory, dstBase, (int) argv[3].value, MemoryHelper.readShortAt(engine.memory, srcBase, (int) argv[1].value));
        },
        new Argument(Argument.Type.BYTE),
        new Argument(Argument.Type.BYTE),
        new Argument(Argument.Type.BYTE),
        new Argument(Argument.Type.BYTE)
    );

    EngineCommand Copy32 = new EngineCommand(
        EngineCommand.Type.REGISTER_MANIPULATION, 0x0C,
        (engine, argv) -> {
            int srcBase = MemoryHelper.registerBase((int) argv[0].value);
            int dstBase = MemoryHelper.registerBase((int) argv[2].value);
            MemoryHelper.writeIntAt(engine.memory, dstBase, (int) argv[3].value, MemoryHelper.readIntAt(engine.memory, srcBase, (int) argv[1].value));
        },
        new Argument(Argument.Type.BYTE),
        new Argument(Argument.Type.BYTE),
        new Argument(Argument.Type.BYTE),
        new Argument(Argument.Type.BYTE)
    );

    EngineCommand Copy64 = new EngineCommand(
        EngineCommand.Type.REGISTER_MANIPULATION, 0x0D,
        (engine, argv) -> {
            int srcBase = MemoryHelper.registerBase((int) argv[0].value);
            int dstBase = MemoryHelper.registerBase((int) argv[1].value);
            MemoryHelper.writeLong(engine.memory, dstBase, MemoryHelper.readLong(engine.memory, srcBase));
        },
        new Argument(Argument.Type.BYTE),
        new Argument(Argument.Type.BYTE)
    );

    EngineCommand Load = new EngineCommand(
        EngineCommand.Type.REGISTER_MANIPULATION, 0x0F,
        (engine, argv) -> {
            int base = MemoryHelper.registerBase((int) argv[0].value);
            int word = (int) argv[1].value;
            MemoryHelper.writeIntAt(engine.memory, base, word, engine.ip);
        },
        new Argument(Argument.Type.BYTE),
        new Argument(Argument.Type.BYTE)
    );
}