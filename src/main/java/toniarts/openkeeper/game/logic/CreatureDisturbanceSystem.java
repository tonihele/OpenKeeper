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
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntitySet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureMood;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.TaskComponent;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Creatures at work in the Library or the Training Room are disturbed by
 * creatures that stand about in the room, as in DK II. Once a second every
 * creature in the room that doesn't work there gives the researchers or
 * trainees at work within 3 tiles of it their no-work anger (DKII.EXE
 * 0x4E8660, 0x4E88A0). A worker whose other anger is over a tenth of its
 * unhappy level turns on the nearest of them instead and tells it off, and
 * gets its no-work anger on every tick while it does (0x4762D0, 0x4859E0,
 * 0x478F00).
 */
public final class CreatureDisturbanceSystem implements IGameLogicUpdatable {

    private static final short LIBRARY_ROOM_ID = 6;
    private static final short TRAINING_ROOM_ID = 7;
    private static final int DISTURBANCE_TILES = 3;
    private static final int TELL_OFF_ANGER_PERCENTAGE = 10;

    private final EntityData entityData;
    private final IKwdFile kwdFile;
    private final IMapController mapController;
    private final ICreaturesController creaturesController;
    private final IGameTimer gameTimer;
    private final EntitySet creatures;

    public CreatureDisturbanceSystem(EntityData entityData, IKwdFile kwdFile, IMapController mapController,
            ICreaturesController creaturesController, IGameTimer gameTimer) {
        this.entityData = entityData;
        this.kwdFile = kwdFile;
        this.mapController = mapController;
        this.creaturesController = creaturesController;
        this.gameTimer = gameTimer;
        creatures = entityData.getEntities(CreatureComponent.class, CreatureAi.class, Position.class);
    }

    @Override
    public void processTick(float tpf) {
        creatures.applyChanges();
        for (Entity entity : creatures) {
            if (entity.get(CreatureAi.class).getCreatureState() == CreatureState.TELLING_OFF) {
                addNoWorkAnger(entity, 1);
            }
        }

        // The rest is once a game second
        double gameTime = gameTimer.getGameTime();
        if (Math.floor(gameTime) == Math.floor(gameTime - tpf)) {
            return;
        }

        // The creatures in each Library and Training Room that don't work there
        Map<IRoomController, List<Entity>> inTheWay = new HashMap<>();
        for (Entity entity : creatures) {
            IRoomController room = getRoom(entity);
            CreatureState state = entity.get(CreatureAi.class).getCreatureState();
            if (room != null && !isWorkingIn(entity, room) && state != CreatureState.DEAD
                    && state != CreatureState.TELLING_OFF) {
                inTheWay.computeIfAbsent(room, key -> new ArrayList<>()).add(entity);
            }
        }

        for (Entity entity : creatures) {
            IRoomController room = inTheWay.isEmpty() ? null : getRoom(entity);
            List<Entity> others = room != null ? inTheWay.get(room) : null;
            if (others == null || !isWorkingIn(entity, room) || entity.get(CreatureComponent.class).worker
                    || !entityData.getComponent(entity.getId(), TaskComponent.class).taskStarted
                    || entity.get(CreatureAi.class).getCreatureState() != CreatureState.WORK) {
                continue;
            }

            Vector3f position = entity.get(Position.class).position;
            int near = 0;
            Vector3f nearest = null;
            for (Entity other : others) {
                Vector3f otherPosition = other.get(Position.class).position;
                float distance = FastMath.sqr(otherPosition.x - position.x) + FastMath.sqr(otherPosition.z - position.z);
                if (distance < DISTURBANCE_TILES * DISTURBANCE_TILES) {
                    near++;
                }
                if (nearest == null || distance < FastMath.sqr(nearest.x - position.x) + FastMath.sqr(nearest.z - position.z)) {
                    nearest = otherPosition;
                }
            }
            CreatureMood mood = entityData.getComponent(entity.getId(), CreatureMood.class);
            Creature creature = kwdFile.getCreature(entity.get(CreatureComponent.class).creatureId);
            if (near == 0 || mood == null || creature == null) {
                continue;
            }

            if (mood.other > CreatureMood.toRuntimeThreshold(creature.getAttributes().getUnhappyThreshold())
                    * TELL_OFF_ANGER_PERCENTAGE / 100) {
                entityData.setComponent(entity.getId(),
                        new Position(FastMath.atan2(nearest.x - position.x, nearest.z - position.z), position));
                creaturesController.createController(entity.getId()).getStateMachine().changeState(CreatureState.TELLING_OFF);
            } else {
                addNoWorkAnger(entity, near);
            }
        }
    }

    private IRoomController getRoom(Entity entity) {
        IRoomController room = mapController.getRoomControllerByCoordinates(
                WorldUtils.vectorToPoint(entity.get(Position.class).position));
        return room != null && (room.getRoom().getRoomId() == LIBRARY_ROOM_ID
                || room.getRoom().getRoomId() == TRAINING_ROOM_ID) ? room : null;
    }

    /**
     * DK II goes by the creature's state: researching in a Library and
     * training in a Training Room, the walk to the bookcase or the dummy
     * included
     */
    private boolean isWorkingIn(Entity entity, IRoomController room) {
        TaskComponent task = entityData.getComponent(entity.getId(), TaskComponent.class);
        return task != null && task.taskType == (room.getRoom().getRoomId() == LIBRARY_ROOM_ID
                ? TaskType.RESEARCH : TaskType.TRAIN);
    }

    private void addNoWorkAnger(Entity entity, int times) {
        CreatureMood mood = entityData.getComponent(entity.getId(), CreatureMood.class);
        Creature creature = kwdFile.getCreature(entity.get(CreatureComponent.class).creatureId);
        if (mood != null && creature != null) {
            entityData.setComponent(entity.getId(), mood.add(CreatureMood.REASON_NO_WORK,
                    times * CreatureMood.toRuntimeValue(creature.getAttributes().getAngerNoWork())));
        }
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
        creatures.release();
    }

}
