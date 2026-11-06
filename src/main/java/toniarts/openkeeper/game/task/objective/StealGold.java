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
package toniarts.openkeeper.game.task.objective;

import com.jme3.math.Vector2f;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.storage.IRoomObjectControl;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Steal gold objective for the thieving heroes. The thief goes to a gold pile
 * in the target keeper's Treasury and takes as much as he can carry. The keeper
 * loses all of it, but the thief only gets away with half.
 *
 * @see EscapeWithGold
 */
public final class StealGold extends AbstractObjectiveTask {

    private final IGameWorldController gameWorldController;
    private final short targetPlayerId;
    private final Consumer<ICreatureController> lootListener;
    private boolean executed = false;

    /**
     * Sends the thief after the gold
     *
     * @param navigationService the navigation
     * @param mapController the map
     * @param gameWorldController takes the gold from the keeper
     * @param goldTile the gold pile the thief goes for
     * @param targetPlayerId the keeper to steal from
     * @param playerId the owner of the thief
     * @param lootListener told when the thief has taken the gold, he then
     * makes his escape
     */
    public StealGold(final INavigationService navigationService, final IMapController mapController,
            final IGameWorldController gameWorldController, Point goldTile, short targetPlayerId, short playerId,
            Consumer<ICreatureController> lootListener) {
        super(navigationService, mapController, goldTile, playerId);

        this.gameWorldController = gameWorldController;
        this.targetPlayerId = targetPlayerId;
        this.lootListener = lootListener;
    }

    /**
     * Finds the nearest gold pile in the keeper's treasuries that the thief can
     * get to. The gold in the Dungeon Heart is left alone
     *
     * @param thief the thief
     * @param treasuries the rooms where the keeper keeps his gold
     * @param navigationService the navigation
     * @return the tile of the gold pile, or {@code null} if there is none
     */
    public static Point findGoldTile(ICreatureController thief, Collection<IRoomController> treasuries,
            final INavigationService navigationService) {
        Point thiefLocation = thief.getCreatureCoordinates();
        List<Point> goldTiles = getGoldTiles(treasuries);
        goldTiles.sort(Comparator.comparingDouble(tile -> tile.distance(thiefLocation)));

        for (Point tile : goldTiles) {
            if (navigationService.findPath(thiefLocation, tile, thief.getParty() != null ? thief.getParty() : thief) != null) {
                return tile;
            }
        }

        return null;
    }

    private static List<Point> getGoldTiles(Collection<IRoomController> treasuries) {
        List<Point> goldTiles = new ArrayList<>();
        for (IRoomController room : treasuries) {
            if (room.isDungeonHeart()) {
                continue;
            }
            IRoomObjectControl<?> goldControl = room.getObjectControl(ObjectType.GOLD);
            for (Point tile : room.getRoomInstance().getCoordinates()) {
                Collection<?> goldPiles = goldControl.getItems(tile);
                if (goldPiles != null && !goldPiles.isEmpty()) {
                    goldTiles.add(tile);
                }
            }
        }

        return goldTiles;
    }

    @Override
    public boolean isValid(ICreatureController creature) {
        if (executed) {
            return false;
        }

        // Check that the objectives are still the same
        return creature == null || (Thing.HeroParty.Objective.STEAL_GOLD.equals(creature.getObjective())
                && targetPlayerId == creature.getObjectiveTargetPlayerId());
    }

    @Override
    public Vector2f getTarget(ICreatureController creature) {
        return WorldUtils.pointToVector2f(getTaskLocation());
    }

    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {
        executed = true;

        // The thief asks for all he can carry, the keeper loses all he gets
        // but he can only hold on to half of it
        int wanted = creature.getMaxGold() - creature.getGold();
        int taken = wanted - gameWorldController.substractGold(wanted, targetPlayerId, getTaskLocation());
        creature.addGold(taken / 2);
        lootListener.accept(creature);
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.GO_TO_LOCATION;
    }

}
