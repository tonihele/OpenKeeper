/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.view.animation;

import com.jme3.asset.DesktopAssetManager;
import com.jme3.material.Material;
import com.jme3.scene.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TextureAnimationControlTest {
    @Test void localOwnersWithoutASharedClockStillAdvanceAndPause() {
        var material = new Material(new DesktopAssetManager(true),"MatDefs/LightingSprite.j3md");
        material.setInt("NumberOfTiles",4);
        var geometry = new Geometry("atlas",new Mesh()); geometry.setMaterial(material);
        var root = new Node(); root.attachChild(geometry);
        TextureAnimationControl.install(root,10);
        root.updateLogicalState(.15f);
        assertEquals(1,material.getParam("AnimationFrame").getValue());
        AnimationLoader.pauseAnimations(root); root.updateLogicalState(1);
        assertEquals(1,material.getParam("AnimationFrame").getValue());
        AnimationLoader.resumeAnimations(root); root.updateLogicalState(.1f);
        assertEquals(2,material.getParam("AnimationFrame").getValue());
    }
    @Test void anUnavailableSharedClockFreezesFramesInsteadOfFallingBackToLocalTime() {
        var material = new Material(new DesktopAssetManager(true),"MatDefs/LightingSprite.j3md");
        material.setInt("NumberOfTiles",4);
        var geometry = new Geometry("atlas",new Mesh()); geometry.setMaterial(material);
        var root = new Node(); root.attachChild(geometry); double[] time = {Double.NaN};
        TextureAnimationControl.install(root,()->time[0],10);
        root.updateLogicalState(.15f);
        assertEquals(0,material.getParam("AnimationFrame").getValue());
        time[0] = .25; root.updateLogicalState(0);
        assertEquals(2,material.getParam("AnimationFrame").getValue());
        time[0] = Double.NaN; root.updateLogicalState(.15f);
        assertEquals(2,material.getParam("AnimationFrame").getValue());
        var clone = (Node)root.deepClone(); clone.updateLogicalState(.1f);
        assertEquals(3,((Geometry)clone.getChild(0)).getMaterial().getParam("AnimationFrame").getValue());
    }
    @Test void textureFramesFollowSharedTimeAndPauseAndSurviveMaterialReplacement() {
        var assets = new DesktopAssetManager(true);
        var material = new Material(assets,"MatDefs/LightingSprite.j3md"); material.setInt("NumberOfTiles",4); material.setInt("Speed",10);
        var geometry = new Geometry("atlas",new Mesh()); geometry.setMaterial(material);
        var root = new Node(); root.attachChild(geometry); double[] time = {.15};
        TextureAnimationControl.install(root,()->time[0],10);
        assertEquals(1,material.getParam("AnimationFrame").getValue());
        AnimationLoader.pauseAnimations(root); time[0] = .35; root.updateLogicalState(.2f);
        assertEquals(1,material.getParam("AnimationFrame").getValue());
        AnimationLoader.resumeAnimations(root); root.updateLogicalState(0);
        assertEquals(3,material.getParam("AnimationFrame").getValue());
        var replacement = new Material(assets,"MatDefs/DkLighting/Vertex.j3md"); replacement.setInt("NumberOfTiles",4); geometry.setMaterial(replacement);
        root.updateLogicalState(0); assertEquals(3,replacement.getParam("AnimationFrame").getValue());
        assertEquals(true,replacement.getParam("UseAnimationFrame").getValue());
        var clone = (Node)root.deepClone(); time[0] = .4; root.updateLogicalState(0);
        assertEquals(0,replacement.getParam("AnimationFrame").getValue());
        assertEquals(3,((Geometry)clone.getChild(0)).getMaterial().getParam("AnimationFrame").getValue());
    }
}
