/*
 * Copyright (C) 2014-2024 OpenKeeper
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
import com.simsilica.es.EntityId;

/**
 * Handles shots. Shots are spells and weapons cast by traps, creatures and
 * keepers
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface IShotsController {

    /**
     * Creates a shot
     *
     * @param shotTypeId shot type to create
     * @param shotData1 arbitrary value, interpreted per shot
     * @param shotData2 arbitrary value, interpreted per shot
     * @param playerId owner of the shot
     * @param position coordinate of the shot origin
     * @param target shot target, can be null
     * @param shooter the creature firing the shot, {@code null} when a keeper
     * casts it
     */
    public void createShot(short shotTypeId, int shotData1, int shotData2, short playerId, Vector3f position, EntityId target, EntityId shooter);

    /**
     * A creature fires a shot at its target. A shot that flies is sent on its
     * way, it does what it does when it hits.
     *
     * @param shotTypeId shot type to fire
     * @param shotData1 arbitrary value, interpreted per shot
     * @param shotData2 arbitrary value, interpreted per shot
     * @param playerId owner of the shot
     * @param position where the shot starts
     * @param target the creature aimed at
     * @param shooter the creature firing the shot
     * @param maxRange how far the creature's spell reaches, in tiles
     * @return whether the shot flies, if not, it is up to the shooter
     */
    public boolean fireShot(short shotTypeId, int shotData1, int shotData2, short playerId, Vector3f position, EntityId target,
            EntityId shooter, float maxRange);

    /**
     * A flying shot hit a thing
     *
     * @param shot the shot entity
     * @param thing the thing hit
     */
    public void hit(EntityId shot, EntityId thing);

}
