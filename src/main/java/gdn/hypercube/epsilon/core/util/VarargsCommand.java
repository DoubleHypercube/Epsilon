package gdn.hypercube.epsilon.core.util;

import java.util.function.ToIntFunction;

public class VarargsCommand extends EngineCommand {
    public final ToIntFunction<char[]> argn;

    public VarargsCommand(Type type, int header, Executor executor, ToIntFunction<char[]> argn, Argument... argv) {
        super(type, header, executor, argv);
        this.argn = argn;
    }

    public int readahead(char[] script, int ip) {
        int bytes = 0;
        for (Argument argument : this.argv) {
            bytes += argument.type.width;
        }
        char[] readahead = new char[bytes];
        System.arraycopy(script, ip + 1, readahead, 0, bytes);
        return this.argn.applyAsInt(readahead);
    }
}