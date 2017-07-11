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
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.component.ObjectEntity;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Spellbook;
import toniarts.openkeeper.game.controller.player.PlayerSpell;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.WorldUtils;
import toniarts.openkeeper.world.MapLoader;

/**
 * This is a controller that controls all the game objects in the world TODO:
 * Hmm, should this be more a factory/loader maybe, or if this offers the
 * ability to load / save, then it is fine
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ObjectsController implements IObjectsController {

    public final static short OBJECT_GOLD_ID = 1;
    //public final static short OBJECT_GOLD_BAG_ID = 2;
    public final static short OBJECT_GOLD_PILE_ID = 3;
    public final static short OBJECT_SPELL_BOOK_ID = 4;

    private KwdFile kwdFile;
    private EntityData entityData;
    private Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings;

    private static final Logger logger = Logger.getLogger(ObjectsController.class.getName());

    public ObjectsController() {
        // For serialization
    }

    /**
     * Load objects from a KWD file straight (new game)
     *
     * @param kwdFile the KWD file
     * @param entityData the entity controller
     */
    public ObjectsController(KwdFile kwdFile, EntityData entityData, Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings) {
        this.kwdFile = kwdFile;
        this.entityData = entityData;
        this.gameSettings = gameSettings;

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
        loadObject(objectThing.getObjectId(), objectThing.getPlayerId(), objectThing.getPosX(), objectThing.getPosY(),
                0, objectThing.getMoneyAmount(), objectThing.getKeeperSpellId(), objectThing.getTriggerId(), null);
    }

    @Override
    public EntityId loadObject(short objectId, short ownerId, int x, int y) {
        return loadObject(objectId, ownerId, x, y, 0, null, null, null, null);
    }

    @Override
    public EntityId loadObject(short objectId, short ownerId, int x, int y, float rotation) {
        return loadObject(objectId, ownerId, x, y, rotation, null, null, null, null);
    }

    @Override
    public EntityId loadObject(short objectId, short ownerId, int x, int y, Integer money, Integer spellId) {
        return loadObject(objectId, ownerId, x, y, 0, money, spellId, null, null);
    }

    private EntityId loadObject(short objectId, short ownerId, int x, int y, float rotation, Integer money, Integer spellId, Integer triggerId, Integer maxMoney) {
        EntityId entity = entityData.createEntity();
        entityData.setComponent(entity, new ObjectEntity(objectId, ownerId));

        // Move to the center of the tile
        Vector3f pos = WorldUtils.pointToVector3f(x, y);
        pos.y = MapLoader.FLOOR_HEIGHT; // FIXME: no
        entityData.setComponent(entity, new Position(rotation, pos));

        // Add additional components
        GameObject obj = kwdFile.getObject(objectId);
        if (obj.getFlags().contains(GameObject.ObjectFlag.OBJECT_TYPE_GOLD)) {
            entityData.setComponent(entity, new Gold(money, (maxMoney == null ? (int) gameSettings.get(Variable.MiscVariable.MiscType.MAX_GOLD_PILE_OUTSIDE_TREASURY).getValue() : maxMoney)));
        }
        if (obj.getFlags().contains(GameObject.ObjectFlag.OBJECT_TYPE_SPELL_BOOK)) {
            entityData.setComponent(entity, new Spellbook(spellId));
        }

        // Trigger
//                    if (objectThing.getTriggerId() != 0) {
//                        objectTriggerState.setThing(objectThing.getTriggerId(), objectControl);
//                    }
        return entity;
    }

    @Override
    public EntityId addRoomGold(short ownerId, int x, int y, int money, int maxMoney) {
        return loadObject(OBJECT_GOLD_PILE_ID, ownerId, x, y, 0, money, null, null, maxMoney);
    }

    @Override
    public EntityId addLooseGold(short ownerId, int x, int y, int money, int maxMoney) {
        return loadObject(OBJECT_GOLD_ID, ownerId, x, y, 0, money, null, null, maxMoney);
    }

    @Override
    public EntityId addRoomSpellBook(short ownerId, int x, int y, PlayerSpell spell) {
        return loadObject(OBJECT_SPELL_BOOK_ID, ownerId, x, y);
    }

    @Override
    public EntityData getEntityData() {
        return entityData;
    }

}
