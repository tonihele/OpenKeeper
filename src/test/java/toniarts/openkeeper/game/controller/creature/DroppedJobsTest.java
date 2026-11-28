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
package toniarts.openkeeper.game.controller.creature;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.DroppedJob;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Creature.JobType;
import toniarts.openkeeper.tools.convert.map.DroppedJobFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Dropping a creature into a room makes it do the room's job, as in DK II
 */
class DroppedJobsTest {

    private static final short KEEPER = 3;
    private static final short OTHER_KEEPER = 4;
    private static final short WARLOCK_ID = 5;

    private final EntityData entityData = new DefaultEntityData();

    @Test
    void roomsGiveTheirJob() {
        assertEquals(JobType.EAT, DroppedJobs.getRoomJob((short) 4));
        assertEquals(JobType.RESEARCH, DroppedJobs.getRoomJob((short) 6));
        assertEquals(JobType.TRAIN, DroppedJobs.getRoomJob((short) 7));
        assertEquals(JobType.GUARD, DroppedJobs.getRoomJob((short) 9));
        assertEquals(JobType.MANUFACTURE, DroppedJobs.getRoomJob((short) 10));
        assertEquals(JobType.TORTURE, DroppedJobs.getRoomJob((short) 12));
        assertEquals(JobType.PRAY, DroppedJobs.getRoomJob((short) 13));
        assertEquals(JobType.DRINK, DroppedJobs.getRoomJob((short) 15));
    }

    @Test
    void roomsWithoutAJobGiveNone() {
        for (short roomId : new short[]{1, 2, 3, 5, 8, 14, 16, 17, 18, 25, 26}) {
            assertNull(DroppedJobs.getRoomJob(roomId), "room " + roomId);
        }
    }

    @Test
    void creatureDroppedIntoOwnTrainingRoomTrains() {
        Creature warlock = DroppedJobFixtures.creatureHating(JobType.MANUFACTURE);

        assertEquals(JobType.TRAIN, DroppedJobs.getDroppedJob(DroppedJobFixtures.room(7), KEEPER, warlock, KEEPER, false));
    }

    @Test
    void creatureDoesNotTakeAHatedJob() {
        Creature warlock = DroppedJobFixtures.creatureHating(JobType.MANUFACTURE);

        assertNull(DroppedJobs.getDroppedJob(DroppedJobFixtures.room(10), KEEPER, warlock, KEEPER, false));
    }

    @Test
    void heroesAndNeutralsIgnoreHatedJobs() {
        Creature creature = DroppedJobFixtures.creatureHating(JobType.TRAIN);

        assertEquals(JobType.TRAIN, DroppedJobs.getDroppedJob(DroppedJobFixtures.room(7), Player.GOOD_PLAYER_ID, creature, Player.GOOD_PLAYER_ID, false));
        assertEquals(JobType.TRAIN, DroppedJobs.getDroppedJob(DroppedJobFixtures.room(7), Player.NEUTRAL_PLAYER_ID, creature, Player.NEUTRAL_PLAYER_ID, false));
    }

    @Test
    void workersHaveTheirOwnDrop() {
        assertNull(DroppedJobs.getDroppedJob(DroppedJobFixtures.room(7), KEEPER, DroppedJobFixtures.creatureHating(), KEEPER, true));
    }

    @Test
    void noJobOutsideRoomsOrInSomeoneElsesRoom() {
        Creature creature = DroppedJobFixtures.creatureHating();

        assertNull(DroppedJobs.getDroppedJob(null, KEEPER, creature, KEEPER, false));
        assertNull(DroppedJobs.getDroppedJob(DroppedJobFixtures.room(7), OTHER_KEEPER, creature, KEEPER, false));
    }

    @Test
    void creatureWithNoHateJobsTakesAnyJob() {
        Creature creature = DroppedJobFixtures.creatureHating((JobType[]) null);

        assertEquals(JobType.RESEARCH, DroppedJobs.getDroppedJob(DroppedJobFixtures.room(6), KEEPER, creature, KEEPER, false));
    }

    @Test
    void droppedJobComesFirstRightAfterTheDrop() {
        assertTrue(DroppedJobs.isDue(new DroppedJob(JobType.TRAIN, true), true));
        assertTrue(DroppedJobs.isDue(new DroppedJob(JobType.TRAIN, true), false));
    }

    @Test
    void laterNeedsComeBeforeTheDroppedJob() {
        assertFalse(DroppedJobs.isDue(new DroppedJob(JobType.TRAIN, false), true));
        assertTrue(DroppedJobs.isDue(new DroppedJob(JobType.TRAIN, false), false));
    }

    @Test
    void noDroppedJobIsNeverDue() {
        assertFalse(DroppedJobs.isDue(null, false));
    }

    @Test
    void droppingIntoARoomRemembersItsJob() {
        EntityId warlock = warlock();

        DroppedJobs.rememberDroppedJob(entityData, kwdFile(), room(7, KEEPER), warlock);

        DroppedJob droppedJob = entityData.getComponent(warlock, DroppedJob.class);
        assertEquals(JobType.TRAIN, droppedJob.jobType);
        assertTrue(droppedJob.justDropped);
    }

    @Test
    void droppingOutsideARoomForgetsTheOldJob() {
        EntityId warlock = warlock();
        entityData.setComponent(warlock, new DroppedJob(JobType.TRAIN, false));

        DroppedJobs.rememberDroppedJob(entityData, kwdFile(), null, warlock);

        assertNull(entityData.getComponent(warlock, DroppedJob.class));
    }

    @Test
    void droppingIntoAHatedJobForgetsTheOldJob() {
        EntityId warlock = warlock();
        entityData.setComponent(warlock, new DroppedJob(JobType.TRAIN, false));

        DroppedJobs.rememberDroppedJob(entityData, kwdFile(), room(10, KEEPER), warlock);

        assertNull(entityData.getComponent(warlock, DroppedJob.class));
    }

    private EntityId warlock() {
        EntityId entity = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = WARLOCK_ID;
        entityData.setComponent(entity, creatureComponent);
        entityData.setComponent(entity, new Owner(KEEPER, KEEPER));
        return entity;
    }

    private static IKwdFile kwdFile() {
        Creature warlock = DroppedJobFixtures.creatureHating(JobType.MANUFACTURE);
        return (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> "getCreature".equals(method.getName()) && (short) args[0] == WARLOCK_ID ? warlock : null);
    }

    private static IRoomController room(int roomId, short ownerId) {
        Room room = DroppedJobFixtures.room(roomId);
        RoomInstance roomInstance = new RoomInstance(room);
        roomInstance.setOwnerId(ownerId);
        return (IRoomController) Proxy.newProxyInstance(IRoomController.class.getClassLoader(), new Class<?>[]{IRoomController.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getRoom" -> room;
                    case "getRoomInstance" -> roomInstance;
                    default -> null;
                });
    }

}
