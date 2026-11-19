/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.animation;

import com.jme3.asset.*;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import java.awt.image.BufferedImage;
import java.io.*;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.utils.AssetUtils;
import static org.junit.jupiter.api.Assertions.*;

class AnimationCoveragePolicyTest {
    @Test void atlasCoverageFollowsSourceAlphaAndPreservesLegacyBlackKeyPolicy() throws Exception {
        for(boolean alpha : new boolean[]{false,true}) {
            String name = alpha ? "coverage-source-alpha" : "coverage-source-rgb";
            var png = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(new BufferedImage(2,1,alpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB),"png",png);
            var assets = new DesktopAssetManager(true) {
                @Override public AssetInfo locateAsset(AssetKey key) {
                    if(key.getName().replace('\\','/').equals("Textures/"+name+"0.png")) return new AssetInfo(this,key) {
                        @Override public InputStream openStream() { return new ByteArrayInputStream(png.toByteArray()); }
                    };
                    return super.locateAsset(key);
                }
            };
            var resource = new ArtResource(name,ArtResource.ArtResourceType.ALPHA);
            resource.getFlags().add(ArtResource.ArtResourceFlag.ANIMATING_TEXTURE);
            var material = AssetUtils.createLightningSpriteMaterial(resource,assets);
            assertEquals(alpha,material.getParam("UseAlphaCoverage").getValue());
            var vertex = AssetUtils.createVertexMaterial(assets,material,ColorRGBA.Black);
            assertEquals(alpha,vertex.getParam("UseAlphaCoverage").getValue());
        }
    }
    @Test void allCreatureMaterialFamiliesHaveExplicitCoverageAwareShadowTechniques() {
        var assets = new DesktopAssetManager(true);
        for(String name : new String[]{"MatDefs/DkLighting/Vertex.j3md","MatDefs/DkLighting/Flat.j3md","MatDefs/LightingSprite.j3md"}) {
            var definition = new Material(assets,name).getMaterialDef();
            assertNotNull(definition.getTechniqueDefs("PreShadow"));
            assertNotNull(definition.getTechniqueDefs("PostShadow"));
            assertNotNull(definition.getMaterialParam("UseAlphaCoverage"));
        }
    }
}
