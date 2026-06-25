package gdn.hypercube.epsilon.core.handler;

import gdn.hypercube.epsilon.core.util.Pair;
import java.util.List;

public abstract class AbstractPlatform {
    public int getArgb(int red, int green, int blue) {
        return (red << 16) | (green << 8) | blue;
    }

    public abstract float getDynamicDeltaTicks();
    public abstract void playSound(PlatformSoundInstance instance, float volume, float pitch);
    public abstract int getWidth(String character);
    public abstract void drawCharacter(String character, int x, int y, int colour);
    public abstract void drawText(int index, List<Pair<String, Integer>> characters, int x, int y);
    public abstract int framerate();
}
