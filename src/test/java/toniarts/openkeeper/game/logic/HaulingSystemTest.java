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

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureFall;
import toniarts.openkeeper.game.component.HauledBy;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.PossessedAbility;
import toniarts.openkeeper.game.component.TaskComponent;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.PossessionFixtures;
import toniarts.openkeeper.tools.convert.map.ShotFixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * An Imp carries a knocked out creature only while it is on the job of
 * carrying it. A possessed Bile Demon holds the worker it carries in front of
 * it on every game logic tick.
 */
class HaulingSystemTest {

    private static final float BILE_DEMON_EYE_HEIGHT = 0.37f;

    private final EntityData entityData = new DefaultEntityData();
    private final Creature bileDemon = ShotFixtures.creature((short) 3, EnumSet.noneOf(Creature.CreatureFlag.class),
            EnumSet.noneOf(Creature.CreatureFlag3.class));
    private final HaulingSystem haulingSystem = new HaulingSystem(entityData, (ICreaturesController) Proxy.newProxyInstance(
            ICreaturesController.class.getClassLoader(), new Class<?>[]{ICreaturesController.class},
            (proxy, method, args) -> Proxy.newProxyInstance(ICreatureController.class.getClassLoader(),
                    new Class<?>[]{ICreatureController.class}, (controller, controllerMethod, controllerArgs) -> "getCreature".equals(controllerMethod.getName()) ? bileDemon : null)));
    private final EntityId imp = entityData.createEntity();
    private final EntityId creature = entityData.createEntity();

    HaulingSystemTest() {
        entityData.setComponent(imp, new Position(0, new Vector3f(5, 1, 5)));
        entityData.setComponents(creature, new Position(0, new Vector3f(2, 1, 2)), new Unconscious(0),
                new HauledBy(imp));
    }

    @Test
    void impOnTheJobCarriesTheCreature() {
        entityData.setComponent(imp, new TaskComponent(1, creature, null, TaskType.CARRY_CREATURE_TO_JAIL, 0, true));

        haulingSystem.processTick(0.25f);

        assertNotNull(entityData.getComponent(creature, HauledBy.class));
        assertEquals(5, entityData.getComponent(creature, Position.class).position.x);
    }

    @Test
    void impSwitchingJobsKeepsTheCreature() {
        entityData.setComponent(imp, new TaskComponent(1, creature, null, TaskType.CAPTURE_ENEMY_CREATURE, 0, true));

        // Like CreatureController.setAssignedTask, the old task goes and the new one comes on the same logic tick
        entityData.removeComponent(imp, TaskComponent.class);
        entityData.setComponent(imp, new TaskComponent(2, creature, null, TaskType.CARRY_CREATURE_TO_JAIL, 0, false));
        haulingSystem.processTick(0.25f);

        assertNotNull(entityData.getComponent(creature, HauledBy.class));
        assertEquals(5, entityData.getComponent(creature, Position.class).position.x);
    }

    @Test
    void impThatLeftTheJobPutsTheCreatureDown() {

        // An Imp that has dropped the job, like one that runs away, can't keep holding the creature. It would
        // never bleed out, and no Imp would take it to a Prison or a lair
        haulingSystem.processTick(0.25f);

        assertNull(entityData.getComponent(creature, HauledBy.class));
        assertEquals(2, entityData.getComponent(creature, Position.class).position.x);
    }

    @Test
    void carriedWorkerIsHeldHalfATileInFrontAtTheEyeHeightOnEachTick() {
        PossessionFixtures.setEyeHeight(bileDemon, BILE_DEMON_EYE_HEIGHT);
        EntityId demon = entityData.createEntity();
        EntityId worker = entityData.createEntity();
        entityData.setComponents(demon, new Position(0, new Vector3f(10, 0, 2)),
                new PossessedAbility(Creature.SpecialAbility.PICK_UP_WORKERS, 0, worker));
        entityData.setComponent(worker, new Position(1, new Vector3f(10, 0, 2.6f)));

        haulingSystem.processTick(0.25f);

        assertEquals(new Vector3f(10, BILE_DEMON_EYE_HEIGHT, 2.5f), entityData.getComponent(worker, Position.class).position);
        assertEquals(0, entityData.getComponent(worker, Position.class).rotation);

        // The steering moves and turns the Bile Demon, the worker follows on the next logic tick
        entityData.setComponent(demon, new Position(FastMath.HALF_PI, new Vector3f(11, 0, 2)));
        assertEquals(new Vector3f(10, BILE_DEMON_EYE_HEIGHT, 2.5f), entityData.getComponent(worker, Position.class).position);

        haulingSystem.processTick(0.25f);

        Position held = entityData.getComponent(worker, Position.class);
        assertEquals(11.5f, held.position.x, 0.0001f);
        assertEquals(BILE_DEMON_EYE_HEIGHT, held.position.y, 0.0001f);
        assertEquals(2, held.position.z, 0.0001f);
        assertEquals(FastMath.HALF_PI, held.rotation);
    }

    @Test
    void thrownWorkerIsLetGoFromWhereItIsHeld() {
        PossessionFixtures.setEyeHeight(bileDemon, BILE_DEMON_EYE_HEIGHT);
        EntityId demon = entityData.createEntity();
        EntityId worker = entityData.createEntity();
        entityData.setComponents(demon, new Position(0, new Vector3f(10, 0, 2)),
                new PossessedAbility(Creature.SpecialAbility.PICK_UP_WORKERS, 0, worker));
        entityData.setComponent(worker, new Position(0, new Vector3f(10, 0.5f, 2.5f)));
        haulingSystem.processTick(0.25f);

        // The keeper's throw from the session thread only marks it, the worker's own turn throws it (0x4831D0)
        Vector3f velocity = new Vector3f(0, 0.25f, 2);
        entityData.setComponent(demon, new PossessedAbility(Creature.SpecialAbility.PICK_UP_WORKERS, 0, worker, velocity));
        entityData.setComponent(demon, new Position(0, new Vector3f(12, 0, 2)));
        haulingSystem.processTick(0.25f);

        assertNull(entityData.getComponent(demon, PossessedAbility.class));
        assertEquals(new Vector3f(10, BILE_DEMON_EYE_HEIGHT, 2.5f), entityData.getComponent(worker, Position.class).position);
        CreatureFall fall = entityData.getComponent(worker, CreatureFall.class);
        assertEquals(velocity, fall.velocity);
        assertEquals(50, fall.damage);

        // It isn't held any more
        haulingSystem.processTick(0.25f);
        assertEquals(new Vector3f(10, BILE_DEMON_EYE_HEIGHT, 2.5f), entityData.getComponent(worker, Position.class).position);
    }

    @Test
    void workerTakenFromTheCarrierIsNotThrown() {
        EntityId demon = entityData.createEntity();
        EntityId worker = entityData.createEntity();
        entityData.setComponents(demon, new Position(0, new Vector3f(10, 0, 2)),
                new PossessedAbility(Creature.SpecialAbility.PICK_UP_WORKERS, 0, worker, new Vector3f()));

        haulingSystem.processTick(0.25f);

        assertNull(entityData.getComponent(demon, PossessedAbility.class));
        assertNull(entityData.getComponent(worker, CreatureFall.class));
    }
}
