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

import java.util.HashMap;
import java.util.Map;

/**
 * The variants of a player message type, see player_message_queue.md §3.1.
 * Each key (room type, spell...) has its own text and timeout, unknown keys get
 * the default entry. If there is no default, the message is dropped.
 *
 * @author ufdada
 */
public final class PlayerMessageSubTable {

    /**
     * A resolved message
     *
     * @param speechTextId the Speech.str index of the text, 0 if the producer
     * gives the text
     * @param timeoutSeconds lifetime of the message
     */
    public record Entry(int speechTextId, int timeoutSeconds) {

    }

    private final Map<Integer, Entry> entries = new HashMap<>();
    private final Entry defaultEntry;

    private PlayerMessageSubTable(Entry defaultEntry) {
        this.defaultEntry = defaultEntry;
    }

    /**
     * @param defaultText the text for unknown keys, 0 to drop the message
     * @param seconds the timeout of all the records, including the default
     * @param keyTextPairs key and Speech.str index pairs
     * @return the table
     */
    static PlayerMessageSubTable of(int defaultText, int seconds, int... keyTextPairs) {
        PlayerMessageSubTable table = new PlayerMessageSubTable(defaultText != 0 ? new Entry(defaultText, seconds) : null);
        for (int i = 0; i < keyTextPairs.length; i += 2) {
            table.entries.put(keyTextPairs[i], new Entry(keyTextPairs[i + 1], seconds));
        }
        return table;
    }

    /**
     * Add a record that has a timeout of its own
     */
    PlayerMessageSubTable with(int key, int text, int seconds) {
        entries.put(key, new Entry(text, seconds));
        return this;
    }

    /**
     * @param key the variant
     * @return the entry for the key, the default if the key is not known, or
     * {@code null} if there is no default either
     */
    public Entry get(int key) {
        return entries.getOrDefault(key, defaultEntry);
    }
}
