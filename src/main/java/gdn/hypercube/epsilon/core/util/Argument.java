package gdn.hypercube.epsilon.core.util;

import gdn.hypercube.epsilon.core.EpsilonEngine;

public class Argument {
    public long value;
    public final Type type;

    public Argument(Type type) {
        this.type = type;
    }

    public void set(EpsilonEngine engine, int index, char[] bytes, boolean literal) {
        if (engine.reading[index] && !literal) {
            char register = bytes[0];
            char[] target = new char[this.type.width];
            System.arraycopy(engine.memory, register * EpsilonEngine.REGISTER_WIDTH, target, 0, target.length);

            this.set(engine, index, target, true);

            return;
        }

        this.value = switch (this.type) {
            case BYTE  -> bytes[0] & 0xFFL;
            case SHORT -> ((bytes[0] & 0xFFL) << 8)  | (bytes[1] & 0xFFL);
            case INT   -> ((bytes[0] & 0xFFL) << 24) | ((bytes[1] & 0xFFL) << 16)
                    |  ((bytes[2] & 0xFFL) << 8)  | (bytes[3] & 0xFFL);
            case LONG  -> ((bytes[0] & 0xFFL) << 56) | ((bytes[1] & 0xFFL) << 48)
                    |  ((bytes[2] & 0xFFL) << 40) | ((bytes[3] & 0xFFL) << 32)
                    |  ((bytes[4] & 0xFFL) << 24) | ((bytes[5] & 0xFFL) << 16)
                    |  ((bytes[6] & 0xFFL) << 8)  | (bytes[7] & 0xFFL);
        };
    }

    public enum Type {
        BYTE(1),
        SHORT(2),
        INT(4),
        LONG(8);

        public final int width;

        Type(int width) {
            this.width = width;
        }
    }
}
