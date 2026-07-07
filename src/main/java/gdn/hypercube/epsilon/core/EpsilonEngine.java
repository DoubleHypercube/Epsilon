package gdn.hypercube.epsilon.core;

import com.github.bsideup.jabel.Desugar;
import gdn.hypercube.epsilon.core.util.Pair;
import gdn.hypercube.epsilon.core.util.Argument;
import gdn.hypercube.epsilon.core.util.EngineCommand;
import gdn.hypercube.epsilon.core.util.VarargsCommand;
import gdn.hypercube.epsilon.core.handler.AbstractPlatform;

import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@SuppressWarnings("unused")
public class EpsilonEngine {
    public enum Status {
        PAUSED,
        HALTED,
        WAITING,
        RUNNING,
        INACTIVE // TODO: do we really need this if we have HALTED?
    }

    public static class Speed {
        public static final float FAST = 0.75f;
        public static final float MEDIUM = 1.25f;
        public static final float SLOW = 1.75f;
        public static final float SNAIL = 2.5f;
        public static final float VISCOUS = 3.25f;
    }

    public int ip = 0;
    public int end = 0;
    public int decindex = 0;
    private float delta = 0F;
    private boolean jumped = false;
    public float speed = Speed.FAST;
    public Status status = Status.INACTIVE;
    public static final int REGISTER_WIDTH = 8;
    public final IntStack stack = new IntStack();
    public final boolean[] reading = new boolean[256];
    public final char[] script = new char[SCRIPT_SPACE];
    public final StringBuilder line = new StringBuilder();
    public static final int SCRIPT_SPACE = 32 * 1024 * 1024;
    public final Map<String, Integer> labels = new HashMap<>();
    public final char[] memory = new char[256 * REGISTER_WIDTH];
    public final List<RenderedLine> rendered = new ArrayList<>();
    public final Map<Integer, Integer> colours = new HashMap<>();
    public HashMap<Integer, Pair<String, Integer>> decisions = new HashMap<>();

    private int blink = 0;
    private int blonk = 0;
    private float pauseDelta = 0F;

    public long pauseTime = 0;
    public long pauseTarget = 0;

    public int drawX = 0;
    public int drawY = 0;

    private final AbstractPlatform platform;
    public EpsilonEngine(AbstractPlatform platform) {
        this.platform = platform;
    }

    public void load(char[] data) {
        int address = this.end;
        System.arraycopy(data, 0, this.script, this.end, data.length);
        this.end += data.length;
        this.ip = address;
        reset();
        this.status = Status.RUNNING;
    }

    public void load(String source) {
        load(source.toCharArray());
    }

    public void load(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        load(new String(bytes, StandardCharsets.UTF_8).toCharArray());
    }

    public void loadBin(ByteBuffer buf) { // TODO: reword errors maybe?
        if (buf.remaining() < 16) throw new BinLoadException("file too short");
        for (byte expected : MAGIC) if (buf.get() != expected) throw new BinLoadException("invalid magic");
        byte major = buf.get();
        byte minor = buf.get();

        if (major != VERSION_MAJOR) throw new BinLoadException(String.format("incompatible version %d.%d", major, minor));
        if (minor != VERSION_MINOR) System.err.printf("potentially incompatible version %d.%d", major, minor);

        int labels = buf.getInt();
        int size = buf.getInt();

        if (script.length - end < size) throw new BinLoadException("out of ROM space " + " (need " + size + " bytes, have " + (script.length - end) + " bytes left)");
        int baseAddress = end;
        for (int i = 0; i < labels; i++) {
            int relAddr = buf.getInt();
            int nameLen = buf.getShort() & 0xFFFF;
            byte[] nameBytes = new byte[nameLen];
            buf.get(nameBytes);
            String name = new String(nameBytes, StandardCharsets.UTF_8);
            this.labels.put(name, baseAddress + relAddr);
        }

        for (int i = 0; i < size; i++) {
            script[end++] = buf.getChar();
        }

        reset();
        this.status = Status.INACTIVE;
    }

    public void loadBin(Path file) throws IOException {
        byte[] raw = Files.readAllBytes(file);
        ByteBuffer buf = ByteBuffer.wrap(raw).order(ByteOrder.BIG_ENDIAN);
        loadBin(buf);
    }

    public void loadBin(String path) throws IOException {
        loadBin(Paths.get(path));
    }

    public void jump(String label) {
        Arrays.fill(reading, false);
        Integer address = labels.get(label);
        if (address == null) throw new IllegalStateException("Unknown label: '" + label + "'");
        this.ip = address;
        reset();
        this.status = Status.RUNNING;
    }

    public void jump(int address) {
        Arrays.fill(reading, false);
        this.ip = address;
        this.jumped = true;
    }

    public void jsr(String label) {
        stack.push(this.ip + 1);
        jump(label);
    }

    public void jsr(int address) {
        stack.push(this.ip + 1);
        jump(address);
    }

    public void ret() {
        if (stack.isEmpty()) throw new IllegalStateException("ret() called with empty call stack");
        this.ip = stack.pop();
        this.jumped = true;
    }

    public void update() {
        switch (this.status) {
            case PAUSED -> pause();
            case WAITING -> blink();
            case RUNNING -> tick();
            default -> { /* HALTED, INACTIVE: nothing to do */ }
        }
    }

    public void tick() {
        this.delta += this.platform.getDynamicDeltaTicks();
        if (this.delta < this.speed) return;
        this.delta = 0;

        if (this.ip >= this.end) {
            this.status = Status.INACTIVE;
            return;
        }

        char next = this.script[this.ip];
        if (next == 0x00) {
            char upper = script[++this.ip];
            char lower = script[++this.ip];

            EngineCommand.Type[] types = EngineCommand.Type.values();
            EngineCommand command = EngineCommand.get(types[upper], lower);

            Argument[] arguments;
            if (command instanceof VarargsCommand varargs) {
                int count = varargs.readahead(this.script, this.ip);
                arguments = new Argument[count];
                for (int index = 0; index < count; index++) {
                    arguments[index] = new Argument(Argument.Type.BYTE);
                    char[] bytes = fill(1);
                    arguments[index].set(this, index, bytes, command.literal[index % command.literal.length]);
                }
            } else {
                arguments = new Argument[command.argv.length];
                for (int index = 0; index < arguments.length; index++) {
                    arguments[index] = new Argument(command.argv[index].type);
                    char[] bytes = fill(arguments[index].type.width);
                    arguments[index].set(this, index, bytes, command.literal[index]);
                }
            }

            command.executor.run(this, arguments);
        } else {
            line.append(next);
        }

        if (jumped) {
            jumped = false;
        } else {
            this.ip++;
        }
    }

    public void pause() {
        this.pauseDelta += this.platform.getDynamicDeltaTicks();
        if (this.pauseDelta > this.speed) {
            this.pauseDelta = 0;
            this.pauseTime++;
            if (this.pauseTime >= this.pauseTarget) {
                this.status = Status.RUNNING;
                this.pauseTarget = 0;
                this.pauseTime = 0;
            }
        }
    }

    public void blink() {
        try {
            this.blink++;
            int framerate = this.platform.framerate();
            if (framerate > 0 && this.blink % (framerate / 48) == 0) this.blonk++;
            String character = this.blonk <= 35 ? "▼" : " ";
            this.platform.drawCharacter(character,
                this.drawX + 237, this.drawY + 55, // TODO: why these numbers?
                this.platform.getArgb(0xFF, 0xFF, 0xFF)
            );
            if (this.blonk >= 70) this.blonk = 0;
        } catch (ArithmeticException ignored) {}
    }

    public void draw() {
        int lineY = this.drawY;
        for (int index = 0; index < this.rendered.size(); index++) {
            RenderedLine line = this.rendered.get(index);
            this.drawLine(index, line.text, line.colours, lineY);
        }

        if (this.line.length() > 0) /* no it can't, intellij */ drawLine(this.rendered.size(), this.line.toString(), this.colours, lineY);
    }

    private void drawLine(int index, String text, Map<Integer, Integer> colours, int y) {
        List<Pair<String, Integer>> targets = new ArrayList<>();
        for (int i = 0; i < text.length(); i++) {
            String there = String.valueOf(text.charAt(i));
            int colour = colours.getOrDefault(i, -1);
            targets.add(new Pair<>(there, colour));
        }
        platform.drawText(index, targets, this.drawX, y);
    }

    public void newline() {
        rendered.add(new RenderedLine(line.toString(), new HashMap<>(colours)));
        line.setLength(0);
        colours.clear();
    }

    private void reset() {
        rendered.clear();
        line.setLength(0);
        colours.clear();
    }

    private char next() {
        return this.script[++this.ip];
    }

    private char[] fill(int length) {
        char[] array = new char[length];
        for (int i = 0; i < length; i++) array[i] = next();
        return array;
    }

    private static final byte[] MAGIC = { 0x45, 0x43, 0x43, 0x53 };
    private static final byte VERSION_MAJOR = 1;
    private static final byte VERSION_MINOR = 0;

    private static void checkMagic(ByteBuffer buf, Path file) {
        for (byte expected : MAGIC) if (buf.get() != expected) throw new BinLoadException(file + ": invalid magic");
    }

    private static void checkVersion(byte major, byte minor, Path file) {
        if (major != VERSION_MAJOR) throw new BinLoadException(String.format("%s: incompatible major version %d (engine is %d) — recompile",file, major, VERSION_MAJOR));
        if (minor != VERSION_MINOR) System.err.printf("warning: %s: minor version mismatch (%d.%d vs engine %d.%d)%n",file, major, minor, VERSION_MAJOR, VERSION_MINOR);
    }

    @Desugar
    public record RenderedLine(String text, Map<Integer, Integer> colours) {}

    public static class IntStack {
        private int[] data = new int[16];
        private int size = 0;

        public void push(int v) {
            if (size == data.length) {
                int[] bigger = new int[data.length * 2]; // TODO: better algorithm?
                System.arraycopy(data, 0, bigger, 0, size);
                data = bigger;
            }
            data[size++] = v;
        }

        public int pop() { if (size == 0) throw new IllegalStateException("empty"); return data[--size]; }
        public int peek() { if (size == 0) throw new IllegalStateException("empty"); return data[size-1]; }
        public boolean isEmpty() { return size == 0; }
        public int size() { return size; }
        public void clear() { size = 0; }
    }

    public static class BinLoadException extends RuntimeException {
        public BinLoadException(String message) { super(message); }
    }
}