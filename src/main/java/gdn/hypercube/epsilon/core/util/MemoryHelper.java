package gdn.hypercube.epsilon.core.util;

import gdn.hypercube.epsilon.core.EpsilonEngine;

public final class MemoryHelper {
    public static void writeByte(EpsilonEngine engine, int position, long value) { engine.memory[position] = (char) (value & 0xFF); }
    public static void writeShort(EpsilonEngine engine, int position, long value) {
        engine.memory[position] = (char) ((value >> 8) & 0xFF);
        engine.memory[position + 1] = (char) (value & 0xFF);
    }
    public static void writeInt(EpsilonEngine engine, int position, long value) { for (int i = 0; i < 4; i++) engine.memory[position + i] = (char) ((value >> (24 - i * 8)) & 0xFF); }

    public static void writeLong(EpsilonEngine engine, int position, long value) {
        if (engine.debug[1]) System.out.println("writeLong called with values: position " + position + ", value " + value);
        for (int i = 0; i < 8; i++) engine.memory[position + i] = (char) ((value >> (56 - i * 8)) & 0xFF);
    }

    public static void writeByteAt(EpsilonEngine engine, int base, int offset, long value) {
        if (engine.debug[1]) System.out.println("writeByteAt called with values: base " + base + ", offset" + offset + ", value " + value);
        writeByte(engine, base + offset, value);
    }
    public static void writeShortAt(EpsilonEngine engine, int base, int offset, long value) {
        if (engine.debug[1]) System.out.println("writeShortAt called with values: base " + base + ", offset" + offset + ", value " + value);
        System.out.println("Base " + base + ", offset" + offset + ", value " + value); writeShort(engine, base + offset * 2, value); }
    public static void writeIntAt(EpsilonEngine engine, int base, int offset, long value) {
        if (engine.debug[1]) System.out.println("writeByteAt called with values: base " + base + ", offset" + offset + ", value " + value);
        writeInt(engine, base + offset * 4, value);
    }

    public static long readByte(EpsilonEngine engine, int position) { return engine.memory[position] & 0xFFL; }
    public static long readShort(EpsilonEngine engine, int position) { return ((engine.memory[position] & 0xFFL) << 8) | (engine.memory[position + 1] & 0xFFL); }
    public static long readInt(EpsilonEngine engine, int position) {
        long value = 0;
        for (int i = 0; i < 4; i++) value |= (engine.memory[position + i] & 0xFFL) << (24 - i * 8);
        return value;
    }

    public static long readLong(EpsilonEngine engine, int position) {
        long value = 0;
        for (int i = 0; i < 8; i++) {
            value |= (engine.memory[position + i] & 0xFFL) << (56 - i * 8);
            if (engine.debug[1]) System.out.println("value at step " + i + ": " + value);
        }
        return value;
    }

    public static long readByteAt(EpsilonEngine engine, int base, int offset) { return readByte(engine, base + offset); }
    public static long readShortAt(EpsilonEngine engine, int base, int half) { return readShort(engine, base + half * 2); }
    public static long readIntAt(EpsilonEngine engine, int base, int word) { return readInt(engine, base + word * 4); }

    public static int registerBase(int register) { return register * EpsilonEngine.REGISTER_WIDTH; }
}