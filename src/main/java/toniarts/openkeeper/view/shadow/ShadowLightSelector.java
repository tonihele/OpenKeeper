/*
 * Copyright (C) 2014-2025 OpenKeeper
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
package toniarts.openkeeper.view.shadow;

import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import toniarts.openkeeper.view.lighting.LightSource;

/**
 * Picks the lights a caster gets its shadows from.
 */
final class ShadowLightSelector {

    /**
     * A chosen light and its 3D distance to the caster
     */
    record Candidate(LightSource light, float distance) {

    }

    /**
     * @param chosen up to {@link ShadowConstants#MAX_SHADOWS_PER_CASTER}
     * lights, nearest first
     * @param fadeRange R', the range the chosen shadows fade over: the
     * global range, or the distance of the fourth nearest light if there is
     * one, so the farthest shadow has faded when another light replaces it
     */
    record Selection(List<Candidate> chosen, float fadeRange) {

    }

    private ShadowLightSelector() {
        // No!
    }

    /**
     * @param tileLights the light set of the tile the caster stands on
     * @param centre the centre of the caster
     * @param range the global shadow range
     * @param ownLight the caster's own light, which never shadows it; may be
     * {@code null}
     */
    static Selection select(List<LightSource> tileLights, Vector3f centre, float range, Object ownLight) {
        if (range <= 0f) {
            return new Selection(List.of(), range);
        }
        List<Candidate> eligible = new ArrayList<>();
        for (LightSource light : tileLights) {
            if (!TileLightSets.castsShadows(light) || light == ownLight) {
                continue;
            }
            float distance = light.getPosition().distance(centre);
            if (distance < range) {
                eligible.add(new Candidate(light, distance));
            }
        }
        eligible.sort(Comparator.comparingDouble(Candidate::distance));

        int max = ShadowConstants.MAX_SHADOWS_PER_CASTER;
        if (eligible.size() <= max) {
            return new Selection(eligible, range);
        }
        return new Selection(new ArrayList<>(eligible.subList(0, max)), eligible.get(max).distance());
    }
}
