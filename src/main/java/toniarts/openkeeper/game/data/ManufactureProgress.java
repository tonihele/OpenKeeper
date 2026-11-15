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

import java.io.Serial;
import java.io.Serializable;

/**
 * Progress towards manufacturing the current item. Safe to send through the
 * game session to clients that render the progress independently.
 *
 * @param progress work points gathered for the current item
 * @param required work points needed to finish the current item
 */
public record ManufactureProgress(int progress, int required) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final ManufactureProgress IDLE = new ManufactureProgress(0, 0);

    /**
     * @return progress as a fraction from zero to one, or zero when no item is
     * being manufactured
     */
    public float fraction() {
        if (required <= 0) {
            return 0;
        }
        return Math.min(1, Math.max(0, (float) progress / required));
    }
}
