/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.animation;

import com.jme3.asset.*;
import com.jme3.material.Material;
import com.jme3.scene.*;
import com.jme3.texture.Texture2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.KmfModelLoader;
import toniarts.openkeeper.tools.convert.map.*;
import toniarts.openkeeper.utils.AssetUtils;
import static org.junit.jupiter.api.Assertions.*;

class AnimationTexturePolicyTest {
    @Test void sharedModelKeepsStaticPolicyAndEachAnimatedRateIndependent() throws Exception {
        var png = new ByteArrayOutputStream(); javax.imageio.ImageIO.write(new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB),"png",png);
        var assets = new DesktopAssetManager(true) {
            @Override public AssetInfo locateAsset(AssetKey key) {
                if (key.getName().startsWith("policy-frame")) return new AssetInfo(this,key) {
                    @Override public InputStream openStream() { return new ByteArrayInputStream(png.toByteArray()); }
                };
                return super.locateAsset(key);
            }
            @Override public Spatial loadModel(ModelKey key) {
                var texture = new Texture2D(); texture.setKey(new TextureKey("policy.png"));
                var material = new Material(this,"MatDefs/DkLighting/Flat.j3md"); material.setName("policy-material"); material.setTexture("DiffuseMap",texture);
                var geometry = new Geometry("policy",new Mesh()); geometry.setMaterial(material);
                geometry.setUserData(KmfModelLoader.MATERIAL_ALTERNATIVE_TEXTURES,new ArrayList<>(java.util.List.of("policy-frame-a","policy-frame-b")));
                var root = new Node(); root.attachChild(geometry); return root;
            }
        };
        var slow = AnimationFixtures.resource("policy-template",3,15); slow.getFlags().add(ArtResource.ArtResourceFlag.ANIMATING_TEXTURE);
        var fast = AnimationFixtures.resource("policy-template",3,50); fast.getFlags().add(ArtResource.ArtResourceFlag.ANIMATING_TEXTURE);
        var plain = AnimationFixtures.resource("policy-template",3,30);
        var first = (Geometry)((Node)AssetUtils.loadModel(assets,slow.getName(),slow)).getChild(0);
        var second = (Geometry)((Node)AssetUtils.loadModel(assets,fast.getName(),fast)).getChild(0);
        var third = (Geometry)((Node)AssetUtils.loadModel(assets,plain.getName(),plain)).getChild(0);
        assertEquals(15,first.getMaterial().getParam("Speed").getValue());
        assertEquals(50,second.getMaterial().getParam("Speed").getValue());
        assertEquals("DK Flat Lighting",third.getMaterial().getMaterialDef().getName());
        assertNotNull(first.getMaterial().getTextureParam("DiffuseMap"));
    }
}
