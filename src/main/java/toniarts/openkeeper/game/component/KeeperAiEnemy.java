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
 * What a computer player has done against one enemy keeper. It digs its
 * tunnels up to the enemy's land first and keeps where each one stopped,
 * then breaks through all of them, one a turn.
 */
public final class KeeperAiEnemy implements EntityComponent {

    /**
     * DK II keeps up to 8 tunnels for each enemy
     */
    public static final int BREACH_POINTS = 8;

    /**
     * The computer player
     */
    public short playerId;

    /**
     * The enemy keeper
     */
    public short enemyId;

    /**
     * Tunnels still to dig up to the enemy's land before breaking through
     */
    public int tunnelsLeft;

    /**
     * Whether it has set out against the enemy, its Call to Arms goes there
     * from then on
     */
    public boolean invaded;

    /**
     * Whether it has dug its first tunnel to the enemy, the first one goes
     * where its settings say, the others to random rooms
     */
    public boolean breached;

    /**
     * Where each tunnel stopped short of the enemy's land, and the room it
     * leads to
     */
    public Point[] breachTiles = new Point[BREACH_POINTS];
    public Point[] breachTargets = new Point[BREACH_POINTS];

    public KeeperAiEnemy() {
        // For serialization
    }

    public KeeperAiEnemy(short playerId, short enemyId, int tunnelsLeft) {
        this.playerId = playerId;
        this.enemyId = enemyId;
        this.tunnelsLeft = tunnelsLeft;
    }

    public KeeperAiEnemy(KeeperAiEnemy keeperAiEnemy) {
        this.playerId = keeperAiEnemy.playerId;
        this.enemyId = keeperAiEnemy.enemyId;
        this.tunnelsLeft = keeperAiEnemy.tunnelsLeft;
        this.invaded = keeperAiEnemy.invaded;
        this.breached = keeperAiEnemy.breached;
        this.breachTiles = keeperAiEnemy.breachTiles.clone();
        this.breachTargets = keeperAiEnemy.breachTargets.clone();
    }

}
