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
package toniarts.openkeeper.world.creature;

import com.jme3.asset.AssetManager;
import com.jme3.math.Vector2f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.logging.Logger;
import toniarts.openkeeper.ai.creature.CreatureState;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.world.ILoader;
import toniarts.openkeeper.world.MapLoader;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.control.AbstractUnitFlowerControl;
import toniarts.openkeeper.world.listener.CreatureListener;

/**
 * Loads up creatures
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class CreatureLoader implements ILoader<Thing.Creature>, CreatureListener {

    private final KwdFile kwdFile;
    private final WorldState worldState;

    private static final Logger logger = Logger.getLogger(CreatureLoader.class.getName());

    public CreatureLoader(KwdFile kwdFile, WorldState worldState) {
        this.kwdFile = kwdFile;
        this.worldState = worldState;
    }

    @Override
    public Spatial load(AssetManager assetManager, Thing.Creature object) {
        return load(assetManager, object, object.getCreatureId(), (short) 0, (short) 0);
    }

    public Spatial load(AssetManager assetManager, short creatureId, short playerId, short level) {
        return load(assetManager, null, creatureId, playerId, level);
    }

    private Spatial load(AssetManager assetManager, Thing.Creature object, short creatureId, short playerId, short level) {
        Creature creature = kwdFile.getCreature(creatureId);
        Node creatureRoot = new Node(creature.getName());
        CreatureControl creatureControl = new CreatureControl(object, creature, worldState, playerId, level) {

            @Override
            public void onSpawn(CreatureControl creature) {
                CreatureLoader.this.onSpawn(creature);
            }

            @Override
            public void onStateChange(CreatureControl creature, CreatureState newState, CreatureState oldState) {
                CreatureLoader.this.onStateChange(creature, newState, oldState);
            }

            @Override
            public void onDie(CreatureControl creature) {
                CreatureLoader.this.onDie(creature);
            }

        };

        // Set map position
        if (object != null) {
            creatureRoot.setLocalTranslation(
                    object.getPosX() * MapLoader.TILE_WIDTH - MapLoader.TILE_WIDTH / 2f,
                    object.getPosZ() * MapLoader.TILE_HEIGHT,
                    object.getPosY() * MapLoader.TILE_WIDTH - MapLoader.TILE_WIDTH / 2f);
        }

        // Add the creature control
        creatureRoot.addControl(creatureControl);

        // Creature flower
        AbstractUnitFlowerControl aufc = new CreatureUnitFlowerControl(assetManager, creatureControl);
        creatureRoot.addControl(aufc);

        return creatureRoot;
    }

    public static void setPosition(Spatial creature, Vector2f position) {
        creature.setLocalTranslation(
                position.x * MapLoader.TILE_WIDTH - MapLoader.TILE_WIDTH / 2f,
                0 * MapLoader.TILE_HEIGHT,
                position.y * MapLoader.TILE_WIDTH - MapLoader.TILE_WIDTH / 2f);

        // Need to re-adjust the steering
        CreatureControl creatureControl = creature.getControl(CreatureControl.class);
        creatureControl.setSpatial(creature);
    }

    static void showUnitFlower(CreatureControl creature, Integer seconds) {
        AbstractUnitFlowerControl aufc = creature.getSpatial().getControl(AbstractUnitFlowerControl.class);
        if (seconds != null) {
            aufc.show(seconds);
        } else {
            aufc.show();
        }
    }

}
