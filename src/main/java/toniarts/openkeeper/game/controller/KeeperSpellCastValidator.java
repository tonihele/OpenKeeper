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
import com.simsilica.es.filter.AndFilter;
import com.simsilica.es.filter.FieldFilter;
import toniarts.openkeeper.game.component.KeeperSpellCast;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.data.ResearchableEntity;
import toniarts.openkeeper.game.map.IMapInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
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

    /**
     * Finds the entity that holds when the keeper last cast the spell
     *
     * @param entityData the entity data
     * @param keeperId the keeper
     * @param keeperSpellId the spell
     * @return the entity, or {@code null} if the keeper has never cast the
     * spell
     */
    public static EntityId findLastCast(EntityData entityData, short keeperId, short keeperSpellId) {
        return entityData.findEntity(AndFilter.create(KeeperSpellCast.class,
                new FieldFilter<>(KeeperSpellCast.class, "keeperId", keeperId),
                new FieldFilter<>(KeeperSpellCast.class, "keeperSpellId", keeperSpellId)), KeeperSpellCast.class);
    }

    /**
     * The spell can be cast again once its recharge time has passed since the
     * last cast (DKII.EXE 0x4BBA40)
     *
     * @param keeperSpell the spell
     * @param entityData the entity data
     * @param keeperId the keeper casting
     * @param gameTime the current game time
     * @return true if the spell has recharged
     */
    public static boolean isRecharged(KeeperSpell keeperSpell, EntityData entityData, short keeperId, double gameTime) {
        EntityId lastCast = findLastCast(entityData, keeperId, keeperSpell.getId());
        return lastCast == null
                || gameTime - entityData.getComponent(lastCast, KeeperSpellCast.class).castTime >= keeperSpell.getRechargeTime();
    }

    /**
     * Checks the cast with the target already resolved. The validator runs on
     * both the server and the client, so it must not read the entity data
     *
     * @param keeperSpell the spell
     * @param mapInformation the map
     * @param mapTile the tile the spell is cast on
     * @param player the casting keeper
     * @param creature the targeted creature, {@code null} if the target is no
     * creature
     * @param owner the keeper in control of the target, {@code null} if
     * there is no target or it has no owner
     * @param chicken whether the targeted creature is a chicken
     * @param manaCost the mana the cast costs, see {@link KeeperSpellCost}
     * @return whether the cast is allowed
     */
    public static boolean isValidCast(
            KeeperSpell keeperSpell,
            IMapInformation mapInformation,
            IMapTileInformation mapTile,
            Keeper player,
            Creature creature,
            Short owner,
            boolean chicken,
            int manaCost) {
        return checkPlayerMana(manaCost, player)
                && checkCastRule(mapInformation, mapTile, keeperSpell, player)
                && checkTargetRule(creature, owner, chicken, keeperSpell, player);
    }

    /**
     * The spell the Reaper Talisman casts: the free one shot Reaper while the
     * keeper still has it, otherwise the Summon Reaper spell once it is
     * researched. The Talisman can't be used while the keeper has a Horned
     * Reaper (DK II 0x424EC0, 0x424F50).
     *
     * @param kwdFile the level
     * @param player the keeper
     * @param hasReaper whether the keeper has a Horned Reaper, see
     * {@link ReaperSummoner#hasReaper}
     * @return the spell, {@code null} if the Talisman can't be used now
     */
    public static KeeperSpell getReaperTalismanSpell(IKwdFile kwdFile, Keeper player, boolean hasReaper) {
        KeeperSpell keeperSpell = null;
        for (ResearchableEntity spell : player.getAvailableSpells()) {
            if (spell.isDiscovered() && (spell.getId() == ReaperSummoner.ONE_SHOT_REAPER_SPELL_ID
                    || (spell.getId() == ReaperSummoner.SUMMON_REAPER_SPELL_ID && keeperSpell == null))) {
                keeperSpell = kwdFile.getKeeperSpellById(spell.getId());
            }
        }

        return hasReaper ? null : keeperSpell;
    }

    private static boolean checkPlayerMana(int manaCost, Keeper player) {
        return manaCost <= player.getMana() - player.getManaLoose();
    }

    private static boolean checkTargetRule(Creature creature, Short owner, boolean chicken, KeeperSpell keeperSpell, Keeper player) {
        short playerId = player.getId();

        switch (keeperSpell.getTargetRule()) {
            case ALL, LAND -> {
            }
            case NONE -> {
                return false;
            }
            case ALL_CREATURES -> {

                // Spells for all creatures, such as Chicken, can't be cast on neutral creatures
                if (creature == null || owner == null || owner == Player.NEUTRAL_PLAYER_ID) {
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

                // The Horned Reaper and a chicken can't be possessed
                if (creature == null || owner == null || owner != playerId || chicken
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
