package gdn.hypercube.epsilon.core.util;

import gdn.hypercube.epsilon.core.EpsilonEngine;

public final class MemoryHelper {
    public static void writeByte(char[] memory, int position, long value) { memory[position] = (char) (value & 0xFF); }
    public static void writeShort(char[] memory, int position, long value) {
        memory[position] = (char) ((value >> 8) & 0xFF);
        memory[position + 1] = (char) (value & 0xFF);
    }
    public static void writeInt(char[] memory, int position, long value) { for (int i = 0; i < 4; i++) memory[position + i] = (char) ((value >> (24 - i * 8)) & 0xFF); }
    public static void writeLong(char[] memory, int position, long value) { for (int i = 0; i < 8; i++) memory[position + i] = (char) ((value >> (56 - i * 8)) & 0xFF); }

    public static void writeByteAt(char[] memory, int base, int offset, long value) { writeByte(memory, base + offset, value); }
    public static void writeShortAt(char[] memory, int base, int offset, long value) { writeShort(memory, base + offset * 2, value); }
    public static void writeIntAt(char[] memory, int base, int offset, long value) { writeInt(memory, base + offset * 4, value); }

    public static long readByte(char[] memory, int position) { return memory[position] & 0xFFL; }
    public static long readShort(char[] memory, int position) { return ((memory[position] & 0xFFL) << 8) | (memory[position + 1] & 0xFFL); }
    public static long readInt(char[] memory, int position) {
        long value = 0;
        for (int i = 0; i < 4; i++) value |= (memory[position + i] & 0xFFL) << (24 - i * 8);
        return value;
    }
    public static long readLong(char[] memory, int position) {
        long value = 0;
        for (int i = 0; i < 8; i++) value |= (memory[position + i] & 0xFFL) << (56 - i * 8);
        return value;
    }

    public static long readByteAt(char[] memory, int base, int offset) { return readByte(memory, base + offset); }
    public static long readShortAt(char[] memory, int base, int half) { return readShort(memory, base + half * 2); }
    public static long readIntAt(char[] memory, int base, int word) { return readInt(memory, base + word * 4); }

    public static int registerBase(int register) { return register * EpsilonEngine.REGISTER_WIDTH; }
}