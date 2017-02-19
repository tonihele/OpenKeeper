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
package toniarts.openkeeper.tools.modelviewer;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;

/**
 * Example Vector Summ
 * @author Alex Cham aka Jcrypto
 */
public class DebugUtils {
    //
    public static Node makeNode(String name) {
        Node n = new Node(name);
        return n;
    }

    public static Geometry makeGeometry(Mesh mesh, Material mat, String name) {
        Geometry geom = new Geometry(name, mesh);
        geom.setMaterial(mat);
        return geom;
    }

    public static Geometry makeGeometry(Vector3f loc, Vector3f scl, Mesh mesh, Material mat, String name) {
        Geometry geom = new Geometry(name, mesh);
        geom.setMaterial(mat);
        geom.setLocalTranslation(loc);
        geom.setLocalScale(scl);
        return geom;
    }
    
    //"Common/MatDefs/Misc/Unshaded.j3md"
    public static Material makeMaterial(AssetManager am, String name, ColorRGBA color) {
        Material mat = new Material(am, name);
        mat.setColor("Color", color);
        return mat;
    }
}
