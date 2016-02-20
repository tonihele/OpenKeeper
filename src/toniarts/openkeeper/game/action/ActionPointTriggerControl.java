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
package toniarts.openkeeper.game.action;

import com.jme3.app.state.AppStateManager;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.trigger.TriggerControl;
import toniarts.openkeeper.game.trigger.TriggerGenericData;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric;
import toniarts.openkeeper.world.MapData;
import toniarts.openkeeper.world.TileData;
import toniarts.openkeeper.world.WorldState;


/**
 *
 * @author ArchDemon
 */


public class ActionPointTriggerControl extends TriggerControl {

    private static final Logger logger = Logger.getLogger(ActionPointTriggerControl.class.getName());

    public ActionPointTriggerControl() { // empty serialization constructor
        super();
    }

    public ActionPointTriggerControl(final AppStateManager stateManager, int triggerId) {
        super(stateManager, triggerId);
    }

    @Override
    protected boolean isActive(TriggerGenericData trigger) {
        boolean result = super.isActive(trigger);
        if (checked) {
            return result;
        }

        int target = 0;
        int value = 0;
        ActionPoint ap = (ActionPoint) parent;

        TriggerGeneric.TargetType targetType = trigger.getType();
        switch (targetType) {
            case AP_CONGREGATE_IN:
                short playerId = trigger.getUserData("playerId", short.class);
                short targetId = trigger.getUserData("targetId", short.class);
                value = trigger.getUserData("value", int.class);
                short type = trigger.getUserData("targetType", short.class);
                switch (type) {
                    case 0:
                    case 3: // Creature
                        break;
                    case 6: // Object
                        break;
                    default:
                        logger.log(Level.WARNING, "AP_CONGREGATE_IN unknown targetType {0}", type);
                        break;
                }
                return false;

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
                        logger.warning("AP_POSESSED_CREATURE_ENTERS unknown targetType");
                        break;
                }
                return false;

            case AP_CLAIM_PART_OF:
                playerId = trigger.getUserData("playerId", short.class);
                value = trigger.getUserData("value", int.class);

                MapData map = stateManager.getState(WorldState.class).getMapData();
                for (int x = (int) ap.getStart().x; x <= (int) ap.getEnd().x; x++) {
                    for (int y = (int) ap.getStart().y; y <= (int) ap.getEnd().y; y++) {
                        if (playerId == map.getTile(x, y).getPlayerId()) {
                            target++;
                        }
                    }
                }
                break;

            case AP_CLAIM_ALL_OF:
                playerId = trigger.getUserData("playerId", short.class);
                // value = trigger.getUserData("value", int.class); // Unusefull ?
                map = stateManager.getState(WorldState.class).getMapData();
                for (int x = (int) ap.getStart().x; x <= (int) ap.getEnd().x; x++) {
                    for (int y = (int) ap.getStart().y; y <= (int) ap.getEnd().y; y++) {
                        if (playerId != map.getTile(x, y).getPlayerId()) {
                            return false;
                        }
                    }
                }
                return true;

            case AP_SLAB_TYPES:
                playerId = trigger.getUserData("playerId", short.class);
                targetId = trigger.getUserData("terrainId", short.class);
                value = trigger.getUserData("value", int.class);

                map = stateManager.getState(WorldState.class).getMapData();
                for (int x = (int) ap.getStart().x; x <= (int) ap.getEnd().x; x++) {
                    for (int y = (int) ap.getStart().y; y <= (int) ap.getEnd().y; y++) {
                        TileData tile = map.getTile(x, y);

                        if (playerId != 0 && playerId != tile.getPlayerId() || targetId != tile.getTerrainId()) {
                            continue;
                        }

                        target++;
                    }
                }
                break;

            case AP_TAG_PART_OF:
                // playerId = trigger.getUserData("playerId", short.class);
                value = trigger.getUserData("value", int.class);

                map = stateManager.getState(WorldState.class).getMapData();
                for (int x = (int) ap.getStart().x; x <= (int) ap.getEnd().x; x++) {
                    for (int y = (int) ap.getStart().y; y <= (int) ap.getEnd().y; y++) {
                        // TODO check who tagged tile
                        if (map.getTile(x, y).isSelected()) {
                            target++;
                        }
                    }
                }
                break;

            case AP_TAG_ALL_OF:
                // playerId = trigger.getUserData("playerId", short.class);
                // value = trigger.getUserData("value", int.class); // Unusefull ?
                map = stateManager.getState(WorldState.class).getMapData();
                for (int x = (int) ap.getStart().x; x <= (int) ap.getEnd().x; x++) {
                    for (int y = (int) ap.getStart().y; y <= (int) ap.getEnd().y; y++) {
                        // TODO check who tagged tile
                        if (!map.getTile(x, y).isSelected()) {
                            return false;
                        }
                    }
                }
                return true;

            default:
                logger.warning("Target Type not supported");
                return false;
        }

        TriggerGeneric.ComparisonType comparisonType = trigger.getComparison();
        if (comparisonType != null && comparisonType != TriggerGeneric.ComparisonType.NONE) {
            result = compare(target, comparisonType, value);
        }

        return result;
    }
}