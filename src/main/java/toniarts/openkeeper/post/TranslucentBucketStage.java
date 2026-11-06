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
package toniarts.openkeeper.post;

import com.jme3.post.Filter;
import com.jme3.post.filters.TranslucentBucketFilter;

/**
 * Independent post-process stage that resolves
 * {@link com.jme3.renderer.queue.RenderQueue.Bucket#Translucent} geometry
 * (torch flames, particles, etc.) against the scene depth after opaque /
 * transparent water has been drawn.
 * <p>
 * <b>Not tied to SSAO or any other AO technique.</b> Ambient occlusion is an
 * optional later colour filter; this stage must run whenever translucent
 * content is used, whether AO is enabled, disabled, or swapped for another
 * implementation.
 * <p>
 * Lifecycle: created by {@link PostProcessPipeline} when the viewport filter
 * chain is (re)built; disposed with the {@link com.jme3.post.FilterPostProcessor}.
 * No shared mutable state with AO filters.
 */
public final class TranslucentBucketStage {

    private TranslucentBucketStage() {
    }

    /**
     * Translucent resolve is a structural requirement of the translucent
     * bucket, not a user graphics toggle.
     */
    public static boolean isRequired() {
        return true;
    }

    /**
     * @return a fresh jME translucent-bucket filter for the post chain
     */
    public static Filter createFilter() {
        return new TranslucentBucketFilter();
    }
}
