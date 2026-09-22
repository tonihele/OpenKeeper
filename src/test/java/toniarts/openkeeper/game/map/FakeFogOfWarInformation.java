/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.game.map;

import java.util.HashSet;
import java.util.Set;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.view.fogofwar.IFogOfWarInformation;

/**
 * An {@link IFogOfWarInformation} test double driven directly by test setup
 * rather than by simulating real fog rules.
 */
final class FakeFogOfWarInformation implements IFogOfWarInformation {

    final Set<Point> explored = new HashSet<>();
    final Set<Point> perceived = new HashSet<>();
    final Set<Point> highlightable = new HashSet<>();

    @Override
    public boolean isVisible(Point p) {
        return isExplored(p) || isPerceived(p);
    }

    @Override
    public boolean isExplored(Point p) {
        return explored.contains(p);
    }

    @Override
    public boolean isPerceived(Point p) {
        return perceived.contains(p);
    }

    @Override
    public boolean isHighlightable(Point p) {
        return highlightable.contains(p);
    }

    @Override
    public boolean isPendingTagged(Point p) {
        return false;
    }

}
