/*
 * Copyright (C) 2014-2016 OpenKeeper
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
import com.jme3.light.AmbientLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import static toniarts.openkeeper.tools.convert.AssetsConverter.TEXTURES_FOLDER;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.ArtResource.ArtResourceType;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.world.control.IInteractiveControl;
import toniarts.openkeeper.world.creature.CreatureLoader;

/**
 * TODO I think we need to move cursor here
 *
 * @author ArchDemon
 */
public class KeeperHand {

    private static final List<String> SLAP_SOUNDS = Arrays.asList(new String[]{"/Global/Slap_1.mp2",
        "/Global/slap_2.mp2", "/Global/Slap_3.mp2", "/Global/Slap_4.mp2"});
    private static final String MORE_CREATURES = "GUI/Creatures/more_creatures";

    //FIXME: 8 in original but two lines by 4
    private static final int CURSOR_ITEMS_INLINE = 4;
    private static final int CURSOR_ITEMS_LINES = 2;
    private static final int CURSOR_ITEM_SIZE = 32;

    private final AssetManager assetManager;
    private final KeeperHandQueue queue;
    private IInteractiveControl item;
    private final Node queueNode;
    private final Node cursor;
    private final Node node;

    public KeeperHand(final AssetManager assetManager, int queueSize) {
        this.assetManager = assetManager;
        queue = new KeeperHandQueue(queueSize);

        node = new Node("Keeper hand");

        queueNode = new Node("Queue");
        queueNode.setLocalScale(500);
        Quaternion rotation = new Quaternion();
        rotation.fromAngleAxis(-FastMath.QUARTER_PI, new Vector3f(-1, 1, 0));
        queueNode.setLocalRotation(rotation);
        queueNode.addLight(new AmbientLight(ColorRGBA.White));

        cursor = new Node("Cursor");
        cursor.setLocalTranslation(75, 0, 0);

        node.attachChild(queueNode);
        node.attachChild(cursor);
    }

    public static String getSlapSound() {
        return Utils.getRandomItem(KeeperHand.SLAP_SOUNDS);
    }

    public Node getNode() {
        return node;
    }

    public IInteractiveControl getItem() {
        return item;
    }

    public IInteractiveControl peek() {
        return queue.peek();
    }

    public void push(IInteractiveControl item) {
        queue.push(item);

        addItem(item);
        addIcon(item.getInHandIcon(), 0);

        if (queue.getCount() >= CURSOR_ITEMS_INLINE * CURSOR_ITEMS_LINES + 1) {
            cursor.getChildren().get(CURSOR_ITEMS_INLINE * CURSOR_ITEMS_LINES).removeFromParent();
            if (queue.getCount() == CURSOR_ITEMS_INLINE * CURSOR_ITEMS_LINES + 1) {
                addIcon(MORE_CREATURES, CURSOR_ITEMS_INLINE * CURSOR_ITEMS_LINES);
            }
        }

        moveIcons();
    }

    public IInteractiveControl pop() {
        IInteractiveControl result = queue.pop();

        removeItem();
        removeIcon();

        if (queue.getCount() >= CURSOR_ITEMS_INLINE * CURSOR_ITEMS_LINES) {
            addIcon(queue.peek(CURSOR_ITEMS_INLINE * CURSOR_ITEMS_LINES - 1).getInHandIcon(), CURSOR_ITEMS_INLINE * CURSOR_ITEMS_LINES - 1);
        }

        moveIcons();

        if (!queue.isEmpty()) {
            addItem(queue.peek());
        }

        return result;
    }

    public boolean isFull() {
        return queue.isFull();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public void setVisible(boolean visible) {
        node.setCullHint(visible ? CullHint.Never : CullHint.Always);
    }

    public void setPosition(float x, float y) {
        node.setLocalTranslation(x, y, 0);
    }

    private void addItem(IInteractiveControl interactiveControl) {

        // Remove the old one
        if (item != null) {
            removeItem();
        }

        // Set the item
        item = interactiveControl;
        if (item.getInHandMesh() != null) {

            // Attach to GUI queueNode and play the animation
            item.getSpatial().setLocalTranslation(0, 0, 0);
            item.getSpatial().setLocalRotation(Matrix3f.ZERO);
            queueNode.attachChild(item.getSpatial());
            CreatureLoader.playAnimation(item.getSpatial(), item.getInHandMesh(), assetManager);
        }
    }

    private void removeItem() {
        if (item != null && item.getInHandMesh() != null) {

            // Remove from GUI queueNode
            item.getSpatial().removeFromParent();
        }
        item = null;
    }

    private void removeIcon() {

        if (cursor.getQuantity() > 0) {
            cursor.getChildren().get(0).removeFromParent();
            if (queue.getCount() == CURSOR_ITEMS_INLINE * CURSOR_ITEMS_LINES) {
                cursor.getChildren().get(CURSOR_ITEMS_INLINE * CURSOR_ITEMS_LINES - 1).removeFromParent();
            }
        }
    }

    private void addIcon(final ArtResource image, final int index) {
        if (image.getType() != ArtResourceType.ALPHA
                || image.getType() != ArtResourceType.ADDITIVE_ALPHA
                || image.getType() != ArtResourceType.SPRITE) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        addIcon(image.getName(), index);
    }

    private void addIcon(String filename, int index) {
        //FIXME if filename is null what todo ?
        final String name = ConversionUtils.getCanonicalAssetKey(TEXTURES_FOLDER + File.separator + filename + ".png");

        Texture tex = assetManager.loadTexture(name);

        Picture picture = new Picture("cursor");
        picture.setTexture(assetManager, (Texture2D) tex, true);
        picture.setWidth(CURSOR_ITEM_SIZE);
        picture.setHeight(CURSOR_ITEM_SIZE);

        cursor.attachChildAt(picture, index);
    }

    private void moveIcons() {
        for (int i = 0; i < cursor.getQuantity(); i++) {
            Spatial child = cursor.getChild(i);
            int y = i / CURSOR_ITEMS_INLINE;
            if (y == CURSOR_ITEMS_LINES) {
                y = CURSOR_ITEMS_LINES - 1;
            }
            int x = i - y * CURSOR_ITEMS_INLINE;
            child.setLocalTranslation(CURSOR_ITEM_SIZE * x, -CURSOR_ITEM_SIZE * y, 0);
        }
    }
}
