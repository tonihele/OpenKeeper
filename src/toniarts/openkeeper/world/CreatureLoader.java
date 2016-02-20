/*
 * Copyright (C) 2014-2015 OpenKeeper
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
package toniarts.openkeeper.world;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.scene.Node;
import java.util.ArrayList;
import java.util.List;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Thing;

/**
 *
 * @author ArchDemon
 */


public class CreatureLoader {

    public static List<Node> load(BulletAppState bulletAppState, AssetManager assetManager, KwdFile kwdFile) {

        ArrayList<Node> result = new ArrayList<>();

        for (Thing obj : kwdFile.getThings()) {
            try {
                if (obj instanceof Thing.Creature) {

                    Thing.Creature cr = (Thing.Creature) obj;
                    GameCreature creature = new GameCreature(bulletAppState, assetManager, cr, kwdFile);

                    result.add(creature);
                }
            } catch (Exception ex) {
                System.err.println(ex);
            }
        }

        return result;
    }

    public static GameCreature load(Thing.GoodCreature object, BulletAppState bulletAppState,
            AssetManager assetManager, KwdFile kwdFile) {

        return new GameCreature(bulletAppState, assetManager, object, null);
    }

}
