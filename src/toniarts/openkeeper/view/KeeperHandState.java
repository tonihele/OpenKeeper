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

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.input.InputManager;
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
import com.simsilica.es.Entity;
import com.simsilica.es.EntityContainer;
import com.simsilica.es.EntityData;
import com.simsilica.es.filter.FieldFilter;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.component.InHand;
import static toniarts.openkeeper.tools.convert.AssetsConverter.TEXTURES_FOLDER;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.ArtResource.ArtResourceType;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.view.animation.AnimationLoader;
import toniarts.openkeeper.view.control.CreatureViewControl;
import toniarts.openkeeper.view.control.IEntityViewControl;
import toniarts.openkeeper.view.control.ObjectViewControl;

/**
 * TODO I think we need to move cursor here
 *
 * @author ArchDemon
 */
public abstract class KeeperHandState extends AbstractAppState {

    private static final List<String> SLAP_SOUNDS = Arrays.asList(new String[]{
        "/Global/GuiHD/Slap_1.mp2",
        "/Global/GuiHD/slap_2.mp2",
        "/Global/GuiHD/Slap_3.mp2",
        "/Global/GuiHD/Slap_4.mp2"
    });
    private static final String MORE_CREATURES = "GUI/Creatures/more_creatures";

    //FIXME: 8 in original but two lines by 4
    private static final int CURSOR_ITEMS_INLINE = 4;
    private static final int CURSOR_ITEMS_LINES = 2;
    private static final int CURSOR_ITEM_SIZE = 32;

    private Main app;
    private AssetManager assetManager;
    private AppStateManager stateManager;
    private InputManager inputManager;
    private final List<KeeperHandItem> queue;
    private final int maxQueueSize;
    private final KwdFile kwdFile;
    private final EntityData entityData;
    private final short playerId;
    private IEntityViewControl currentItem;
    private final Node queueNode;
    private final Node cursor;
    private final Node rootNode;
    private final InHandLoaderCreatureModelContainer inHandLoader;

    public KeeperHandState(int maxQueueSize, KwdFile kwdFile, EntityData entityData, short playerId) {
        this.queue = new ArrayList<>(maxQueueSize);
        this.maxQueueSize = maxQueueSize;
        this.kwdFile = kwdFile;
        this.entityData = entityData;
        this.playerId = playerId;

        rootNode = new Node("Keeper hand");

        queueNode = new Node("Queue");
        queueNode.setLocalScale(500);
        Quaternion rotation = new Quaternion();
        rotation.fromAngleAxis(-FastMath.QUARTER_PI, new Vector3f(-1, 1, 0));
        queueNode.setLocalRotation(rotation);
        queueNode.addLight(new AmbientLight(ColorRGBA.White));

        cursor = new Node("Cursor");
        cursor.setLocalTranslation(75, 0, 0);

        rootNode.attachChild(queueNode);
        rootNode.attachChild(cursor);

        // Create the model "listener"
        inHandLoader = new InHandLoaderCreatureModelContainer(entityData);
    }

    @Override
    public void initialize(final AppStateManager stateManager, final Application app) {
        super.initialize(stateManager, app);

        this.app = (Main) app;
        this.assetManager = this.app.getAssetManager();
        this.stateManager = this.app.getStateManager();
        this.inputManager = this.app.getInputManager();

        this.app.getGuiNode().attachChild(rootNode);

        // Start loading stuff (maybe we should do this earlier...)
        inHandLoader.start();
    }

    @Override
    public void update(float tpf) {
        if (inHandLoader.update()) {
            updateHand();
            updateCursor();
        }
    }

    @Override
    public void cleanup() {
        inHandLoader.stop();
        queue.clear();
        app.getGuiNode().detachChild(rootNode);

        super.cleanup();
    }

    public static String getSlapSound() {
        return Utils.getRandomItem(KeeperHandState.SLAP_SOUNDS);
    }

    public IEntityViewControl getItem() {
        return currentItem;
    }

    private KeeperHandItem addToHand(IEntityViewControl item, int index) {
        KeeperHandItem handItem = new KeeperHandItem();
        handItem.index = index;
        handItem.item = item;
        handItem.picture = getIcon(item.getInHandIcon());
        queue.add(handItem);

        return handItem;
    }

    private void removeFromHand(KeeperHandItem item) {
        queue.remove(item);
    }

    public boolean isFull() {
        return queue.size() == maxQueueSize;
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public void setVisible(boolean visible) {
        rootNode.setCullHint(visible ? CullHint.Never : CullHint.Always);
    }

    public void setPosition(float x, float y) {
        rootNode.setLocalTranslation(x, y, 0);
    }

    private Picture getIcon(final ArtResource image) {
        if (image.getType() != ArtResourceType.ALPHA
                && image.getType() != ArtResourceType.ADDITIVE_ALPHA
                && image.getType() != ArtResourceType.SPRITE) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        return getIcon(image.getName());
    }

    private Picture getIcon(String filename) {
        //FIXME if filename is null what todo ?
        final String name = ConversionUtils.getCanonicalAssetKey(TEXTURES_FOLDER + File.separator + filename + ".png");

        Texture tex = assetManager.loadTexture(name);

        Picture picture = new Picture("cursor");
        picture.setTexture(assetManager, (Texture2D) tex, true);
        picture.setWidth(CURSOR_ITEM_SIZE);
        picture.setHeight(CURSOR_ITEM_SIZE);

        return picture;
    }

    private void updateHand() {

        // While it is fairly certain that we always have an intact situation on the client...
        // wise not to bet on it, as really the amount of data is so insignifigant that any optimization doesn't give much
        queueNode.detachAllChildren();
        cursor.detachAllChildren();
        currentItem = null;

        Collections.sort(queue);

        // Set the current item (the next out)
        if (!queue.isEmpty()) {
            currentItem = queue.get(0).item;
        } else {
            return;
        }

        // Add the icons
        for (int i = 0; i < Math.min(CURSOR_ITEMS_INLINE * CURSOR_ITEMS_LINES + 1, queue.size()); i++) {
            Spatial child;
            if (i == CURSOR_ITEMS_INLINE * CURSOR_ITEMS_LINES) {
                child = getIcon(MORE_CREATURES);
            } else {
                child = queue.get(i).picture;
            }
            int y = i / CURSOR_ITEMS_INLINE;
            if (y == CURSOR_ITEMS_LINES) {
                y = CURSOR_ITEMS_LINES - 1;
            }
            int x = i - y * CURSOR_ITEMS_INLINE;
            child.setLocalTranslation(CURSOR_ITEM_SIZE * x, -CURSOR_ITEM_SIZE * y, 0);
            cursor.attachChild(child);
        }

        // Set the actual current object graphics
        if (currentItem.getInHandMesh() != null) {

            // Attach to GUI queueNode and play the animation
            Node itemNode = new Node("CurrentItem");
            currentItem.setSpatial(null);
            itemNode.addControl(currentItem);
            itemNode.setLocalTranslation(0, 0, 0);
            itemNode.setLocalRotation(Matrix3f.ZERO);
            queueNode.attachChild(itemNode);
            AnimationLoader.playAnimation(itemNode, currentItem.getInHandMesh(), assetManager);
        }
    }

    protected abstract void updateCursor();

    private class InHandLoaderCreatureModelContainer extends EntityContainer<KeeperHandItem> {

        public InHandLoaderCreatureModelContainer(EntityData entityData) {
            super(entityData, new FieldFilter(InHand.class, "playerId", playerId), InHand.class);
        }

        @Override
        protected KeeperHandItem addObject(Entity e) {
            IEntityViewControl viewControl;
            InHand inHand = entityData.getComponent(e.getId(), InHand.class);
            switch (inHand.type) {
                case CREATURE: {
                    viewControl = new CreatureViewControl(e.getId(), entityData, kwdFile.getCreature(inHand.id), Creature.AnimationType.IN_HAND, assetManager, null);
                    break;
                }
                case OBJECT: {
                    viewControl = new ObjectViewControl(e.getId(), entityData, kwdFile.getObject(inHand.id), GameObject.State.BEING_DROPPED, assetManager, null);
                    break;
                }
                default:
                    throw new UnsupportedOperationException("Type " + inHand.type + " can't be picked up yet!");
            }

            return addToHand(viewControl, inHand.index);
        }

        @Override
        protected void updateObject(KeeperHandItem object, Entity e) {

            // Only support index changes (these should not happen)
            InHand inHand = entityData.getComponent(e.getId(), InHand.class);
            object.index = inHand.index;
        }

        @Override
        protected void removeObject(KeeperHandItem object, Entity e) {
            removeFromHand(object);
        }

    }

    private class KeeperHandItem implements Comparable<KeeperHandItem> {

        private int index;
        private IEntityViewControl item;
        private Picture picture;

        @Override
        public int compareTo(KeeperHandItem o) {
            return Integer.compare(index, o.index);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 79 * hash + Objects.hashCode(this.item);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final KeeperHandItem other = (KeeperHandItem) obj;
            if (!Objects.equals(this.item, other.item)) {
                return false;
            }
            return true;
        }

    }
}
