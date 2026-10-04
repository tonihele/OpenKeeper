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
package toniarts.openkeeper.game.data;

/**
 * Player message types, a subset of the original game's 100 entry type table
 */
public enum PlayerMessageType {

    /**
     * Stub for messages that have no counterpart in the original table. Never
     * de-duplicated.
     */
    CUSTOM(-1, Category.INFO, 50, false, false, 0),
    HEART_UNDER_ATTACK(0x01, Category.FIGHT, 120, true, false, 345),
    NEW_SPECIES(0x0D, Category.CREATURE, 50, false, false, 376),
    LEVEL_CREATURE_INTRO(0x0E, Category.INFO, 50, false, true, 0),
    PORTAL_FULL(0x0F, Category.INFO, 420, true, false, 374),
    PORTALS_FULL(0x10, Category.INFO, 420, true, false, 375),
    LEVEL_SPEECH(0x15, Category.INFO, 50, false, true, 0),
    SPELL_RESEARCHED(0x16, Category.INFO, 10, true, false, 391),
    NOTHING_TO_RESEARCH(0x18, Category.INFO, 30, false, false, 395),
    MAGIC_ITEM_FOUND(0x30, Category.INFO, 10, true, true, 486),
    SPECIAL_EFFECT(0x31, Category.INFO, 20, false, true, 0),
    REALM_CONQUERED(0x32, Category.INFO, 50, true, false, 1159),
    PLAYER_DEFEATED(0x33, Category.INFO, 50, true, false, 1160);

    /**
     * Tab icon categories
     */
    public enum Category {
        INFO,
        FIGHT,
        ALLY_MSG,
        PLAYER_EXIT,
        CREATURE
    }

    private final int id;
    private final Category category;
    private final int timeoutSeconds;
    private final boolean deduplicate;
    private final boolean alwaysAllowed;
    private final int speechTextId;

    /**
     * @param id the original type id
     * @param category tab icon category
     * @param timeoutSeconds lifetime of the message, which is also the minimum
     * repeat interval for de-duplicated types
     * @param deduplicate if true, a new message is dropped while a live message
     * of the same type (and key) exists
     * @param alwaysAllowed if true, the message passes even when the system
     * messages are switched off (trigger action SET_SYSTEM_MESSAGES)
     * @param speechTextId the Speech.str index of the message text, or
     * 0 if the text is given by the producer
     */
    private PlayerMessageType(int id, Category category, int timeoutSeconds, boolean deduplicate, boolean alwaysAllowed, int speechTextId) {
        this.id = id;
        this.category = category;
        this.timeoutSeconds = timeoutSeconds;
        this.deduplicate = deduplicate;
        this.alwaysAllowed = alwaysAllowed;
        this.speechTextId = speechTextId;
    }

    /**
     * @param id the original type id
     * @return the type, {@link #CUSTOM} if not known
     */
    public static PlayerMessageType fromId(int id) {
        for (PlayerMessageType type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        return CUSTOM;
    }

    public int getId() {
        return id;
    }

    public Category getCategory() {
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

    /**
     * @return the Speech.str index of the message text, 0 if the producer
     * gives the text
     */
    public int getSpeechTextId() {
        return speechTextId;
    }
}
