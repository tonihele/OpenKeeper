/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.animation;

import java.io.IOException;
import java.io.Writer;
import java.util.Locale;

/** Synthetic authoritative input, independent of render cadence and gameplay RNG. */
public final class MotionTrace {
    public enum Scenario { CONSTANT, ACCELERATE_STOP, CORNER, REVERSE, BLOCKED, TELEPORT, PAUSE }
    public record Sample(double time, long sequence, int epoch, double x, double z, double yaw,
                         double phaseTime, boolean discontinuity) { }

    private MotionTrace() { }

    public static Sample sample(Scenario scenario, int tick, double step) {
        if (scenario == null || tick < 0 || !Double.isFinite(step) || step <= 0)
            throw new IllegalArgumentException("Expected scenario, nonnegative tick and positive finite step");
        double time = tick * step;
        if (!Double.isFinite(time)) throw new IllegalArgumentException("Trace time overflow");
        double x = time, z = 0, yaw = 0, phase = time;
        int epoch = 0;
        boolean cut = tick == 0;
        switch (scenario) {
            case ACCELERATE_STOP -> x = time < 1 ? time * time / 2 : time < 2 ? time - .5 : 1.5;
            case CORNER -> { x = Math.min(time, 1); z = Math.max(0, time - 1); yaw = time < 1 ? 0 : Math.PI / 2; }
            case REVERSE -> { x = time < 1 ? time : 2 - time; yaw = time < 1 ? 0 : Math.PI; }
            case BLOCKED -> x = Math.min(time, 1);
            case TELEPORT -> { epoch = time < 1 ? 0 : 1; x += epoch * 10; cut |= tick > 0 && (tick - 1) * step < 1 && time >= 1; }
            case PAUSE -> { phase = Math.min(time, 1) + Math.max(0, time - 2); x = phase; }
            default -> { }
        }
        return new Sample(time, tick, epoch, x, z, yaw, phase, cut);
    }

    public static void write(Writer writer, Scenario scenario, int ticks, double step) throws IOException {
        if (ticks < 1) throw new IllegalArgumentException("Expected at least one sample");
        writer.write("time,sequence,epoch,x,z,yaw,phase_time,discontinuity\n");
        for (int tick = 0; tick < ticks; tick++) {
            Sample s = sample(scenario, tick, step);
            writer.write(String.format(Locale.ROOT, "%.9f,%d,%d,%.9f,%.9f,%.9f,%.9f,%s%n",
                    s.time, s.sequence, s.epoch, s.x, s.z, s.yaw, s.phaseTime, s.discontinuity));
        }
    }
}
