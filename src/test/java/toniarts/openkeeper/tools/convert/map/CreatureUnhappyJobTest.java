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
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureMood;
import toniarts.openkeeper.game.component.CreatureMoodJob;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.creature.CreatureController;
import toniarts.openkeeper.game.task.ITaskManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Like in DK II, an unhappy or angry creature takes a job from its unhappy
 * list before the jobs it likes, at most once every five seconds. An angry
 * creature tries its angry list first, which has a five seconds of its own.
 */
class CreatureUnhappyJobTest {

    private static final int UNHAPPY = 50 * 32767 / 100;
    private static final int ANGRY = 32768;

    private EntityData entityData;
    private EntityId entityId;
    private double gameTime;
    private Set<Creature.JobType> availableJobs;
    private List<Creature.JobType> assignedJobs;
    private CreatureController controller;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        entityId = entityData.createEntity();
        entityData.setComponent(entityId, new Owner(Player.KEEPER1_ID, Player.KEEPER1_ID));
        entityData.setComponent(entityId, new CreatureComponent());
        entityData.setComponent(entityId, new CreatureMood(0, 0, 0, 0, UNHAPPY, 0));
        gameTime = 100;
        availableJobs = Set.of(Creature.JobType.RESEARCH, Creature.JobType.SULK, Creature.JobType.DRINK,
                Creature.JobType.LEAVE);
        assignedJobs = new ArrayList<>();

        Creature warlock = new Creature();
        warlock.setHappyJobs(new Creature.JobPreference[]{job(Creature.JobType.RESEARCH, 100)});
        warlock.setUnhappyJobs(new Creature.JobPreference[]{job(Creature.JobType.SULK, 100), job(Creature.JobType.DRINK, 0)});
        warlock.setAngryJobs(new Creature.JobPreference[]{job(Creature.JobType.LEAVE, 100)});
        warlock.getAttributes().setUnhappyThreshold((short) 50);

        ITaskManager taskManager = (ITaskManager) Proxy.newProxyInstance(ITaskManager.class.getClassLoader(),
                new Class<?>[]{ITaskManager.class}, (proxy, method, args) -> switch (method.getName()) {
            case "isTaskAvailable" -> availableJobs.contains((Creature.JobType) args[1]);
            case "assignTask" -> availableJobs.contains((Creature.JobType) args[1])
                    && assignedJobs.add((Creature.JobType) args[1]);
            default -> throw new UnsupportedOperationException(method.getName());
        });
        IGameTimer gameTimer = () -> gameTime;
        controller = new CreatureController(entityId, entityData, warlock, null, taskManager, gameTimer, Map.of(),
                null, null, null, null, null, null, null);
    }

    @Test
    void unhappyCreatureTakesItsUnhappyJob() {
        controller.findWork(foundWork -> {
        });

        assertEquals(List.of(Creature.JobType.SULK), assignedJobs);
        assertEquals(100, entityData.getComponent(entityId, CreatureMoodJob.class).unhappyJobTime);
    }

    @Test
    void angryCreatureTakesItsAngryJob() {
        entityData.setComponent(entityId, new CreatureMood(0, 0, 0, 0, ANGRY, 0));

        controller.findWork(foundWork -> {
        });

        assertEquals(List.of(Creature.JobType.LEAVE), assignedJobs);
        assertEquals(100, entityData.getComponent(entityId, CreatureMoodJob.class).angryJobTime);
    }

    @Test
    void creatureJustShortOfAngryKeepsToItsUnhappyJob() {
        entityData.setComponent(entityId, new CreatureMood(0, 0, 0, 0, ANGRY - 1, 0));

        controller.findWork(foundWork -> {
        });

        assertEquals(List.of(Creature.JobType.SULK), assignedJobs);
    }

    @Test
    void angryCreatureWithNoAngryJobToStartTakesItsUnhappyJob() {
        entityData.setComponent(entityId, new CreatureMood(0, 0, 0, 0, ANGRY, 0));
        availableJobs = Set.of(Creature.JobType.RESEARCH, Creature.JobType.SULK);

        controller.findWork(foundWork -> {
        });

        assertEquals(List.of(Creature.JobType.SULK), assignedJobs);
    }

    @Test
    void angryJobsAreFiveSecondsApart() {
        entityData.setComponent(entityId, new CreatureMood(0, 0, 0, 0, ANGRY, 0));

        controller.findWork(foundWork -> {
        });
        gameTime = 104;
        controller.findWork(foundWork -> {
        });
        gameTime = 104.5;
        controller.findWork(foundWork -> {
        });
        gameTime = 105;
        controller.findWork(foundWork -> {
        });

        assertEquals(List.of(Creature.JobType.LEAVE, Creature.JobType.SULK, Creature.JobType.RESEARCH,
                Creature.JobType.LEAVE), assignedJobs);
    }

    @Test
    void contentCreatureTakesTheJobItLikes() {
        entityData.setComponent(entityId, new CreatureMood(0, 0, 0, 0, UNHAPPY - 1, 0));

        controller.findWork(foundWork -> {
        });

        assertEquals(List.of(Creature.JobType.RESEARCH), assignedJobs);
        assertNull(entityData.getComponent(entityId, CreatureMoodJob.class));
    }

    @Test
    void unhappyJobThatCannotStartGivesWayToTheNextOnTheList() {
        availableJobs = Set.of(Creature.JobType.RESEARCH, Creature.JobType.DRINK);

        controller.findWork(foundWork -> {
        });

        assertEquals(List.of(Creature.JobType.DRINK), assignedJobs);
    }

    @Test
    void unhappyCreatureWithNoUnhappyJobToStartTakesTheJobItLikes() {
        availableJobs = Set.of(Creature.JobType.RESEARCH);

        controller.findWork(foundWork -> {
        });

        assertEquals(List.of(Creature.JobType.RESEARCH), assignedJobs);
        assertNull(entityData.getComponent(entityId, CreatureMoodJob.class));
    }

    @Test
    void unhappyJobsAreFiveSecondsApart() {
        controller.findWork(foundWork -> {
        });
        gameTime = 104.75;
        controller.findWork(foundWork -> {
        });
        gameTime = 105;
        controller.findWork(foundWork -> {
        });

        assertEquals(List.of(Creature.JobType.SULK, Creature.JobType.RESEARCH, Creature.JobType.SULK), assignedJobs);
    }

    private static Creature.JobPreference job(Creature.JobType jobType, int chance) {
        Creature.JobPreference job = new Creature.JobPreference();
        job.setJobType(jobType);
        job.setChance((short) chance);
        return job;
    }

}
