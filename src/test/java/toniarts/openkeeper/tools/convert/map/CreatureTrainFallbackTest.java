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
package toniarts.openkeeper.tools.convert.map;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.controller.creature.CreatureController;
import toniarts.openkeeper.game.task.ITaskManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Like in DK II, a keeper's creature that can start none of its own jobs
 * trains. Lives in {@code Creature}'s own package because its setters are
 * {@code protected} (parser-only).
 */
class CreatureTrainFallbackTest {

    private final EntityData entityData = new DefaultEntityData();
    private final List<Creature.JobType> assigned = new ArrayList<>();

    @Test
    void creatureWithoutAJobOfItsOwnTrains() {
        assertTrue(findWork(Player.KEEPER1_ID, EnumSet.noneOf(Creature.CreatureFlag.class),
                Set.of(Creature.JobType.TRAIN), Creature.JobType.MANUFACTURE));
        assertEquals(List.of(Creature.JobType.TRAIN), assigned);
    }

    @Test
    void ownJobComesBeforeTraining() {
        assertTrue(findWork(Player.KEEPER1_ID, EnumSet.noneOf(Creature.CreatureFlag.class),
                Set.of(Creature.JobType.MANUFACTURE, Creature.JobType.TRAIN), Creature.JobType.MANUFACTURE));
        assertEquals(List.of(Creature.JobType.MANUFACTURE), assigned);
    }

    @Test
    void creatureThatDoesNotTrainWhenIdleFindsNoWork() {
        assertFalse(findWork(Player.KEEPER1_ID, EnumSet.of(Creature.CreatureFlag.NOT_TRAIN_WHEN_IDLE),
                Set.of(Creature.JobType.TRAIN)));
        assertTrue(assigned.isEmpty());
    }

    @Test
    void noTrainingWithoutAPlaceToTrain() {
        assertFalse(findWork(Player.KEEPER1_ID, EnumSet.noneOf(Creature.CreatureFlag.class), Set.of()));
        assertTrue(assigned.isEmpty());
    }

    @Test
    void heroesDoNotFallBackToTraining() {
        assertFalse(findWork(Player.GOOD_PLAYER_ID, EnumSet.noneOf(Creature.CreatureFlag.class),
                Set.of(Creature.JobType.TRAIN)));
        assertTrue(assigned.isEmpty());
    }

    /**
     * Lets a creature look for work
     *
     * @param owner the creature's owner
     * @param flags the creature's flags
     * @param available the jobs that can be started
     * @param happyJobs the creature's own jobs
     * @return whether the creature found work
     */
    private boolean findWork(short owner, EnumSet<Creature.CreatureFlag> flags, Set<Creature.JobType> available,
            Creature.JobType... happyJobs) {
        Creature creature = new Creature();
        creature.setFlags(flags);
        Creature.JobPreference[] preferences = new Creature.JobPreference[happyJobs.length];
        for (int i = 0; i < happyJobs.length; i++) {
            preferences[i] = new Creature.JobPreference();
            preferences[i].setJobType(happyJobs[i]);
            preferences[i].setChance((short) 100);
        }
        creature.setHappyJobs(preferences);

        EntityId id = entityData.createEntity();
        entityData.setComponents(id, new CreatureComponent(), new Owner(owner, owner));
        ITaskManager taskManager = (ITaskManager) Proxy.newProxyInstance(
                ITaskManager.class.getClassLoader(), new Class<?>[]{ITaskManager.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "isTaskAvailable" -> available.contains((Creature.JobType) args[1]);
                    case "assignTask" -> available.contains((Creature.JobType) args[1]) && assigned.add((Creature.JobType) args[1]);
                    default -> method.getReturnType() == boolean.class ? Boolean.FALSE : null;
                });
        AtomicReference<Boolean> foundWork = new AtomicReference<>();
        new CreatureController(id, entityData, creature, null, taskManager, null, null, null, null, null, null, null, null, null)
                .findWork(foundWork::set);

        return foundWork.get();
    }
}
