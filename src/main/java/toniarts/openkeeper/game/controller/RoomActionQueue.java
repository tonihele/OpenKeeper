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
package toniarts.openkeeper.game.controller;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.Supplier;
import toniarts.openkeeper.utils.Point;

/**
 * Advances room selections in DK II's diagonal waves, one wave per game tick.
 * Requests may arrive from a network thread; preparation and tile changes run
 * exclusively on the game loop.
 */
final class RoomActionQueue {

    private final ConcurrentLinkedQueue<Selection> incoming = new ConcurrentLinkedQueue<>();
    private final List<Selection> active = new ArrayList<>();

    void add(Point start, Point end, Supplier<Consumer<List<Point>>> prepare) {
        incoming.add(new Selection(new Point(start.x, start.y), new Point(end.x, end.y), prepare));
    }

    void processTick() {
        // Capture the incoming requests before invoking any callbacks.
        Selection request;
        while ((request = incoming.poll()) != null) {
            active.add(request);
        }
        for (Iterator<Selection> iterator = active.iterator(); iterator.hasNext();) {
            Selection selection = iterator.next();
            if (selection.wave == 0) {
                selection.apply = selection.prepare.get();
            }
            if (selection.apply == null) {
                iterator.remove();
                continue;
            }
            selection.apply.accept(selection.nextWave());
            if (selection.wave > selection.width + selection.height) {
                iterator.remove();
            }
        }
    }

    void clear() {
        incoming.clear();
        active.clear();
    }

    private static final class Selection {
        private final Point start;
        private final Point end;
        private final int width;
        private final int height;
        private final Supplier<Consumer<List<Point>>> prepare;
        private Consumer<List<Point>> apply;
        private int wave;

        private Selection(Point start, Point end, Supplier<Consumer<List<Point>>> prepare) {
            this.start = start;
            this.end = end;
            width = Math.abs(start.x - end.x);
            height = Math.abs(start.y - end.y);
            this.prepare = prepare;
        }

        private List<Point> nextWave() {
            List<Point> tiles = new ArrayList<>();
            int dx = Integer.compare(start.x, end.x);
            int dy = Integer.compare(start.y, end.y);
            for (int xDistance = Math.max(0, wave - height); xDistance <= Math.min(width, wave); xDistance++) {
                tiles.add(new Point(end.x + dx * xDistance, end.y + dy * (wave - xDistance)));
            }
            wave++;
            return tiles;
        }
    }
}
