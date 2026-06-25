package gdn.hypercube.epsilon.core.command;

import gdn.hypercube.epsilon.core.util.Argument;
import gdn.hypercube.epsilon.core.util.EngineCommand;
import gdn.hypercube.epsilon.core.util.MemoryHelper;

@SuppressWarnings("unused")
public class RegisterMath {
    EngineCommand Add8 = new EngineCommand(
        EngineCommand.Type.REGISTER_MANIPULATION, 0x10,
        (engine, argv) -> {
            int base   = MemoryHelper.registerBase((int) argv[0].value);
            int offset = (int) argv[2].value;
            long value  = (MemoryHelper.readByteAt(engine.memory, base, offset) + argv[1].value) & 0xFFL;
            MemoryHelper.writeByteAt(engine.memory, base, offset, value);
        },
        new Argument(Argument.Type.BYTE),
        new Argument(Argument.Type.BYTE),
        new Argument(Argument.Type.BYTE)
    );

    EngineCommand Add16 = new EngineCommand(
        EngineCommand.Type.REGISTER_MANIPULATION, 0x11,
        (engine, argv) -> {
            int base = MemoryHelper.registerBase((int) argv[0].value);
            int half = (int) argv[2].value;
            long value = (MemoryHelper.readShortAt(engine.memory, base, half) + argv[1].value) & 0xFFFFL;
            MemoryHelper.writeShortAt(engine.memory, base, half, value);
        },
        new Argument(Argument.Type.BYTE),
        new Argument(Argument.Type.SHORT),
        new Argument(Argument.Type.BYTE)
    );

    EngineCommand Add32 = new EngineCommand(
        EngineCommand.Type.REGISTER_MANIPULATION, 0x12,
        (engine, argv) -> {
            int base = MemoryHelper.registerBase((int) argv[0].value);
            int word = (int) argv[2].value;
            long value = (MemoryHelper.readIntAt(engine.memory, base, word) + argv[1].value) & 0xFFFFFFFFL;
            MemoryHelper.writeIntAt(engine.memory, base, word, value);
        },
        new Argument(Argument.Type.BYTE),
        new Argument(Argument.Type.INT),
        new Argument(Argument.Type.BYTE)
    );

    EngineCommand Add64 = new EngineCommand(
        EngineCommand.Type.REGISTER_MANIPULATION, 0x05,
        (engine, argv) -> {
            int base = MemoryHelper.registerBase((int) argv[0].value);
            long value = MemoryHelper.readLong(engine.memory, base) + argv[1].value;
            MemoryHelper.writeLong(engine.memory, base, value);
        },
        new Argument(Argument.Type.BYTE),
        new Argument(Argument.Type.LONG)
    );

    EngineCommand Sub8 = new EngineCommand(
        EngineCommand.Type.REGISTER_MANIPULATION, 0x13,
        (engine, argv) -> {
            int base = MemoryHelper.registerBase((int) argv[0].value);
            int offset = (int) argv[2].value;
            long value = (MemoryHelper.readByteAt(engine.memory, base, offset) - argv[1].value) & 0xFFL;
            MemoryHelper.writeByteAt(engine.memory, base, offset, value);
        },
        new Argument(Argument.Type.BYTE),
        new Argument(Argument.Type.BYTE),
        new Argument(Argument.Type.BYTE)
    );

    EngineCommand Sub16 = new EngineCommand(
        EngineCommand.Type.REGISTER_MANIPULATION, 0x14,
        (engine, argv) -> {
            int base = MemoryHelper.registerBase((int) argv[0].value);
            int half = (int) argv[2].value;
            long value = (MemoryHelper.readShortAt(engine.memory, base, half) - argv[1].value) & 0xFFFFL;
            MemoryHelper.writeShortAt(engine.memory, base, half, value);
        },
        new Argument(Argument.Type.BYTE),
        new Argument(Argument.Type.SHORT),
        new Argument(Argument.Type.BYTE)
    );

    EngineCommand Sub32 = new EngineCommand(
        EngineCommand.Type.REGISTER_MANIPULATION, 0x15,
        (engine, argv) -> {
            int base = MemoryHelper.registerBase((int) argv[0].value);
            int word = (int) argv[2].value;
            long value = (MemoryHelper.readIntAt(engine.memory, base, word) - argv[1].value) & 0xFFFFFFFFL;
            MemoryHelper.writeIntAt(engine.memory, base, word, value);
        },
        new Argument(Argument.Type.BYTE),
        new Argument(Argument.Type.INT),
        new Argument(Argument.Type.BYTE)
    );

    EngineCommand Sub64 = new EngineCommand(
        EngineCommand.Type.REGISTER_MANIPULATION, 0x06,
        (engine, argv) -> {
            int base = MemoryHelper.registerBase((int) argv[0].value);
            long value = MemoryHelper.readLong(engine.memory, base) - argv[1].value;
            MemoryHelper.writeLong(engine.memory, base, value);
        },
        new Argument(Argument.Type.BYTE),
        new Argument(Argument.Type.LONG)
    );
}