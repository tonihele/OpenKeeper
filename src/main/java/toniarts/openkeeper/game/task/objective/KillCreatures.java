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
import java.util.function.Supplier;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Kill creatures objective for the goodly heroes. The hero goes after one of
 * the target keeper's creatures, the fighting starts when he meets his enemies.
 * Once he gets to his prey he walks to where a random creature of the keeper
 * stands then, again and again, and to the keeper's Dungeon Heart when that
 * creature can't be reached (DKII.EXE 0x462310, 0x462560). When his prey is
 * knocked out he picks a new one. When the keeper has no creatures left, the
 * hero goes for the keeper's Dungeon Heart.
 */
public final class KillCreatures extends AbstractObjectiveTask {

    private final short targetPlayerId;
    private final ICreatureController prey;
    private final Supplier<Collection<ICreatureController>> keeperCreatures;
    private final ILevelInfo levelInfo;
    private Point spot;
    private boolean toHeart = false;
    private boolean executed = false;

    private KillCreatures(final INavigationService navigationService, final IMapController mapController, ILevelInfo levelInfo,
            short targetPlayerId, ICreatureController prey, Supplier<Collection<ICreatureController>> keeperCreatures, short playerId) {
        super(navigationService, mapController, prey.getCreatureCoordinates(), playerId);

        this.levelInfo = levelInfo;
        this.targetPlayerId = targetPlayerId;
        this.prey = prey;
        this.keeperCreatures = keeperCreatures;
    }

    /**
     * Creates the task for a hero told to kill the creatures of a keeper. He
     * goes after the nearest creature he can get to, or for the keeper's heart
     * if the keeper has no creatures left
     *
     * @param navigationService the navigation
     * @param mapController the map
     * @param levelInfo the level
     * @param hero the hero
     * @param keeperCreatures the creatures of the keeper the hero is after
     * @return the task, or {@code null} if the hero has nothing to go after
     */
    public static AbstractObjectiveTask create(final INavigationService navigationService, final IMapController mapController,
            final ILevelInfo levelInfo, ICreatureController hero, Supplier<Collection<ICreatureController>> keeperCreatures) {
        short targetPlayerId = hero.getObjectiveTargetPlayerId();
        if (keeperCreatures.get().isEmpty()) {
            return isHeartToAttack(levelInfo.getPlayer(targetPlayerId))
                    ? new KillPlayer(navigationService, mapController, levelInfo, targetPlayerId, hero) : null;
        }

        ICreatureController prey = findPrey(navigationService, hero, keeperCreatures.get());
        return prey != null ? new KillCreatures(navigationService, mapController, levelInfo, targetPlayerId, prey, keeperCreatures,
                hero.getOwnerId()) : null;
    }

    private static boolean isHeartToAttack(Keeper keeper) {
        return keeper != null && !keeper.isDestroyed() && keeper.getDungeonHeartLocation() != null;
    }

    private static ICreatureController findPrey(final INavigationService navigationService, ICreatureController hero,
            Collection<ICreatureController> keeperCreatures) {
        Point heroLocation = hero.getCreatureCoordinates();
        List<ICreatureController> candidates = new ArrayList<>();
        for (ICreatureController creature : keeperCreatures) {
            if (isAlive(creature)) {
                candidates.add(creature);
            }
        }
        candidates.sort(Comparator.comparingDouble(creature -> creature.getCreatureCoordinates().distance(heroLocation)));

        for (ICreatureController creature : candidates) {
            if (navigationService.findPath(heroLocation, creature.getCreatureCoordinates(),
                    hero.getParty() != null ? hero.getParty() : hero) != null) {
                return creature;
            }
        }

        return null;
    }

    private static boolean isAlive(ICreatureController creature) {
        return !creature.isRemoved() && !creature.isIncapacitated();
    }

    @Override
    public boolean isValid(ICreatureController creature) {
        if (executed || (spot == null && (!isAlive(prey) || prey.getOwnerId() != targetPlayerId))) {
            return false;
        }

        // Check that the objectives are still the same
        return creature == null || (Thing.HeroParty.Objective.KILL_CREATURES.equals(creature.getObjective())
                && targetPlayerId == creature.getObjectiveTargetPlayerId());
    }

    @Override
    public Vector2f getTarget(ICreatureController creature) {
        return WorldUtils.pointToVector2f(spot != null ? spot : prey.getCreatureCoordinates());
    }

    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {
        if (toHeart) {
            executed = true;
            return;
        }

        // We got there: walk to where a random creature of the keeper stands now, or to the keeper's heart if the
        // keeper has none or it can't be reached (0x462560)
        Point from = creature.getCreatureCoordinates();
        List<ICreatureController> creatures = new ArrayList<>(keeperCreatures.get());
        creatures.removeIf(c -> !isAlive(c));
        if (!creatures.isEmpty()) {
            Point target = Utils.getRandomItem(creatures).getCreatureCoordinates();
            if (navigationService.findPath(from, target, creature.getParty() != null ? creature.getParty() : creature) != null) {
                spot = target;
                return;
            }
        }
        Keeper keeper = levelInfo.getPlayer(targetPlayerId);
        if (isHeartToAttack(keeper)) {
            spot = keeper.getDungeonHeartLocation();
            toHeart = true;
        } else {
            executed = true;
        }
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.GO_TO_LOCATION;
    }

}
