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
package toniarts.openkeeper.game.fogofwar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A precomputed ring-cast offset table used to build a creature's
 * line-of-sight "stamp" (see design decision D1 in the fog-of-war design
 * document). Generated once from cell geometry rather than imported from the
 * original game data - the shape is close to the original's but not
 * guaranteed bit-identical.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class LineOfSightTable {

    /**
     * One cell offset from a line-of-sight centre, with the angular interval
     * (in 1/256ths of a full turn, inclusive at both ends, possibly wrapping
     * past 255 back to 0) that the cell's square subtends as seen from the
     * centre.
     */
    public static final class Offset {

        public final int dx;
        public final int dy;
        public final int angleStart;
        public final int angleEnd;

        Offset(int dx, int dy, int angleStart, int angleEnd) {
            this.dx = dx;
            this.dy = dy;
            this.angleStart = angleStart;
            this.angleEnd = angleEnd;
        }
    }

    private final List<Offset> offsets;
    private final int maxRadius;

    private LineOfSightTable(Offset[] offsets, int maxRadius) {
        this.offsets = Collections.unmodifiableList(Arrays.asList(offsets));
        this.maxRadius = maxRadius;
    }

    /**
     * @return the offsets, sorted by ascending distance from the centre
     */
    public List<Offset> getOffsets() {
        return offsets;
    }

    public int getMaxRadius() {
        return maxRadius;
    }

    /**
     * Generates a table covering every cell within {@code maxRadius} tiles
     * (plus a one-tile margin so that the outermost ring's angular occlusion
     * is still resolved correctly).
     */
    public static LineOfSightTable generate(int maxRadius) {
        List<Offset> offsets = new ArrayList<>();
        long limitSq = (long) maxRadius * maxRadius + maxRadius;
        int reach = maxRadius + 1;
        for (int dy = -reach; dy <= reach; dy++) {
            for (int dx = -reach; dx <= reach; dx++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }
                long distSq = (long) dx * dx + (long) dy * dy;
                if (distSq > limitSq) {
                    continue;
                }
                offsets.add(buildOffset(dx, dy));
            }
        }
        offsets.sort((a, b) -> {
            long da = (long) a.dx * a.dx + (long) a.dy * a.dy;
            long db = (long) b.dx * b.dx + (long) b.dy * b.dy;
            if (da != db) {
                return Long.compare(da, db);
            }
            return Double.compare(centerAngleTurns(a.dx, a.dy), centerAngleTurns(b.dx, b.dy));
        });
        return new LineOfSightTable(offsets.toArray(new Offset[0]), maxRadius);
    }

    private static double centerAngleTurns(int dx, int dy) {
        return normalizeTurns(Math.atan2(dy, dx) / (2 * Math.PI));
    }

    private static double normalizeTurns(double turns) {
        double t = turns % 1.0;
        return t < 0 ? t + 1.0 : t;
    }

    private static double normalizeAngleDelta(double delta) {
        double d = delta % (2 * Math.PI);
        if (d > Math.PI) {
            d -= 2 * Math.PI;
        } else if (d < -Math.PI) {
            d += 2 * Math.PI;
        }
        return d;
    }

    /**
     * The angular interval subtended by the unit square centred at (dx, dy),
     * seen from the origin: the widest span, among the square's four
     * corners, that contains the angle of the cell's own centre.
     */
    private static Offset buildOffset(int dx, int dy) {
        double centerAngle = Math.atan2(dy, dx);
        double minDelta = 0;
        double maxDelta = 0;
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sy = -1; sy <= 1; sy += 2) {
                double cornerAngle = Math.atan2(dy + sy * 0.5, dx + sx * 0.5);
                double delta = normalizeAngleDelta(cornerAngle - centerAngle);
                minDelta = Math.min(minDelta, delta);
                maxDelta = Math.max(maxDelta, delta);
            }
        }
        double startTurns = normalizeTurns((centerAngle + minDelta) / (2 * Math.PI));
        double endTurns = normalizeTurns((centerAngle + maxDelta) / (2 * Math.PI));
        int angleStart = ((int) Math.floor(startTurns * 256)) & 255;
        int angleEnd = ((int) Math.ceil(endTurns * 256)) & 255;
        return new Offset(dx, dy, angleStart, angleEnd);
    }

}
