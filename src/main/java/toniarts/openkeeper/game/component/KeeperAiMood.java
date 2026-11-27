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

/**
 * The mood a keeper's creature is in, unhappy or angry, as its keeper was
 * told of it last. The keeper is told of each turn for the worse once, DK II
 * posts a message for it
 */
public final class KeeperAiMood implements EntityComponent {

    /**
     * 1 for unhappy, 2 for angry
     */
    public int state;

    public KeeperAiMood() {
        // For serialization
    }

    public KeeperAiMood(int state) {
        this.state = state;
    }

}
