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
package toniarts.openkeeper.game.logic;

import com.simsilica.es.*;
import com.simsilica.es.filter.FieldFilter;
import java.util.HashSet;
import java.util.Set;
import toniarts.openkeeper.game.component.*;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/** Drives each workbench from manufacturing at that bench, rather than room occupancy. */
public final class WorkshopViewSystem implements IGameLogicUpdatable {
    public static final short WORKBENCH_OBJECT_ID = 46;
    private final EntityData data;
    private final EntitySet benches;
    private final EntitySet workers;

    private record WorkSpot(short owner, Point tile) { }

    public WorkshopViewSystem(EntityData data) {
        this.data = data;
        benches = data.getEntities(new FieldFilter<>(ObjectViewState.class, "objectId", WORKBENCH_OBJECT_ID),
                ObjectViewState.class, Position.class, Owner.class);
        workers = data.getEntities(TaskComponent.class, CreatureAi.class, Position.class, Owner.class);
    }

    @Override
    public void processTick(float tpf) {
        benches.applyChanges();
        workers.applyChanges();
        Set<WorkSpot> active = new HashSet<>();
        for (Entity worker : workers) {
            TaskComponent task = worker.get(TaskComponent.class);
            if (task.taskType == TaskType.MANUFACTURE && task.taskStarted && task.targetLocation != null
                    && worker.get(CreatureAi.class).getCreatureState() == CreatureState.WORK
                    && data.getComponent(worker.getId(), Navigation.class) == null
                    && data.getComponent(worker.getId(), InHand.class) == null
                    && data.getComponent(worker.getId(), Death.class) == null) {
                active.add(new WorkSpot(worker.get(Owner.class).ownerId, task.targetLocation));
            }
        }
        for (Entity bench : benches) {
            ObjectViewState view = bench.get(ObjectViewState.class);
            boolean working = active.contains(new WorkSpot(bench.get(Owner.class).ownerId,
                    WorldUtils.vectorToPoint(bench.get(Position.class).position)))
                    && data.getComponent(bench.getId(), Death.class) == null;
            GameObject.State state = working ? GameObject.State.ACTIVATE_ANIM : GameObject.State.NONE;
            if (view.state != state) {
                data.setComponent(bench.getId(), new ObjectViewState(view.objectId, state,
                        view.animState, view.visible, view.lairCreatureTypeId));
            }
        }
    }

    @Override public void start() { }
    @Override public void stop() {
        benches.release();
        workers.release();
    }
}
