/*
 * Copyright (C) 2014-2018 OpenKeeper
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

import com.jme3.math.Vector2f;
import com.simsilica.es.EntityId;
import java.util.List;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.creature.IPartyController;
import toniarts.openkeeper.game.controller.creature.PartyType;
import toniarts.openkeeper.tools.convert.map.Thing;

/**
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface ICreaturesController extends IEntityWrapper<ICreatureController> {

    /**
     * Spawn a creature
     *
     * @param creature creature data
     * @param position the position to spawn to, may be {@code null}
     * @return the actual spawned creature
     */
    EntityId spawnCreature(Thing.Creature creature, Vector2f position);

    /**
     * Spawn a creature
     *
     * @param creatureId the creature ID to generate
     * @param playerId the owner
     * @param level the creature level
     * @param position the position to spawn to, may be {@code null}
     * @param entrance whether this an enrance for the creature (coming out of a
     * portal)
     * @return the actual spawned creature
     */
    EntityId spawnCreature(short creatureId, short playerId, int level, Vector2f position, boolean entrance);

    /**
     * Spawn a hero party
     *
     * @param partyId ID of the party
     * @param partyType the type of the spawned party
     * @param position the position to spawn the party
     */
    void spawnHeroParty(short partyId, PartyType partyType, Vector2f position);

    /**
     * Get party by party ID that is specified in the level files.
     *
     * @param partyId the party ID
     * @see #getPartyById(long)
     * @return party controller
     */
    IPartyController getParty(short partyId);

    /**
     * Get party by an unique ID
     *
     * @param id the ID
     * @see #getParty(short)
     * @return party controller
     */
    IPartyController getPartyById(long id);

    /**
     * Get parties. Note that this returns the level defined parties. Those
     * might not have even been spawn yet.
     *
     * @return the level defined creature partied
     */
    List<IPartyController> getParties();

    /**
     * Creates an creature controller interface on top of a real entity
     *
     * @param entityId the entity ID
     * @return the creature controller wrapper
     */
    @Override
    ICreatureController createController(EntityId entityId);

    /**
     * Levels up a creature
     *
     * @param entityId the entity ID
     * @param level the new experience level
     * @param experience <i>leftover</i> experience for new level
     */
    void levelUpCreature(EntityId entityId, int level, int experience);

}
