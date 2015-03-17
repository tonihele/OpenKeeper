/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.openkeeper.world;

import com.jme3.asset.AssetManager;
import com.jme3.scene.Spatial;

/**
 * A simple interface for loading DK2 map classes to JME spatials
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface ILoader<T> {

    public Spatial load(AssetManager assetManager, T object);
}
