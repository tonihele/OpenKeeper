/*
 * Copyright (C) 2014-2026 OpenKeeper
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
package toniarts.openkeeper.game.map;

/**
 * The 16-bit colour-class values {@link MapColourClassifier} emits
 */
public final class MapColourClass {

    private MapColourClass() {
    }

    /**
     * Non-solid, non-liquid, not claimable, owned by neutral, no room - e.g.
     * a neutral-owned Claimed Path. Renders as magenta
     */
    public static final short NEUTRAL_OWNED_SENTINEL = 0x00;

    /**
     * Unexplored, or impenetrable rock / map edge. Not a colour - rendered
     * with the rock texture.
     */
    public static final short UNEXPLORED_OR_IMPENETRABLE = 0x01;

    /**
     * Claimable floor (Dirt Path, Mana Vault) - see {@link TerrainClaimability}.
     */
    public static final short CLAIMABLE_FLOOR = 0x02;

    /**
     * Diggable rock (solid, no gold value, not impenetrable).
     */
    public static final short DIGGABLE_ROCK = 0x04;

    /**
     * Gems (solid, gold value, impenetrable).
     */
    public static final short GEMS = 0x05;

    /**
     * Gold (solid, gold value, not impenetrable).
     */
    public static final short GOLD = 0x07;

    public static final short WATER = 0x09;

    public static final short LAVA = 0x0B;

    /**
     * Drag-box highlight. Stored directly by {@link MapColourGrid#setHighlight}
     * rather than emitted from {@link MapColourClassifier#classify} - see
     * that method's javadoc.
     */
    public static final short HIGHLIGHT = 0x0D;

    /**
     * Owned solid (fortified wall) tile of player number n (1..7): add n.
     */
    public static final short OWNED_SOLID_BASE = 0x11;

    /**
     * Owned floor / room tile of player number n (1..7): add n.
     */
    public static final short OWNED_FLOOR_BASE = 0x1A;

    /**
     * Dungeon Heart tile of player number n (1..7): add n.
     */
    public static final short DUNGEON_HEART_BASE = 0x23;

}
