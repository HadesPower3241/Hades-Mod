package dev.hadesclient.input;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Polls the GLFW mouse buttons every client tick and records the timestamps of
 * left- and right-click rising edges. Anything that needs CPS reads from this;
 * no mixin required.
 *
 * <p>Ticked from {@code HadesClient.onInitializeClient()}'s tick handler.</p>
 */
public final class ClickTracker {

    /** Only count clicks in the last second. */
    private static final long WINDOW_MS = 1000L;
    /** Max click history to keep — a very fast clicker at 30 CPS still fits. */
    private static final int CAP = 64;

    private final Deque<Long> leftClicks = new ArrayDeque<>();
    private final Deque<Long> rightClicks = new ArrayDeque<>();
    private boolean leftDown;
    private boolean rightDown;

    public void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) return;
        long handle = client.getWindow().getHandle();
        if (handle == 0L) return;

        boolean nowLeft = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean nowRight = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

        long now = System.currentTimeMillis();
        if (nowLeft && !leftDown) push(leftClicks, now);
        if (nowRight && !rightDown) push(rightClicks, now);
        leftDown = nowLeft;
        rightDown = nowRight;
    }

    public int cps() { return leftCps() + rightCps(); }
    public int leftCps() { return countIn(leftClicks); }
    public int rightCps() { return countIn(rightClicks); }

    private static void push(Deque<Long> queue, long now) {
        queue.addLast(now);
        while (queue.size() > CAP) queue.removeFirst();
    }

    private int countIn(Deque<Long> queue) {
        long cutoff = System.currentTimeMillis() - WINDOW_MS;
        while (!queue.isEmpty() && queue.peekFirst() < cutoff) queue.removeFirst();
        return queue.size();
    }
}
