/*
 * Copyright (C) 2014-2016 OpenKeeper
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
package toniarts.openkeeper.game.controller.room.storage;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureImprisoned;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.tools.convert.map.IKwdFile;

/**
 * Holds out the prisoners populating a room. When the room is lost (sold,
 * destroyed or captured) the prisoners are set free. As in DKII (DKII.EXE
 * 0x4E39C0, the room clearing) they are taken out of the room, put to their
 * default state and keep their own owner.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class RoomPrisonerControl extends AbstractRoomObjectControl<EntityId> {

    private final IGameTimer gameTimer;
    private final Set<EntityId> prisoners = new LinkedHashSet<>();

    public RoomPrisonerControl(IKwdFile kwdFile, IRoomController parent, EntityData entityData, IGameTimer gameTimer) {
        super(kwdFile, parent, entityData, gameTimer, ObjectType.PRISONER);

        this.gameTimer = gameTimer;
    }

    @Override
    protected int getObjectsPerTile() {
        return 1;
    }


    @Override
    public EntityId addItem(EntityId creature, Point p) {
        setRoomStorageToItem(creature, false);
        addCurrentCapacity(1);
        prisoners.add(creature);

        return creature;
    }

    @Override
    public void removeItem(EntityId object) {
        super.removeItem(object);

        addCurrentCapacity(-1);
        prisoners.remove(object);
    }

    @Override
    public void destroy() {
        releasePrisoners();
        super.destroy();
    }

    @Override
    public void captured(short playerId) {
        releasePrisoners();
    }

    /**
     * Sets all the prisoners held here free
     */
    private void releasePrisoners() {
        for (EntityId prisoner : new ArrayList<>(prisoners)) {
            removeItem(prisoner);
            release(prisoner);
        }
    }

    private void release(EntityId prisoner) {

        // The ones that have died or gone otherwise are not brought back
        CreatureAi ai = entityData.getComponent(prisoner, CreatureAi.class);
        if (ai == null || entityData.getComponent(prisoner, CreatureImprisoned.class) == null) {
            return;
        }
        entityData.removeComponent(prisoner, CreatureImprisoned.class);

        // Back to the control of the real owner
        Owner owner = entityData.getComponent(prisoner, Owner.class);
        if (owner != null) {
            entityData.setComponent(prisoner, new Owner(owner.ownerId, owner.ownerId));
        }
        entityData.setComponent(prisoner, new CreatureAi(gameTimer.getGameTime(), CreatureState.IDLE, ai.creatureId));
    }

}
