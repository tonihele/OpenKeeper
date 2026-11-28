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
package toniarts.openkeeper.view.minimap;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.awt.image.BufferedImage;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.InHand;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.TrapViewState;
import toniarts.openkeeper.game.component.ViewType;
import toniarts.openkeeper.game.map.FakeFogOfWarInformation;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinimapMarkerPainterTest {

    private static final short VIEWER_ID = 3;
    private static final short GUARD_POST_ID = 11;
    private static final short ALARM_ID = 13;

    private EntityData entityData;
    private MinimapMarkerPainter painter;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        BufferedImage paletteImage = new BufferedImage(64, 16, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 64; x++) {
                paletteImage.setRGB(x, y, 0x00FF00);
            }
        }
        painter = new MinimapMarkerPainter(entityData, MinimapPalette.load(paletteImage), VIEWER_ID,
                Set.of(GUARD_POST_ID), 7);
    }

    @AfterEach
    void tearDown() {
        painter.dispose();
        entityData.close();
    }

    @Test
    void ownCreatureInHandIsNotPainted() {
        EntityId creature = entityData.createEntity();
        entityData.setComponents(creature,
                new Position(0, new Vector3f(5, 0, 5)),
                new Owner(VIEWER_ID, VIEWER_ID),
                new CreatureComponent());

        painter.update();
        assertTrue(paintsAnything());

        entityData.setComponent(creature, new InHand(0, VIEWER_ID, ViewType.CREATURE, (short) 1, 0));
        painter.update();
        assertFalse(paintsAnything());

        entityData.removeComponent(creature, InHand.class);
        painter.update();
        assertTrue(paintsAnything());
    }

    @Test
    void hiddenEnemyTrapIsNotPaintedUntilRevealed() {
        short enemyId = 4;
        EntityId trap = entityData.createEntity();
        entityData.setComponents(trap,
                new Position(0, new Vector3f(5, 0, 5)),
                new Owner(enemyId, enemyId),
                new TrapViewState((short) 1, false, true));
        FakeFogOfWarInformation fogOfWar = new FakeFogOfWarInformation();
        fogOfWar.explored.add(new Point(5, 5));

        painter.update();
        assertFalse(paintsAnything(fogOfWar));

        entityData.setComponent(trap, new TrapViewState((short) 1, false, false));
        painter.update();
        assertTrue(paintsAnything(fogOfWar));
    }

    @Test
    void ownHiddenTrapIsPainted() {
        EntityId trap = entityData.createEntity();
        entityData.setComponents(trap,
                new Position(0, new Vector3f(5, 0, 5)),
                new Owner(VIEWER_ID, VIEWER_ID),
                new TrapViewState((short) 1, false, true));

        painter.update();
        assertTrue(paintsAnything());
    }

    private boolean paintsAnything() {
        return paintsAnything(new FakeFogOfWarInformation());
    }

    @Test
    void ownGuardPostGetsItsReportingDistanceRing() {
        EntityId trap = entityData.createEntity();
        entityData.setComponents(trap,
                new Position(0, new Vector3f(8, 0, 8)),
                new Owner(VIEWER_ID, VIEWER_ID),
                new TrapViewState(ALARM_ID, false));
        FakeFogOfWarInformation fogOfWar = new FakeFogOfWarInformation();
        painter.update();
        int trapDotOnly = paintedBytes(fogOfWar);

        entityData.setComponent(trap, new TrapViewState(GUARD_POST_ID, true));
        painter.update();
        assertEquals(trapDotOnly, paintedBytes(fogOfWar)); // a blueprint doesn't report yet

        entityData.setComponent(trap, new TrapViewState(GUARD_POST_ID, false));
        painter.update();
        assertTrue(paintedBytes(fogOfWar) > trapDotOnly);

        fogOfWar.explored.add(new Point(8, 8));
        entityData.setComponent(trap, new Owner((short) (VIEWER_ID + 1), (short) (VIEWER_ID + 1)));
        painter.update();
        assertEquals(trapDotOnly, paintedBytes(fogOfWar)); // another keeper's guard post, only its dot
    }

    private boolean paintsAnything(FakeFogOfWarInformation fogOfWar) {
        return paintedBytes(fogOfWar) > 0;
    }

    private int paintedBytes(FakeFogOfWarInformation fogOfWar) {
        byte[] raster = new byte[MinimapRasteriser.RASTER_SIZE * MinimapRasteriser.RASTER_SIZE * 3];
        painter.paint(raster, 16, 16, -1, 0, 0, fogOfWar, true, 0, 0, null, 0, null);
        int painted = 0;
        for (byte b : raster) {
            if (b != 0) {
                painted++;
            }
        }
        return painted;
    }
}
