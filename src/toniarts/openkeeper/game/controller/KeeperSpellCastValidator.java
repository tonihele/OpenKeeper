/*
 * Copyright (C) 2014-2024 OpenKeeper
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
package toniarts.openkeeper.game.controller;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.map.IMapInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;
import static toniarts.openkeeper.tools.convert.map.KeeperSpell.CastRule.ANYWHERE;
import static toniarts.openkeeper.tools.convert.map.KeeperSpell.CastRule.ANY_LAND;
import static toniarts.openkeeper.tools.convert.map.KeeperSpell.CastRule.ENEMY_LAND;
import static toniarts.openkeeper.tools.convert.map.KeeperSpell.CastRule.OWN_AND_NEUTRAL_LAND;
import static toniarts.openkeeper.tools.convert.map.KeeperSpell.CastRule.OWN_LAND;
import static toniarts.openkeeper.tools.convert.map.KeeperSpell.TargetRule.ALL;
import static toniarts.openkeeper.tools.convert.map.KeeperSpell.TargetRule.ALL_CREATURES;
import static toniarts.openkeeper.tools.convert.map.KeeperSpell.TargetRule.ENEMY_CREATURES;
import static toniarts.openkeeper.tools.convert.map.KeeperSpell.TargetRule.LAND;
import static toniarts.openkeeper.tools.convert.map.KeeperSpell.TargetRule.NONE;
import static toniarts.openkeeper.tools.convert.map.KeeperSpell.TargetRule.OWN_CREATURES;
import static toniarts.openkeeper.tools.convert.map.KeeperSpell.TargetRule.POSESSION;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Player;

/**
 * Simple util class to check whether casting is allowed
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class KeeperSpellCastValidator {

    private KeeperSpellCastValidator() {
        // Nope
    }

    public static boolean isValidCast(
            KeeperSpell keeperSpell,
            KwdFile kwdFile,
            IMapInformation mapInformation,
            IMapTileInformation mapTile,
            Keeper player,
            EntityData entityData,
            EntityId target) {
        return checkCastRule(mapInformation, mapTile, keeperSpell, player)
                && checkTargetRule(target, entityData, kwdFile, keeperSpell, player);
    }

    private static boolean checkTargetRule(EntityId target, EntityData entityData, KwdFile kwdFile, KeeperSpell keeperSpell, Keeper player) {
        short playerId = player.getId();
        Creature creature = null;
        Short owner = null;
        if (target != null) {
            CreatureComponent creatureComponent = entityData.getComponent(target, CreatureComponent.class);
            if (creatureComponent != null) {
                creature = kwdFile.getCreature(creatureComponent.creatureId);
            }
            Owner ownerComponent = entityData.getComponent(target, Owner.class);
            if (ownerComponent != null) {
                owner = ownerComponent.ownerId;
            }
        }

        switch (keeperSpell.getTargetRule()) {
            case ALL, LAND -> {
            }
            case NONE -> {
                return false;
            }
            case ALL_CREATURES -> {
                if (creature == null) {
                    return false;
                }
            }
            case ENEMY_CREATURES -> {
                if (owner == null) {
                    return false;
                }
                if (!player.isEnemy(owner)) {
                    return false;
                }
            }
            case OWN_CREATURES, POSESSION -> {
                if (creature == null || owner == null || owner != playerId) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean checkCastRule(IMapInformation mapInformation, IMapTileInformation mapTile, KeeperSpell keeperSpell, Keeper player) {
        short playerId = player.getId();
        boolean isSolid = mapInformation.isSolid(mapTile.getLocation());
        switch (keeperSpell.getCastRule()) {
            case OWN_LAND: {
                if (isSolid || mapTile.getOwnerId() != playerId) {
                    return false;
                }
                break;
            }
            case OWN_AND_NEUTRAL_LAND: {
                if (isSolid || mapTile.getOwnerId() != playerId || mapTile.getOwnerId() != Player.NEUTRAL_PLAYER_ID) {
                    return false;
                }
                break;
            }
            case ENEMY_LAND: {
                if (isSolid || !player.isEnemy(mapTile.getOwnerId())) {
                    return false;
                }
            }
            case ANY_LAND:
                if (isSolid) {
                    return false;
                }
            case ANYWHERE:
                break;
            case NONE:
                return false;
        }

        return true;
    }

}
