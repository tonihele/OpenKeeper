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
package toniarts.openkeeper.game.controller.room;

import java.util.ArrayList;
import java.util.List;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.tools.convert.map.Variable.SacrificeType;

/**
 * The sacrifices made in one temple. Like in DK II, the temple remembers the
 * last three things sacrificed to it. After each sacrifice the recipes are
 * tried in the level's order, and a recipe matches when its items are the
 * latest sacrifices in the same order. A match starts the sacrifices over;
 * anything else is kept for a later recipe.
 */
public final class SacrificeHistory {

    public static final int MAX_ITEMS = 3;

    private final List<Offering> offerings = new ArrayList<>(MAX_ITEMS);

    /**
     * A matched recipe
     *
     * @param recipe the recipe
     * @param level the level a rewarded creature gets: the average level of the
     * sacrificed creatures
     */
    public record Match(Variable.Sacrifice recipe, int level) {

    }

    private record Offering(SacrificeType type, short id, int level) {

    }

    /**
     * Makes a sacrifice
     *
     * @param type what is sacrificed
     * @param id the creature, object or keeper spell ID
     * @param level the creature level, if a creature
     * @param recipes the level's sacrifice recipes, in their original order
     * @return the matched recipe, or {@code null} if none matched
     */
    public Match offer(SacrificeType type, short id, int level, List<Variable.Sacrifice> recipes) {
        if (offerings.size() == MAX_ITEMS) {
            offerings.removeFirst();
        }
        offerings.add(new Offering(type, id, level));

        for (Variable.Sacrifice recipe : recipes) {
            Match match = match(recipe);
            if (match != null) {
                offerings.clear();
                return match;
            }
        }

        return null;
    }

    /**
     * Get the number of sacrifices remembered
     *
     * @return the sacrifice count
     */
    public int size() {
        return offerings.size();
    }

    private Match match(Variable.Sacrifice recipe) {
        List<Offering> items = getItems(recipe);
        if (items.isEmpty() || items.size() > offerings.size()) {
            return null;
        }

        int start = offerings.size() - items.size();
        int creatures = 0;
        int levels = 0;
        for (int i = 0; i < items.size(); i++) {
            Offering item = items.get(i);
            Offering offering = offerings.get(start + i);
            if (item.type() != offering.type() || item.id() != offering.id()) {
                return null;
            }
            if (offering.type() == SacrificeType.CREATURE) {
                creatures++;
                levels += offering.level();
            }
        }

        return new Match(recipe, creatures > 0 ? Math.max(1, levels / creatures) : 1);
    }

    private static List<Offering> getItems(Variable.Sacrifice recipe) {
        List<Offering> items = new ArrayList<>(MAX_ITEMS);
        addItem(items, recipe.getType1(), recipe.getId1());
        addItem(items, recipe.getType2(), recipe.getId2());
        addItem(items, recipe.getType3(), recipe.getId3());
        return items;
    }

    private static void addItem(List<Offering> items, SacrificeType type, short id) {
        if (type != null && type != SacrificeType.NONE) {
            items.add(new Offering(type, id, 0));
        }
    }

}
