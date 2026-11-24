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
package toniarts.openkeeper.game.task.creature;

import com.jme3.math.Vector2f;
import com.simsilica.es.EntityData;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;
import toniarts.openkeeper.game.component.CombatPitSpectator;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.CombatPitController;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.AbstractTask;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Watches a fight in the Combat Pit, what a creature with nothing else to do
 * does in DK II (DKII.EXE 0x457780, states 0x8E to 0x90). The creature stands
 * on a tile round the pit, faces the middle and cheers or idles. After each
 * animation it steps to a free tile next to it or to a new place on its own
 * tile, and it leaves once no
 * fight was seen for a while. Nobody shares a tile: the one that finds company
 * on its tile moves on.
 */
public final class WatchCombatPit extends AbstractTask {

    /**
     * Out of a hundred, how often a spectator moves to a tile next to it
     * after an animation (0x457D60)
     */
    private static final int MOVE_CHANCE = 36;
    /**
     * Out of a hundred, how often a spectator idles instead of cheering
     * (0x457F40)
     */
    private static final int IDLE_CHANCE = 31;

    private final CombatPitController pit;
    private final short playerId;
    private final EntityData entityData;
    private final IEntityPositionLookup entityPositionLookup;
    private final RandomGenerator random;
    private Point spot;
    private Vector2f place;
    private float animationEnd = -1;
    private boolean over = false;

    /**
     * Watching a fight in the Combat Pit
     *
     * @param navigationService the navigation service
     * @param mapController the map controller
     * @param pit the Combat Pit
     * @param spot the tile round the pit to watch from
     * @param playerId the owner of the pit
     * @param entityData the entity data
     * @param entityPositionLookup the entity lookup, for the other creatures
     * on the tiles round the pit
     * @param random the random number generator
     */
    public WatchCombatPit(final INavigationService navigationService, final IMapController mapController,
            CombatPitController pit, Point spot, short playerId, EntityData entityData,
            IEntityPositionLookup entityPositionLookup, RandomGenerator random) {
        super(navigationService, mapController);

        this.pit = pit;
        this.playerId = playerId;
        this.entityData = entityData;
        this.entityPositionLookup = entityPositionLookup;
        this.random = random;
        standOn(spot);
    }

    @Override
    public boolean isValid(ICreatureController creature) {
        return !over && !pit.isRemoved() && !pit.isDestroyed() && pit.getOwnerId() == playerId;
    }

    @Override
    public Vector2f getTarget(ICreatureController creature) {
        return WorldUtils.pointToVector2f(spot);
    }

    @Override
    public Vector2f getWorkPosition(ICreatureController creature) {
        return place;
    }

    /**
     * The middle of the pit, the spectators face it
     *
     * @return the middle tile of the Combat Pit
     */
    @Override
    public Point getTaskLocation() {
        return pit.getRoomInstance().getCenter();
    }

    @Override
    public boolean isFaceTarget() {
        return true;
    }

    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {

        // Just got to the spot
        if (animationEnd < 0) {
            if (isShared(creature)) {
                if (!moveOn() && !pit.isFightToWatch()) {
                    stop(creature);
                }
                return;
            }

            // Mostly it cheers, once or twice, otherwise it idles
            Creature.AnimationType animation = Creature.AnimationType.IDLE_1;
            int plays = 1;
            if (random.nextInt(100) >= IDLE_CHANCE) {
                animation = Creature.AnimationType.HAPPY;
                plays += random.nextInt(2);
            }
            entityData.setComponent(creature.getEntityId(), new CombatPitSpectator(animation));
            animationEnd = executionDuration + plays * getLength(creature.getCreature().getAnimation(animation));
            return;
        }
        if (executionDuration < animationEnd) {
            return;
        }

        // The animation is over: leave with nothing to watch anymore, otherwise move a tile or shuffle on our own
        animationEnd = -1;
        if (!pit.isFightToWatch()) {
            stop(creature);
        } else if ((!isShared(creature) && random.nextInt(100) >= MOVE_CHANCE) || !moveOn()) {
            standOn(spot);
            creature.navigateToAssignedTask();
        }
    }

    @Override
    public void unassign(ICreatureController creature) {
        super.unassign(creature);

        entityData.removeComponent(creature.getEntityId(), CombatPitSpectator.class);
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.WATCH_COMBAT_PIT;
    }

    private void stop(ICreatureController creature) {
        over = true;
        entityData.removeComponent(creature.getEntityId(), CombatPitSpectator.class);
    }

    private boolean isShared(ICreatureController creature) {
        for (ICreatureController other : entityPositionLookup.getEntityTypesInLocation(spot, ICreatureController.class)) {
            if (!other.getEntityId().equals(creature.getEntityId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Picks a tile round the pit next to ours with no creature on it
     *
     * @return false if there is no such tile, we stay
     */
    private boolean moveOn() {
        List<Point> spectatorTiles = pit.getSpectatorTiles();
        List<Point> free = new ArrayList<>(4);
        for (Point p : new Point[]{new Point(spot.x, spot.y - 1), new Point(spot.x + 1, spot.y),
            new Point(spot.x, spot.y + 1), new Point(spot.x - 1, spot.y)}) {
            if (spectatorTiles.contains(p) && entityPositionLookup.getEntityTypesInLocation(p, ICreatureController.class).isEmpty()) {
                free.add(p);
            }
        }
        if (free.isEmpty()) {
            return false;
        }
        standOn(free.get(random.nextInt(free.size())));
        return true;
    }

    /**
     * Takes the tile, and a random place in the middle third of it to stand
     * on (0x457D60)
     */
    private void standOn(Point tile) {
        spot = tile;
        place = new Vector2f(tile.x + (random.nextFloat() - 0.5f) / 3, tile.y + (random.nextFloat() - 0.5f) / 3);
    }

    /**
     * The length of an animation in seconds, a second for one without timing
     */
    private static float getLength(ArtResource animation) {
        Number frames = animation != null ? animation.getData(ArtResource.KEY_FRAMES) : null;
        Number fps = animation != null ? animation.getData(ArtResource.KEY_FPS) : null;
        return (frames == null || fps == null || fps.intValue() <= 0) ? 1 : frames.floatValue() / fps.floatValue();
    }

}
