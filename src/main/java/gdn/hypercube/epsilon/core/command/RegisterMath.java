package gdn.hypercube.epsilon.core.command;

import gdn.hypercube.epsilon.core.util.Argument;
import gdn.hypercube.epsilon.core.util.EngineCommand;
import gdn.hypercube.epsilon.core.util.MemoryHelper;

@SuppressWarnings("unused")
public class RegisterMath { // TODO: Replace 8/16/32/64 with a length operand
    EngineCommand Add8 = new EngineCommand(
        EngineCommand.Type.REGISTER_MANIPULATION, 0x10,
        (engine, argv) -> {
            int base   = MemoryHelper.registerBase((int) argv[0].value);
            int offset = (int) argv[1].value;
            long value  = (MemoryHelper.readByteAt(engine, base, offset) + argv[2].value) & 0xFFL;
            MemoryHelper.writeByteAt(engine, base, offset, value);
        },
        new Argument(Argument.Type.BYTE),
        new Argument(Argument.Type.BYTE),
        new Argument(Argument.Type.BYTE)
    );

    EngineCommand Add16 = new EngineCommand(
        EngineCommand.Type.REGISTER_MANIPULATION, 0x11,
        (engine, argv) -> {
            int base = MemoryHelper.registerBase((int) argv[0].value);
            int half = (int) argv[1].value;
            long value = (MemoryHelper.readShortAt(engine, base, half) + argv[2].value) & 0xFFFFL;
            MemoryHelper.writeShortAt(engine, base, half, value);
        },
        new Argument(Argument.Type.BYTE),
        new Argument(Argument.Type.SHORT),
        new Argument(Argument.Type.BYTE)
    );

    EngineCommand Add32 = new EngineCommand(
        EngineCommand.Type.REGISTER_MANIPULATION, 0x12,
        (engine, argv) -> {
            int base = MemoryHelper.registerBase((int) argv[0].value);
            int word = (int) argv[1].value;
            long value = (MemoryHelper.readIntAt(engine, base, word) + argv[2].value) & 0xFFFFFFFFL;
            MemoryHelper.writeIntAt(engine, base, word, value);
        },
        new Argument(Argument.Type.BYTE),
        new Argument(Argument.Type.INT),
        new Argument(Argument.Type.BYTE)
    );

    EngineCommand Add64 = new EngineCommand(
        EngineCommand.Type.REGISTER_MANIPULATION, 0x05,
        (engine, argv) -> {
            int base = MemoryHelper.registerBase((int) argv[0].value);
            long value = MemoryHelper.readLong(engine, base) + argv[1].value;
            MemoryHelper.writeLong(engine, base, value);
        },
        new Argument(Argument.Type.BYTE),
        new Argument(Argument.Type.LONG)
    );

    EngineCommand Sub8 = new EngineCommand(
        EngineCommand.Type.REGISTER_MANIPULATION, 0x13,
        (engine, argv) -> {
            int base = MemoryHelper.registerBase((int) argv[0].value);
            int offset = (int) argv[1].value;
            long value = (MemoryHelper.readByteAt(engine, base, offset) - argv[2].value) & 0xFFL;
            MemoryHelper.writeByteAt(engine, base, offset, value);
        },
        new Argument(Argument.Type.BYTE),
        new Argument(Argument.Type.BYTE),
        new Argument(Argument.Type.BYTE)
    );

    EngineCommand Sub16 = new EngineCommand(
        EngineCommand.Type.REGISTER_MANIPULATION, 0x14,
        (engine, argv) -> {
            int base = MemoryHelper.registerBase((int) argv[0].value);
            int half = (int) argv[1].value;
            long value = (MemoryHelper.readShortAt(engine, base, half) - argv[2].value) & 0xFFFFL;
            MemoryHelper.writeShortAt(engine, base, half, value);
        },
        new Argument(Argument.Type.BYTE),
        new Argument(Argument.Type.SHORT),
        new Argument(Argument.Type.BYTE)
    );

    EngineCommand Sub32 = new EngineCommand(
        EngineCommand.Type.REGISTER_MANIPULATION, 0x15,
        (engine, argv) -> {
            int base = MemoryHelper.registerBase((int) argv[0].value);
            int word = (int) argv[1].value;
            long value = (MemoryHelper.readIntAt(engine, base, word) - argv[2].value) & 0xFFFFFFFFL;
            MemoryHelper.writeIntAt(engine, base, word, value);
        },
        new Argument(Argument.Type.BYTE),
        new Argument(Argument.Type.INT),
        new Argument(Argument.Type.BYTE)
    );

    EngineCommand Sub64 = new EngineCommand(
        EngineCommand.Type.REGISTER_MANIPULATION, 0x06,
        (engine, argv) -> {
            int base = MemoryHelper.registerBase((int) argv[0].value);
            long value = MemoryHelper.readLong(engine, base) - argv[1].value;
            MemoryHelper.writeLong(engine, base, value);
        },
        new Argument(Argument.Type.BYTE),
        new Argument(Argument.Type.LONG)
    );
}