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
package toniarts.openkeeper.game.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * What the workshop manufactures next. Every blueprint is an order for a
 * crate of its type, the orders are worked on in the order the blueprints were
 * placed. A crate that already exists, in the workshop or on its way,
 * fulfills the oldest open order of its type.
 */
public final class ManufactureOrders {

    private ManufactureOrders() {
        // Nope
    }

    /**
     * The next type to manufacture
     *
     * @param blueprints the type of each blueprint, in the order they were
     * placed
     * @param crates the number of existing crates by type
     * @return the type of the oldest blueprint without a crate, {@code null}
     * if every blueprint has one
     */
    public static Short next(List<Short> blueprints, Map<Short, Integer> crates) {
        Map<Short, Integer> unassignedCrates = new HashMap<>(crates);
        for (Short type : blueprints) {
            int available = unassignedCrates.getOrDefault(type, 0);
            if (available > 0) {
                unassignedCrates.put(type, available - 1);
            } else {
                return type;
            }
        }
        return null;
    }

}
