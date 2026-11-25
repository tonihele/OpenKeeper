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
package toniarts.openkeeper.game.component;

import com.simsilica.es.EntityComponent;
import toniarts.openkeeper.utils.Point;

/**
 * What a computer player is doing. One entity per computer player carries
 * this, so the computer player itself holds no state.
 */
public final class KeeperAi implements EntityComponent {

    /**
     * The steps of the computer player's main task
     */
    public enum Task {

        /**
         * Decide what to do next
         */
        PLAN,
        /**
         * Look for a place for the room
         */
        FIND_ROOM_SITE,
        /**
         * Tag the place of the room for digging
         */
        TAG_ROOM,
        /**
         * Place the room on the tiles the imps have dug out and claimed
         */
        BUILD_ROOM,
        /**
         * The job is done
         */
        FINISH,
        /**
         * Head for the nearest gold
         */
        GO_TO_GOLD,
        /**
         * Tag the gold around the place the path was dug to
         */
        TAG_GOLD,
        /**
         * Dig a path tile by tile, then carry on with the next task
         */
        DIG_PATH
    }

    public short playerId;
    public Task task = Task.PLAN;
    public Task nextTask = Task.PLAN;

    /**
     * Game ticks spent on the task
     */
    public int taskTicks;

    /**
     * Game ticks until the tagged gold is let go, 0 when not counting
     */
    public int untagTicks;

    /**
     * The gold tiles it tagged, the newest first, as many as the spiral
     * round a seam has. DK II keeps its dig orders in the order it gave them
     */
    public Point[] goldTags = new Point[49];

    /**
     * The room being built
     */
    public short roomId;

    /**
     * The room it plans next, when one is picked. DK II picks a room and
     * lines the Portal up for the plan after it
     */
    public short wantedRoomId;
    public boolean roomWanted;

    /**
     * Whether the room it plans next is to be grown rather than built anew
     */
    public boolean wantedRoomExpand;

    /**
     * How many Portals it had when it last looked, a new one may ask for a
     * bigger Lair
     */
    public int portals;

    /**
     * The direction the room site is looked for in
     */
    public int direction;

    /**
     * The tile being worked on
     */
    public Point current;

    /**
     * Where the path being dug leads
     */
    public Point target;

    /**
     * Whether the path being dug follows the edge of rock that can't be
     * dug, with the rock on its left hand or on its right, and the way it
     * is heading: 0 north, 1 east, 2 south, 3 west
     */
    public boolean wallFollowing;
    public boolean wallOnLeft;
    public int wallHeading;

    /**
     * Where the path met the rock, and the tile behind the rock where it
     * goes straight on again
     */
    public Point wallStart;
    public Point wallExit;

    /**
     * The room site, from its first tile to the opposite corner
     */
    public Point corner;
    public Point oppositeCorner;

    /**
     * How many tiles around the gold have been looked at
     */
    public int goldTiles;

    /** The last gold seam it tagged, and the remaining work it saw there. */
    public Point goldAttemptTarget;
    public int goldAttemptWorkAtStart;
    public boolean goldAttemptLooked;

    /**
     * The middles of the gold seams nearest to its rooms when it last
     * looked, the nearest first. DK II heads for the seam of its last look
     * and then looks again
     */
    public Point[] goldDeposits = new Point[4];

    /**
     * Whether its Call to Arms flag is up, and where
     */
    public boolean callToArms;
    public Point callToArmsTile;

    /**
     * Game ticks until the Call to Arms flag has been up for its time
     */
    public int callToArmsTicks;

    /**
     * Game ticks until it may call its creatures to arms again
     */
    public int cannotAttackTicks;

    /**
     * The attacks it still has to answer, up to three, as a ring: how many,
     * and which one is first
     */
    public int attacks;
    public int firstAttack;

    /**
     * For each attack the threat it still wants to drop there, or when
     * below zero the threat it still wants to carry away from there
     */
    public int[] attackNeeds = new int[3];

    /**
     * For each attack the tile it drops its creatures on, or the tile it
     * carries them away from
     */
    public Point[] attackTiles = new Point[3];

    /**
     * The player that attacked it last, 0 for none
     */
    public short attackerId;

    /**
     * Game ticks until it looks for a new attack on it
     */
    public int attackLookTicks;

    /**
     * The enemy the tunnel being dug goes to, 0 for none. The tunnel stops
     * at the enemy's land
     */
    public short invasionTargetId;

    /**
     * Where it calls its creatures to arms on the way to an enemy
     */
    public Point rallyTile;

    /**
     * The targets of the last paths it could not dig to the end, and for
     * each the game ticks until it may pick that neutral room or gold seam
     * again. DK II keeps no such list and picks the same one each time
     */
    public Point[] failedTargets = new Point[4];
    public int[] failedTargetTicks = new int[4];

    /**
     * How many times in a row the search for a room site gave up. For each
     * the next search is for a room a tile shorter each way, down to 3 by 3.
     * DK II keeps looking for a room of the default size
     */
    public int roomSitesNotFound;

    public KeeperAi() {
        // For serialization
    }

    public KeeperAi(short playerId) {
        this.playerId = playerId;
    }

    public KeeperAi(KeeperAi keeperAi) {
        this.playerId = keeperAi.playerId;
        this.task = keeperAi.task;
        this.nextTask = keeperAi.nextTask;
        this.taskTicks = keeperAi.taskTicks;
        this.untagTicks = keeperAi.untagTicks;
        this.goldTags = keeperAi.goldTags.clone();
        this.roomId = keeperAi.roomId;
        this.wantedRoomId = keeperAi.wantedRoomId;
        this.roomWanted = keeperAi.roomWanted;
        this.wantedRoomExpand = keeperAi.wantedRoomExpand;
        this.portals = keeperAi.portals;
        this.direction = keeperAi.direction;
        this.current = keeperAi.current;
        this.target = keeperAi.target;
        this.wallFollowing = keeperAi.wallFollowing;
        this.wallOnLeft = keeperAi.wallOnLeft;
        this.wallHeading = keeperAi.wallHeading;
        this.wallStart = keeperAi.wallStart;
        this.wallExit = keeperAi.wallExit;
        this.corner = keeperAi.corner;
        this.oppositeCorner = keeperAi.oppositeCorner;
        this.goldTiles = keeperAi.goldTiles;
        this.goldAttemptTarget = keeperAi.goldAttemptTarget;
        this.goldAttemptWorkAtStart = keeperAi.goldAttemptWorkAtStart;
        this.goldAttemptLooked = keeperAi.goldAttemptLooked;
        this.goldDeposits = keeperAi.goldDeposits.clone();
        this.callToArms = keeperAi.callToArms;
        this.callToArmsTile = keeperAi.callToArmsTile;
        this.callToArmsTicks = keeperAi.callToArmsTicks;
        this.cannotAttackTicks = keeperAi.cannotAttackTicks;
        this.attacks = keeperAi.attacks;
        this.firstAttack = keeperAi.firstAttack;
        this.attackNeeds = keeperAi.attackNeeds.clone();
        this.attackTiles = keeperAi.attackTiles.clone();
        this.attackerId = keeperAi.attackerId;
        this.attackLookTicks = keeperAi.attackLookTicks;
        this.invasionTargetId = keeperAi.invasionTargetId;
        this.rallyTile = keeperAi.rallyTile;
        this.failedTargets = keeperAi.failedTargets.clone();
        this.failedTargetTicks = keeperAi.failedTargetTicks.clone();
        this.roomSitesNotFound = keeperAi.roomSitesNotFound;
    }

}
