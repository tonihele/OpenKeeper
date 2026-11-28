/*
 * Copyright (C) 2014-2015 OpenKeeper
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
package toniarts.openkeeper.game.controller.room;

import com.jme3.math.FastMath;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.filter.FieldFilter;
import java.util.ArrayList;
import java.util.List;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.CombatPitFight;
import toniarts.openkeeper.game.component.CombatPitFighter;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.InHand;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.tools.convert.map.IKwdFile;

/**
 * Manages combat pit
 *
 * @author ArchDemon
 */
public final class CombatPitController extends DoubleQuadController {

    /**
     * How long the creatures watching the pit stay after the last fight they
     * saw, in seconds (DKII.EXE 0x4580D0)
     */
    private static final int WATCH_SECONDS_AFTER_FIGHT = 4;

    private final IGameTimer gameTimer;
    private Point door;

    public CombatPitController(EntityId entityId, EntityData entityData, IKwdFile kwdFile,
            RoomInstance roomInstance, IObjectsController objectsController, IGameTimer gameTimer) {
        super(entityId, entityData, kwdFile, roomInstance, objectsController);

        this.gameTimer = gameTimer;
    }

    @Override
    public void construct() {
        super.construct();

        // Store the door position for spawning the creatures in
        door = null;
        for (Point p : roomInstance.getCoordinates()) {
            if (door == null && insideCoordinates.contains(new Point(p.x, p.y + 1))) {
                door = p;
                break;
            }
        }
    }

    @Override
    public boolean isTileAccessible(Integer fromX, Integer fromY, int toX, int toY) {

        // The pit's door is the way in and out, for the imps that carry the knocked out fighters off too
        if (door != null && fromX != null && fromY != null) {
            Point from = new Point(fromX, fromY);
            Point to = new Point(toX, toY);
            if ((insideCoordinates.contains(from) && to.equals(door)) || (insideCoordinates.contains(to) && from.equals(door))) {
                return true;
            }
        }
        return super.isTileAccessible(fromX, fromY, toX, toY);
    }

    /**
     * Is the tile in the pit itself, where the creatures fight, rather than on
     * its edge
     *
     * @param tile the tile
     * @return true if the creatures fight on the tile
     */
    public boolean isFightingTile(Point tile) {
        return insideCoordinates.contains(tile);
    }

    /**
     * Picks a tile in the pit itself, where a fighter without an opponent
     * walks to wait for one
     *
     * @return a random tile the creatures fight on
     */
    public Point getRandomFightingTile() {
        return insideCoordinates.stream().skip(FastMath.nextRandomInt(0, insideCoordinates.size() - 1)).findFirst().orElseThrow();
    }

    /**
     * The tiles round the pit itself, where the creatures that watch a fight
     * stand
     *
     * @return the tiles of the room the creatures don't fight on
     */
    public List<Point> getSpectatorTiles() {
        List<Point> tiles = new ArrayList<>(roomInstance.getCoordinates());
        tiles.removeAll(insideCoordinates);
        return tiles;
    }

    /**
     * Is there a fighter on its feet in the pit. Like DK II (0x4D7CF0), the
     * pit remembers when it was last asked and had one
     *
     * @return true if a fight is on
     */
    public boolean isFightOn() {
        for (EntityId fighter : entityData.findEntities(new FieldFilter<>(CombatPitFighter.class, "pit", getEntityId()), CombatPitFighter.class)) {
            if (entityData.getComponent(fighter, Unconscious.class) == null && entityData.getComponent(fighter, InHand.class) == null) {
                entityData.setComponent(getEntityId(), new CombatPitFight(gameTimer.getGameTime()));
                return true;
            }
        }
        return false;
    }

    /**
     * Is there something to watch: a fight, or one that was seen no more than
     * 4 seconds ago
     *
     * @return true if the creatures watching the pit stay
     */
    public boolean isFightToWatch() {
        if (isFightOn()) {
            return true;
        }
        CombatPitFight fight = entityData.getComponent(getEntityId(), CombatPitFight.class);
        return fight != null && gameTimer.getGameTime() - fight.lastFightTime <= WATCH_SECONDS_AFTER_FIGHT;
    }

    /**
     * Takes a creature dropped on the tile as a fighter. Like in DK II, any
     * creature dropped into the pit itself fights there, also another
     * keeper's, such as a prisoner. While it fights it is held by the pit's
     * keeper, who can pick it up again. Workers don't fight, they just go
     * back to work
     *
     * @param creature the dropped creature
     * @param tile the tile it was dropped on
     * @return true if the creature fights here
     */
    public boolean addFighter(EntityId creature, Point tile) {
        if (!isFightingTile(tile) || entityData.getComponent(creature, CreatureComponent.class).worker) {
            return false;
        }
        Owner owner = entityData.getComponent(creature, Owner.class);
        entityData.setComponents(creature, new CombatPitFighter(getEntityId()), new Owner(owner.ownerId, roomInstance.getOwnerId()));
        return true;
    }

    @Override
    public void remove() {
        releaseFighters();
        super.remove();
    }

    @Override
    public void captured(short playerId) {
        releaseFighters();
        super.captured(playerId);
    }

    /**
     * Ends the fight for all the fighters here. The ones held here are back
     * in the control of their owner, so they walk free
     */
    private void releaseFighters() {
        for (EntityId fighter : entityData.findEntities(new FieldFilter<>(CombatPitFighter.class, "pit", getEntityId()), CombatPitFighter.class)) {
            entityData.removeComponent(fighter, CombatPitFighter.class);
            Owner owner = entityData.getComponent(fighter, Owner.class);
            entityData.setComponent(fighter, new Owner(owner.ownerId, owner.ownerId));
        }
    }

}
