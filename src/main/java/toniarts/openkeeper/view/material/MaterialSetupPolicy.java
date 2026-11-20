/*
 * Copyright (C) 2026 OpenKeeper
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package toniarts.openkeeper.view.material;

/** Instance lighting policy; shared image contents are independent of this policy. */
public record MaterialSetupPolicy(boolean ignoreObjectLights, boolean useTileAmbient, boolean fullBright) {
    public static final MaterialSetupPolicy WORLD = new MaterialSetupPolicy(false, true, false);
    public static final MaterialSetupPolicy CREATURE = new MaterialSetupPolicy(false, false, false);
    public static final MaterialSetupPolicy FULL_BRIGHT = new MaterialSetupPolicy(false, false, true);
}
