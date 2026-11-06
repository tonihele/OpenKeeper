/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper.  If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.benchmark;

import com.jme3.renderer.Renderer;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;

/**
 * Double-buffered {@code GL_TIME_ELAPSED} queries via jME
 * {@link Renderer#startProfiling(int)} / {@link Renderer#stopProfiling()}.
 * Results are available one frame later (async; does not stall the pipe).
 */
final class GpuFrameTimer {

    private static final Logger logger = System.getLogger(GpuFrameTimer.class.getName());

    private int[] tasks;
    private int writeSlot;
    private int activeSlot = -1;
    private boolean queryActive;
    private boolean supported = true;
    private double lastCompletedMs = Double.NaN;

    /**
     * Begins a GPU elapsed-time query for work submitted until {@link #end}.
     *
     * @param renderer active renderer; ignored when unsupported
     */
    void begin(Renderer renderer) {
        if (!supported || renderer == null) {
            return;
        }
        try {
            ensureTasks(renderer);
            harvest(renderer);
            if (queryActive) {
                renderer.stopProfiling();
                queryActive = false;
            }
            renderer.startProfiling(tasks[writeSlot]);
            activeSlot = writeSlot;
            queryActive = true;
        } catch (RuntimeException ex) {
            disable(ex);
        }
    }

    /**
     * Ends the active query and advances the write slot.
     *
     * @param renderer active renderer
     * @return last <em>completed</em> GPU frame time in ms, or {@link Double#NaN}
     *         until a result is available
     */
    double end(Renderer renderer) {
        if (!supported || renderer == null) {
            return lastCompletedMs;
        }
        try {
            if (queryActive) {
                renderer.stopProfiling();
                queryActive = false;
                writeSlot = 1 - writeSlot;
            }
            harvest(renderer);
        } catch (RuntimeException ex) {
            disable(ex);
        }
        return lastCompletedMs;
    }

    boolean isSupported() {
        return supported;
    }

    private void ensureTasks(Renderer renderer) {
        if (tasks == null) {
            tasks = renderer.generateProfilingTasks(2);
        }
    }

    private void harvest(Renderer renderer) {
        if (tasks == null) {
            return;
        }
        for (int i = 0; i < tasks.length; i++) {
            if (queryActive && i == activeSlot) {
                continue;
            }
            if (renderer.isTaskResultAvailable(tasks[i])) {
                lastCompletedMs = renderer.getProfilingTime(tasks[i]) / 1_000_000.0;
            }
        }
    }

    private void disable(RuntimeException ex) {
        supported = false;
        queryActive = false;
        logger.log(Level.WARNING, "GPU timer queries unavailable; gpuFrameTimeMs stays null", ex);
    }
}
