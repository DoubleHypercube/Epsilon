package gdn.hypercube.epsilon.cli;

import gdn.hypercube.epsilon.core.util.Triple;
import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.HashSet;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.lang.reflect.Method;
import com.github.bsideup.jabel.Desugar;
import java.nio.file.NoSuchFileException;
import java.nio.file.AccessDeniedException;
import gdn.hypercube.epsilon.core.EpsilonEngine;
import gdn.hypercube.epsilon.cli.CommandLine.Option;
import gdn.hypercube.epsilon.cli.CommandLine.Command;
import gdn.hypercube.epsilon.cli.CommandLine.Parameters;

import java.nio.ByteOrder;
import java.nio.ByteBuffer;
import java.util.stream.Stream;
import java.util.stream.Collectors;
import java.util.concurrent.Callable;
import java.nio.charset.StandardCharsets;

@Command(
        name = "ECCS",
        description = "Epsilon CCScript compiler",
        mixinStandardHelpOptions = true, // haha mixin
        version = "eccs 1.0"
)
public class EpsilonCCScriptCompiler implements Callable<Integer> {

    static final int EX_OK        = 00; /* successful termination */
    static final int EX_DATAERR   = 65; /* data format error */
    static final int EX_NOINPUT   = 66; /* cannot open input */
    static final int EX_SOFTWARE  = 70; /* internal software error */
    static final int EX_IOERR     = 74; /* input/output error */
    static final int EX_NOPERM    = 77; /* permission denied */
    static final int EX_CANTCREAT = 73;  /* can't create (user) output file */

    static final byte[] MAGIC = { 0x45, 0x43, 0x43, 0x53 }; // "ECCS"
    static final byte VERSION_MAJOR = 1;
    static final byte VERSION_MINOR = 0;

    enum ArgType {
        BYTE(1, false, false),
        SHORT(2, false, false),
        INT(4, false, false),
        LONG(8, false, false),
        ADDR(4, false, true),
        READAHEAD(-1, false, false),

        LITERAL_BYTE(1, true, false),
        LITERAL_SHORT(2, true, false),
        LITERAL_INT(4, true, false),
        LITERAL_LONG(8, true, false);

        final int width;
        final boolean literal;
        final boolean addr;

        ArgType(int width, boolean literal, boolean addr) {
            this.width   = width;
            this.literal = literal;
            this.addr    = addr;
        }

        static ArgType parse(String token, String context) {
            return switch (token.toLowerCase()) {
                case "byte"      -> BYTE;
                case "short"     -> SHORT;
                case "int"       -> INT;
                case "long"      -> LONG;
                case "addr"      -> ADDR;
                case "readahead" -> READAHEAD;
                case "$byte"     -> LITERAL_BYTE;
                case "$short"    -> LITERAL_SHORT;
                case "$int"      -> LITERAL_INT;
                case "$long"     -> LITERAL_LONG;
                default -> throw new CompilerException(context + ": unknown argument type '" + token + "'" + " (expected byte/short/int/long/addr/readahead)");
            };
        }

        int expectedRegisterWidth() {
            if (addr) return 4;
            return width;
        }
    }

    @Desugar
    @SuppressWarnings("unused")
    record RegisterRef(int register, Type type, Triple<Integer, Integer, Integer> offsets) {
        public enum Type {
            Byte(1),
            Half(2),
            Word(4),
            Quad(8)
            ;

            public final int width;
            Type(int width) {
                this.width = width;
            }
        }

        public int literal() {
            try {
                Method method = RegisterRef.class.getDeclaredMethod("absolute" + this.type.name() + "Index");
                method.setAccessible(true);
                return (int) method.invoke(this);
            } catch (ReflectiveOperationException exception) {
                return register;
            }
        }

        RegisterRef(int regNum) {
            this(regNum, Type.Quad, new Triple<>(-1, -1, -1));
        }

        boolean isSubRegister() {
            return this.type != Type.Quad;
        }

        int width() {
            return this.type.width;
        }

        int absoluteByteIndex() {
            if (this.offsets.middle() == -1 || this.offsets.right() == -1) return -1;
            int wordBase = (this.offsets.left() == -1) ? 0 : this.offsets.left() * 4;
            return wordBase + this.offsets.middle() * 2 + this.offsets.right();
        }

        int absoluteHalfIndex() {
            if (this.offsets.middle() == -1) return -1;
            int wordBase = (this.offsets.left() == -1) ? 0 : this.offsets.left() * 2;
            return wordBase + this.offsets.middle();
        }

        int absoluteWordIndex() {
            return this.offsets.middle();
        }

        String name() {
            return switch (width()) {
                case 1 -> "n 8-bit";
                case 2 -> " 16-bit";
                case 4 -> " 32-bit";
                default -> " 64-bit";
            };
        }
    }

    static class CommandDef {
        final String name;
        final byte[] opcode;
        final ArgType[] args;
        final boolean varargs;

        CommandDef(String name, byte[] opcode, ArgType[] args) {
            this.name = name;
            this.opcode = opcode;
            this.args = args;
            this.varargs = args.length > 0
            && args[args.length - 1] == ArgType.READAHEAD;
        }

        int fixedSize() {
            int size = opcode.length + 1;
            for (ArgType type : args) {
                if (type == ArgType.READAHEAD) break;
                size += type.width;
            }
            return size;
        }
    }

    @Option(names = {"--verbose", "-v"}, description = "Verbose output") private boolean verbose;
    @Option(names = {"--scratch-stats", "-s"}, description = "Print scratch register allocation statistics") private boolean scratches;
    @Option(names = {"--output", "-o"}, description = "Output path for compiled.bin (default: ./compiled.bin)", defaultValue = "compiled.bin") private Path output;
    @Option(names = {"--scratch-hint", "-S"}, description = "Registers to exclude from scratch allocation (hex, e.g. AF B2)", arity = "0..*") private List<String> hints = new ArrayList<>();

    @Parameters(description = "Source directories or individual .ccs/.ccsd files") private List<Path> targets = new ArrayList<>();

    private final List<Path> definitions = new ArrayList<>();
    private final List<Path> sources     = new ArrayList<>();
    private final Map<String, CommandDef> commands    = new LinkedHashMap<>();
    private final Map<String, RegisterRef> registers   = new LinkedHashMap<>();
    private final Map<String, Integer> addresses   = new LinkedHashMap<>();

    private final Set<Integer> references = new HashSet<>();
    private final Set<Integer> excludes = new HashSet<>();
    private final List<Integer> pool = new ArrayList<>();

    private int depth = 0;

    public static void main(String[] args) {
        System.exit(new CommandLine(new EpsilonCCScriptCompiler()).execute(args));
    }

    @Override
    public Integer call() {
        if (targets.isEmpty()) targets.add(Paths.get("."));
        int code = EX_OK;

        for (String hint : hints) {
            try {
                excludes.add(Integer.parseInt(hint, 16));
            } catch (NumberFormatException exception) {
                System.err.println("warning: invalid --scratch-hint '" + hint + "'");
            }
        }

        for (Path target : targets) {
            int result = collect(target);
            if (result != EX_OK) return result;
        }

        if (definitions.isEmpty() && sources.isEmpty()) {
            System.err.println("warning: no .ccs or .ccsd files found");
            return EX_NOINPUT;
        }

        for (Path def : definitions) {
            System.out.println("Loading definitions: " + def);
            try {
                parseDefinitions(def);
            } catch (IOException exception) {
                System.err.println("error: I/O reading " + def + ": " + exception.getMessage());
                code = code == EX_OK ? EX_IOERR : code;
            } catch (CompilerException exception) {
                System.err.println("error: " + exception.getMessage());
                code = code == EX_OK ? EX_DATAERR : code;
            }
        }

        if (code != EX_OK) return code;
        System.out.println("Loaded " + commands.size() + " command(s), " + registers.size() + " register alias(es).\n");

        try {
            scanRegisterReferences();
        } catch (IOException exception) {
            System.err.println("error: failed to scan register references: " + exception.getMessage());
            return EX_IOERR;
        }
        buildScratchPool();
        System.out.println("Scratch pool: " + pool.size() + " register(s) available.\n");

        Map<String, Integer> labels = new LinkedHashMap<>();
        int offset = 0;
        for (Path source : sources) {
            System.out.println("Preprocessing file " + source);
            try {
                offset = preprocess(source, labels, offset);
            } catch (IOException exception) {
                System.err.println("error: I/O reading " + source + ": " + exception.getMessage());
                code = code == EX_OK ? EX_IOERR : code;
            } catch (CompilerException exception) {
                System.err.println("error: " + exception.getMessage());
                code = code == EX_OK ? EX_DATAERR : code;
            }
        }

        if (code != EX_OK) return code;

        if (verbose) {
            System.out.println("Labels: " + labels.keySet());
            System.out.println("Total size: " + offset + " chars\n");
        }

        StringBuilder bytecode = new StringBuilder();
        for (Path source : sources) {
            System.out.println("Compiling file " + source);
            try {
                compile(source, labels, bytecode);
            } catch (IllegalStateException exception) {
                System.err.println("error: internal failure while parsing " + source + ": " + exception.getMessage());
                code = code == EX_OK ? EX_SOFTWARE : code;
            } catch (IOException exception) {
                System.err.println("error: I/O reading " + source + ": " + exception.getMessage());
                code = code == EX_OK ? EX_IOERR : code;
            } catch (CompilerException exception) {
                System.err.println("error: " + exception.getMessage());
                code = code == EX_OK ? EX_DATAERR : code;
            }
        }

        if (code != EX_OK) return code;

        if (scratches) {
            System.out.println("Scratch allocation stats:");
            System.out.println("  Max depth: " + depth);
            if (depth > 0) System.out.println("  Registers: " + pool.subList(0, Math.min(depth, pool.size())).stream().map(register -> String.format("$%02X", register)).collect(Collectors.joining(", ")));
        }

        try {
            writeBin(output, labels, bytecode.toString().toCharArray());
            System.out.println("-> " + output + "  (" + bytecode.length() + " chars, " + labels.size() + " label(s))");
        } catch (NoSuchFileException exception) {
            System.err.println("error: target does not exist (missing directory parents?): " + exception.getMessage());
            code = EX_CANTCREAT;
        } catch (AccessDeniedException exception) {
            System.err.println("error: permission denied: " + exception.getMessage());
            code = EX_NOPERM;
        } catch (IOException exception) {
            System.err.println("error: could not write output: " + exception.getMessage());
            code = EX_IOERR;
        }

        return code;
    }

    private int collect(Path target) {
        if (Files.isRegularFile(target)) return collectFile(target);
        if (Files.isDirectory(target)) {
            try (Stream<Path> stream = Files.walk(target)) {
                stream.filter(Files::isRegularFile)
                .filter(Files::isReadable)
                .filter(path -> {
                    String name = path.getFileName().toString().toLowerCase();
                    return name.endsWith(".ccs") || name.endsWith(".ccsd");
                })
                .sorted()
                .forEach(this::collectFile);
            } catch (AccessDeniedException exception) {
                System.err.println("error: permission denied: " + target);
                return EX_NOPERM;
            } catch (IOException exception) {
                System.err.println("error: could not walk directory: " + target);
                return EX_IOERR;
            }
            return EX_OK;
        }
        System.err.println("error: not a file or directory: " + target);
        return EX_NOINPUT;
    }

    private int collectFile(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        if (name.endsWith(".ccsd")) definitions.add(path);
        if (name.endsWith(".ccs"))  sources.add(path);
        return EX_OK;
    }

    private void parseDefinitions(Path file) throws IOException {
        List<String> lines = readLines(file);

        for (int index = 0; index < lines.size(); index++) {
            String contents = lines.get(index).trim();
            int line = index + 1;
            String ctx = file + ":" + line;

            int comment = contents.indexOf(';');
            if (comment >= 0) contents = contents.substring(0, comment).trim();
            if (contents.isEmpty()) continue;

            if (contents.contains("#")) {
                parseRegisterDef(contents, ctx);
                continue;
            }

            if (contents.matches("\\w+\\s+\\$[0-9A-Fa-f]{8}")) {
                String[] parts = contents.split("\\s+");
                int addr = (int) Long.parseLong(parts[1].substring(1), 16);
                addresses.put(parts[0], addr);
                if (verbose) System.out.printf("  Address: %-16s -> 0x%08X%n", parts[0], addr);
                continue;
            }

            int open  = contents.indexOf('[');
            int close = contents.indexOf(']');
            if (open == -1 || close == -1) throw new CompilerException(ctx + ": unrecognised definition line");

            String name = contents.substring(0, open).trim();
            String body = contents.substring(open + 1, close).trim();
            String[] tokens = body.split("\\s+");

            List<Byte> bytes = new ArrayList<>();
            List<ArgType> types = new ArrayList<>();
            boolean inArgs = false;

            for (String token : tokens) {
                if (token.isEmpty()) continue;
                if (!inArgs && token.matches("[0-9A-Fa-f]{2}")) {
                    bytes.add((byte) Integer.parseInt(token, 16));
                } else {
                    inArgs = true;
                    types.add(ArgType.parse(token, ctx));
                }
            }

            for (int j = 0; j < types.size() - 1; j++) {
                if (types.get(j) == ArgType.READAHEAD) throw new CompilerException(ctx + ": 'readahead' must be last in '" + name + "'");
            }

            byte[] opcodes = new byte[bytes.size()];
            ArgType[] arguments = types.toArray(new ArgType[0]);
            for (int j = 0; j < bytes.size(); j++) opcodes[j] = bytes.get(j);

            if (commands.containsKey(name)) System.err.printf("warning: %s: redefining command '%s'%n", ctx, name);
            commands.put(name, new CommandDef(name, opcodes, arguments));
            if (verbose) System.out.printf("  Command: %-16s opcode=[%s] args=%s%n", name, formatBytes(opcodes), types);
        }
    }

    @Desugar record Qualifier(RegisterRef.Type type, int word, int half, int segment) {}
    private Qualifier qualify(String qualifier) {
        return switch (qualifier.toLowerCase()) {
            case "intl"   -> new Qualifier(RegisterRef.Type.Word, 0, -1, -1);
            case "inth"   -> new Qualifier(RegisterRef.Type.Word, 1, -1, -1);
            case "shortl" -> new Qualifier(RegisterRef.Type.Half, -1, 0, -1);
            case "shorth" -> new Qualifier(RegisterRef.Type.Half, -1, 1, -1);
            case "bytel"  -> new Qualifier(RegisterRef.Type.Byte, -1, -1, 0);
            case "byteh"  -> new Qualifier(RegisterRef.Type.Byte, -1, -1, 1);
            default       -> throw new CompilerException("Unknown: " + qualifier);
        };
    }

    private void parseRegisterDef(String line, String ctx) {
        int start = line.indexOf('#');
        String name = line.substring(0, start).trim();
        String spec = line.substring(start + 1).trim();
        String[] parts = spec.split(":");

        int register;
        try {
            register = Integer.parseInt(parts[0], 16);
        } catch (NumberFormatException exception) {
            throw new CompilerException(ctx + ": invalid register number '" + parts[0] + "'");
        }
        if (register < 0 || register > 0xFF) throw new CompilerException(ctx + ": register number out of range (00-FF)");

        RegisterRef.Type type = RegisterRef.Type.Quad;
        int word = -1;
        int half = -1;
        int segment = -1;
        for (int index = 1; index < parts.length; index++) {
            Qualifier qual = qualify(parts[index]);
            type = qual.type();
            if (qual.word() != -1) word = qual.word();
            if (qual.half() != -1) half = qual.half();
            if (qual.segment() != -1) segment = qual.segment();
        }

        if (segment != -1 && half == -1) throw new CompilerException(ctx + ": byte qualifier requires a short qualifier (e.g. #XX:intl:shortl:bytel)");
        if (half != -1 && word == -1) throw new CompilerException(ctx + ": short qualifier requires an int qualifier (e.g. #XX:intl:shortl)");
        if (registers.containsKey(name)) System.err.printf("warning: %s: redefining register alias '%s'%n", ctx, name);
        registers.put(name, new RegisterRef(register, type, new Triple<>(word, half, segment)));

        if (verbose) {
            StringBuilder qual = new StringBuilder();
            if (word != -1) qual.append(":").append(word == 0 ? "intl" : "inth");
            if (half != -1) qual.append(":").append(half == 0 ? "shortl" : "shorth");
            if (segment != -1) qual.append(":").append(segment == 0 ? "bytel"  : "byteh");
            System.out.printf("  Register: %-16s -> $%02X%s%n", name, register, qual);
        }
    }

    private void scanRegisterReferences() throws IOException {
        for (RegisterRef ref : registers.values()) references.add(ref.register);
        references.addAll(excludes);

        for (Path source : sources) {
            List<String> lines = readLines(source);
            for (String line : lines) {
                int pos = 0;
                while (pos < line.length()) {
                    if (line.charAt(pos) == '`') {
                        int close = line.indexOf('`', pos + 1);
                        if (close == -1) break;
                        scanCallForRegisters(line.substring(pos + 1, close).trim());
                        pos = close + 1;
                    } else pos++;
                }
            }
        }
    }

    private void scanCallForRegisters(String call) {
        String[] parts = call.split("\\s+");
        for (int index = 1; index < parts.length; index++) {
            String arg = parts[index];
            if (!arg.startsWith("$")) continue;
            String ref = arg.substring(1);
            if (registers.containsKey(ref)) references.add(registers.get(ref).register);
            else {
                try { references.add(Integer.parseInt(ref, 16)); }
                catch (NumberFormatException ignored) {}
            }
        }
    }

    private void buildScratchPool() {
        for (int i = 0xFF; i >= 0; i--) {
            if (!references.contains(i)) pool.add(i);
        }

        if (verbose) System.out.println("Referenced registers: " + references.size() + ", scratch available: " + pool.size());
    }

    private List<Integer> allocateScratch(int count, String ctx) {
        if (count > pool.size()) throw new CompilerException(ctx + ": not enough scratch registers (need " + count + ", have " + pool.size() + "). Use --scratch-hint to free registers.");
        depth = Math.max(depth, count);
        return new ArrayList<>(pool.subList(0, count));
    }

    private RegisterRef resolveRegister(String token) {
        if (!token.startsWith("$")) return null;
        String ref = token.substring(1);
        if (registers.containsKey(ref)) return registers.get(ref);
        if (ref.matches("[0-9A-Fa-f]{2}")) return new RegisterRef(Integer.parseInt(ref, 16));

        return null;
    }

    private void checkRegisterWidth(RegisterRef ref, ArgType type, String token, String ctx) {
        int expected = type.expectedRegisterWidth();
        int actual = ref.width();

        if (type.addr) {
            if (actual != 4) throw new CompilerException(
                ctx + ": 'addr' argument requires a 32-bit register," + " but '" + token + "' is a" + ref.name() + " register." + " Use the full 32-bit alias (e.g. 'eax' not 'rax', 'ax', or 'al')."
            );
            return;
        }

        if (actual == expected) return;
        if (actual < expected) {
            System.err.printf("warning: %s: '%s' is a%s register passed to a '%s' argument." + " Value will be zero-extended via scratch register.%n", ctx, token, ref.name(), type.name().toLowerCase());
            return;
        }

        throw new CompilerException(ctx + ": type mismatch: '" + token + "' is a" + ref.name() + " register but argument expects " + expected + " byte(s)." + " Use a register of the correct width.");
    }

    private int preprocess(Path file, Map<String, Integer> labels, int offset) throws IOException {
        List<String> lines = readLines(file);

        for (int index = 0; index < lines.size(); index++) {
            String contents = lines.get(index).trim();
            int line = index + 1;
            String ctx = file + ":" + line;

            int ci = contents.indexOf(';');
            if (ci >= 0) contents = contents.substring(0, ci).trim();
            if (contents.isEmpty()) continue;

            if (isLabel(contents)) {
                String name = contents.substring(0, contents.length() - 1);
                if (labels.containsKey(name)) throw new CompilerException(ctx + ": duplicate label '" + name + "'");
                labels.put(name, offset);
                continue;
            }

            offset += measureLine(contents, line, file);
        }
        return offset;
    }

    private int measureLine(String line, int lineNo, Path file) {
        int size = 0;
        int index = 0;
        String ctx = file + ":" + lineNo;

        while (index < line.length()) {
            if (line.charAt(index) == '`') {
                int close = line.indexOf('`', index + 1);
                if (close == -1) throw new CompilerException(ctx + ": unclosed backtick");
                size += measureCall(line.substring(index + 1, close).trim(), ctx);
                index = close + 1;
            } else {
                size++;
                index++;
            }
        }
        return size;
    }

    private boolean needsScratch(RegisterRef ref, ArgType type) {
        if (!ref.isSubRegister() || type.addr) return false;
        return ref.literal() != EpsilonEngine.REGISTER_WIDTH || ref.width() > type.width;
    }

    private int scratchCopySize(RegisterRef ref) {
        return (ref.width() == 4) ? 5 : 7;
    }

    // everything is at least 3 overhead; arguments are +(width) overhead.
    private int measureCall(String call, String ctx) {
        String[] parts = call.split("\\s+");
        CommandDef command = commands.get(parts[0]);
        if (command == null) throw new CompilerException(ctx + ": unknown command '" + parts[0] + "'");

        int scratchOverhead = 0;
        int toggleOverhead  = 0;

        for (int index = 0; index < command.args.length && index + 1 < parts.length; index++) {
            ArgType type = command.args[index];
            if (type == ArgType.READAHEAD) break;
            if (type.literal) continue;

            String token = parts[index + 1];
            RegisterRef ref = resolveRegister(token);

            if (ref != null) {
                toggleOverhead += 8; // [02 00 XX] pair, each 4 chars
                if (ref.isSubRegister() && needsScratch(ref, type)) {
                    scratchOverhead += scratchCopySize(ref);
                }
            }
        }

        if (command.varargs) {
            int arguments = command.args.length - 1;
            int bytes = 0;
            for (int index = 0; index < arguments; index++) bytes += command.args[index].width;
            int readaheads = parts.length - 1 - arguments;
            if (readaheads < 0) throw new CompilerException(ctx + ": too few arguments for '" + parts[0] + "'");
            return scratchOverhead
                    + toggleOverhead + 1
                    + command.opcode.length
                    + bytes + readaheads;
        }

        return scratchOverhead + toggleOverhead + command.fixedSize();
    }

    private void compile(Path file, Map<String, Integer> labels, StringBuilder out) throws IOException {
        List<String> lines = readLines(file);

        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index).trim();
            int there = index + 1;

            int comment = line.indexOf(';');
            if (comment >= 0) line = line.substring(0, comment).trim();
            if (line.isEmpty()) continue;
            if (isLabel(line)) continue;

            emitLine(line, there, file, labels, out);
        }
    }

    private void emitLine(String line, int number, Path file, Map<String, Integer> labels, StringBuilder out) {
        int index = 0;
        String ctx = file + ":" + number;

        while (index < line.length()) {
            if (line.charAt(index) == '`') {
                int close = line.indexOf('`', index + 1);
                if (close == -1) throw new CompilerException(ctx + ": unclosed backtick");
                emitCall(line.substring(index + 1, close).trim(), ctx, labels, out);
                index = close + 1;
            } else {
                out.append(line.charAt(index));
                index++;
            }
        }
    }

    private void emitCall(String call, String ctx, Map<String, Integer> labels, StringBuilder out) {
        String[] parts = call.split("\\s+");
        String name = parts[0];
        CommandDef command = commands.get(name);
        if (command == null) throw new CompilerException(ctx + ": unknown command '" + name + "'");

        RegisterRef[] regs = new RegisterRef[command.args.length];
        for (int index = 0; index < command.args.length && index + 1 < parts.length; index++) {
            ArgType argType = command.args[index];
            if (argType == ArgType.READAHEAD) break;

            String token = parts[index + 1];
            RegisterRef ref = resolveRegister(token);
            if (ref != null && !argType.literal) checkRegisterWidth(ref, argType, token, ctx);

            regs[index] = ref;
        }

        List<Integer> subreads = new ArrayList<>();
        for (int index = 0; index < regs.length; index++) if (regs[index] != null
            && !command.args[index].literal
            && !command.args[index].addr
            && regs[index].isSubRegister()
            && needsScratch(regs[index], command.args[index])
        ) subreads.add(index);

        List<Integer> allocated = allocateScratch(subreads.size(), ctx);
        Map<Integer, Integer> targets   = new LinkedHashMap<>();
        for (int index = 0; index < subreads.size(); index++) targets.put(subreads.get(index), allocated.get(index));

        for (int index : subreads) {
            RegisterRef ref = regs[index];
            int scratch = targets.get(index);
            if (ref.width() <= 2) {
                try {
                    Method method = RegisterRef.class.getMethod("absolute" + (ref.width() == 1 ? "Byte" : "Half") + "Index");
                    emitRaw(new int[]{0x00, 0x02, 0x09 + ref.width(), ref.register, (int) method.invoke(ref), scratch, 0x00}, out);
                } catch (ReflectiveOperationException exception) {
                    throw new CompilerException(ctx + ": failed to reflectively invoke scratch allocaion");
                }
            } else emitRaw(new int[]{0x00, 0x02, 0x0C, ref.register, scratch}, out);
        }

        for (int index = 0; index < regs.length; index++) {
            ArgType type = command.args[index];
            if (type.literal) continue;
            if (type == ArgType.READAHEAD) break;
            if (regs[index] == null) continue;
            emitRaw(new int[]{0x00, 0x02, 0x00, index}, out);
        }

        out.append((char) 0x00);
        for (byte that : command.opcode) out.append((char) (that & 0xFF));

        if (command.varargs) {
            int fixedCount = command.args.length - 1;
            for (int index = 0; index < fixedCount; index++) {
                if (index + 1 >= parts.length) throw new CompilerException(ctx + ": too few arguments for '" + name + "'");
                emitArgValue(parts[index + 1], command.args[index], regs[index], targets.getOrDefault(index, -1), labels, ctx, out);
            }

            for (int index = fixedCount + 1; index < parts.length; index++) {
                if (!parts[index].matches("[0-9A-Fa-f]{2}")) throw new CompilerException(ctx + ": readahead argument '" + parts[index] + "' must be a hex byte");
                out.append((char) Integer.parseInt(parts[index], 16));
            }
        } else {
            for (int index = 0; index < command.args.length; index++) {
                if (index + 1 >= parts.length) throw new CompilerException(ctx + ": too few arguments for '" + name + "'" + " (expected " + command.args.length + ")");
                emitArgValue(parts[index + 1], command.args[index], regs[index], targets.getOrDefault(index, -1), labels, ctx, out);
            }
            if (parts.length - 1 > command.args.length) throw new CompilerException(ctx + ": too many arguments for '" + name + "'" + " (expected " + command.args.length + ")");
        }

        for (int index = 0; index < regs.length; index++) {
            ArgType type = command.args[index];
            if (type.literal) continue;
            if (type == ArgType.READAHEAD) break;
            if (regs[index] == null) continue;
            emitRaw(new int[]{0x00, 0x02, 0x00, index}, out);
        }
    }

    private void emitArgValue(String token, ArgType type, RegisterRef ref, int scratchReg, Map<String, Integer> labels, String ctx, StringBuilder out) {
        if (type.addr) {
            int address;
            if (labels.containsKey(token)) {
                address = labels.get(token);
            } else if (addresses.containsKey(token)) {
                address = addresses.get(token);
            } else if (ref != null) {
                out.append((char) ref.register);
                for (int i = 0; i < 3; i++) out.append((char) 0x00); // TODO: worth replicating String#repeat via reflection for this?
                return;
            } else {
                String raw = token.startsWith("$") ? token.substring(1) : token;
                try {
                    address = (int) Long.parseLong(raw, 16);
                } catch (NumberFormatException exception) {
                    throw new CompilerException(ctx + ": '" + token + "' is not a label, address alias," + " register reference, or hex address");
                }
            }
            emitInt(address, out);
            return;
        }

        if (type.literal && ref != null) {
            switch (type) {
                case LITERAL_BYTE -> out.append((char) ref.literal());
                case LITERAL_SHORT -> out.append((char) 0x00).append((char) ref.register);
                case LITERAL_INT -> emitInt(ref.register, out);
                case LITERAL_LONG -> emitLong(ref.register, out);
                default -> out.append((char) ref.register);
            }
            return;
        }

        if (ref != null) {
            int regOrScratch = (scratchReg >= 0) ? scratchReg : ref.register;
            out.append((char) regOrScratch);
            for (int p = 1; p < type.width; p++) out.append((char) 0x00);
            return;
        }

        emitArg(token, type, ctx, out);
    }

    private void emitArg(String token, ArgType type, String ctx, StringBuilder out) {
        String raw = token.startsWith("$") ? token.substring(1) : token;
        long value;
        try {
            value = Long.parseUnsignedLong(raw, 16);
        } catch (NumberFormatException exception) {
            throw new CompilerException(ctx + ": expected hex value, got '" + token + "'");
        }

        switch (type) {
            case BYTE:
            case LITERAL_BYTE:
                if (Long.compareUnsigned(value, 0xFFL) > 0) throw new CompilerException(ctx + ": 0x" + raw + " out of range for byte");
                out.append((char) value);
                break;
            case SHORT:
            case LITERAL_SHORT:
                if (Long.compareUnsigned(value, 0xFFFFL) > 0) throw new CompilerException(ctx + ": 0x" + raw + " out of range for short");
                out.append((char) ((value >> 8) & 0xFF));
                out.append((char) (value & 0xFF));
                break;
            case INT:
            case LITERAL_INT:
            case ADDR:
                emitInt((int) value, out);
                break;
            case LONG:
            case LITERAL_LONG:
                emitLong(value, out);
                break;
            default:
                throw new IllegalStateException(ctx + ": internal error in emitArg for type " + type);
        }
    }

    private void emitInt(int value, StringBuilder out) {
        for (int index = 3; index >= 0; index--) {
            out.append((char) ((value >> 8 * index) & 0xFF));
        }
    }

    private void emitLong(long value, StringBuilder out) {
        for (int index = 7; index >= 0; index--) {
            out.append((char) ((value >> 8 * index) & 0xFF));
        }
    }

    private void emitRaw(int[] bytes, StringBuilder out) {
        for (int that : bytes) out.append((char) (that & 0xFF));
    }

    private boolean isLabel(String trimmed) {
        return trimmed.endsWith(":")
            && !trimmed.contains(" ")
            && !trimmed.contains("`")
            && trimmed.length() > 1;
    }

    private List<String> readLines(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        return Arrays.asList(new String(bytes, StandardCharsets.UTF_8).split("\n", -1));
    }

    private String formatBytes(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (byte that : bytes) {
            if (builder.length() > 0) builder.append(' ');
            builder.append(String.format("%02X", that & 0xFF));
        }
        return builder.toString();
    }

    private void writeBin(Path output, Map<String, Integer> labels, char[] bytecode) throws IOException {
        int ltb = 0;
        for (String name : labels.keySet()) {
            ltb += 4 // address
                + 2  // name size as short
                + name.getBytes(StandardCharsets.UTF_8).length; // 6 byte label header
        }
        int totalSize =
            4 + // Magic
            2 + // Major
            2 + // Minor
            4 + // Label count
            4 + // Total length
            ltb + bytecode.length * 2; // 16-byte header
        ByteBuffer buf = ByteBuffer.allocate(totalSize).order(ByteOrder.BIG_ENDIAN);

        buf.put(MAGIC);
        buf.put(VERSION_MAJOR);
        buf.put(VERSION_MINOR);
        buf.putInt(labels.size());
        buf.putInt(bytecode.length);

        for (Map.Entry<String, Integer> entry : labels.entrySet()) {
            byte[] bytes = entry.getKey().getBytes(StandardCharsets.UTF_8);
            buf.putInt(entry.getValue());
            buf.putShort((short) bytes.length);
            buf.put(bytes);
        }

        for (char that : bytecode) buf.putChar(that);
        Files.write(output, buf.array());
    }

    public static class CompilerException extends RuntimeException { public CompilerException(String message) { super(message); } }
}