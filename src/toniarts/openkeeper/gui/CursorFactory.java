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
package toniarts.openkeeper.gui;

import com.jme3.asset.AssetManager;
import com.jme3.cursors.plugins.JmeCursor;
import java.util.HashMap;

/**
 * Small utility for creating cursors from the original DK II assets
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class CursorFactory {

    public enum CursorType {

        IDLE,
        POINTER,
        HOLD_SPELL,
        HOLD_THING,
        HOLD_GOLD,
        HOLD_PICKAXE,
        HOLD_PICKAXE_TAGGING,
        SLAP,
        DROP_GOLD,
        DROP_THING,
        PICKAXE_TAG,
        // We are having issues because of the file format
        // SPELL_POSSESS,
        // NO_SPELL_POSSESS,
        SPELL_CAST;
    }
    private static volatile HashMap<CursorType, JmeCursor> cursors;
    private static final Object lock = new Object();

    private CursorFactory() {
        // Nope
    }

    public static JmeCursor getCursor(CursorType cursor, AssetManager assetManager) {
        if (cursors == null) {
            synchronized (lock) {
                if (cursors == null) {
                    initializeCursors(assetManager);
                }
            }
        }
        return cursors.get(cursor);
    }

    private static void initializeCursors(AssetManager assetManager) {

        cursors = new HashMap<>(CursorType.values().length);

        //
        // Animated cursors
        //
        cursors.put(CursorType.IDLE, new Cursor(assetManager, "Point.png", 6, 4, 41));
        cursors.put(CursorType.SPELL_CAST, new Cursor(assetManager, "SpellCast.png", 5, 65, 12));
        //cursors.put(CursorType.SPELL_POSSESS, new Cursor(assetManager, "SpellPossess.png", 2, 2, 6));
        cursors.put(CursorType.DROP_GOLD, new Cursor(assetManager, "DropGold.png", 10, 40, 16));
        cursors.put(CursorType.DROP_THING, new Cursor(assetManager, "DropThing.png", 5, 40, 14));
        cursors.put(CursorType.SLAP, new Cursor(assetManager, "Slap.png", 5, 40, 15));

        //
        // Static mouse cursors
        //
        cursors.put(CursorType.POINTER, new Cursor(assetManager, "Idle.png", 2, 14));
        cursors.put(CursorType.PICKAXE_TAG, new Cursor(assetManager, "PickAxeTag.png", 10, 65));
        cursors.put(CursorType.HOLD_PICKAXE, new Cursor(assetManager, "PickAxeHold.png", 3, 40));
        cursors.put(CursorType.HOLD_PICKAXE_TAGGING, new Cursor(assetManager, "PickAxeHoldTagging.png", 10, 65));
        cursors.put(CursorType.HOLD_SPELL, new Cursor(assetManager, "SpellHold.png", 30, 50));
        cursors.put(CursorType.HOLD_GOLD, new Cursor(assetManager, "HoldGold.png", 5, 5));
        cursors.put(CursorType.HOLD_THING, new Cursor(assetManager, "HoldThing.png", 5, 40));
        //cursors.put(CursorType.NO_SPELL_POSSESS, new Cursor(assetManager, "SpellPossessNoGo.png", 32, 32));
    }
}
