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
package toniarts.openkeeper.game.controller.creature;

import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.badlogic.gdx.ai.pfa.GraphPath;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.util.List;
import java.util.function.Predicate;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.utils.Point;

/**
 * An enemy door as a target of an attack. A door that stands in the way of a
 * creature is not walked round, the creature goes up to it and hits it with its
 * melee attack until the door gives way.
 */
public final class DoorAttackTarget {

    /**
     * A creature that has a door next to it on its path is at this index of
     * the path (the creature itself is the index 0)
     */
    public static final int ADJACENT_DOOR_INDEX = 1;

    private DoorAttackTarget() {
        // Nope
    }

    /**
     * Can the door be attacked at all. Door blueprints are not doors yet, the
     * secret doors that are still undiscovered are walls to the enemy, and a
     * door with no health left is gone already.
     *
     * @param entityData the entity data
     * @param door the entity, any entity
     * @return true if the entity is a door that stands and shows
     */
    public static boolean isBreakable(EntityData entityData, EntityId door) {
        DoorComponent doorComponent = entityData.getComponent(door, DoorComponent.class);
        if (doorComponent == null || doorComponent.blueprint || doorComponent.secret) {
            return false;
        }

        Health health = entityData.getComponent(door, Health.class);
        return health != null && health.health > 0;
    }

    /**
     * Does a creature break the door down when it is in the way. The workers
     * leave the doors alone, and so does everyone with the doors of their own
     * side.
     *
     * @param entityData the entity data
     * @param door the entity, any entity
     * @param fightStyle the fight style of the creature
     * @param enemyDoor is the door an enemy's
     * @return true if the creature attacks the door
     */
    public static boolean canBreak(EntityData entityData, EntityId door, Creature.FightStyle fightStyle, boolean enemyDoor) {
        return enemyDoor && fightStyle != Creature.FightStyle.NON_FIGHTER && isBreakable(entityData, door);
    }

    /**
     * Finds the first door to break on the path. The tile we start from is not
     * looked at.
     *
     * @param path the tiles of the path, the first one being where we are
     * @param isDoorToBreak tells if the tile has a door we need to break
     * @return the index of the door tile in the path, or -1 if there is none
     */
    public static int getDoorIndex(List<Point> path, Predicate<Point> isDoorToBreak) {
        for (int i = ADJACENT_DOOR_INDEX; i < path.size(); i++) {
            if (isDoorToBreak.test(path.get(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Cuts the path short, so that it stops before the door
     *
     * @param path the path
     * @param doorIndex the index of the door tile in the path
     * @return the path up to, but not including, the door tile
     */
    public static GraphPath<IMapTileInformation> stopBefore(GraphPath<IMapTileInformation> path, int doorIndex) {
        DefaultGraphPath<IMapTileInformation> shortPath = new DefaultGraphPath<>(doorIndex);
        for (int i = 0; i < doorIndex; i++) {
            shortPath.add(path.get(i));
        }
        return shortPath;
    }

}
