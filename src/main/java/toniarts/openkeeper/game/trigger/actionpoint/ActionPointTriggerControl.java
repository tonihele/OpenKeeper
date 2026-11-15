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


import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.HashSet;
import java.util.Set;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.object.IObjectController;
import toniarts.openkeeper.game.data.ActionPoint;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.trigger.TriggerControl;
import toniarts.openkeeper.game.trigger.TriggerGenericData;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric;

/**
 *
 * @author ArchDemon
 */
public final class ActionPointTriggerControl extends TriggerControl {

    private static final Logger logger = System.getLogger(ActionPointTriggerControl.class.getName());

    private final ActionPoint ap;
    private final IEntityPositionLookup entityPositionLookup;
    private final boolean actionPointWithinMap;
    private boolean invalidActionPointWarningLogged;
    private final Set<Integer> warnedCongregateInTriggers = new HashSet<>();

    public ActionPointTriggerControl(final IGameController gameController, final ILevelInfo levelInfo, final IGameTimer gameTimer,
            final IMapController mapController, final ICreaturesController creaturesController, int triggerId, ActionPoint ap,
            final IEntityPositionLookup entityPositionLookup) {
        super(gameController, levelInfo, gameTimer, mapController, creaturesController, triggerId);
        this.ap = ap;
        this.entityPositionLookup = entityPositionLookup;
        this.actionPointWithinMap = isActionPointWithinMap(ap, mapController);
    }

    @Override
    protected boolean isActive(TriggerGenericData trigger) {
        boolean result = false;

        int target = 0;
        int value = 0;
        short playerId;
        short targetId;
        short type;

        TriggerGeneric.TargetType targetType = trigger.getType();
        switch (targetType) {
            case AP_CONGREGATE_IN:
                return isCongregateInActive(trigger);

            case AP_POSESSED_CREATURE_ENTERS:
                playerId = trigger.getUserData("playerId", short.class);
                targetId = trigger.getUserData("targetId", short.class);
                value = trigger.getUserData("value", int.class);
                type = trigger.getUserData("targetType", short.class);
                switch (type) {
                    //case 0:
                    case 3: // Creature
                        break;
                    case 6: // Object. Chicken
                        break;
                    default:
                        logger.log(Level.WARNING, "AP_POSESSED_CREATURE_ENTERS unknown targetType");
                        break;
                }
                return false;

            case AP_CLAIM_PART_OF:
                playerId = trigger.getUserData("playerId", short.class);
                value = trigger.getUserData("value", int.class);

                IMapDataInformation map = mapController.getMapData();
                for (int x = (int) ap.getStart().x; x <= (int) ap.getEnd().x; x++) {
                    for (int y = (int) ap.getStart().y; y <= (int) ap.getEnd().y; y++) {
                        if (playerId == map.getTile(x, y).getOwnerId()) {
                            target++;
                        }
                    }
                }
                break;

            case AP_CLAIM_ALL_OF:
                playerId = trigger.getUserData("playerId", short.class);
                // value = trigger.getUserData("value", int.class); // Unusefull ?
                map = mapController.getMapData();
                for (int x = (int) ap.getStart().x; x <= (int) ap.getEnd().x; x++) {
                    for (int y = (int) ap.getStart().y; y <= (int) ap.getEnd().y; y++) {
                        if (playerId != map.getTile(x, y).getOwnerId()) {
                            return false;
                        }
                    }
                }
                return true;

            case AP_SLAB_TYPES:
                playerId = trigger.getUserData("playerId", short.class);
                targetId = trigger.getUserData("terrainId", short.class);
                value = trigger.getUserData("value", int.class);

                map = mapController.getMapData();
                for (int x = (int) ap.getStart().x; x <= (int) ap.getEnd().x; x++) {
                    for (int y = (int) ap.getStart().y; y <= (int) ap.getEnd().y; y++) {
                        IMapTileInformation tile = map.getTile(x, y);

                        if (playerId != 0 && playerId != tile.getOwnerId() || targetId != tile.getTerrainId()) {
                            continue;
                        }

                        target++;
                    }
                }
                break;

            case AP_TAG_PART_OF:
                playerId = trigger.getUserData("playerId", short.class);
                value = trigger.getUserData("value", int.class);

                map = mapController.getMapData();
                for (int x = (int) ap.getStart().x; x <= (int) ap.getEnd().x; x++) {
                    for (int y = (int) ap.getStart().y; y <= (int) ap.getEnd().y; y++) {
                        // TODO check who tagged tile
                        if (map.getTile(x, y).isSelected(playerId)) {
                            target++;
                        }
                    }
                }
                break;

            case AP_TAG_ALL_OF:
                playerId = trigger.getUserData("playerId", short.class);
                // value = trigger.getUserData("value", int.class); // Unusefull ?
                map = mapController.getMapData();
                for (int x = (int) ap.getStart().x; x <= (int) ap.getEnd().x; x++) {
                    for (int y = (int) ap.getStart().y; y <= (int) ap.getEnd().y; y++) {
                        // TODO check who tagged tile
                        if (!map.getTile(x, y).isSelected(playerId)) {
                            return false;
                        }
                    }
                }
                return true;

            default:
                return super.isActive(trigger);
        }

        TriggerGeneric.ComparisonType comparisonType = trigger.getComparison();
        if (comparisonType != null && comparisonType != TriggerGeneric.ComparisonType.NONE) {
            result = compare(target, comparisonType, value);
        }

        return result;
    }

    private boolean isCongregateInActive(TriggerGenericData trigger) {
        if (!actionPointWithinMap) {
            if (!invalidActionPointWarningLogged) {
                logger.log(Level.WARNING, "AP_CONGREGATE_IN trigger {0} has an invalid action point rectangle", trigger.getId());
                invalidActionPointWarningLogged = true;
            }
            return false;
        }

        Short playerIdValue = trigger.getUserData("playerId", short.class);
        Short targetIdValue = trigger.getUserData("targetId", short.class);
        Short typeValue = trigger.getUserData("targetType", short.class);
        Integer valueValue = trigger.getUserData("value", int.class);
        if (playerIdValue == null || targetIdValue == null || typeValue == null || valueValue == null) {
            warnInvalidCongregateIn(trigger, "missing required parameters");
            return false;
        }

        short playerId = playerIdValue;
        short targetId = targetIdValue;
        short type = typeValue;
        int value = valueValue;

        int target = switch (type) {
            // Stock maps use both zero and three for creature counts. In either
            // case a targetId of zero means any creature type.
            case 0, 3 -> countCreatures(playerId, targetId);
            case 6 -> countObjects(playerId, targetId);
            default -> {
                warnInvalidCongregateIn(trigger, "unknown targetType " + type);
                yield -1;
            }
        };
        if (target < 0) {
            return false;
        }

        TriggerGeneric.ComparisonType comparisonType = trigger.getComparison();
        if (comparisonType == null || comparisonType == TriggerGeneric.ComparisonType.NONE) {
            return false;
        }

        return compare(target, comparisonType, value);
    }

    private void warnInvalidCongregateIn(TriggerGenericData trigger, String reason) {
        if (warnedCongregateInTriggers.add(trigger.getId())) {
            logger.log(Level.WARNING, "AP_CONGREGATE_IN trigger {0}: {1}", new Object[]{trigger.getId(), reason});
        }
    }

    private int countCreatures(short playerId, short targetId) {
        int count = 0;
        for (int x = (int) ap.getStart().x; x <= (int) ap.getEnd().x; x++) {
            for (int y = (int) ap.getStart().y; y <= (int) ap.getEnd().y; y++) {
                for (ICreatureController creature : entityPositionLookup.getEntityTypesInLocation(x, y, ICreatureController.class)) {
                    if ((playerId == 0 || creature.getOwnerId() == playerId)
                            && (targetId == 0 || creature.getCreature().getCreatureId() == targetId)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private int countObjects(short playerId, short targetId) {
        int count = 0;
        for (int x = (int) ap.getStart().x; x <= (int) ap.getEnd().x; x++) {
            for (int y = (int) ap.getStart().y; y <= (int) ap.getEnd().y; y++) {
                for (IObjectController object : entityPositionLookup.getEntityTypesInLocation(x, y, IObjectController.class)) {
                    if ((playerId == 0 || object.getOwnerId() == playerId)
                            && (targetId == 0 || object.getObjectId() == targetId)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static boolean isActionPointWithinMap(ActionPoint ap, IMapController mapController) {
        if (ap == null || mapController == null) {
            return false;
        }

        IMapDataInformation map = mapController.getMapData();
        if (map == null) {
            return false;
        }
        return ap.getStart().x >= 0 && ap.getStart().y >= 0
                && ap.getEnd().x >= ap.getStart().x && ap.getEnd().y >= ap.getStart().y
                && ap.getEnd().x < map.getWidth() && ap.getEnd().y < map.getHeight();
    }
}
