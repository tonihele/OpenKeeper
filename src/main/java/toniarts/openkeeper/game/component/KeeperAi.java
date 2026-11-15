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
     * The room site, from its first tile to the opposite corner
     */
    public Point corner;
    public Point oppositeCorner;

    /**
     * How many tiles around the gold have been looked at
     */
    public int goldTiles;

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
        this.roomId = keeperAi.roomId;
        this.wantedRoomId = keeperAi.wantedRoomId;
        this.roomWanted = keeperAi.roomWanted;
        this.direction = keeperAi.direction;
        this.current = keeperAi.current;
        this.target = keeperAi.target;
        this.corner = keeperAi.corner;
        this.oppositeCorner = keeperAi.oppositeCorner;
        this.goldTiles = keeperAi.goldTiles;
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
    }

}
