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
import com.simsilica.es.filter.FieldFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.game.component.AttackTarget;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.room.FiveByFiveRotatedController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.tools.convert.map.GameLevel;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.utils.Point;

/**
 * Watches the Dungeon Hearts. A keeper whose heart is destroyed is defeated:
 * he is marked destroyed, loses the game and his heart room is left in ruins.
 * In a skirmish or multiplayer game the last keeper standing wins. In the
 * campaign, a level can have the last enemy heart leave a Portal Gem.
 */
public final class KeeperDefeatSystem implements IGameLogicUpdatable {

    private static final short OBJECT_PORTAL_GEM_ID = 28;

    private final EntityData entityData;
    private final ILevelInfo levelInfo;
    private final IGameController gameController;
    private final IMapController mapController;
    private final IObjectsController objectsController;
    private final boolean lastKeeperWins;
    private final EntitySet hearts;
    private final Map<EntityId, Short> heartOwners = new HashMap<>();

    /**
     * Watches the hearts
     *
     * @param entityData the entity data
     * @param levelInfo the level
     * @param gameController ends the game for the keepers
     * @param mapController the map, for the heart rooms
     * @param objectsController creates the Portal Gem
     * @param lastKeeperWins whether the last keeper standing wins the game,
     * as in a skirmish or multiplayer game
     */
    public KeeperDefeatSystem(EntityData entityData, ILevelInfo levelInfo, IGameController gameController,
            IMapController mapController, IObjectsController objectsController, boolean lastKeeperWins) {
        this.entityData = entityData;
        this.levelInfo = levelInfo;
        this.gameController = gameController;
        this.mapController = mapController;
        this.objectsController = objectsController;
        this.lastKeeperWins = lastKeeperWins;

        hearts = entityData.getEntities(new FieldFilter(ObjectComponent.class, "objectId", FiveByFiveRotatedController.OBJECT_HEART_ID),
                ObjectComponent.class, Owner.class, Health.class);
        processAddedHearts(hearts);
    }

    @Override
    public void processTick(float tpf) {
        if (!hearts.applyChanges()) {
            return;
        }

        processAddedHearts(hearts.getAddedEntities());
        processChangedHearts(hearts.getChangedEntities());

        // A removed entity has no components left, so we remember the owners
        for (Entity heart : hearts.getRemovedEntities()) {
            heartDestroyed(heart.getId());
        }
    }

    private void processAddedHearts(Set<Entity> addedHearts) {
        for (Entity heart : addedHearts) {
            heartOwners.put(heart.getId(), heart.get(Owner.class).ownerId);
        }
    }

    private void processChangedHearts(Set<Entity> changedHearts) {
        for (Entity heart : changedHearts) {
            if (heart.get(Health.class).health <= 0) {
                heartDestroyed(heart.getId());
            }
        }
    }

    private void heartDestroyed(EntityId heart) {
        Short ownerId = heartOwners.remove(heart);
        Keeper keeper = ownerId != null ? levelInfo.getPlayer(ownerId) : null;
        if (keeper == null || keeper.isDestroyed()) {
            return;
        }

        keeper.setDestroyed(getKiller(heart, keeper.getId()));
        ruinHeartRoom(keeper);
        if (!isAi(keeper)) {
            gameController.endGame(keeper.getId(), false);
        }
        if (lastKeeperWins) {
            checkLastKeeperStanding();
        } else {
            checkPortalGem(keeper);
        }
    }

    /**
     * The keeper is killed by whoever was hitting the heart
     *
     * @param heart the heart
     * @param keeperId the owner of the heart
     * @return the player who destroyed the heart, 0 if not known
     */
    private short getKiller(EntityId heart, short keeperId) {
        for (EntityId attacker : entityData.findEntities(new FieldFilter(AttackTarget.class, "entityId", heart), AttackTarget.class, Owner.class)) {
            short attackerOwner = entityData.getComponent(attacker, Owner.class).ownerId;
            if (attackerOwner != keeperId) {
                return attackerOwner;
            }
        }

        return 0;
    }

    private void ruinHeartRoom(Keeper keeper) {
        if (keeper.getDungeonHeartLocation() == null) {
            return;
        }

        IRoomController room = mapController.getRoomControllerByCoordinates(keeper.getDungeonHeartLocation());
        if (room != null && room.isDungeonHeart()) {
            room.destroy();
        }
    }

    private void checkLastKeeperStanding() {
        List<Keeper> standing = getKeepersStanding();
        if (standing.size() == 1 && !isAi(standing.getFirst())) {
            gameController.endGame(standing.getFirst().getId(), true);
        }
    }

    /**
     * When the level says so, the heart of the last enemy keeper leaves a
     * Portal Gem behind for the heroes
     *
     * @param keeper the keeper who was just destroyed
     */
    private void checkPortalGem(Keeper keeper) {
        if (!isHeartMakingGem(keeper) || getKeepersStanding().size() > 1) {
            return;
        }

        Point heartTile = keeper.getDungeonHeartLocation();
        objectsController.loadObject(OBJECT_PORTAL_GEM_ID, Player.GOOD_PLAYER_ID, heartTile.x, heartTile.y);
    }

    private boolean isHeartMakingGem(Keeper keeper) {
        return levelInfo.getLevelData().getGameLevel().getLvlFlags().contains(GameLevel.LevFlag.HEART_MAKES_GEM)
                && isAi(keeper) && keeper.getDungeonHeartLocation() != null;
    }

    private boolean isAi(Keeper keeper) {
        // Skirmish and multiplayer players come from the lobby, the map's player record only tells the campaign
        if (lastKeeperWins) {
            return keeper.isAi();
        }
        return keeper.getPlayer() != null && keeper.getPlayer().isAi();
    }

    private List<Keeper> getKeepersStanding() {
        List<Keeper> standing = new ArrayList<>();
        for (Keeper keeper : levelInfo.getPlayers().values()) {
            if (keeper.getId() >= Player.KEEPER1_ID && !keeper.isDestroyed()) {
                standing.add(keeper);
            }
        }

        return standing;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        hearts.release();
        heartOwners.clear();
    }

}
