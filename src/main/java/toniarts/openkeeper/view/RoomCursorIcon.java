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
package toniarts.openkeeper.view;

import com.jme3.asset.AssetManager;
import com.jme3.math.Vector2f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.ui.Picture;

/**
 * The small icon next to the hand while placing a room, as in the original:
 * the room placement icon, crossed out by the no go sign while the selection
 * can't be built. The original loads both from gui\cursors.
 */
public final class RoomCursorIcon {

    private static final String PLACE_ROOM = "Textures/GUI/Cursors/I-Place_Room.png";
    private static final String NO_GO = "Textures/GUI/Cursors/I-No_Go.png";
    private static final int SIZE = 32;

    /**
     * From the click point of the hand to the icon, the hand reaches 68 pixels
     * to the right of it
     */
    private static final float OFFSET_X = 68;
    private static final float OFFSET_Y = -SIZE / 2f;

    private final Node node = new Node("RoomCursorIcon");
    private final Picture noGo;

    public RoomCursorIcon(AssetManager assetManager, Node guiNode) {
        node.attachChild(createPicture(assetManager, PLACE_ROOM));
        noGo = createPicture(assetManager, NO_GO);
        node.attachChild(noGo);
        node.setCullHint(CullHint.Always);
        guiNode.attachChild(node);
    }

    private static Picture createPicture(AssetManager assetManager, String texture) {
        Picture picture = new Picture(texture);
        picture.setImage(assetManager, texture, true);
        picture.setWidth(SIZE);
        picture.setHeight(SIZE);
        return picture;
    }

    /**
     * Shows or hides the icon next to the cursor
     *
     * @param cursorPosition the cursor on the screen
     * @param visible whether a room is being placed on the map
     * @param buildable whether the selection can be built
     */
    public void update(Vector2f cursorPosition, boolean visible, boolean buildable) {
        if (!visible) {
            node.setCullHint(CullHint.Always);
            return;
        }

        node.setLocalTranslation(cursorPosition.x + OFFSET_X, cursorPosition.y + OFFSET_Y, 0);
        noGo.setCullHint(buildable ? CullHint.Always : CullHint.Inherit);
        node.setCullHint(CullHint.Inherit);
    }

    public void cleanup() {
        node.removeFromParent();
    }

}
