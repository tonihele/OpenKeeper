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
import java.util.function.Consumer;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * A thief with stolen gold runs back to where he came into the level. There he
 * leaves the level, and the gold is gone with him.
 *
 * @see StealGold
 */
public final class EscapeWithGold extends AbstractObjectiveTask {

    private final Consumer<ICreatureController> escapeListener;
    private boolean executed = false;

    /**
     * Sends the thief off with his loot
     *
     * @param navigationService the navigation
     * @param mapController the map
     * @param exit where the thief came into the level
     * @param playerId the owner of the thief
     * @param escapeListener told when the thief gets away, to take him out of
     * the level
     */
    public EscapeWithGold(final INavigationService navigationService, final IMapController mapController, Point exit,
            short playerId, Consumer<ICreatureController> escapeListener) {
        super(navigationService, mapController, exit, playerId);

        this.escapeListener = escapeListener;
    }

    @Override
    public boolean isValid(ICreatureController creature) {
        if (executed) {
            return false;
        }

        return creature == null || Thing.HeroParty.Objective.STEAL_GOLD.equals(creature.getObjective());
    }

    @Override
    public Vector2f getTarget(ICreatureController creature) {
        return WorldUtils.pointToVector2f(getTaskLocation());
    }

    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {
        executed = true;
        creature.substractGold(creature.getGold());
        escapeListener.accept(creature);
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.GO_TO_LOCATION;
    }

}
