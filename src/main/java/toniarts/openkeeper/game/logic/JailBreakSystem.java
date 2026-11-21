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
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureImprisoned;
import toniarts.openkeeper.game.component.CreatureTortured;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.JailBreaker;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.PrisonEscape;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
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
 * Prison, opens the door on one tick and on the next one sends the prisoners
 * on their way out through the door (0x4E2E70, their state 0x79). This system
 * asks for that state, {@link CreatureState#JAIL_BREAK}, which the hero takes
 * up when it is next idle.
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
            CreatureAi ai = entityData.getComponent(hero.getId(), CreatureAi.class);
            if (ai != null && ai.getCreatureState() != CreatureState.JAIL_BREAK
                    && ai.getTargetState() != CreatureState.JAIL_BREAK && canBreakOut(hero) && hasFriendToFree(hero)) {
                entityData.setComponent(hero.getId(), new CreatureAi(ai.stateStartTime, ai.getCreatureState(),
                        ai.creatureId, CreatureState.JAIL_BREAK, ai.stateDuration));
            }
        }
    }

    private boolean canBreakOut(Entity hero) {
        return !hero.get(CreatureComponent.class).worker
                && entityData.getComponent(hero.getId(), CreatureImprisoned.class) == null
                && entityData.getComponent(hero.getId(), CreatureTortured.class) == null
                && entityData.getComponent(hero.getId(), Unconscious.class) == null;
    }

    private boolean hasFriendToFree(Entity hero) {
        IRoomController room = mapController.getRoomControllerByCoordinates(
                WorldUtils.vectorToPoint(hero.get(Position.class).position));
        if (room == null || !room.hasObjectControl(ObjectType.PRISONER)) {
            return false;
        }
        Keeper keeper = levelInfo.getPlayer(hero.get(Owner.class).ownerId);
        IRoomObjectControl<?> control = room.getObjectControl(ObjectType.PRISONER);
        if (keeper == null || !keeper.isEnemy(room.getOwnerId()) || !(control instanceof RoomPrisonerControl prison)) {
            return false;
        }
        for (EntityId prisoner : prison.getItems()) {
            if (isFriendHeldInside(keeper, prisoner)) {
                return true;
            }
        }
        return false;
    }

    private boolean isFriendHeldInside(Keeper keeper, EntityId prisoner) {
        Owner owner = entityData.getComponent(prisoner, Owner.class);
        return owner != null && keeper.isAlly(owner.ownerId)
                && entityData.getComponent(prisoner, CreatureImprisoned.class) != null
                && entityData.getComponent(prisoner, PrisonEscape.class) == null;
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
        heroes.release();
    }
}
