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
package toniarts.openkeeper.view;

import toniarts.openkeeper.view.SystemMessageState.MessageType;

/**
 * Player message types, a subset of the original game's 100 entry type table
 */
public enum PlayerMessageType {

    /**
     * Stub for messages that have no counterpart in the original table. Never
     * de-duplicated.
     */
    CUSTOM(-1, MessageType.INFO, 50, false, false),
    HEART_UNDER_ATTACK(0x01, MessageType.FIGHT, 120, true, false),
    LEVEL_CREATURE_INTRO(0x0E, MessageType.INFO, 50, false, true),
    LEVEL_SPEECH(0x15, MessageType.INFO, 50, false, true),
    MAGIC_ITEM_FOUND(0x30, MessageType.INFO, 10, true, true),
    SPECIAL_EFFECT(0x31, MessageType.INFO, 20, false, true);

    private final int id;
    private final MessageType category;
    private final int timeoutSeconds;
    private final boolean deduplicate;
    private final boolean alwaysAllowed;

    /**
     * @param id the original type id
     * @param category tab icon category
     * @param timeoutSeconds lifetime of the message, which is also the minimum
     * repeat interval for de-duplicated types
     * @param deduplicate if true, a new message is dropped while a live message
     * of the same type (and key) exists
     * @param alwaysAllowed if true, the message passes even when the system
     * messages are switched off (trigger action SET_SYSTEM_MESSAGES)
     */
    private PlayerMessageType(int id, MessageType category, int timeoutSeconds, boolean deduplicate, boolean alwaysAllowed) {
        this.id = id;
        this.category = category;
        this.timeoutSeconds = timeoutSeconds;
        this.deduplicate = deduplicate;
        this.alwaysAllowed = alwaysAllowed;
    }

    public int getId() {
        return id;
    }

    public MessageType getCategory() {
        return category;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public boolean isDeduplicated() {
        return deduplicate;
    }

    public boolean isAlwaysAllowed() {
        return alwaysAllowed;
    }
}
