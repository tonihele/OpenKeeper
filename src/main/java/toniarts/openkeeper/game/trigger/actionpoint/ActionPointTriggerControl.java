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
package toniarts.openkeeper.game.trigger.actionpoint;


import java.util.function.Predicate;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.data.ActionPoint;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.state.session.PlayerService;
import toniarts.openkeeper.game.trigger.TriggerControl;
import toniarts.openkeeper.game.trigger.TriggerGenericData;

/**
 *
 * @author ArchDemon
 */
public final class ActionPointTriggerControl extends TriggerControl {

    private final ActionPoint ap;
    private final IEntityPositionLookup entityPositionLookup;

    public ActionPointTriggerControl(final IGameController gameController, final ILevelInfo levelInfo, final IGameTimer gameTimer,
            final IMapController mapController, final ICreaturesController creaturesController, int triggerId, ActionPoint ap,
            final PlayerService playerService, final IEntityPositionLookup entityPositionLookup) {
        super(gameController, levelInfo, gameTimer, mapController, creaturesController, playerService, triggerId);
        this.ap = ap;
        this.entityPositionLookup = entityPositionLookup;
    }

    @Override
    protected boolean isActive(TriggerGenericData trigger) {
        switch (trigger.getType()) {
            case AP_CONGREGATE_IN:
                return isCongregateActive(trigger);

            case AP_POSESSED_CREATURE_ENTERS:
                // STUB: a possessed creature (or object, kind 6) of the given player and type is on the
                // AP. Whether a creature is possessed is a component of the entity (Possessed), and
                // this scope has no access to the entity data. Nothing in it either tells that the
                // creature just entered
                logStub(trigger, "Whether a creature is possessed is a component of the entity (Possessed), and this scope has no access to the entity data. Nothing in it either tells that the creature just entered");
                return false;

            case AP_CLAIM_PART_OF: {
                short playerId = trigger.getUserData("playerId", short.class);
                return compare(trigger, countTiles(tile -> playerId == tile.getOwnerId()),
                        trigger.getUserData("value", int.class));
            }

            case AP_CLAIM_ALL_OF: {
                short playerId = trigger.getUserData("playerId", short.class);
                return allTiles(tile -> playerId == tile.getOwnerId());
            }

            case AP_SLAB_TYPES: {
                short playerId = trigger.getUserData("playerId", short.class);
                short terrainId = trigger.getUserData("terrainId", short.class);
                return compare(trigger,
                        countTiles(tile -> (playerId == 0 || playerId == tile.getOwnerId()) && terrainId == tile.getTerrainId()),
                        trigger.getUserData("value", int.class));
            }

            case AP_TAG_PART_OF: {
                short playerId = trigger.getUserData("playerId", short.class);
                // TODO check who tagged tile
                return compare(trigger, countTiles(tile -> tile.isSelected(playerId)),
                        trigger.getUserData("value", int.class));
            }

            case AP_TAG_ALL_OF: {
                short playerId = trigger.getUserData("playerId", short.class);
                // TODO check who tagged tile
                return allTiles(tile -> tile.isSelected(playerId));
            }

            default:
                return super.isActive(trigger);
        }
    }

    private boolean isCongregateActive(TriggerGenericData trigger) {
        short playerId = trigger.getUserData("playerId", short.class);
        short targetId = trigger.getUserData("targetId", short.class);
        int value = trigger.getUserData("value", int.class);
        short type = trigger.getUserData("targetType", short.class);
        switch (type) {
            case 0:
            case 3: // Creature
                int target = 0;
                for (int x = (int) ap.getStart().x; x <= (int) ap.getEnd().x; x++) {
                    for (int y = (int) ap.getStart().y; y <= (int) ap.getEnd().y; y++) {
                        for (ICreatureController creature : entityPositionLookup.getEntityTypesInLocation(x, y, ICreatureController.class)) {
                            if ((playerId == 0 || creature.getOwnerId() == playerId) && (targetId == 0 || creature.getCreature().getCreatureId() == targetId)) {
                                target++;
                            }
                        }
                    }
                }
                return compare(trigger, target, value);
            case 6: // Object
                // STUB: count the objects (by object type, e.g. a chicken) on the AP. The
                // object controllers do not know their object type ID, only the room
                // function they serve, so they can not be filtered by the ID in the map
                logStub(trigger, "Objects on the AP can not be counted: the object controllers do not know their object type ID, only the room function they serve, so they can not be filtered by the ID in the map");
                return false;
            default:
                logStub(trigger, "unknown targetType " + type);
                return false;
        }
    }

    private int countTiles(Predicate<IMapTileInformation> filter) {
        IMapDataInformation map = mapController.getMapData();
        int count = 0;
        for (int x = (int) ap.getStart().x; x <= (int) ap.getEnd().x; x++) {
            for (int y = (int) ap.getStart().y; y <= (int) ap.getEnd().y; y++) {
                if (filter.test(map.getTile(x, y))) {
                    count++;
                }
            }
        }
        return count;
    }

    private boolean allTiles(Predicate<IMapTileInformation> filter) {
        IMapDataInformation map = mapController.getMapData();
        for (int x = (int) ap.getStart().x; x <= (int) ap.getEnd().x; x++) {
            for (int y = (int) ap.getStart().y; y <= (int) ap.getEnd().y; y++) {
                if (!filter.test(map.getTile(x, y))) {
                    return false;
                }
            }
        }
        return true;
    }
}
