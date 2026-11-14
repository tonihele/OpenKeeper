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

import com.jme3.math.Vector3f;
import com.simsilica.es.*;
import com.simsilica.es.base.DefaultEntityData;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.*;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.utils.Point;
import static org.junit.jupiter.api.Assertions.*;

class WorkshopViewSystemTest {
    final EntityData data = new DefaultEntityData();
    final WorkshopViewSystem system = new WorkshopViewSystem(data);
    final EntityId bench = bench(4, 4, (short) 3);

    EntityId bench(int x, int y, short owner) {
        EntityId id = data.createEntity();
        data.setComponents(id, new ObjectViewState((short) 46, GameObject.State.NONE,
                ObjectViewState.GameObjectAnimState.MESH_RESOURCE, true),
                new Position(0, new Vector3f(x, 0, y)), new Owner(owner, owner));
        return id;
    }
    EntityId worker() {
        EntityId id = data.createEntity();
        data.setComponents(id, new CreatureAi(0, CreatureState.WORK, (short) 1),
                new Position(0, new Vector3f(4.5f, 0, 4)), new Owner((short) 3, (short) 3));
        task(id, true);
        return id;
    }
    void task(EntityId id, boolean started) {
        data.setComponent(id, new TaskComponent(1, null, new Point(4,4), TaskType.MANUFACTURE, 0, started));
    }
    void tick() { system.processTick(.25f); }
    GameObject.State state(EntityId id) { return data.getComponent(id, ObjectViewState.class).state; }

    @Test void emptyAndApproachingWorkshopsRemainStill() {
        tick(); assertEquals(GameObject.State.NONE, state(bench));
        EntityId worker = worker(); task(worker, false);
        tick(); assertEquals(GameObject.State.NONE, state(bench));
        task(worker, true); data.setComponent(worker, new Navigation());
        tick(); assertEquals(GameObject.State.NONE, state(bench));
    }
    @Test void onlyTheBenchBeingWorkedAtSpinsAndStopsWhenWorkEnds() {
        EntityId unused = bench(7,4,(short) 3);
        EntityId enemy = bench(4,4,(short) 4);
        EntityId worker = worker();
        tick(); assertEquals(GameObject.State.ACTIVATE_ANIM, state(bench));
        assertEquals(GameObject.State.NONE, state(unused));
        assertEquals(GameObject.State.NONE, state(enemy));
        ObjectViewState unchanged = data.getComponent(bench, ObjectViewState.class);
        tick(); assertSame(unchanged, data.getComponent(bench, ObjectViewState.class));
        data.removeComponent(worker, TaskComponent.class);
        tick(); assertEquals(GameObject.State.NONE, state(bench));
        task(worker, true); tick(); assertEquals(GameObject.State.ACTIVATE_ANIM, state(bench));
    }
    @Test void interruptionsStopTheSawAndOtherWorkersKeepItRunning() {
        EntityId a = worker();
        EntityId b = worker();
        tick();
        data.setComponent(a, new InHand());
        tick(); assertEquals(GameObject.State.ACTIVATE_ANIM, state(bench));
        data.setComponent(b, new CreatureAi(0, CreatureState.FIGHT, (short) 1));
        tick(); assertEquals(GameObject.State.NONE, state(bench));
        data.removeComponent(a, InHand.class); tick();
        assertEquals(GameObject.State.ACTIVATE_ANIM, state(bench));
        data.setComponent(a, new Death(0)); tick();
        assertEquals(GameObject.State.NONE, state(bench));
    }
    @Test void rebuiltSystemRecoversActivityFromExistingComponents() {
        worker(); tick(); system.stop();
        WorkshopViewSystem restored = new WorkshopViewSystem(data);
        restored.processTick(.25f);
        assertEquals(GameObject.State.ACTIVATE_ANIM, state(bench));
        data.setComponent(bench, new Owner((short) 4, (short) 4));
        restored.processTick(.25f);
        assertEquals(GameObject.State.NONE, state(bench));
        data.removeEntity(bench); restored.processTick(.25f); restored.stop();
    }
}
