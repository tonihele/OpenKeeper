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
}
