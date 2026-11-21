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

import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntitySet;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureMood;
import toniarts.openkeeper.game.component.TaskComponent;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;

/**
 * Evil creatures and creatures that are not evil, such as converted heroes,
 * grow angrier while they share a Guard Room, Library, Training Room or
 * Workshop. Every 32 game ticks, while a room holds at least one of each,
 * every creature with a job in it gets eight times the level's anger for the
 * company of hated creatures, 150 a second by default, so 1200. Creature
 * kinds that don't get angry with enemies are spared (DKII.EXE 0x4E8C60,
 * called for the rooms from 0x4DDD70, 0x4E0740, 0x4F2E90 and 0x4F4B20).
 * <br>
 * DK II spreads the rooms over the 32 ticks by their id, here they are all
 * looked at on the same tick.
 */
public final class HatedCompanySystem implements IGameLogicUpdatable {

    private static final int INTERVAL_TICKS = 32;
    private static final Set<TaskType> ROOM_JOBS = EnumSet.of(TaskType.GUARD, TaskType.RESEARCH, TaskType.TRAIN, TaskType.MANUFACTURE);

    private final EntityData entityData;
    private final IKwdFile kwdFile;
    private final IMapController mapController;
    private final IGameTimer gameTimer;
    private final int anger;
    private final EntitySet workers;

    /**
     * The hated company system
     *
     * @param entityData the entity data
     * @param kwdFile the level data
     * @param gameSettings the level variables, for the anger
     * @param mapController the map, for the rooms
     * @param gameTimer the game time
     */
    public HatedCompanySystem(EntityData entityData, IKwdFile kwdFile, Map<MiscType, Variable.MiscVariable> gameSettings,
            IMapController mapController, IGameTimer gameTimer) {
        this.entityData = entityData;
        this.kwdFile = kwdFile;
        this.mapController = mapController;
        this.gameTimer = gameTimer;
        anger = (int) gameSettings.get(MiscType.MODIFY_ANGER_IN_COMPANY_OF_HATED_CREATURES_PER_SECOND).getValue() * 8;

        workers = entityData.getEntities(TaskComponent.class, CreatureMood.class, CreatureComponent.class);
    }

    @Override
    public void processTick(float tpf) {
        if (Math.round(gameTimer.getGameTime() * kwdFile.getGameLevel().getTicksPerSec()) % INTERVAL_TICKS != 0) {
            return;
        }
        workers.applyChanges();

        Map<IRoomController, List<Entity>> workersByRoom = new HashMap<>();
        for (Entity worker : workers) {
            TaskComponent task = worker.get(TaskComponent.class);
            IRoomController room = ROOM_JOBS.contains(task.taskType) && task.targetLocation != null
                    ? mapController.getRoomControllerByCoordinates(task.targetLocation) : null;
            if (room != null) {
                workersByRoom.computeIfAbsent(room, key -> new ArrayList<>()).add(worker);
            }
        }
        for (List<Entity> company : workersByRoom.values()) {
            if (company.stream().anyMatch(this::isEvil) && !company.stream().allMatch(this::isEvil)) {
                for (Entity worker : company) {
                    if (!getCreature(worker).getFlags().contains(Creature.CreatureFlag.DOESNT_GET_ANGRY_WITH_ENEMIES)) {
                        entityData.setComponent(worker.getId(), worker.get(CreatureMood.class).add(CreatureMood.REASON_OTHER, anger));
                    }
                }
            }
        }
    }

    private boolean isEvil(Entity worker) {
        return getCreature(worker).getFlags().contains(Creature.CreatureFlag.IS_EVIL);
    }

    private Creature getCreature(Entity worker) {
        return kwdFile.getCreature(worker.get(CreatureComponent.class).creatureId);
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        workers.release();
    }

}
