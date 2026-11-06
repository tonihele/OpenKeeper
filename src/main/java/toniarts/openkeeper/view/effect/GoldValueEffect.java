/*
 * Copyright (C) 2014-2026 OpenKeeper
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
package toniarts.openkeeper.view.effect;

import com.jme3.asset.AssetManager;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.control.AbstractControl;
import java.util.ArrayList;
import java.util.List;
import toniarts.openkeeper.tools.convert.map.EffectElement;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.utils.AssetUtils;

/** Effect 22's value-dependent digits, rather than a random burst of digits 0–7. */
final class GoldValueEffect {
    private GoldValueEffect() {
    }

    static Node create(int value, IKwdFile level, AssetManager assets, Camera camera) {
        Node node = new Node("Gold value " + value);
        String digits = Integer.toString(Math.max(0, value));
        List<ParticleEmitter> emitters = new ArrayList<>(digits.length());
        Vector3f right = camera.getLeft().negate();
        for (int i = 0; i < digits.length(); i++) {
            String name = "InGameDigit" + digits.charAt(i);
            EffectElement element = level.getEffectElements().values().stream()
                    .filter(candidate -> name.equals(candidate.getName())).findFirst().orElse(null);
            if (element == null || element.getArtResource() == null) {
                continue;
            }
            var resource = element.getArtResource();
            ParticleEmitter emitter = new ParticleEmitter(name, ParticleMesh.Type.Triangle, 1);
            emitter.setMaterial(AssetUtils.createParticleMaterial(resource, assets));
            emitter.setParticlesPerSec(0);
            emitter.setInWorldSpace(false);
            emitter.setRandomAngle(false);
            emitter.setStartColor(ColorRGBA.White);
            emitter.setEndColor(new ColorRGBA(1, 1, 1, 0));
            float size = (element.getMinScale() + element.getMaxScale()) / 4f;
            emitter.setStartSize(size);
            emitter.setEndSize(size);
            emitter.setLowLife(element.getMinHp() / 20f);
            emitter.setHighLife(element.getMaxHp() / 20f);
            emitter.getParticleInfluencer().setInitialVelocity(new Vector3f(0,
                    (element.getMinSpeedYz() + element.getMaxSpeedYz()) / 8f, 0));
            emitter.getParticleInfluencer().setVelocityVariation(0);
            emitter.setLocalTranslation(right.mult((i - (digits.length() - 1) / 2f) * size * 1.6f));
            node.attachChild(emitter);
            emitters.add(emitter);
        }
        node.addControl(new AbstractControl() {
            private boolean started;

            @Override
            protected void controlUpdate(float tpf) {
                if (!started) {
                    emitters.forEach(ParticleEmitter::emitAllParticles);
                    started = true;
                } else if (emitters.stream().allMatch(emitter -> emitter.getNumVisibleParticles() == 0)) {
                    spatial.removeFromParent();
                }
            }

            @Override
            protected void controlRender(RenderManager manager, ViewPort viewport) {
            }
        });
        return node;
    }
}
