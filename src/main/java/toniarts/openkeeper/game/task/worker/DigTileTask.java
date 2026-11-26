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
package toniarts.openkeeper.game.task.worker;

import com.jme3.math.Vector2f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.controller.GameWorldController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.TileWork;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.AbstractTileTask;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Dig a tile task, for workers
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class DigTileTask extends AbstractTileTask {

    /**
     * The spots to dig from along each open side of the tile, DK II's spot
     * search (0x469AF0)
     */
    private static final int SPOTS_PER_SIDE = 3;

    private final EntityData entityData;
    private final IEntityPositionLookup entityPositionLookup;
    private final int maxGoldPile;

    public DigTileTask(final INavigationService navigationService, final IMapController mapController, Point p, short playerId) {
        this(navigationService, mapController, p, playerId, null, null, 0);
    }

    /**
     * A worker's dig task that minds the gold piling up at the diggers' feet,
     * like DK II's mining (0x465870)
     *
     * @param navigationService the navigation service
     * @param mapController the map controller
     * @param p the tile to dig
     * @param playerId the player that wants it dug
     * @param entityData the entity data
     * @param entityPositionLookup the entities by tile
     * @param maxGoldPile the level's MAX_GOLD_PILE_OUTSIDE_TREASURY
     */
    public DigTileTask(final INavigationService navigationService, final IMapController mapController, Point p, short playerId,
            EntityData entityData, IEntityPositionLookup entityPositionLookup, int maxGoldPile) {
        super(navigationService, mapController, p, playerId);

        this.entityData = entityData;
        this.entityPositionLookup = entityPositionLookup;
        this.maxGoldPile = maxGoldPile;
    }

    @Override
    public int getMaxAllowedNumberOfAsignees() {
        // Three for each side of the tile that is open
        int openSides = 0;
        for (Point p : WorldUtils.getSurroundingTiles(mapController.getMapData(), getTaskLocation(), false)) {
            if (!p.equals(getTaskLocation()) && !mapController.isSolid(p)) {
                openSides++;
            }
        }
        return SPOTS_PER_SIDE * openSides;
    }

    @Override
    public Vector2f getTarget(ICreatureController creature) {
        return getAccessibleTargetNextToLocation(creature);
    }

    @Override
    public boolean isReachable(ICreatureController creature) {
        return (getTarget(creature) != null); // To avoid multiple path finds
    }

    /**
     * Like DK II's search for a spot to dig from (0x469AF0), a side whose
     * spots all hold a full loose gold pile is skipped
     */
    @Override
    protected boolean isWorkSpot(Point p) {
        return !isFullGoldPile(p);
    }

    /**
     * Whether the loose gold on a side leaves no spot to dig from. In DK II the
     * gold a digger can't carry piles up at its feet (0x4510C0), a pile for
     * each of the spots along the side, and a spot is full when its pile holds
     * more than the level's biggest pile outside a Treasury (0x469AF0,
     * 0x465870). A digger on a full spot moves to another spot of the side.
     * OpenKeeper keeps one pile a tile, so the side is full once that pile
     * holds more than all its spots may.
     */
    private boolean isFullGoldPile(Point p) {
        if (entityData == null) {
            return false;
        }
        EntityId pile = GameWorldController.findLooseGoldPile(entityData, entityPositionLookup, p);
        return pile != null && entityData.getComponent(pile, Gold.class).gold > SPOTS_PER_SIDE * maxGoldPile;
    }

    @Override
    public boolean isValid(ICreatureController creature) {
        return (isTileToDig() || getExecutionDuration(creature) > 0) && (creature == null || hasWorkSpot());
    }

    /**
     * Whether any open side is left to dig from. Once the gold has piled up
     * over the limit on every side, there is no spot for DK II's search
     * (0x469AF0) to find, and a digger on its way or still swinging gives the
     * task up and looks for other work, as it does on a full pile (0x465870).
     * Otherwise it would wait for a spot to dig from forever.
     *
     * @return true if there is a side to dig from
     */
    private boolean hasWorkSpot() {
        for (Point p : WorldUtils.getSurroundingTiles(mapController.getMapData(), getTaskLocation(), false)) {
            if (!p.equals(getTaskLocation()) && !mapController.isSolid(p) && isWorkSpot(p)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether the tile still stands to be dug. A digger that has started
     * swinging keeps the task after the tile gives way, until its swing has
     * played out.
     *
     * @return true if the tile is still there to dig
     */
    protected boolean isTileToDig() {
        IMapTileInformation tile = mapController.getMapData().getTile(getTaskLocation());
        return tile != null && tile.isSelected(playerId)
                && TileWork.canDamageTile(mapController.getTerrain(tile), tile.getOwnerId(), playerId);
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public String toString() {
        return "Dig tile at " + getTaskLocation();
    }

    /**
     * Digs in swings, as DK II's dig action does (0x496AA0, 0x496E00). Each
     * swing hits the tile once, at its start, and then plays out. The first
     * swing also plays the start of the animation. The swing that brings the
     * tile down also plays the end of the animation, and only then does the
     * digger move on. The stored execution duration is the time on the task
     * when the current swing ends, 0 before the first swing and once the
     * digger is done.
     *
     * @param creature the digger
     * @param executionDuration the time the digger has spent on the task
     */
    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {
        float swingEnd = getExecutionDuration(creature);
        if (executionDuration < swingEnd) {
            return;
        }
        if (!isTileToDig()) {
            setExecutionDuration(creature, 0);
            return;
        }

        // Like DK II (0x465870), before each swing the digger looks at the loose gold at its feet: the gold it can't
        // carry piles up there. When every spot of its side is full, it stops and looks for other work
        if (isFullGoldPile(creature.getCreatureCoordinates())) {
            creature.unassingCurrentTask();
            return;
        }

        creature.addGold(mapController.damageTile(getTaskLocation(), playerId, creature));
        setExecutionDuration(creature, executionDuration + getSwingDuration(creature.getCreature(), swingEnd == 0, !isTileToDig()));
    }

    /**
     * The length of one dig swing in seconds. DK II's dig action plays the
     * creature's melee animation (the imp's is called Imp_Dig), frames / fps.
     *
     * @param creature the digger
     * @param first whether this is the first swing, which plays the start of
     * the animation
     * @param last whether this swing brought the tile down and plays the end of
     * the animation
     * @return the swing length in seconds
     */
    private static float getSwingDuration(Creature creature, boolean first, boolean last) {
        ArtResource animation = creature.getAnimation(Creature.AnimationType.MELEE_ATTACK);
        Number fps = animation != null ? animation.getData(ArtResource.KEY_FPS) : null;
        if (fps == null || fps.intValue() <= 0) {
            return 1f;
        }
        int frames = animation.<Number>getData(ArtResource.KEY_FRAMES).intValue();
        if (first && animation.getFlags().contains(ArtResource.ArtResourceFlag.HAS_START_ANIMATION)) {
            frames += animation.<Number>getData(ArtResource.KEY_START_AF).intValue();
        }
        if (last && animation.getFlags().contains(ArtResource.ArtResourceFlag.HAS_END_ANIMATION)) {
            frames += animation.<Number>getData(ArtResource.KEY_END_AF).intValue();
        }
        return (float) frames / fps.intValue();
    }

    @Override
    public boolean isFaceTarget() {
        return true;
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.DIG_TILE;
    }

}
