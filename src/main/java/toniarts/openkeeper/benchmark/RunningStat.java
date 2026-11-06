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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Online average / min / max for a scalar series (draw calls, triangles, …).
 */
public final class RunningStat {

    private long samples;
    private double sum;
    private double min = Double.POSITIVE_INFINITY;
    private double max = Double.NEGATIVE_INFINITY;

    public void add(double value) {
        if (!Double.isFinite(value)) {
            return;
        }
        samples++;
        sum += value;
        min = Math.min(min, value);
        max = Math.max(max, value);
    }

    public Map<String, Object> summarize() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("samples", samples);
        if (samples == 0) {
            out.put("avg", 0.0);
            out.put("min", 0.0);
            out.put("max", 0.0);
            return out;
        }
        out.put("avg", Math.round((sum / samples) * 1000.0) / 1000.0);
        out.put("min", Math.round(min * 1000.0) / 1000.0);
        out.put("max", Math.round(max * 1000.0) / 1000.0);
        return out;
    }
}
