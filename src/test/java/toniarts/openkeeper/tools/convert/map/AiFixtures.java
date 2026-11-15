/*
 * Copyright (C) 2014-2019 OpenKeeper
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
package toniarts.openkeeper.tools.convert.map;

import java.util.EnumSet;
import java.util.List;

/**
 * Builds computer players for tests, the setters are protected
 */
public final class AiFixtures {

    private AiFixtures() {
    }

    /**
     * A computer player that takes one action a tick, builds rooms next to
     * the rooms they go well with and may build on the whole map
     *
     * @param maximumImps the most imps it creates
     * @param tilesPerImp the owned tiles it wants for each imp
     * @param roomSize the size of its rooms in tiles
     * @param tilesBetweenRooms the gap it leaves between rooms
     * @param openness how many percent of the tiles around a room may be open
     * @param mineGoldBelow the gold it wants to have before it stops mining
     * @param buildOrder the rooms it builds, in order
     * @return the computer player settings
     */
    public static AI ai(int maximumImps, int tilesPerImp, int roomSize, int tilesBetweenRooms, int openness,
            int mineGoldBelow, List<Short> buildOrder) {
        AI ai = new AI();
        ai.setSpeed((short) 1);
        ai.setMaximumNumberOfImps((short) maximumImps);
        ai.setImpsToTilesRatio(tilesPerImp);
        ai.setDefaultSize((short) roomSize);
        ai.setTilesLeftBetweenRooms((short) tilesBetweenRooms);
        ai.setOpenness((short) openness);
        ai.setMineGoldUntilGoldHeldIsGreaterThan(mineGoldBelow);
        ai.setBuildOrder(buildOrder);
        ai.setDistanceBetweenRoomsThatShouldBeCloseMan(AI.Distance.CLOSE);
        ai.setBuildAreaEndX(Short.MAX_VALUE);
        ai.setBuildAreaEndY(Short.MAX_VALUE);
        return ai;
    }

    /**
     * Limits where the computer player may build
     *
     * @param ai the computer player settings
     * @param endX the last column of the build area
     * @return the same settings
     */
    public static AI withBuildAreaEndX(AI ai, int endX) {
        ai.setBuildAreaEndX(endX);
        return ai;
    }

    /**
     * Sets how far the computer player digs to claim a neutral room
     *
     * @param ai the computer player settings
     * @param tilesOfHeart how near its Dungeon Heart the room has to be
     * @param tilesOfClaimedArea or how near its land
     * @return the same settings
     */
    public static AI withNeutralRoomRange(AI ai, int tilesOfHeart, int tilesOfClaimedArea) {
        ai.setDigToNeutralRoomsWithinTilesOfHeart((short) tilesOfHeart);
        ai.setDigToNeutralRoomsWithinTilesOfClaimedArea((short) tilesOfClaimedArea);
        return ai;
    }

    /**
     * Sets how the computer player answers an attack
     *
     * @param ai the computer player settings
     * @param threatPercentage the threat it wants at the attack, in percent
     * of the enemy's
     * @param pullOut whether it carries its creatures out of a fight it is
     * losing away from its land
     * @param imprisonPercentage how many percent of the enemy creatures it
     * means to take prisoner
     * @return the same settings
     */
    public static AI withAttackResponse(AI ai, int threatPercentage, boolean pullOut, int imprisonPercentage) {
        ai.setThreatValueOfDroppedCreaturesIsPercentageOfEnemyThreatValue((short) threatPercentage);
        ai.setPullCreaturesFromFightIfOutnumberedAndUnableToDropReinforcements(pullOut);
        ai.setAttemptToImprisonPercentageOfEnemyCreatures((short) imprisonPercentage);
        return ai;
    }

    /**
     * Sets how the computer player uses its spells in a fight
     *
     * @param ai the computer player settings
     * @param use how seldom it casts: 0 always, 4 never
     * @param style how often it defends rather than attacks: 0 never, 4
     * always
     * @param power how dear a spell it takes: 4 any
     * @return the same settings
     */
    public static AI withBattleSpells(AI ai, int use, int style, int power) {
        ai.setUseSpellsInBattle((short) use);
        ai.setSpellStyle((short) style);
        ai.setSpellsPowerPreference((short) power);
        return ai;
    }

    /**
     * A keeper spell for a fight
     *
     * @param keeperSpellId the spell id
     * @param manaCost what it costs to cast
     * @param flag whether it attacks or defends
     * @return the spell
     */
    public static KeeperSpell battleSpell(short keeperSpellId, int manaCost, KeeperSpell.KeeperSpellFlag flag) {
        KeeperSpell keeperSpell = new KeeperSpell();
        keeperSpell.setKeeperSpellId(keeperSpellId);
        keeperSpell.setName("Spell " + keeperSpellId);
        keeperSpell.setManaCost(manaCost);
        keeperSpell.setFlags(EnumSet.of(flag));
        return keeperSpell;
    }

    /**
     * Sets when the computer player pulls a hurt creature out
     *
     * @param ai the computer player settings
     * @param percentage the health below which a creature is carried away
     * @return the same settings
     */
    public static AI withRetreatHealth(AI ai, int percentage) {
        ai.setIfCreatureHealthIsPercentageAndNotInOwnRoomMoveToLairOrTemple((short) percentage);
        return ai;
    }

    /**
     * Sets when the computer player casts Call to Arms and lifts it again
     *
     * @param ai the computer player settings
     * @param threat the enemy threat around a fighter above which it calls
     * @param ticks the game ticks the flag stays up
     * @param enemies the enemies that have to be near the flag to keep it
     * @param tiles how near
     * @param fighters the fighters it has to have to keep the flag
     * @param waitSeconds the seconds before it calls again
     * @return the same settings
     */
    public static AI withCallToArms(AI ai, int threat, int ticks, int enemies, int tiles, int fighters, int waitSeconds) {
        ai.setPlaceCallToArmsWhereThreatValueIsGreaterThan(threat);
        ai.setRemoveCallToArmsAfterSeconds(ticks);
        ai.setRemoveCallToArmsIfLessThanEnemyCreatures((short) enemies);
        ai.setRemoveCallToArmsIfLessThanEnemyCreaturesWithinTiles((short) tiles);
        ai.setRemoveCallToArmsIfTotalCreaturesLessThan((short) fighters);
        ai.setWaitSecondsAfterPreviousAttackBeforeAttackingAgain(waitSeconds);
        return ai;
    }

    /**
     * A keeper the level gives to the computer
     *
     * @param playerId the player id
     * @param ai the computer player settings
     * @return the player
     */
    public static Player player(short playerId, AI ai) {
        Player player = new Player();
        player.setPlayerId(playerId);
        player.setAi(true);
        player.setAiAttributes(ai);
        return player;
    }
}
