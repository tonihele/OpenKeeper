/*
 * Copyright (C) 2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper. If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.tools.convert.map;

import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.CombatPitFighter;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureMood;
import toniarts.openkeeper.game.component.JobPicked;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.creature.CreatureController;
import toniarts.openkeeper.game.controller.room.CombatPitController;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.ITaskManager;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Like DKII, a creature that looks for a job and finds none gets angry for the
 * lack of work.
 */
class CreatureNoWorkAngerTest {

    private final List<Creature.JobType> assigned = new ArrayList<>();
    private Creature.JobType unavailable;
    private double gameTime;

    @Test
    void creatureWithNoJobToDoGetsAngry() {
        EntityData entityData = new DefaultEntityData();
        EntityId entityId = createCreature(entityData);
        AtomicBoolean foundWork = new AtomicBoolean(true);

        controller(entityData, entityId, false).findWork(foundWork::set);

        assertFalse(foundWork.get());
        assertEquals(50 / 4, entityData.getComponent(entityId, CreatureMood.class).noWork);
    }

    @Test
    void creatureThatFindsAJobStaysCalm() {
        EntityData entityData = new DefaultEntityData();
        EntityId entityId = createCreature(entityData);
        AtomicBoolean foundWork = new AtomicBoolean(false);

        controller(entityData, entityId, true).findWork(foundWork::set);

        assertTrue(foundWork.get());
        assertEquals(0, entityData.getComponent(entityId, CreatureMood.class).noWork);
    }

    @Test
    void creatureThatPickedAJobDoesNotPickAgainForFiveSeconds() {
        EntityData entityData = new DefaultEntityData();
        EntityId entityId = createCreature(entityData);
        AtomicBoolean foundWork = new AtomicBoolean(false);
        gameTime = 100;
        controller(entityData, entityId, true).findWork(foundWork::set);
        assertEquals(List.of(Creature.JobType.RESEARCH), assigned);
        assertEquals(100, entityData.getComponent(entityId, JobPicked.class).time);

        // The job is over 4 seconds later: it doesn't pick from its jobs but goes on to its other choices, here
        // training, and that doesn't anger it (0x4780E0)
        gameTime = 104;
        controller(entityData, entityId, true).findWork(foundWork::set);

        assertEquals(List.of(Creature.JobType.RESEARCH, Creature.JobType.TRAIN), assigned);
        assertEquals(0, entityData.getComponent(entityId, CreatureMood.class).noWork);
        assertEquals(100, entityData.getComponent(entityId, JobPicked.class).time);

        // After 5 seconds it picks again
        gameTime = 105;
        controller(entityData, entityId, true).findWork(foundWork::set);

        assertEquals(List.of(Creature.JobType.RESEARCH, Creature.JobType.TRAIN, Creature.JobType.RESEARCH), assigned);
        assertEquals(105, entityData.getComponent(entityId, JobPicked.class).time);
    }

    @Test
    void creatureThatTrainsForLackOfAJobGetsAngryFirst() {
        EntityData entityData = new DefaultEntityData();
        EntityId entityId = createCreature(entityData);
        AtomicBoolean foundWork = new AtomicBoolean(false);
        unavailable = Creature.JobType.RESEARCH;

        // None of its own jobs can start: that angers it, and only then does it go on to train (0x4780E0, 0x478680)
        controller(entityData, entityId, true).findWork(foundWork::set);

        assertTrue(foundWork.get());
        assertEquals(List.of(Creature.JobType.TRAIN), assigned);
        assertEquals(50 / 4, entityData.getComponent(entityId, CreatureMood.class).noWork);
    }

    @Test
    void pitFighterWaitingForAnOpponentGetsAngry() {
        EntityData entityData = new DefaultEntityData();
        EntityId entityId = createCreature(entityData);
        entityData.setComponent(entityId, new Position(0, new Vector3f(3, 0, 3)));

        // A 5 x 5 pit, the creature in the middle of it
        RoomInstance roomInstance = new RoomInstance(RoomFixtures.roomWithObjects((short) 16, (short) 0, (short) 0, (short) 0, (short) 0, (short) 0, (short) 0));
        for (int x = 1; x <= 5; x++) {
            for (int y = 1; y <= 5; y++) {
                roomInstance.addCoordinate(new Point(x, y));
            }
        }
        IObjectsController objectsController = (IObjectsController) Proxy.newProxyInstance(IObjectsController.class.getClassLoader(),
                new Class<?>[]{IObjectsController.class}, (proxy, method, args) -> entityData.createEntity());
        CombatPitController pit = new CombatPitController(entityData.createEntity(), entityData, null, roomInstance, objectsController, () -> 0);
        pit.construct();
        entityData.setComponent(entityId, new CombatPitFighter(pit.getEntityId()));
        IMapController mapController = (IMapController) Proxy.newProxyInstance(IMapController.class.getClassLoader(),
                new Class<?>[]{IMapController.class}, (proxy, method, args) -> pit);
        INavigationService navigationService = (INavigationService) Proxy.newProxyInstance(INavigationService.class.getClassLoader(),
                new Class<?>[]{INavigationService.class}, (proxy, method, args) -> new DefaultGraphPath<>());
        Creature fighter = new Creature();
        fighter.getAttributes().setAngerNoWork((short) 50);

        new CreatureController(entityId, entityData, fighter, navigationService, null, null, Map.of(),
                null, null, mapController, null, null, null, null).navigateToRandomPointInCombatPit();

        assertEquals(50 / 4, entityData.getComponent(entityId, CreatureMood.class).noWork);
        assertTrue(pit.isFightingTile(entityData.getComponent(entityId, Navigation.class).target));
    }

    private static EntityId createCreature(EntityData entityData) {
        EntityId entityId = entityData.createEntity();
        entityData.setComponent(entityId, new Owner(Player.KEEPER1_ID, Player.KEEPER1_ID));
        entityData.setComponent(entityId, new CreatureComponent());
        entityData.setComponent(entityId, new CreatureMood());
        return entityId;
    }

    private CreatureController controller(EntityData entityData, EntityId entityId, boolean jobAvailable) {
        Creature.JobPreference research = new Creature.JobPreference();
        research.setJobType(Creature.JobType.RESEARCH);
        research.setChance((short) 100);
        Creature mistress = new Creature();
        mistress.setHappyJobs(new Creature.JobPreference[]{research});
        mistress.getAttributes().setAngerNoWork((short) 50);
        mistress.setFlags(EnumSet.noneOf(Creature.CreatureFlag.class));

        ITaskManager taskManager = (ITaskManager) Proxy.newProxyInstance(ITaskManager.class.getClassLoader(),
                new Class<?>[]{ITaskManager.class}, (proxy, method, args) -> switch (method.getName()) {
            case "isTaskAvailable" -> jobAvailable && args[1] != unavailable;
            case "assignTask" -> {
                if (jobAvailable) {
                    assigned.add((Creature.JobType) args[1]);
                }
                yield jobAvailable;
            }
            default -> throw new UnsupportedOperationException(method.getName());
        });
        return new CreatureController(entityId, entityData, mistress, null, taskManager, () -> gameTime, Map.of(),
                null, null, null, null, null, null, null);
    }

}
