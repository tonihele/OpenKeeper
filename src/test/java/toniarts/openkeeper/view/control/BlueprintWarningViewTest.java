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
package toniarts.openkeeper.view.control;

import com.jme3.asset.DesktopAssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.simsilica.es.base.DefaultEntityData;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.*;
import toniarts.openkeeper.tools.convert.map.*;
import toniarts.openkeeper.utils.AssetUtils;
import static org.junit.jupiter.api.Assertions.*;
import static toniarts.openkeeper.view.map.MapViewController.COLOR_FLASH;

class BlueprintWarningViewTest {
    @Test void bothBlueprintTypesUseRedAndBlueAndRestoreTheirBuiltMaterials() {
        for (boolean door : new boolean[]{false, true}) {
            Fixture f = new Fixture(door);
            f.warning(true);
            assertEquals(COLOR_FLASH, f.color());
            f.warning(false);
            assertEquals(new ColorRGBA(0, 0, .8f, .4f), f.color());
            f.root.setCullHint(Spatial.CullHint.Always); // No sound backend in this fixture.
            f.built();
            assertSame(f.original, f.geometry.getMaterial());
            f.warning(true);
            assertSame(f.original, f.geometry.getMaterial(), "A stale warning cannot recolour a built entity");
        }
    }

    @Test void hoverOverridesBlueAndEndingHoverRestoresTheCurrentWarningPhase() {
        Fixture f = new Fixture(false);
        f.warning(false);
        f.control.onHoverStart((short) 3);
        assertEquals(COLOR_FLASH, f.color());
        f.control.onHoverEnd((short) 3);
        assertEquals(new ColorRGBA(0, 0, .8f, .4f), f.color());
        f.warning(true);
        f.control.onHoverStart((short) 3);
        f.control.onHoverEnd((short) 3);
        assertEquals(COLOR_FLASH, f.color());
        f.entities.removeComponent(f.id, BlueprintWarning.class);
        f.control.update(0);
        assertEquals(new ColorRGBA(0, 0, .8f, .4f), f.color());
    }

    @Test void materialsAreIndependentAndNonRemovableAttachmentsStayUnchanged() {
        Fixture f = new Fixture(true);
        Geometry attachment = new Geometry("lock", new Box(1, 1, 1));
        attachment.setMaterial(f.original);
        attachment.setUserData(AssetUtils.USER_DATA_KEY_REMOVABLE, false);
        f.root.attachChild(attachment);
        Fixture other = new Fixture(false);
        f.warning(true);
        assertEquals(COLOR_FLASH, f.color());
        assertEquals(new ColorRGBA(0, 0, .8f, .4f), other.color());
        assertSame(f.original, attachment.getMaterial());
        assertEquals(ColorRGBA.White, f.original.getParam("Color").getValue());
        assertTrue(new TrapViewState((short) 1, true).isShownTo((short) 3, (short) 3));
        assertFalse(new TrapViewState((short) 1, true).isShownTo((short) 3, (short) 4));
    }

    private static class Fixture {
        final DefaultEntityData entities = new DefaultEntityData();
        final com.simsilica.es.EntityId id = entities.createEntity();
        final Node root = new Node();
        final Geometry geometry = new Geometry("synthetic", new Box(1, 1, 1));
        final Material original;
        final EntityViewControl control;
        final boolean door;
        Fixture(boolean door) {
            this.door = door;
            var assets = new DesktopAssetManager(true);
            original = new Material(assets, "Common/MatDefs/Misc/Unshaded.j3md");
            original.setColor("Color", ColorRGBA.White.clone());
            geometry.setMaterial(original); root.attachChild(geometry);
            entities.setComponents(id, new Owner((short) 3, (short) 3), new Interaction(true, false, false, false, false));
            control = door ? new DoorViewControl(id, entities,
                    DoorFixtures.door((short) 1, 0, Door.DoorFlag.IS_BARRICADE),
                    new DoorViewState((short) 1, false, true, false), assets, null, null)
                    : new TrapViewControl(id, entities, new Trap(), new TrapViewState((short) 1, true), assets, null);
            root.addControl(control);
        }
        void warning(boolean red) {
            entities.setComponent(id, new BlueprintWarning(red)); control.update(0);
        }
        void built() {
            if (door) ((DoorViewControl) control).setTargetState(new DoorViewState((short) 1, false, false, false));
            else ((TrapViewControl) control).setTargetState(new TrapViewState((short) 1, false));
        }
        ColorRGBA color() { return (ColorRGBA) geometry.getMaterial().getParam("Color").getValue(); }
    }
}
