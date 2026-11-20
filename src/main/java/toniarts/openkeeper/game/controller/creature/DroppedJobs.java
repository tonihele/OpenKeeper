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
import java.util.Map;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.DroppedJob;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Creature.JobType;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;

/**
 * The rules of dropping a creature into a room, as in DK II. A creature that
 * lands on a room tile takes on the room's job: it trains in a training room,
 * researches in a library and so on. It keeps going back to that job, before
 * any other work, until the job can't be done any more or the keeper picks it
 * up again. Right after the drop the job comes first, later the creature's
 * needs (a lair, sleep and food) come before it.
 */
public final class DroppedJobs {

    /**
     * The job of each room type, keyed by the room ID of the DK II data. The
     * rest of the rooms have no job
     */
    private static final Map<Short, JobType> ROOM_JOBS = Map.of(
            (short) 4, JobType.EAT,
            (short) 6, JobType.RESEARCH,
            (short) 7, JobType.TRAIN,
            (short) 9, JobType.GUARD,
            (short) 10, JobType.MANUFACTURE,
            (short) 12, JobType.TORTURE,
            (short) 13, JobType.PRAY,
            (short) 15, JobType.DRINK);

    private DroppedJobs() {
        // Nope
    }

    /**
     * The job a room gives to the creatures dropped into it
     *
     * @param roomId the room ID of the DK II data
     * @return the job, {@code null} if the room has none
     */
    public static JobType getRoomJob(short roomId) {
        return ROOM_JOBS.get(roomId);
    }

    /**
     * The job a creature takes on when the keeper drops it into a room.
     * Workers have their own work around the drop, and a keeper's creature
     * refuses a job it hates
     *
     * @param room the room the creature lands in, {@code null} if none
     * @param roomOwnerId the owner of the room
     * @param creature the creature
     * @param creatureOwnerId the owner of the creature
     * @param worker whether the creature is a worker
     * @return the job, {@code null} if the creature takes on no job
     */
    public static JobType getDroppedJob(Room room, short roomOwnerId, Creature creature, short creatureOwnerId, boolean worker) {
        if (room == null || worker || roomOwnerId != creatureOwnerId) {
            return null;
        }
        JobType job = getRoomJob(room.getId());
        if (job == null || (isKeeper(creatureOwnerId) && hates(creature, job))) {
            return null;
        }
        return job;
    }

    /**
     * Remembers the job of the room the keeper dropped the creature into, or
     * forgets the old one if the room has no job for the creature
     *
     * @param entityData the entities
     * @param kwdFile the level
     * @param room the room the creature lands in, {@code null} if none or if
     * the creature is taken captive there
     * @param creature the creature
     */
    public static void rememberDroppedJob(EntityData entityData, IKwdFile kwdFile, IRoomController room, EntityId creature) {
        JobType job = null;
        if (room != null) {
            CreatureComponent creatureComponent = entityData.getComponent(creature, CreatureComponent.class);
            short ownerId = entityData.getComponent(creature, Owner.class).ownerId;
            job = getDroppedJob(room.getRoom(), room.getRoomInstance().getOwnerId(),
                    kwdFile.getCreature(creatureComponent.creatureId), ownerId, creatureComponent.worker);
        }
        if (job != null) {
            entityData.setComponent(creature, new DroppedJob(job, true));
        } else {
            entityData.removeComponent(creature, DroppedJob.class);
        }
    }

    /**
     * Whether the creature should do the job it was dropped into now. Right
     * after the drop it always should, later only if no need comes first
     *
     * @param droppedJob the job the creature was dropped into, may be
     * {@code null}
     * @param needFirst whether the creature has a need (lair, sleep, food) to
     * see to
     * @return {@code true} if the creature should do the dropped job now
     */
    public static boolean isDue(DroppedJob droppedJob, boolean needFirst) {
        if (droppedJob == null) {
            return false;
        }
        return droppedJob.justDropped || !needFirst;
    }

    private static boolean isKeeper(short playerId) {
        return playerId != Player.GOOD_PLAYER_ID && playerId != Player.NEUTRAL_PLAYER_ID;
    }

    private static boolean hates(Creature creature, JobType job) {
        if (creature.getHateJobs() == null) {
            return false;
        }
        for (JobType hateJob : creature.getHateJobs()) {
            if (hateJob == job) {
                return true;
            }
        }
        return false;
    }

}
