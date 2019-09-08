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
import toniarts.openkeeper.game.controller.chicken.IChickenController;
import toniarts.openkeeper.game.controller.object.IObjectController;
import toniarts.openkeeper.game.data.PlayerSpell;

/**
 * Handles the object space
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface IObjectsController extends IEntityWrapper<IObjectController> {

    public EntityId loadObject(short objectId, short ownerId, int x, int y);

    public EntityId loadObject(short objectId, short ownerId, int x, int y, float rotation);

    public EntityId loadObject(short objectId, short ownerId, Vector3f pos, float rotation);

    public EntityId loadObject(short objectId, short ownerId, int x, int y, Integer money, Short spellId);

    public EntityId addRoomGold(short ownerId, int x, int y, int money, int maxMoney);

    public EntityId addLooseGold(short ownerId, int x, int y, int money, int maxMoney);

    public EntityId addRoomSpellBook(short ownerId, int x, int y, PlayerSpell spell);

    public EntityData getEntityData();

    /**
     * Spawns an chicken in specified position. Yes, chickens are object. And an
     * egg is actually spawned.
     *
     * @param ownerId the chicken owner
     * @param pos spawn position
     * @return return the entity ID for the generated chicken
     */
    public EntityId spawnChicken(short ownerId, Vector3f pos);

    /**
     * Spawns an freerange chicken in specified position.
     *
     * @param ownerId the chicken owner
     * @param pos spawn position
     * @param gameTime the current game time
     * @return return the entity ID for the generated chicken
     */
    public EntityId spawnFreerangeChicken(short ownerId, Vector3f pos, double gameTime);

    /**
     * Transforms entity into a chicken (destructive operation). Mainly meant to
     * be used when eggs hatch
     *
     * @param entityId the entity to turn into a chicken
     */
    public void transformToChicken(EntityId entityId);

    /**
     * Creates a chicken controller around given entity
     *
     * @param id the entity ID to create the controller for
     * @return the controller
     */
    public IChickenController createChickenController(EntityId id);

}
