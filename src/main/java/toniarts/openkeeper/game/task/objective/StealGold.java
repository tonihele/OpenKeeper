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
import com.simsilica.es.EntityData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import toniarts.openkeeper.game.component.StolenGold;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.storage.IRoomObjectControl;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.Task;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Steal gold objective for the thieving heroes. The thief first goes to a gold
 * pile in the target keeper's Treasury and takes as much as he can carry. The
 * keeper loses all of it, but the thief only gets away with half. That tags him
 * with {@link StolenGold}, and he runs back to where he came into the level,
 * where he is gone with the gold. A thief that has the tag, in a loaded game
 * too, just runs back.
 */
public final class StealGold extends AbstractObjectiveTask {

    private final IGameWorldController gameWorldController;
    private final EntityData entityData;
    private final short targetPlayerId;
    private final Consumer<ICreatureController> escapeListener;
    private final boolean somethingToDo;
    private boolean executed = false;

    /**
     * Sends the thief after the gold, or back to the exit if he already has it
     *
     * @param navigationService the navigation
     * @param mapController the map
     * @param gameWorldController takes the gold from the keeper
     * @param entityData the entities, the thief with the gold is tagged there
     * @param thief the thief
     * @param escapeListener told when the thief gets away, to take him out of
     * the level
     */
    public StealGold(final INavigationService navigationService, final IMapController mapController,
            final IGameWorldController gameWorldController, final EntityData entityData, ICreatureController thief,
            Consumer<ICreatureController> escapeListener) {
        super(navigationService, mapController, getExit(entityData, thief), thief.getOwnerId());

        this.gameWorldController = gameWorldController;
        this.entityData = entityData;
        this.targetPlayerId = thief.getObjectiveTargetPlayerId();
        this.escapeListener = escapeListener;
        this.somethingToDo = hasStolenGold(entityData, thief) || createStealTask(thief);
    }

    private static Point getExit(EntityData entityData, ICreatureController thief) {
        StolenGold stolenGold = entityData.getComponent(thief.getEntityId(), StolenGold.class);
        return stolenGold != null ? stolenGold.exit : thief.getCreatureCoordinates();
    }

    private static boolean hasStolenGold(EntityData entityData, ICreatureController thief) {
        return entityData.getComponent(thief.getEntityId(), StolenGold.class) != null;
    }

    /**
     * Adds the trip to the gold as the first step
     *
     * @return false if there is no gold to steal
     */
    private boolean createStealTask(ICreatureController thief) {
        if (targetPlayerId == thief.getOwnerId()) {
            return false;
        }
        Point goldTile = findGoldTile(thief, mapController.getRoomsByFunction(ObjectType.GOLD, targetPlayerId), navigationService);
        if (goldTile == null) {
            return false;
        }

        addSubTask(new ObjectiveTaskDecorator(getId(), new GoToTask(navigationService, mapController, goldTile, playerId)) {

            @Override
            public boolean isValid(ICreatureController creature) {
                return super.isValid(creature) && StealGold.this.isValid(creature);
            }

            @Override
            public void executeTask(ICreatureController creature, float executionDuration) {
                steal(creature, goldTile);
                super.executeTask(creature, executionDuration);
            }

        });
        return true;
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

    /**
     * The thief asks for all he can carry, the keeper loses all he gets but he
     * can only hold on to half of it
     */
    private void steal(ICreatureController thief, Point goldTile) {
        int wanted = thief.getMaxGold() - thief.getGold();
        int taken = wanted - gameWorldController.substractGold(wanted, targetPlayerId, goldTile);
        thief.addGold(taken / 2);
        entityData.setComponent(thief.getEntityId(), new StolenGold(getTaskLocation()));
    }

    @Override
    public boolean isValid(ICreatureController creature) {
        if (executed) {
            return false;
        }
        if (creature == null) {
            return true;
        }

        // Check that the objective is still the same, with the gold the keeper no longer matters
        return Thing.HeroParty.Objective.STEAL_GOLD.equals(creature.getObjective())
                && (hasStolenGold(entityData, creature) || targetPlayerId == creature.getObjectiveTargetPlayerId());
    }

    @Override
    public Vector2f getTarget(ICreatureController creature) {
        return WorldUtils.pointToVector2f(getTaskLocation());
    }

    /**
     * Back at the exit the thief is gone, and the gold with him
     */
    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {
        executed = true;
        creature.substractGold(creature.getGold());
        escapeListener.accept(creature);
    }

    /**
     * With no gold to steal there is nothing the thief can do for his objective
     * yet. He doesn't take the task, and tries again the next time he looks for
     * something to do
     *
     * @return the current task, null when there is nothing to steal
     */
    @Override
    public Task getCurrentTask() {
        return somethingToDo ? super.getCurrentTask() : null;
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.GO_TO_LOCATION;
    }

}
