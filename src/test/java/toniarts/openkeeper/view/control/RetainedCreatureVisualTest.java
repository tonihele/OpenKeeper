/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.view.control;

import com.jme3.asset.*;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.*;
import com.jme3.texture.*;
import com.jme3.util.BufferUtils;
import com.simsilica.es.base.DefaultEntityData;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.*;
import toniarts.openkeeper.tools.convert.map.*;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.view.CharredOverlay;
import toniarts.openkeeper.view.animation.PreparedAnimationCache;
import static org.junit.jupiter.api.Assertions.*;

class RetainedCreatureVisualTest {
    private static Texture texture(int color) {
        var texture = new Texture2D(new Image(Image.Format.RGBA8,1,1,BufferUtils.createByteBuffer(new byte[]{(byte)color,0,0,(byte)255}),com.jme3.texture.image.ColorSpace.Linear));
        texture.setKey(new TextureKey("retained-source-"+color+".png"));
        return texture;
    }
    @Test void reuseRestoresOwnerHighlightAndCharredTextureAcrossMaterialReplacement() {
        for (String definition : new String[]{"Common/MatDefs/Light/Lighting.j3md","MatDefs/DkLighting/Flat.j3md"}) {
            var assets = new DesktopAssetManager(true) {
                @Override public Texture loadTexture(TextureKey key) { return texture(32); }
                @Override public Spatial loadModel(ModelKey key) {
                    Node wrapper = new Node(); Node model = new Node(); wrapper.attachChild(model);
                    Mesh mesh = new Mesh(); mesh.setBuffer(VertexBuffer.Type.Position,3,new float[]{0,0,0});
                    Geometry geometry = new Geometry("vertex",mesh);
                    var material = new Material(this,definition); material.setTexture("DiffuseMap",texture(255));
                    geometry.setMaterial(material); model.attachChild(geometry); return wrapper;
                }
            };
            var data = new DefaultEntityData(); var id = data.createEntity();
            var interaction = new Interaction(); interaction.pickUppable = true;
            data.setComponents(id,new Owner((short)1,(short)1),interaction);
            var creature = new Creature(); String suffix = definition.contains("Common") ? "standard" : "dk";
            creature.getAnimations().put(Creature.AnimationType.WALK,new ArtResource("retained-visual-a-"+suffix,ArtResource.ArtResourceType.MESH));
            creature.getAnimations().put(Creature.AnimationType.IDLE_1,new ArtResource("retained-visual-b-"+suffix,ArtResource.ArtResourceType.MESH));
            Node root = new Node(); var cache = new PreparedAnimationCache(3,1024); root.addControl(cache);
            var control = new CreatureViewControl(id,data,creature,Creature.AnimationType.WALK,assets,null); root.addControl(control);
            try {
                var a = (Node)root.getChild(0); var geometry = (Geometry)a.getChild("vertex");
                Texture original = geometry.getMaterial().getTextureParam("DiffuseMap").getTextureValue();
                control.onHoverStart((short)1);
                if (definition.contains("Common")) assertEquals(true,geometry.getMaterial().getParam("UseMaterialColors").getValue());
                else {
                    assertNotEquals(ColorRGBA.BlackNoAlpha,geometry.getMaterial().getParam("TintOverlay").getValue());
                    geometry.setMaterial(AssetUtils.createVertexMaterial(assets,geometry.getMaterial(),ColorRGBA.Black));
                    assertNotEquals(ColorRGBA.BlackNoAlpha,geometry.getMaterial().getParam("TintOverlay").getValue());
                }
                CharredOverlay.apply(root,assets); assertNotSame(original,geometry.getMaterial().getTextureParam("DiffuseMap").getTextureValue());
                control.setTargetState(Creature.AnimationType.IDLE_1); control.onHoverEnd((short)1);
                control.setTargetState(Creature.AnimationType.WALK);
                assertSame(a,root.getChild(0)); assertSame(original,geometry.getMaterial().getTextureParam("DiffuseMap").getTextureValue());
                if (definition.contains("Common")) assertEquals(false,geometry.getMaterial().getParam("UseMaterialColors").getValue());
                else assertEquals(ColorRGBA.BlackNoAlpha,geometry.getMaterial().getParam("TintOverlay").getValue());
                assertNull(root.getUserData("charred"));
                control.onHoverStart((short)1); control.setTargetState(Creature.AnimationType.IDLE_1);
                var b = (Geometry)((Node)root.getChild(0)).getChild("vertex");
                if (definition.contains("Common")) assertEquals(true,b.getMaterial().getParam("UseMaterialColors").getValue());
                else assertNotEquals(ColorRGBA.BlackNoAlpha,b.getMaterial().getParam("TintOverlay").getValue());
            } finally { control.cleanup(); assertEquals(0,cache.size()); data.close(); }
        }
    }
}
