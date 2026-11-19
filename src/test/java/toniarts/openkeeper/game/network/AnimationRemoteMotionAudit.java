/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.game.network;

import com.jme3.math.Transform;
import com.jme3.network.Client;
import com.jme3.network.ClientStateListener;
import com.jme3.network.Network;
import com.jme3.network.Server;
import com.jme3.network.serializing.Serializer;
import com.simsilica.es.EntitySet;
import com.simsilica.es.base.DefaultEntityData;
import com.simsilica.es.client.EntityDataClientService;
import com.simsilica.es.server.EntityDataHostedService;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.DatagramSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import toniarts.openkeeper.game.component.CreatureMotion;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.SimulationTime;
import toniarts.openkeeper.view.animation.MotionHistory;
import toniarts.openkeeper.view.animation.PresentationClock;

/** Opt-in localhost TCP/Zay-ES acceptance; no rendered camera, gameplay, socket-byte or native-fidelity claim. */
public final class AnimationRemoteMotionAudit {
    private static final String GAME = "OpenKeeper-Animation-Audit";

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Expected output CSV");
        Serializer.initialize();
        var initialize = NetworkServer.class.getDeclaredMethod("initialize");
        initialize.setAccessible(true); initialize.invoke(null);
        var rows = new ArrayList<String>();
        rows.add("mode,source_publications,accepted_motion_publications,messages_received,sampled_steps,immediate_steps,max_component_error,max_sample_lag_seconds");
        for (boolean coalesced : new boolean[]{false, true}) rows.add(run(coalesced));
        Path output = Path.of(args[0]); Files.createDirectories(output.getParent()); Files.write(output, rows);
        System.out.println("REMOTE_MOTION_PASS cases=2 csv=" + output);
    }

    private static String run(boolean coalesced) throws Exception {
        Serializer.setReadOnly(false); // The preceding standalone context is already closed.
        var source = new DefaultEntityData(); var actor = source.createEntity(); var clockId = source.createEntity();
        int port = availablePortBlock();
        Server server = null; Client client = null; EntitySet positions = null, motions = null;
        var messages = new AtomicLong();
        int updates = 0, sampled = 0, immediate = 0, tick = 0;
        double error = 0, lag = 0;
        try {
            server = Network.createServer(GAME, 1, port, port);
            // Match OpenKeeper's three additional reliable channels and actual ES-channel number.
            for (int channel = 1; channel <= 3; channel++) server.addChannel(port + channel);
            var hosted = new EntityDataHostedService(NetworkConstants.ES_CHANNEL, source);
            server.getServices().addService(hosted); server.start();
            client = Network.connectToServer(GAME, 1, "127.0.0.1", port, port);
            var remote = new EntityDataClientService(NetworkConstants.ES_CHANNEL); client.getServices().addService(remote);
            var connected = new CountDownLatch(1);
            client.addClientStateListener(new ClientStateListener() {
                @Override public void clientConnected(Client connection) { connected.countDown(); }
                @Override public void clientDisconnected(Client connection, DisconnectInfo info) { }
            });
            client.addMessageListener((connection, message) -> messages.incrementAndGet());
            client.start();
            if (!connected.await(5, TimeUnit.SECONDS)) throw new AssertionError("Local TCP handshake timed out");
            positions = remote.getEntityData().getEntities(Position.class);
            motions = remote.getEntityData().getEntities(CreatureMotion.class);
            var history = new MotionHistory(8); var transform = new Transform();
            Position latest = null; boolean ownerCreated = false;
            long start = System.nanoTime(), previous = start, nextFlush = start;
            try (var clock = new PresentationClock(remote.getEntityData())) {
                while (System.nanoTime() - start < TimeUnit.SECONDS.toNanos(5)) {
                    long now = System.nanoTime(); double wall = (now - start) / 1e9;
                    while (tick <= 80 && tick * .05 <= wall) {
                        double time = tick * .05;
                        var root = AnimationMotionDeliveryAudit.position(time);
                        var snapshot = new CreatureMotion(time, .05, tick, root, AnimationMotionDeliveryAudit.request(time),
                                time >= 1 && time < 1.25);
                        source.setComponents(actor, root, snapshot);
                        source.setComponent(clockId, new SimulationTime(time, .05)); tick++;
                    }
                    boolean hold = coalesced && wall >= 2.7 && wall < 3.1;
                    if (!hold && now >= nextFlush) {
                        hosted.sendUpdates(); nextFlush = now + (coalesced ? 120_000_000 : 5_000_000);
                    }
                    positions.applyChanges(); boolean changed = motions.applyChanges();
                    for (var entity : positions) latest = entity.get(Position.class);
                    if (latest != null) {
                        if (!ownerCreated) {
                            for (var entity : motions) if (history.add(entity.get(CreatureMotion.class), latest.motionEpoch)) updates++;
                            ownerCreated = true;
                        } else if (changed) {
                            for (var entity : motions.getAddedEntities()) if (history.add(entity.get(CreatureMotion.class), latest.motionEpoch)) updates++;
                            for (var entity : motions.getChangedEntities()) if (history.add(entity.get(CreatureMotion.class), latest.motionEpoch)) updates++;
                        }
                        history.synchronizeEpoch(latest.motionEpoch);
                    }
                    clock.update((now - previous) / 1e9f); previous = now;
                    if (history.isReady() && !history.isImmediate() && history.sample(clock.getTime() - .05, transform)) {
                        sampled++;
                        double expected = history.getSampledTime() + (history.getEpoch() == 12 ? 100 : 0);
                        error = Math.max(error, Math.abs(transform.getTranslation().x - expected));
                        lag = Math.max(lag, Math.max(0, Math.min(4, wall) - history.getSampledTime()));
                        if (history.getSampledRequest().stateStartTime > history.getSampledTime() + 1e-9)
                            throw new AssertionError("Remote future request selected for a historical root");
                    } else if (latest != null && history.isImmediate()) immediate++;
                    if (!client.isConnected()) throw new AssertionError("TCP connection dropped during playback");
                    LockSupport.parkNanos(8_333_333);
                }
                if (tick != 81 || updates < 10 || sampled < 50 || immediate == 0 || !Double.isFinite(error) || error > 1e-5)
                    throw new AssertionError("Incomplete or corrupted remote presentation: updates=" + updates + " error=" + error);
                if (coalesced && updates >= tick) throw new AssertionError("Coalescing path was not exercised");
                if (latest == null || latest.motionEpoch != 12 || Math.abs(latest.position.x - 104) > 1e-5)
                    throw new AssertionError("Final authoritative root did not reach the remote client");
                source.removeEntity(actor);
                long removalDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                do {
                    hosted.sendUpdates(); positions.applyChanges(); motions.applyChanges();
                    if (positions.isEmpty() && motions.isEmpty()) break;
                    LockSupport.parkNanos(5_000_000);
                } while (System.nanoTime() < removalDeadline);
                if (!positions.isEmpty() || !motions.isEmpty()) throw new AssertionError("Remote removal did not retire component sets");
                history.clear();
            }
        } finally {
            try {
                if (positions != null) positions.release();
                if (motions != null) motions.release();
            } finally {
                try { if (client != null && client.isStarted()) client.close(); }
                finally {
                    try { if (server != null && server.isRunning()) server.close(); }
                    finally { source.close(); Serializer.setReadOnly(false); }
                }
            }
        }
        String row = String.format(Locale.ROOT, "%s,%d,%d,%d,%d,%d,%.9f,%.6f",
                coalesced ? "COALESCED_STALL" : "REGULAR_FLUSH", tick, updates, messages.get(), sampled, immediate, error, lag);
        System.out.println("REMOTE_MOTION " + row);
        return row;
    }

    private static int availablePortBlock() throws Exception {
        var loopback = InetAddress.getByName("127.0.0.1");
        for (int attempt = 0; attempt < 20; attempt++) {
            var reservations = new ArrayList<ServerSocket>();
            try {
                var first = new ServerSocket(0, 1, loopback); reservations.add(first);
                int base = first.getLocalPort();
                if (base > 65532) continue;
                for (int offset = 1; offset <= 3; offset++) reservations.add(new ServerSocket(base + offset, 1, loopback));
                try (var udp = new DatagramSocket(base, loopback)) { return base; }
            } catch (java.net.BindException occupied) {
                // Retry a new ephemeral range; never depend on one fixed test port.
            } finally { for (var socket : reservations) socket.close(); }
        }
        throw new AssertionError("No available four-port localhost range");
    }
}
