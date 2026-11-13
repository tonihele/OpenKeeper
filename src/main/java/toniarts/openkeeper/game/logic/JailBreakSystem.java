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
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureImprisoned;
import toniarts.openkeeper.game.component.CreatureTortured;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.JailBreaker;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.storage.IRoomObjectControl;
import toniarts.openkeeper.game.controller.room.storage.RoomPrisonerControl;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * The jail break. A hero with the party flag FREE_FRIENDS_ON_JAIL_BREAK that
 * stands in the Prison of an enemy frees the imprisoned creatures that are
 * friends of the hero (DKII.EXE 0x4E8660, called for a creature standing in a
 * room from its tick at 0x488930). In DKII workers do not, nor do creatures
 * that are imprisoned or tortured.
 * <p>
 * DKII has no timer for this. 0x4E8660 puts the hero into its jail break state
 * (id 0x7a, tick handler 0x473C60), which walks the hero to the door of the
 * Prison, opens the door on one tick and frees the prisoners on the next one
 * (0x4E2E70). The delay is a couple of logic ticks, not a number of seconds.
 * Here the hero is not sent to the door, and the prisoners are freed on the
 * tick the hero is in the Prison.
 * <p>
 * Not from DKII: an unconscious hero does nothing, and the prisoners of the
 * hero's allies are freed too (0x4E2E70 frees the ones with the hero's owner).
 */
public final class JailBreakSystem implements IGameLogicUpdatable {

    private final EntityData entityData;
    private final ILevelInfo levelInfo;
    private final IMapController mapController;
    private final EntitySet heroes;

    /**
     * @param entityData the entity data
     * @param levelInfo the level players, to tell friends from enemies
     * @param mapController finds the room a hero stands in
     */
    public JailBreakSystem(EntityData entityData, ILevelInfo levelInfo, IMapController mapController) {
        this.entityData = entityData;
        this.levelInfo = levelInfo;
        this.mapController = mapController;
        heroes = entityData.getEntities(JailBreaker.class, Position.class, Owner.class, CreatureComponent.class,
                Health.class);
    }

    @Override
    public void processTick(float tpf) {
        heroes.applyChanges();
        for (Entity hero : heroes) {
            if (canBreakOut(hero)) {
                while (freeFriend(hero) != null) {
                    // Free all the friends
                }
            }
        }
    }

    private boolean canBreakOut(Entity hero) {
        return !hero.get(CreatureComponent.class).worker
                && entityData.getComponent(hero.getId(), CreatureImprisoned.class) == null
                && entityData.getComponent(hero.getId(), CreatureTortured.class) == null
                && entityData.getComponent(hero.getId(), Unconscious.class) == null;
    }

    private EntityId freeFriend(Entity hero) {
        IRoomController room = mapController.getRoomControllerByCoordinates(
                WorldUtils.vectorToPoint(hero.get(Position.class).position));
        if (room == null || !room.hasObjectControl(ObjectType.PRISONER)) {
            return null;
        }
        Keeper keeper = levelInfo.getPlayer(hero.get(Owner.class).ownerId);
        IRoomObjectControl<?> control = room.getObjectControl(ObjectType.PRISONER);
        if (keeper == null || !keeper.isEnemy(room.getOwnerId()) || !(control instanceof RoomPrisonerControl prison)) {
            return null;
        }
        return prison.releaseFirst(prisoner -> isFriend(keeper, prisoner));
    }

    private boolean isFriend(Keeper keeper, EntityId prisoner) {
        Owner owner = entityData.getComponent(prisoner, Owner.class);
        return owner != null && keeper.isAlly(owner.ownerId);
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
        heroes.release();
    }
}
