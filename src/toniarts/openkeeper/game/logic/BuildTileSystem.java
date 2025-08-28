/*
 * Copyright (C) 2014-2025 OpenKeeper
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
package toniarts.openkeeper.game.logic;

import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntitySet;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import toniarts.openkeeper.game.component.TileBuildOrSell;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.view.selection.SelectionArea;

/**
 * @author ArchDemon
 */
public final class BuildTileSystem implements IGameLogicUpdatable {

    private float tick = 0;
    public final float PERIOD = 0.2f;

    private final IGameWorldController gameWorldController;

    private final EntitySet entities;
    private final Queue<Line> queue = new ArrayDeque<>();

    public BuildTileSystem(EntityData entityData, IGameWorldController gameWorldController) {
        this.gameWorldController = gameWorldController;
        entities = entityData.getEntities(TileBuildOrSell.class);
    }

    @Override
    public void processTick(float tpf) {
        // Add new & remove old
        entities.applyChanges();

        synchronized (this.queue) {
            for (Entity entity : entities) {
                TileBuildOrSell item = entity.get(TileBuildOrSell.class);
                for (Iterator<Set<Point>> it = new SelectionArea.AreaIterator(item.start, item.end); it.hasNext();) {
                    queue.add(new Line(it.next(), item.playerId, item.roomId));
                }
            }
            entities.clear();
        }

        if (tick >= PERIOD) {
            tick -= PERIOD;
            synchronized (this.queue) {
                if (!queue.isEmpty()) {
                    Line item = queue.poll();
                    if (item.isBuild()) {
                        gameWorldController.build(item.getPoints(), item.getPlayerId(), item.getRoomId());
                    } else {
                        gameWorldController.sell(item.getPoints(), item.getPlayerId());
                    }
                }
            }
        }

        tick += tpf;
    }

    @Override
    public void start() {
        entities.applyChanges();
    }

    @Override
    public void stop() {
        entities.release();
    }

    public static class Line {

        private final Set<Point> points;
        private final short playerId;
        private final short roomId;

        public Line(Set<Point> points, short playerId, short roomId) {
            this.points = points;
            this.playerId = playerId;
            this.roomId = roomId;
        }

        public Set<Point> getPoints() {
            return points;
        }

        public short getPlayerId() {
            return playerId;
        }

        public short getRoomId() {
            return roomId;
        }

        public boolean isBuild() {
            return roomId != 0;
        }

    }
}
