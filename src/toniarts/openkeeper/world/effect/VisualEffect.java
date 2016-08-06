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
package toniarts.openkeeper.world.effect;

import com.jme3.asset.AssetManager;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Effect;
import toniarts.openkeeper.tools.convert.map.EffectElement;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Light;
import toniarts.openkeeper.utils.AssetUtils;

/**
 * An effect & its elements, or a tree of effects & their elements. The logic is
 * quite here now, how the effects are chained and what they do, but:<br>
 * TODO
 * <ul>
 * <li>We probably need our own particle emitter, the stock wont probably do..
 * Just pass the Effect & EffectElement to our custom one and boom</li>
 * <li>Maybe cache the emitters?</li>
 * </ul>
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class VisualEffect {

    private final Effect effect;
    private final Map<EffectElement, ParticleEmitter> effectElements;
    private final List<VisualEffect> effects;
    private final Node effectNode;
    private final KwdFile kwdFile;
    private final AssetManager assetManager;
    private final EffectManagerState effectManagerState;
    private boolean infinite;
    private PointLight light;
    private static final Logger logger = Logger.getLogger(VisualEffect.class.getName());

    public VisualEffect(KwdFile kwdFile, AssetManager assetManager, EffectManagerState effectManagerState, Node node, Effect effect) {
        this(kwdFile, assetManager, effectManagerState, node, null, effect, false);
    }

    public VisualEffect(KwdFile kwdFile, AssetManager assetManager, EffectManagerState effectManagerState, Node node, Vector3f location, Effect effect, boolean infinite) {
        this.effect = effect;
        this.kwdFile = kwdFile;
        this.assetManager = assetManager;
        this.effectManagerState = effectManagerState;
        this.infinite = infinite;

        // Create the lists
        if (effect.getFlags().contains(Effect.EffectFlag.GENERATE_EFFECT_ELEMENTS)) {
            effects = new ArrayList<>();
            effectElements = new HashMap<>(effect.getGenerateIds().size());
        } else {
            effects = new ArrayList<>(effect.getGenerateIds().size());
            effectElements = Collections.emptyMap();
        }

        // Attach to scene graph
        effectNode = new Node(effect.getName());
        if (location != null) {
            effectNode.setLocalTranslation(location);
        }
        node.attachChild(effectNode); // We need to attach before emiting, it doesn't work otherwise

        // Load the effect
        load();
    }

    private void load() {

        // FIXME: light should not be static, it should be treated as particle
        // Light
        light = getLight(effect.getLight());
        if (light != null) {
            effectNode.addLight(light);
        }

        // Elements/effects
        if (effect.getFlags().contains(Effect.EffectFlag.GENERATE_EFFECT_ELEMENTS)) {
            for (Integer id : effect.getGenerateIds()) {
                addEffectElement(id);
            }
        } else {
            for (Integer id : effect.getGenerateIds()) {
                addEffect(id);
            }
        }

        // The next effect is chaining the effects, they'll start immediately
        if (effect.getNextEffectId() != 0) {
            addEffect(effect.getNextEffectId());
        }
    }

    private void addEffect(Integer id) {
        VisualEffect visualEffect = new VisualEffect(kwdFile, assetManager, effectManagerState, effectNode, kwdFile.getEffect(id));
        effects.add(visualEffect);
        effectNode.attachChild(visualEffect.effectNode);
    }

    private void addEffectElement(Integer id) {
        EffectElement effectElement = kwdFile.getEffectElement(id);
        ParticleEmitter emitter = loadElement(effectElement);
        if (emitter != null) {
            effectElements.put(effectElement, emitter);
            effectNode.attachChild(emitter);
            emitter.emitAllParticles();
        }

        // The next effect is chaining the effects, they'll start immediately
        if (effectElement.getNextEffectId() != 0) {
            addEffect(effectElement.getNextEffectId());
        }
    }

    private ParticleEmitter loadElement(EffectElement element) {
        ArtResource resource = element.getArtResource();

        // TODO: mesh particles
        if (!(resource.getSettings() instanceof ArtResource.Image)) {
            logger.log(Level.WARNING, "Only image type particles are supported currently! Was of {0} type!", resource.getSettings().getClass().getName());
            return null;
        }

        ParticleEmitter emitter = new ParticleEmitter(element.getName(), ParticleMesh.Type.Triangle, effect.getElementsPerTurn());
        emitter.setParticlesPerSec(0);

        if (effect.getGenerationType() == Effect.GenerationType.CUBE_GEN) {
            //emitter.setShape(new EmitterSphereShape(new Vector3f(), 1));
        }
        Material material = AssetUtils.createParticleMaterial(assetManager, resource);
        emitter.setMaterial(material);
        emitter.setImagesX(Math.max(1, ((ArtResource.Image) resource.getSettings()).getFrames()));
        emitter.setImagesY(1);
        emitter.setSelectRandomImage(resource.getSettings().getFlags().contains(ArtResource.ArtResourceFlag.RANDOM_START_FRAME));

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
        if (element.getFlags().contains(EffectElement.EffectElementFlag.SHRINKS)) {
            emitter.setEndSize((effect.getMaxScale() + element.getMaxScale()) * scaleRatio);
            emitter.setStartSize((effect.getMinScale() + element.getMinScale()) * scaleRatio);
        } else {
            emitter.setStartSize((effect.getMaxScale() + element.getMaxScale()) * scaleRatio);
            emitter.setEndSize((effect.getMinScale() + element.getMinScale()) * scaleRatio);
        }
        //
        emitter.setFacingVelocity(element.getFlags().contains(EffectElement.EffectElementFlag.ROTATE_TO_MOVEMENT_DIRECTION));
        //
        emitter.setGravity(0, -(effect.getMass() + element.getMass()) * (element.getAirFriction() + effect.getAirFriction()), 0);
        emitter.setLowLife((effect.getMinHp() + element.getMinHp()) / 15f);
        emitter.setHighLife((effect.getMaxHp() + element.getMaxHp()) / 15f);
        //
        float delta = Math.max((effect.getMaxSpeedXy() + element.getMaxSpeedXy() - effect.getMinSpeedXy() - element.getMinSpeedXy())
                / (effect.getMaxSpeedXy() + element.getMaxSpeedXy() + 1),
                (effect.getMaxSpeedYz() + element.getMaxSpeedYz() - effect.getMinSpeedYz() - element.getMinSpeedYz())
                / (effect.getMaxSpeedYz() + element.getMaxSpeedYz() + 1));
        emitter.getParticleInfluencer().setVelocityVariation(delta);

        return emitter;
    }

    private PointLight getLight(Light effectLight) {
        if (effectLight == null) {
            return null;
        }

        PointLight realLight = new PointLight();
        realLight.setColor(new ColorRGBA(effectLight.getColor().getRed() / 255f,
                effectLight.getColor().getGreen() / 255f,
                effectLight.getColor().getBlue() / 255f,
                effectLight.getColor().getAlpha() / 255f));
        realLight.setRadius(effectLight.getRadius());
        realLight.setPosition(new Vector3f(effectLight.getmKPos().x, effectLight.getmKPos().y, effectLight.getmKPos().z));
        return realLight;
    }

    /**
     * Regular style update, but also signals whether this effect has come to an
     * end
     *
     * @param tpf the update time
     * @return true if the effect is still valid, false if the effect has died
     */
    public boolean update(float tpf) {

        // Update the child effects
        Iterator<VisualEffect> iterator = effects.iterator();
        while (iterator.hasNext()) {
            VisualEffect visualEffect = iterator.next();
            if (!visualEffect.update(tpf)) {
                iterator.remove();
            }
        }

        // Check the elements
        Iterator<Entry<EffectElement, ParticleEmitter>> iter = effectElements.entrySet().iterator();
        List<Integer> deathEffectElements = null;
        while (iter.hasNext()) {
            Entry<EffectElement, ParticleEmitter> entry = iter.next();
            if (entry.getValue().getNumVisibleParticles() == 0) {

                // Kill
                entry.getValue().removeFromParent();
                iter.remove();

                // Attach on death element
                if (entry.getKey().getDeathElementId() != 0) {
                    if (deathEffectElements == null) {
                        deathEffectElements = new ArrayList<>();
                    }
                    deathEffectElements.add(entry.getKey().getDeathElementId());
                }
            }
        }

        // Init the death elements
        if (deathEffectElements != null) {
            for (Integer id : deathEffectElements) {
                addEffectElement(id);
            }
        }

        // If the whole effect has died, create the death effect
        if (effect.getFlags().contains(Effect.EffectFlag.GENERATE_EFFECT_ELEMENTS) && effectElements.isEmpty()) {
            if (effect.getDeathEffectId() != 0) {
                addEffect(effect.getDeathEffectId());
            }
        }

        // If no children at all, remove us
        if (effectElements.isEmpty() && effects.isEmpty()) {

            // If infitine, just restart
            if (infinite) {
                if (light != null) {
                    effectNode.removeLight(light);
                }
                load();
            } else {
                effectNode.removeFromParent();
                return false;
            }
        }
        return true;
    }

    public void removeEffect() {
        effects.clear();
        effectElements.clear();
        infinite = false;
    }
}
