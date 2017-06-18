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
package toniarts.openkeeper.game.controller;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.game.entity.Gold;
import toniarts.openkeeper.game.entity.ObjectEntity;
import toniarts.openkeeper.game.entity.Position;
import toniarts.openkeeper.game.entity.Spellbook;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.utils.WorldUtils;
import toniarts.openkeeper.world.MapLoader;

/**
 * This is a controller that controls all the game objects in the world
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ObjectController {

    public final static short OBJECT_GOLD_ID = 1;
    //public final static short OBJECT_GOLD_BAG_ID = 2;
    public final static short OBJECT_GOLD_PILE_ID = 3;
    public final static short OBJECT_SPELL_BOOK_ID = 4;

    private KwdFile kwdFile;
    private EntityData entityData;

    private static final Logger logger = Logger.getLogger(ObjectController.class.getName());

    public ObjectController() {
        // For serialization
    }

    /**
     * Load objects from a KWD file straight (new game)
     *
     * @param kwdFile the KWD file
     * @param entityData the entity controller
     */
    public ObjectController(KwdFile kwdFile, EntityData entityData) {
        this.kwdFile = kwdFile;
        this.entityData = entityData;

        // Load objects
        loadObjects();
    }

    private void loadObjects() {
        for (toniarts.openkeeper.tools.convert.map.Thing obj : kwdFile.getThings()) {
            try {
                if (obj instanceof Thing.Object) {

                    Thing.Object objectThing = (Thing.Object) obj;
                    loadObject(objectThing);
                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Could not load Thing.", ex);
            }
        }
    }

    private void loadObject(Thing.Object objectThing) {
        EntityId entity = entityData.createEntity();
        entityData.setComponent(entity, new ObjectEntity(objectThing.getObjectId(), objectThing.getPlayerId()));

        // Move to the center of the tile
        Vector3f pos = WorldUtils.pointToVector3f(objectThing.getPosX(), objectThing.getPosY());
        pos.y = MapLoader.FLOOR_HEIGHT; // FIXME: no
        entityData.setComponent(entity, new Position(0, pos));

        // Add additional components
        GameObject obj = kwdFile.getObject(objectThing.getObjectId());
        if (obj.getFlags().contains(GameObject.ObjectFlag.OBJECT_TYPE_GOLD)) {
            // FIXME: max money
            entityData.setComponent(entity, new Gold(objectThing.getMoneyAmount(), 1000));
        }
        if (obj.getFlags().contains(GameObject.ObjectFlag.OBJECT_TYPE_SPELL_BOOK)) {
            entityData.setComponent(entity, new Spellbook(objectThing.getKeeperSpellId()));
        }

        // Trigger
//                    if (objectThing.getTriggerId() != 0) {
//                        objectTriggerState.setThing(objectThing.getTriggerId(), objectControl);
//                    }
    }

}
