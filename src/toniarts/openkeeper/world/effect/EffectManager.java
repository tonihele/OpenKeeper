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
package toniarts.openkeeper.world.effect;

import com.jme3.asset.AssetManager;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.awt.Color;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Effect;
import toniarts.openkeeper.tools.convert.map.EffectElement;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Light;
import toniarts.openkeeper.utils.AssetUtils;

/**
 *
 * @author ArchDemon
 */
public class EffectManager {

    private final KwdFile kwdFile;
    private final AssetManager assetManager;
    private static final Logger logger = Logger.getLogger(EffectManager.class.getName());

    public EffectManager(AssetManager assetManager, KwdFile kwdFile) {
        this.kwdFile = kwdFile;
        this.assetManager = assetManager;
    }

    public Node load(int effectId) {
        Effect effect = kwdFile.getEffect(effectId);

        Node root = new Node(effect.getName());
        root.setBatchHint(Spatial.BatchHint.Never);

        PointLight light = getLight(effect.getLight());
        if (light != null) {
            root.addLight(light);
        }

        if (effect.getFlags().contains(Effect.EffectFlag.GENERATE_EFFECT_ELEMENTS)) {
            for (Integer id : effect.getGenerateIds()) {
                EffectElement element = kwdFile.getEffectElement(id);
                ParticleEmitter emitter = getEmitter(effect, element);
                if (emitter != null) {
                    root.attachChild(emitter);
                }
            }
        }
        return root;
    }

    protected ParticleEmitter getEmitter(Effect effect, EffectElement element) {

        ArtResource resource = element.getArtResource();

        // TODO: mesh particles
        if (!(resource.getSettings() instanceof ArtResource.Image)) {
            logger.log(Level.WARNING, "Only image type particles are supported currently! Was of {0} type!", resource.getSettings().getClass().getName());
            return null;
        }

        ParticleEmitter emitter = new ParticleEmitter(element.getName(), ParticleMesh.Type.Triangle, effect.getElementsPerTurn());
        if (effect.getGenerationType() == Effect.GenerationType.CUBE_GEN) {
            //emitter.setShape(new EmitterSphereShape(new Vector3f(), 1));
        }
        emitter.setParticlesPerSec(effect.getElementsPerTurn());
        Material material = AssetUtils.createParticleMaterial(assetManager, resource);
        emitter.setMaterial(material);
        emitter.setImagesX(Math.max(1, ((ArtResource.Image) resource.getSettings()).getFrames()));
        emitter.setImagesY(1);

        Color color = element.getColor();
        float alpha = 1f;
        if (element.getFlags().contains(EffectElement.EffectElementFlag.FADES)) {
            alpha -= element.getFadePercentage() / 100;
        }
        emitter.setStartColor(new ColorRGBA(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, 1f));
        emitter.setEndColor(new ColorRGBA(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, alpha));
        //
        Vector3f velocity = new Vector3f(effect.getMinSpeedXy() + element.getMinSpeedXy(),
                effect.getMaxSpeedYz() + element.getMinSpeedYz(),
                effect.getMinSpeedXy() + element.getMinSpeedXy());
        emitter.getParticleInfluencer().setInitialVelocity(velocity);
        //
        float scaleRatio = element.getScaleRatio() == 0 ? 1 : element.getScaleRatio();
        emitter.setStartSize((effect.getMaxScale() + element.getMaxScale()) * scaleRatio * 2);
        emitter.setEndSize((effect.getMinScale() + element.getMinScale()) * scaleRatio * 2);
        //
        emitter.setGravity(0, (effect.getMass() + element.getMass()) * (element.getAirFriction() + effect.getAirFriction()), 0);
        emitter.setLowLife((effect.getMinHp() + element.getMinHp()) * 0.1f);
        emitter.setHighLife((effect.getMaxHp() + element.getMaxHp()) * 0.1f);
        //
        float delta = Math.max((effect.getMaxSpeedXy() + element.getMaxSpeedXy() - effect.getMinSpeedXy() - element.getMinSpeedXy())
                / (effect.getMaxSpeedXy() + element.getMaxSpeedXy() + 1),
                (effect.getMaxSpeedYz() + element.getMaxSpeedYz() - effect.getMinSpeedYz() - element.getMinSpeedYz())
                / (effect.getMaxSpeedYz() + element.getMaxSpeedYz() + 1));
        emitter.getParticleInfluencer().setVelocityVariation(delta);

        return emitter;
    }

    protected PointLight getLight(Light effectLight) {
        if (effectLight == null) {
            return null;
        }

        PointLight light = new PointLight();
        light.setColor(new ColorRGBA(effectLight.getColor().getRed() / 255f,
                effectLight.getColor().getGreen() / 255f,
                effectLight.getColor().getBlue() / 255f,
                effectLight.getColor().getAlpha() / 255f));
        light.setRadius(effectLight.getRadius());
        light.setPosition(new Vector3f(effectLight.getmKPos().x, effectLight.getmKPos().y, effectLight.getmKPos().z));
        return light;
    }

}
