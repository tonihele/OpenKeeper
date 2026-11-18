/*
 * Copyright (C) 2026 OpenKeeper
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package toniarts.openkeeper.view.material;

import com.jme3.material.Material;
import com.jme3.scene.Spatial;
import java.util.List;

/** Injectable jME preparation boundary. Implementations must return instance-owned materials. */
@FunctionalInterface
public interface MaterialProvider {
    /** Collects changed geometry, preserving ownership of materials in {@code previous}. */
    List<Material> prepare(Spatial root, MaterialSetupPolicy policy, List<Material> previous);
}
