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
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.game.component.ChickenAi;
import toniarts.openkeeper.game.component.Decay;
import toniarts.openkeeper.game.component.Food;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Interaction;
import toniarts.openkeeper.game.component.Mobile;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.ObjectViewState;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Spellbook;
import toniarts.openkeeper.game.component.Trigger;
import toniarts.openkeeper.game.controller.chicken.ChickenController;
import toniarts.openkeeper.game.controller.chicken.ChickenState;
import toniarts.openkeeper.game.controller.chicken.IChickenController;
import toniarts.openkeeper.game.controller.object.IObjectController;
import toniarts.openkeeper.game.controller.object.ObjectController;
import toniarts.openkeeper.game.controller.room.FiveByFiveRotatedController;
import static toniarts.openkeeper.game.controller.room.FiveByFiveRotatedController.OBJECT_HEART_ID;
import toniarts.openkeeper.game.controller.room.TempleController;
import toniarts.openkeeper.game.data.PlayerSpell;
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
    public final static short OBJECT_CHICKEN_ID = 9;
    public final static short OBJECT_EGG_ID = 47;

    /**
     * Some objects have these die over time, I'm not sure what variable governs
     * these so.. here we go for now
     */
    public static final double OBJECT_TIME_TO_LIVE = 60;

    private KwdFile kwdFile;
    private EntityData entityData;
    private Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings;
    private IGameTimer gameTimer;
    private IGameController gameController;

    /**
     * I don't know how to design this perfectly in the entity world, we have
     * the state machine running inside an CreatureController. That is probably
     * wrong (should be inside a system instead). But while it is in there, we
     * should share the instances for it to function properly.<br>
     * The value needs to be weak reference also since it references the key
     */
    private final Map<EntityId, WeakReference<IChickenController>> chickenControllersByEntityId = new WeakHashMap<>();

    private static final Logger LOGGER = Logger.getLogger(ObjectsController.class.getName());

    public ObjectsController() {
        // For serialization
    }

    /**
     * Load objects from a KWD file straight (new game)
     *
     * @param kwdFile the KWD file
     * @param entityData the entity controller
     * @param gameSettings the game settings
     * @param gameTimer
     * @param gameController
     */
    public ObjectsController(KwdFile kwdFile, EntityData entityData, Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings,
            IGameTimer gameTimer, IGameController gameController) {
        this.kwdFile = kwdFile;
        this.entityData = entityData;
        this.gameSettings = gameSettings;
        this.gameTimer = gameTimer;
        this.gameController = gameController;

        // Load objects
        loadObjects();
    }

    private void loadObjects() {
        for (Thing.Object object : kwdFile.getThings(Thing.Object.class)) {
            try {
                loadObject(object);
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Could not load Thing.", ex);
            }
        }
    }

    private void loadObject(Thing.Object objectThing) {
        loadObject(objectThing.getObjectId(), objectThing.getPlayerId(), objectThing.getPosX(), objectThing.getPosY(),
                0, objectThing.getMoneyAmount(), (short) objectThing.getKeeperSpellId(), objectThing.getTriggerId() != 0 ? objectThing.getTriggerId() : null, null);
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
    public EntityId loadObject(short objectId, short ownerId, Vector3f pos, float rotation) {
        return loadObject(objectId, ownerId, pos, rotation, null, null, null, null);
    }

    @Override
    public EntityId loadObject(short objectId, short ownerId, int x, int y, Integer money, Short spellId) {
        return loadObject(objectId, ownerId, x, y, 0, money, spellId, null, null);
    }

    private EntityId loadObject(short objectId, short ownerId, int x, int y, float rotation, Integer money, Short spellId, Integer triggerId, Integer maxMoney) {
        Vector3f pos = WorldUtils.pointToVector3f(x, y);
        return loadObject(objectId, ownerId, pos, rotation, money, spellId, triggerId, maxMoney);
    }

    private EntityId loadObject(short objectId, short ownerId, Vector3f pos, float rotation, Integer money, Short spellId, Integer triggerId, Integer maxMoney) {
        EntityId entity = entityData.createEntity();
        loadObject(entity, objectId, ownerId, pos, rotation, money, maxMoney, spellId, triggerId);

        return entity;
    }

    private void loadObject(EntityId entity, short objectId, short ownerId, Vector3f pos, float rotation, Integer money, Integer maxMoney, Short spellId, Integer triggerId) {
        entityData.setComponent(entity, new ObjectComponent(objectId));
        entityData.setComponent(entity, new Owner(ownerId));

        // Move to the center of the tile
        pos.y = (objectId == OBJECT_HEART_ID || objectId == FiveByFiveRotatedController.OBJECT_BIG_STEPS_ID || objectId == FiveByFiveRotatedController.OBJECT_ARCHES_ID || objectId == TempleController.OBJECT_TEMPLE_HAND_ID ? MapLoader.UNDERFLOOR_HEIGHT : MapLoader.FLOOR_HEIGHT); // FIXME: no
        entityData.setComponent(entity, new Position(rotation, pos));

        // Add additional components
        GameObject obj = kwdFile.getObject(objectId);
        if (obj.getFlags().contains(GameObject.ObjectFlag.OBJECT_TYPE_GOLD)) {
            entityData.setComponent(entity, new Gold(money, (maxMoney == null ? (int) gameSettings.get(Variable.MiscVariable.MiscType.MAX_GOLD_PILE_OUTSIDE_TREASURY).getValue() : maxMoney)));
        }
        if (obj.getFlags().contains(GameObject.ObjectFlag.OBJECT_TYPE_SPELL_BOOK)) {
            entityData.setComponent(entity, new Spellbook(spellId));

            // Hmm
            pos.y++;
        }
        if (obj.getFlags().contains(GameObject.ObjectFlag.OBJECT_TYPE_FOOD)) {
            entityData.setComponent(entity, new Food());
        }
        if (obj.getHp() > 0) {
            entityData.setComponent(entity, new Health(objectId == OBJECT_HEART_ID ? (int) gameSettings.get(Variable.MiscVariable.MiscType.DUNGEON_HEART_HEALTH_REGENERATION_PER_SECOND).getValue() : 0,
                    objectId == OBJECT_HEART_ID ? (int) gameSettings.get(Variable.MiscVariable.MiscType.DUNGEON_HEART_OBJECT_HEALTH).getValue() : obj.getHp(),
                    objectId == OBJECT_HEART_ID ? (int) gameSettings.get(Variable.MiscVariable.MiscType.DUNGEON_HEART_OBJECT_HEALTH).getValue() : obj.getHp(), false));
        }
        if (obj.getSpeed() > 0) {
            entityData.setComponent(entity, new Mobile(false, false, false, obj.getSpeed()));
        }

        // Trigger
        if (triggerId != null) {
            entityData.setComponent(entity, new Trigger(triggerId));
        }

        // Add some interaction properties
        if (obj.getFlags().contains(GameObject.ObjectFlag.CAN_BE_SLAPPED) || obj.getFlags().contains(GameObject.ObjectFlag.CAN_BE_PICKED_UP)) {
            entityData.setComponent(entity, new Interaction(obj.getFlags().contains(GameObject.ObjectFlag.HIGHLIGHTABLE),
                    obj.getFlags().contains(GameObject.ObjectFlag.CAN_BE_SLAPPED), obj.getFlags().contains(GameObject.ObjectFlag.CAN_BE_PICKED_UP),
                    obj.getFlags().contains(GameObject.ObjectFlag.CAN_BE_DROPPED_ON_ANY_LAND),
                    obj.getFlags().contains(GameObject.ObjectFlag.DIE_WHEN_SLAPPED)));
        }

        // The visual state
        entityData.setComponent(entity, new ObjectViewState(objectId, null, ObjectViewState.GameObjectAnimState.MESH_RESOURCE, true));
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
        return loadObject(OBJECT_SPELL_BOOK_ID, ownerId, x, y, null, spell.getKeeperSpellId());
    }

    @Override
    public EntityData getEntityData() {
        return entityData;
    }

    @Override
    public IObjectController createController(EntityId entityId) {
        ObjectComponent objectComponent = entityData.getComponent(entityId, ObjectComponent.class);
        if (objectComponent == null) {
            throw new RuntimeException("Entity " + entityId + " doesn't represent a object!");
        }
        return new ObjectController(entityId, entityData, kwdFile.getObject(objectComponent.objectId), this, gameController.getGameWorldController().getMapController());
    }

    @Override
    public boolean isValidEntity(EntityId entityId) {
        return entityData.getComponent(entityId, ObjectComponent.class) != null;
    }

    @Override
    public EntityId spawnChicken(short ownerId, Vector3f pos) {

        // Yeah, chicken & egg, two different objects
        // Current design is that the state machine handles the transformation then
        // I don't know could we make it anymore generic or easy
        EntityId entity = loadObject(OBJECT_EGG_ID, ownerId, pos, 0);

        // Add the chicken AI :)
        entityData.setComponent(entity, new ChickenAi(gameTimer.getGameTime(), ChickenState.HATCHING_START));

        return entity;
    }

    @Override
    public EntityId spawnFreerangeChicken(short ownerId, Vector3f pos, double gameTime) {
        EntityId entity = spawnChicken(ownerId, pos);

        entityData.setComponent(entity, new Decay(gameTime, OBJECT_TIME_TO_LIVE));

        return entity;
    }

    @Override
    public void transformToChicken(EntityId entityId) {
        Owner owner = entityData.getComponent(entityId, Owner.class);
        Position position = entityData.getComponent(entityId, Position.class);
        loadObject(entityId, OBJECT_CHICKEN_ID, owner.ownerId, position.position, position.rotation, null, null, null, null);
    }

    @Override
    public IChickenController createChickenController(EntityId entityId) {
        ChickenAi chickenAi = entityData.getComponent(entityId, ChickenAi.class);
        if (chickenAi == null) {
            throw new RuntimeException("Entity " + entityId + " doesn't represent a chicken!");
        }
        IChickenController chickenController = chickenControllersByEntityId.computeIfAbsent(entityId, (id) -> {
            return new WeakReference<>(createChickenControllerInternal(id));
        }).get();

        if (chickenController == null) {
            chickenController = createChickenControllerInternal(entityId);
            chickenControllersByEntityId.put(entityId, new WeakReference<>(chickenController));
        }

        return chickenController;
    }

    private IChickenController createChickenControllerInternal(EntityId id) {
        return new ChickenController(id, entityData, kwdFile.getObject(OBJECT_EGG_ID), kwdFile.getObject(OBJECT_CHICKEN_ID), gameController.getNavigationService(), gameTimer, this, gameController.getGameWorldController().getMapController());
    }

}
