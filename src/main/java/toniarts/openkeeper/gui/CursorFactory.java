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
import java.util.Map;

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
        SPELL_POSSESS,
        NO_SPELL_POSSESS,
        SPELL_CAST;
    }
    private static Map<CursorType, JmeCursor> cursors;

    private CursorFactory() {
        // Nope
    }

    public static JmeCursor getCursor(CursorType cursor, AssetManager assetManager) {
        if (cursors == null) {
            populateCursors(assetManager);
        }

        return cursors.get(cursor);
    }

    private static void populateCursors(AssetManager assetManager) {

        Map<CursorType, JmeCursor> cursorsLocal = new HashMap<>(CursorType.values().length);

        //
        // Animated cursors
        //
        cursorsLocal.put(CursorType.IDLE, new Cursor(assetManager, "Point.png", 14, 32, 41));
        cursorsLocal.put(CursorType.SPELL_CAST, new Cursor(assetManager, "SpellCast.png", 5, 65, 12));
        cursorsLocal.put(CursorType.SPELL_POSSESS, new Cursor(assetManager, "SpellPossess.png", 2, 2, 6));
        cursorsLocal.put(CursorType.DROP_GOLD, new Cursor(assetManager, "DropGold.png", 10, 40, 16));
        cursorsLocal.put(CursorType.DROP_THING, new Cursor(assetManager, "DropThing.png", 5, 40, 14));
        cursorsLocal.put(CursorType.SLAP, new Cursor(assetManager, "Slap.png", 5, 40, 15));

        //
        // Static mouse cursors
        //
        cursorsLocal.put(CursorType.POINTER, new Cursor(assetManager, "Idle.png", 2, 14));
        cursorsLocal.put(CursorType.PICKAXE_TAG, new Cursor(assetManager, "PickAxeTag.png", 10, 65));
        cursorsLocal.put(CursorType.HOLD_PICKAXE, new Cursor(assetManager, "PickAxeHold.png", 3, 40));
        cursorsLocal.put(CursorType.HOLD_PICKAXE_TAGGING, new Cursor(assetManager, "PickAxeHoldTagging.png", 10, 65));
        cursorsLocal.put(CursorType.HOLD_SPELL, new Cursor(assetManager, "SpellHold.png", 30, 50));
        cursorsLocal.put(CursorType.HOLD_GOLD, new Cursor(assetManager, "HoldGold.png", 5, 5));
        cursorsLocal.put(CursorType.HOLD_THING, new Cursor(assetManager, "HoldThing.png", 5, 40));
        cursorsLocal.put(CursorType.NO_SPELL_POSSESS, new Cursor(assetManager, "SpellPossessNoGo.png", 32, 32));

        cursors = cursorsLocal;
    }
}
