/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.view.animation;

/** Immutable normalized markers; counts crossings independently of audible-event budgets. */
public final class FootstepMarkers {
    private final double left;
    private final double right;

    public FootstepMarkers(int[] frames, double fps) {
        if (frames == null || frames.length != 2 || !Double.isFinite(fps) || fps <= 0)
            throw new IllegalArgumentException("Two markers and a positive finite FPS are required");
        left = frames[0] / fps;
        right = frames[1] / fps;
    }

    /** Traversal ages are unwrapped, with explicit looping and ping-pong semantics. */
    public long crossings(double from, double to, double length, LoopMode mode) {
        if (!Double.isFinite(from) || !Double.isFinite(to) || !Double.isFinite(length) || length <= 0 || to <= from) return 0;
        long count = count(left,from,to,length,mode);
        return count + (left == right ? 0 : count(right,from,to,length,mode));
    }

    private static long count(double mark, double from, double to, double length, LoopMode mode) {
        if (mark < 0 || mark > length) return 0;
        if (mode == LoopMode.DontLoop) return mark > from && mark <= to ? 1 : 0;
        double period = mode == LoopMode.Cycle ? length * 2 : length;
        long forward = repeated(mark,from,to,period);
        return forward + (mode == LoopMode.Cycle && mark > 0 && mark < length
                ? repeated(period-mark,from,to,period) : 0);
    }

    private static long repeated(double mark, double from, double to, double period) {
        return Math.max(0,(long)(Math.floor((to-mark)/period)-Math.floor((from-mark)/period)));
    }
}
