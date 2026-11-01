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
import toniarts.openkeeper.tools.convert.map.IKwdFile;
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
            IKwdFile kwdFile,
            IMapInformation mapInformation,
            IMapTileInformation mapTile,
            Keeper player,
            EntityData entityData,
            EntityId target) {
        Creature creature = null;
        Short owner = null;
        if (target != null) {
            CreatureComponent creatureComponent = entityData.getComponent(target, CreatureComponent.class);
            if (creatureComponent != null) {
                creature = kwdFile.getCreature(creatureComponent.creatureId);
            }
            Owner ownerComponent = entityData.getComponent(target, Owner.class);
            if (ownerComponent != null) {
                owner = ownerComponent.controlId;
            }
        }

        return isValidCast(keeperSpell, mapInformation, mapTile, player, creature, owner);
    }

    /**
     * Checks the cast with the target already known, the client knows it from
     * what it shows and must not look it up from the entity data
     *
     * @param keeperSpell the spell
     * @param mapInformation the map
     * @param mapTile the tile the spell is cast on
     * @param player the casting keeper
     * @param creature the targeted creature, {@code null} if the target is no
     * creature
     * @param owner the keeper in control of the target, {@code null} if
     * there is no target or it has no owner
     * @return whether the cast is allowed
     */
    public static boolean isValidCast(
            KeeperSpell keeperSpell,
            IMapInformation mapInformation,
            IMapTileInformation mapTile,
            Keeper player,
            Creature creature,
            Short owner) {
        return checkPlayerMana(keeperSpell, player)
                && checkCastRule(mapInformation, mapTile, keeperSpell, player)
                && checkTargetRule(creature, owner, keeperSpell, player);
    }

    private static boolean checkPlayerMana(KeeperSpell keeperSpell, Keeper player) {
        return keeperSpell.getManaCost() <= player.getMana() - player.getManaLoose();
    }

    private static boolean checkTargetRule(Creature creature, Short owner, KeeperSpell keeperSpell, Keeper player) {
        short playerId = player.getId();

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
                if (creature == null || owner == null) {
                    return false;
                }
                if (!player.isEnemy(owner)) {
                    return false;
                }
            }
            case OWN_CREATURES -> {
                if (creature == null || owner == null || owner != playerId) {
                    return false;
                }
            }
            case POSESSION -> {

                // The Horned Reaper can't be possessed
                if (creature == null || owner == null || owner != playerId
                        || creature.getFlags().contains(Creature.CreatureFlag.IS_HORNY)) {
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
