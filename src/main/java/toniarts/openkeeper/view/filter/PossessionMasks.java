/* Copyright (C) 2026 OpenKeeper. Licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.view.filter;

import java.util.Locale;
import toniarts.openkeeper.tools.convert.map.Creature;

/** View-mask artwork is separate from the creature's gamma-effect selection. */
public final class PossessionMasks {
    private static final String DIRECTORY = "Textures/GUI/Filters/";
    private PossessionMasks() { }

    public static String forCreature(Creature creature) {
        var resource = creature.getFirstPersonFilterResource();
        return resolve(creature.getCreatureId(), resource == null ? null : resource.getName());
    }

    public static String resolve(int creatureId, String authoredName) {
        if (authoredName != null && !authoredName.isBlank()) {
            String name = authoredName.replace('\\', '/');
            if (!name.toLowerCase(Locale.ROOT).endsWith(".png")) name += ".png";
            if (name.regionMatches(true, 0, "Textures/", 0, 9)) return name;
            if (name.regionMatches(true, 0, "GUI/", 0, 4)) return "Textures/" + name;
            return DIRECTORY + name;
        }
        return switch (creatureId) {
            case 12 -> DIRECTORY + "F-FireFly.png";
            case 13 -> DIRECTORY + "F-Knight.png";
            case 22 -> DIRECTORY + "F-Black_Knight.png";
            case 25 -> DIRECTORY + "F-Guard.png";
            default -> null;
        };
    }

}
