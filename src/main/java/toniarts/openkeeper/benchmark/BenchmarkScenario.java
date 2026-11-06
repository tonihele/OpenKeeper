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
package toniarts.openkeeper.benchmark;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import toniarts.openkeeper.game.state.CheatState;
import toniarts.openkeeper.game.state.GameClientState;
import toniarts.openkeeper.game.state.session.GameSessionClientService;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;
import toniarts.openkeeper.view.PlayerCameraState;
import toniarts.openkeeper.view.PlayerMapViewState;
import toniarts.openkeeper.view.lighting.LightRules;

/**
 * Deterministic late-game stress setup: reveal FOW, unlock systems, seed
 * presentation RNGs, flood the dungeon heart with workers + combat units, and
 * drive a fixed camera orbit for a stable GPU/CPU workload.
 */
public final class BenchmarkScenario {

    private final BenchmarkConfig config;
    private final Map<String, Object> report = new LinkedHashMap<>();
    private Vector3f orbitCenter = Vector3f.ZERO;
    private boolean prepared;

    public BenchmarkScenario(BenchmarkConfig config) {
        this.config = config;
    }

    public boolean isPrepared() {
        return prepared;
    }

    public Map<String, Object> getReport() {
        return report;
    }

    /**
     * Applies cheats / spawns once the client map is loaded and the game has
     * started. Safe to call repeatedly; runs at most once.
     */
    public void prepare(GameClientState client, PlayerCameraState cameraState) {
        if (prepared || client == null) {
            return;
        }
        PlayerMapViewState mapView = client.getPlayerMapViewState();
        if (mapView == null || !mapView.isMapLoaded() || !client.isGameStarted()) {
            return;
        }

        GameSessionClientService session = client.getGameClientService();
        IKwdFile kwd = client.getLevelData();

        LightRules lights = mapView.getMapLoader().getLightRules();
        lights.setModulatorSeed(config.getSeed() ^ 0x4C19F7E3L);

        session.triggerCheat(CheatState.CheatType.REMOVE_FOW);
        session.triggerCheat(CheatState.CheatType.MONEY);
        session.triggerCheat(CheatState.CheatType.MANA);
        session.triggerCheat(CheatState.CheatType.UNLOCK_ROOMS);
        session.triggerCheat(CheatState.CheatType.UNLOCK_SPELLS);
        session.triggerCheat(CheatState.CheatType.UNLOCK_DOORS_TRAPS);

        int impsSpawned = 0;
        if (config.getImpCount() > 0) {
            short impId = kwd.getImp().getCreatureId();
            session.triggerSpawnCreatureCheat(impId, 1, config.getImpCount());
            impsSpawned = config.getImpCount();
        }

        List<Creature> combatRoster = combatCreatures(kwd);
        int combatSpawned = spawnCombatWave(session, combatRoster, config.getCreatureCount());

        session.triggerCheat(CheatState.CheatType.LEVEL_MAX);

        Point heart = client.getPlayer().getDungeonHeartLocation();
        if (heart != null) {
            orbitCenter = WorldUtils.pointToVector3f(heart);
            if (cameraState != null && cameraState.isInitialized()) {
                cameraState.setCameraLookAt(orbitCenter);
                cameraState.zoomToPoint(orbitCenter);
            }
        }

        report.put("level", config.getLevel());
        report.put("workloadId", config.getWorkloadId());
        report.put("seed", config.getSeed());
        report.put("impsSpawned", impsSpawned);
        report.put("combatCreaturesSpawned", combatSpawned);
        report.put("combatRosterSize", combatRoster.size());
        report.put("orbitCenter", List.of(orbitCenter.x, orbitCenter.y, orbitCenter.z));
        report.put("cheats", List.of(
                "REMOVE_FOW", "MONEY", "MANA", "UNLOCK_ROOMS", "UNLOCK_SPELLS",
                "UNLOCK_DOORS_TRAPS", "LEVEL_MAX"));
        prepared = true;
    }

    /**
     * Mid-measure reinforcement wave — escalates AI/pathing/render load at a
     * fixed fraction of the run for sustained late-game pressure.
     */
    public void reinforce(GameClientState client, int extraCombat, int extraImps) {
        if (!prepared || client == null) {
            return;
        }
        GameSessionClientService session = client.getGameClientService();
        IKwdFile kwd = client.getLevelData();
        int imps = 0;
        if (extraImps > 0) {
            session.triggerSpawnCreatureCheat(kwd.getImp().getCreatureId(), 1, extraImps);
            imps = extraImps;
        }
        int combat = spawnCombatWave(session, combatCreatures(kwd), extraCombat);
        session.triggerCheat(CheatState.CheatType.LEVEL_MAX);
        report.put("reinforceImps", imps);
        report.put("reinforceCombat", combat);
    }

    /**
     * Deterministic camera orbit as a function of elapsed benchmark seconds
     * (not frame index), so the path is stable across machines even when FPS
     * differs.
     */
    public void updateCamera(PlayerCameraState cameraState, double elapsedSeconds) {
        applyCamera(cameraState, elapsedSeconds, false, 0);
    }

    /**
     * Fixed viewpoint for visual regression — identical pose for a given
     * checkpoint index regardless of wall-clock FPS.
     */
    public void applyCheckpointCamera(PlayerCameraState cameraState, int checkpointIndex) {
        applyCamera(cameraState, 0, true, checkpointIndex);
    }

    private void applyCamera(PlayerCameraState cameraState, double elapsedSeconds,
            boolean checkpoint, int checkpointIndex) {
        if (!prepared || cameraState == null || !cameraState.isInitialized()) {
            return;
        }
        float angle;
        float radius;
        float height;
        if (checkpoint) {
            // Four cardinal-ish views around the heart
            angle = checkpointIndex * FastMath.HALF_PI;
            radius = 24f;
            height = 16f + (checkpointIndex % 2) * 4f;
        } else {
            angle = (float) (elapsedSeconds * (FastMath.TWO_PI / 20.0));
            radius = 28f;
            height = 18f + 4f * FastMath.sin(angle * 0.5f);
        }
        Vector3f look = orbitCenter.add(0f, 1.5f, 0f);
        Vector3f eye = look.add(
                FastMath.cos(angle) * radius,
                height,
                FastMath.sin(angle) * radius);
        cameraState.setCameraLookAt(look);
        if (cameraState.getCamera() != null) {
            cameraState.getCamera().getCamera().setLocation(eye);
            cameraState.getCamera().getCamera().lookAt(look, Vector3f.UNIT_Y);
        }
    }

    private static List<Creature> combatCreatures(IKwdFile kwd) {
        short impId = kwd.getImp().getCreatureId();
        List<Creature> list = new ArrayList<>();
        for (Creature creature : kwd.getCreatureList()) {
            if (creature == null || creature.getCreatureId() == 0 || creature.getCreatureId() == impId) {
                continue;
            }
            if (!creature.getFlags().contains(Creature.CreatureFlag.IS_EVIL)) {
                continue;
            }
            if (creature.getFlags().contains(Creature.CreatureFlag.IS_WORKER)) {
                continue;
            }
            list.add(creature);
        }
        list.sort(Comparator.comparingInt(Creature::getCreatureId));
        return list;
    }

    private static int spawnCombatWave(GameSessionClientService session, List<Creature> roster, int total) {
        if (total <= 0 || roster.isEmpty()) {
            return 0;
        }
        int spawned = 0;
        int perType = Math.max(1, total / roster.size());
        int remaining = total;
        for (int i = 0; i < roster.size() && remaining > 0; i++) {
            int amount = Math.min(perType, remaining);
            // Spread levels 3..10 for AI / animation variety, deterministic by index.
            int level = 3 + (i % 8);
            session.triggerSpawnCreatureCheat(roster.get(i).getCreatureId(), level, amount);
            spawned += amount;
            remaining -= amount;
        }
        if (remaining > 0) {
            Creature first = roster.get(0);
            session.triggerSpawnCreatureCheat(first.getCreatureId(), 10, remaining);
            spawned += remaining;
        }
        return spawned;
    }
}
