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
package toniarts.openkeeper.view.map;

import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class CeilingVisibilityTest {

    @Test
    void hidingAndShowingCeilingOnlyChangesCullingAndPreservesGeometry() {
        Node map = new Node("map");
        Node ceiling = new Node("ceiling");
        Geometry ceilingPatch = new Geometry("ceiling patch", new Box(1f, 0.1f, 1f));
        ceiling.attachChild(ceilingPatch);
        map.attachChild(ceiling);

        MapViewController.applyCeilingVisibility(ceiling, false);

        assertEquals(Spatial.CullHint.Always, ceiling.getCullHint());
        assertSame(map, ceiling.getParent());
        assertSame(ceilingPatch, ceiling.getChild(0));
        assertSame(ceiling, ceilingPatch.getParent());

        MapViewController.applyCeilingVisibility(ceiling, true);

        assertEquals(Spatial.CullHint.Dynamic, ceiling.getCullHint());
        assertSame(map, ceiling.getParent());
        assertSame(ceilingPatch, ceiling.getChild(0));
    }
}
