package gdn.hypercube.epsilon.core.util;

import gdn.hypercube.epsilon.core.EpsilonEngine;
import gdn.hypercube.epsilon.core.command.Debug;
import gdn.hypercube.epsilon.core.command.ControlFlow;
import gdn.hypercube.epsilon.core.command.RegisterManipulation;
import gdn.hypercube.epsilon.core.command.RegisterMath;
import gdn.hypercube.epsilon.core.command.TextProcessing;
import java.util.HashMap;

public class EngineCommand {
    public final Argument[] argv;
    public final Executor executor;
    public final boolean[] literal; // do we ignore register reads? mostly for [02 00 XX]
    private static final HashMap<Type, HashMap<Character, EngineCommand>> REGISTRY = new HashMap<>();

    public EngineCommand(Type type, int header, Executor executor, Argument... argv) {
        this.argv = (argv == null) ? new Argument[]{} : argv;
        this.executor = executor;
        this.literal = new boolean[this.argv.length];
        HashMap<Character, EngineCommand> commands = REGISTRY.get(type);
        commands.put((char) header, this);
        REGISTRY.put(type, commands);
    }

    public EngineCommand(Type type, int header, Executor executor, boolean[] literals, Argument... argv) {
        this(type, header, executor, argv);
        if (literals.length != this.argv.length) {
            throw new UnsupportedOperationException("Literal-argument specifications must match argument counts!");
        }
        System.arraycopy(literals, 0, this.literal, 0, this.literal.length);
    }

    public static EngineCommand get(Type type, char lower) {
        return REGISTRY.get(type).get(lower);
    }

    @FunctionalInterface
    public interface Executor {
        void run(EpsilonEngine engine, Argument... argv);
    }

    public enum Type {
        PLAFORM_SPECIFIC,
        TEXT_PROCESSING,
        REGISTER_MANIPULATION,
        CONTROL_FLOW,
    }

    static {
        for (Type type : Type.values()) {
            REGISTRY.put(type, new HashMap<>());
        }

        new Debug();
        new RegisterManipulation();
        new RegisterMath();
        new TextProcessing();
        new ControlFlow();
    }
}
