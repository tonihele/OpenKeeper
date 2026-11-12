/*
 * Copyright (C) 2014-2017 OpenKeeper
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
package toniarts.openkeeper.tools.modelviewer;

import com.jme3.asset.AssetManager;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.ArrayList;
import java.util.List;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.view.fogofwar.IFogOfWarInformation;
import toniarts.openkeeper.view.map.RoomFactory;
import toniarts.openkeeper.view.loader.ILoader;
import toniarts.openkeeper.view.effect.EffectManagerState;

/**
 *
 * @author ArchDemon
 */
public final class RoomsLoader implements ILoader<Room> {

    /** Assemble the Portal exactly as the map renderer does, centered on its pit. */
    public Spatial loadPortalContext(AssetManager assetManager, IKwdFile data) {
        Room portal = data.getRooms().stream()
                .filter(room -> "Portal".equals(room.getName()))
                .findFirst().orElseThrow(() -> new IllegalStateException("No Portal room definition"));
        RoomInstance instance = new RoomInstance(portal);
        for (int y = -1; y <= 1; y++) {
            for (int x = -1; x <= 1; x++) {
                instance.addCoordinate(new Point(x, y));
            }
        }
        // A viewer has no unexplored tiles or gameplay fog state.
        IFogOfWarInformation visible = new IFogOfWarInformation() {
            @Override public boolean isVisible(Point p) { return true; }
            @Override public boolean isExplored(Point p) { return true; }
            @Override public boolean isPerceived(Point p) { return true; }
            @Override public boolean isHighlightable(Point p) { return false; }
            @Override public boolean isPendingTagged(Point p) { return false; }
        };
        Spatial room = RoomFactory.constructRoom(instance, assetManager, data).construct(visible);
        room.setName("Portal room context");
        return room;
    }

    @Override
    public Spatial load(AssetManager assetManager, Room object) {
        //Create a root
        Node root = new Node(object.getName());

        List<ArtResource> resources = new ArrayList<>();
        resources.add(object.getCapResource());
        resources.add(object.getCeilingResource());
        resources.add(object.getCompleteResource());
        //resources.add(object.getEditorIcon());
        //resources.add(object.getGuiIcon());
        resources.add(object.getInsideCornerResource());
        resources.add(object.getOutsideCornerResource());
        resources.add(object.getStraightResource());
        resources.add(object.getTorch());
        resources.add(object.getUnknownResource());
        resources.add(object.getWallResource());

        float height = 1;
        for (ArtResource resource : resources) {
            if (resource == null || resource.getType() == ArtResource.ArtResourceType.NONE) {
                continue;
            }

            Spatial s = UniversalArtResourceLoader.load(assetManager, resource);
            s.move(0, height++, 0);
            root.attachChild(s);
        }

        return root;
    }

    public Spatial load(AssetManager assetManager, EffectManagerState effectManagerState, Room object) {
        Spatial root = load(assetManager, object);

        List<Integer> effects = new ArrayList<>();
        effects.add(object.getFightEffectId());

        float height = 1;
        for (Integer effectId : effects) {
            if (effectId == 0) {
                continue;
            }

            effectManagerState.loadSingleEffect((Node) root, new Vector3f(1, height++, 1), effectId, true);
        }

        return root;
    }
}
