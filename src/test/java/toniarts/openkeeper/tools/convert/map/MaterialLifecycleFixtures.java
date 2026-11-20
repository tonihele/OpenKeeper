/*
 * Copyright (C) 2026 OpenKeeper
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package toniarts.openkeeper.tools.convert.map;

import java.util.EnumSet;
import java.util.List;

/** Small parsed-data fixtures, using the package's ordinary conversion setters. */
public final class MaterialLifecycleFixtures {
    private MaterialLifecycleFixtures() {
    }

    public static Effect effect() {
        var effect = new Effect();
        effect.setName("loop");
        effect.setEffectId(1);
        effect.setFlags(EnumSet.noneOf(Effect.EffectFlag.class));
        effect.setGenerateIds(List.of());
        effect.setGenerationType(Effect.GenerationType.NONE);
        effect.setMinHp(1);
        effect.setMaxHp(1);
        effect.setMinScale(1);
        effect.setMaxScale(1);
        effect.setArtResource(new ArtResource("test procedural target", ArtResource.ArtResourceType.PROCEDURAL_MESH));
        var light = new Light();
        light.setColor(255, 120, 30, 255);
        light.setFlags(EnumSet.noneOf(Light.LightFlag.class));
        light.setRadius(2);
        light.setmKPos(0, 0, 0);
        effect.setLight(light);
        return effect;
    }

    public static EffectElement element() {
        var element = new EffectElement();
        element.setName("self respawn");
        element.setFlags(EnumSet.noneOf(EffectElement.EffectElementFlag.class));
        element.setEffectElementId(1);
        element.setDeathElementId(1);
        element.setMinHp(1);
        element.setMaxHp(1);
        element.setMinScale(1);
        element.setMaxScale(1);
        return element;
    }

    public static Effect artlessEffect(int id, List<Integer> children, int deathEffectId) {
        var effect = effect();
        effect.setEffectId(id);
        effect.setArtResource(null);
        effect.setFlags(EnumSet.of(children.isEmpty()
                ? Effect.EffectFlag.GENERATE_EFFECT_ELEMENTS : Effect.EffectFlag.GENERATE_EFFECTS));
        effect.setGenerateIds(children);
        effect.setDeathEffectId(deathEffectId);
        return effect;
    }

    public static Effect meshEffect(String modelName) {
        var effect = effect();
        effect.setArtResource(meshArt(modelName));
        effect.setFlags(EnumSet.of(Effect.EffectFlag.GENERATE_EFFECT_ELEMENTS));
        effect.setGenerationType(Effect.GenerationType.DEFAULT);
        effect.setGenerateIds(List.of(1));
        effect.setElementsPerTurn((short) 1);
        return effect;
    }

    public static EffectElement meshElement(String modelName) {
        var element = element();
        element.setArtResource(meshArt(modelName));
        return element;
    }

    private static ArtResource meshArt(String modelName) {
        var art = new ArtResource(modelName, ArtResource.ArtResourceType.MESH);
        art.setData(ArtResource.KEY_SCALE, 1f);
        return art;
    }
}
