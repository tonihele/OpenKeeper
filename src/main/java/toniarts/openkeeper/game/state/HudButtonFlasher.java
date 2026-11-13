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
package toniarts.openkeeper.game.state;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import toniarts.openkeeper.tools.convert.map.TriggerAction;

/**
 * Keeps track of the HUD buttons that a level script has asked to flash
 * ({@code FLASH_BUTTON} action). Requests may come from any thread, the
 * display is only touched from {@link #update(float)}, which belongs to the
 * render thread.
 */
final class HudButtonFlasher {

    /**
     * How often the flashing is applied again, so that icons that were created
     * after the request (researched rooms etc.) start flashing too
     */
    static final float REAPPLY_INTERVAL = 0.25f;
    /**
     * The key of the creature tab button
     */
    static final String CREATURE_TAB = "tab-creature";

    /**
     * Switches the flashing of a HUD button on or off. Needs to be safe to call
     * repeatedly and for buttons that do not exist.
     */
    @FunctionalInterface
    interface Display {

        void setFlashing(String buttonKey, boolean flashing);
    }

    private final Display display;
    private final Map<String, Float> remaining = new HashMap<>();
    private final Set<String> stopped = new HashSet<>();
    private float sinceApplied = 0;
    private boolean changed = false;

    HudButtonFlasher(Display display) {
        this.display = display;
    }

    /**
     * Find the key of a HUD button, the id of its element (the creature tab
     * has a key of its own)
     *
     * @param type the button type
     * @param targetId the room, spell, door, trap or button id
     * @param buttonType the misc button type, when the type is a misc button
     * @return the key, {@code null} if the HUD has no such button
     */
    @Nullable
    static String buttonKey(TriggerAction.MakeType type, short targetId, @Nullable TriggerAction.ButtonType buttonType) {
        return switch (type) {
            case ROOM ->
                "room_" + targetId;
            case KEEPER_SPELL ->
                "spell_" + targetId;
            case DOOR ->
                "door_" + targetId;
            case TRAP ->
                "trap_" + targetId;
            case MISC_BUTTON ->
                miscButtonKey(buttonType);
            default ->
                null;
        };
    }

    @Nullable
    private static String miscButtonKey(@Nullable TriggerAction.ButtonType buttonType) {
        if (buttonType == null) {
            return null;
        }
        return switch (buttonType) {
            case SELL_ROOM ->
                "sell_0";
            case ZOOM_IN_OUT ->
                "zoomButton";
            case GUI_MIN_MAX ->
                "toggleButton";
            case GOLD_VALUE ->
                "goldButton";
            case MANA_VALUE ->
                "manaButton";
            case CREATURE_TAB ->
                CREATURE_TAB;
            default ->
                null;
        };
    }

    /**
     * Start or stop the flashing of a button
     *
     * @param type the button type
     * @param targetId the room, spell, door, trap or button id
     * @param buttonType the misc button type, when the type is a misc button
     * @param enabled flash on or off
     * @param seconds how long to flash, 0 flashes until stopped
     */
    synchronized void flash(TriggerAction.MakeType type, short targetId, @Nullable TriggerAction.ButtonType buttonType,
            boolean enabled, int seconds) {
        String key = buttonKey(type, targetId, buttonType);
        if (key == null) {
            return;
        }
        if (enabled) {
            stopped.remove(key);
            remaining.put(key, seconds > 0 ? (float) seconds : Float.POSITIVE_INFINITY);
            changed = true;
        } else {
            stop(key);
        }
    }

    /**
     * Stop the flashing of a button, e.g. because the player pressed it
     *
     * @param buttonKey the button key
     */
    synchronized void stop(String buttonKey) {
        if (remaining.remove(buttonKey) != null) {
            stopped.add(buttonKey);
        }
    }

    /**
     * Apply the changes to the display, to be called from the render thread
     *
     * @param tpf time per frame
     */
    synchronized void update(float tpf) {
        for (String key : stopped) {
            display.setFlashing(key, false);
        }
        stopped.clear();

        sinceApplied += tpf;
        boolean apply = changed || sinceApplied >= REAPPLY_INTERVAL;
        Iterator<Map.Entry<String, Float>> it = remaining.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Float> entry = it.next();
            entry.setValue(entry.getValue() - tpf);
            if (entry.getValue() <= 0) {
                display.setFlashing(entry.getKey(), false);
                it.remove();
            } else if (apply) {
                display.setFlashing(entry.getKey(), true);
            }
        }
        if (apply) {
            sinceApplied = 0;
            changed = false;
        }
    }
}
