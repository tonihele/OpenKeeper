/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.game.network;

import com.jme3.math.Transform;
import com.jme3.network.serializing.Serializer;
import com.simsilica.es.EntityComponent;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import com.simsilica.es.net.EntitySerializers;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import toniarts.openkeeper.game.component.CreatureMotion;
import toniarts.openkeeper.game.component.CreatureViewState;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.SimulationTime;
import toniarts.openkeeper.tools.convert.map.Creature.AnimationType;
import toniarts.openkeeper.view.ClientEntityData;
import toniarts.openkeeper.view.animation.MotionHistory;
import toniarts.openkeeper.view.animation.PresentationClock;

/** Deterministic serialized-component delivery, not a socket, camera or gameplay latency benchmark. */
public final class AnimationMotionDeliveryAudit {
    private enum Stream { POSITION, MOTION, CLOCK }
    private enum Delivery { ORDERED, MOTION_FIRST, POSITION_FIRST, COALESCED_STALL }
    private record Packet(double arrival, int order, Stream stream, byte[] bytes) { }
    private record Bucket(long slot, Stream stream) { }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Expected output CSV");
        Serializer.initialize();
        var initialize = NetworkServer.class.getDeclaredMethod("initialize");
        initialize.setAccessible(true); initialize.invoke(null);
        EntitySerializers.initialize(); // EntityDataHostedService performs this after the game registrations.
        var rows = new ArrayList<String>();
        rows.add("delivery,publisher_hz,render_hz,generated_components,delivered_components,component_payload_bytes,presentation_steps,sampled_steps,immediate_or_fallback_steps,paused_steps,max_root_error");
        for (Delivery delivery : Delivery.values()) for (int ticks : new int[]{20, 30})
            for (int hz : new int[]{60, 120, 144}) rows.add(run(delivery, ticks, hz));
        Path path = Path.of(args[0]); Files.createDirectories(path.getParent()); Files.write(path, rows);
        Position root = position(0);
        var request = request(0);
        var motion = new CreatureMotion(0, .05, 0, root, request, false);
        int ordinary = encode(motion).length;
        motion.hauler = new EntityId(99); motion.immediate = true;
        System.out.printf(Locale.ROOT,
                "MOTION_WIRE_PAYLOAD position=%d request=%d clock=%d motion=%d attached_motion=%d bytes (class tags included; ES/socket framing excluded)%n",
                encode(root).length, encode(request).length, encode(new SimulationTime(0, .05)).length,
                ordinary, encode(motion).length);
        System.out.println("MOTION_DELIVERY_PASS cases=" + (rows.size() - 1) + " csv=" + path);
    }

    private static String run(Delivery delivery, int ticks, int hz) throws Exception {
        var packets = new ArrayList<Packet>();
        int generated = 0;
        for (int tick = 0; tick <= ticks * 4; tick++) {
            double time = (double) tick / ticks;
            double wall = time < 1.4 ? time : time + .4; // Authoritative pause: simulation time stops.
            Position root = position(time);
            var motion = new CreatureMotion(time, 1.0 / ticks, tick, root, request(time),
                    time >= 1 && time < 1.25 || time >= 2.5 && time < 2.75);
            if (time >= 2.5 && time < 2.75) motion.hauler = new EntityId(99);
            for (Stream stream : Stream.values()) {
                double arrival = wall;
                if (delivery == Delivery.MOTION_FIRST) arrival += stream == Stream.POSITION ? .08 : .01;
                if (delivery == Delivery.POSITION_FIRST) arrival += stream == Stream.MOTION ? .08 : .01;
                if (delivery == Delivery.COALESCED_STALL) {
                    arrival += .025 + ((tick * 17 + stream.ordinal() * 7) % 5) * .012;
                    arrival = Math.ceil(arrival / .12) * .12;
                    if (arrival >= 2.7 && arrival < 3.1) arrival = 3.12;
                }
                EntityComponent component = switch (stream) {
                    case POSITION -> root;
                    case MOTION -> motion;
                    case CLOCK -> new SimulationTime(time, 1.0 / ticks);
                };
                packets.add(new Packet(arrival, generated++, stream, encode(component)));
            }
            if (root.position.x != (float) (time + (time >= 2 ? 100 : 0)))
                throw new AssertionError("Serialization mutated an authoritative root");
        }
        if (delivery == Delivery.COALESCED_STALL) {
            var newest = new HashMap<Bucket, Packet>();
            for (Packet packet : packets) newest.put(new Bucket(Math.round(packet.arrival * 1e6), packet.stream), packet);
            packets = new ArrayList<>(newest.values());
        }
        packets.sort(Comparator.comparingDouble(Packet::arrival).thenComparingInt(Packet::order));

        var data = new DefaultEntityData(); var entity = data.createEntity(); var clockId = data.createEntity();
        var client = new ClientEntityData(data);
        var positions = client.getEntities(Position.class); var motions = client.getEntities(CreatureMotion.class);
        int cursor = 0, sampled = 0, immediate = 0, paused = 0, frames = 0;
        long bytes = 0;
        double maxError = 0, lastClock = Double.NEGATIVE_INFINITY;
        Position latestPosition = null;
        var history = new MotionHistory(8); var transform = new Transform();
        float presentedX = 0;
        boolean hasPresented = false;
        try (var clock = new PresentationClock(client)) {
            for (int frame = 0; frame <= hz * 5; frame++) {
                double wall = (double) frame / hz;
                while (cursor < packets.size() && packets.get(cursor).arrival <= wall + 1e-9) {
                    Packet packet = packets.get(cursor++); bytes += packet.bytes.length;
                    var buffer = ByteBuffer.wrap(packet.bytes);
                    var component = (EntityComponent) Serializer.readClassAndObject(buffer);
                    if (buffer.hasRemaining()) throw new AssertionError("Unread component bytes");
                    data.setComponent(packet.stream == Stream.CLOCK ? clockId : entity, component);
                }
                positions.applyChanges(); motions.applyChanges();
                for (var e : positions) latestPosition = e.get(Position.class);
                if (latestPosition != null) {
                    // Like creature construction, seed an existing motion when Position first creates the owner.
                    if (!hasPresented) for (var e : motions) history.add(e.get(CreatureMotion.class), latestPosition.motionEpoch);
                    else {
                        for (var e : motions.getAddedEntities()) history.add(e.get(CreatureMotion.class), latestPosition.motionEpoch);
                        for (var e : motions.getChangedEntities()) history.add(e.get(CreatureMotion.class), latestPosition.motionEpoch);
                    }
                }
                boolean pause = wall >= 1.4 && wall < 1.8;
                clock.setPaused(pause); clock.update(1f / hz);
                if (latestPosition == null || !Double.isFinite(clock.getTime())) continue;
                if (clock.getTime() < lastClock) throw new AssertionError("Clock moved backwards");
                if (pause && Double.isFinite(lastClock) && clock.getTime() != lastClock)
                    throw new AssertionError("Late clock packets advanced paused presentation");
                lastClock = clock.getTime(); frames++;
                if (pause && hasPresented) { paused++; continue; }
                history.synchronizeEpoch(latestPosition.motionEpoch);
                if (history.isReady() && !history.isImmediate()
                        && history.sample(clock.getTime() - history.getInterval(), transform)) {
                    sampled++;
                    double expected = history.getSampledTime() + (history.getEpoch() == 12 ? 100 : 0);
                    maxError = Math.max(maxError, Math.abs(transform.getTranslation().x - expected));
                    if (history.getSampledRequest().stateStartTime > history.getSampledTime() + 1e-9)
                        throw new AssertionError("A future action was selected for a historical root");
                    presentedX = transform.getTranslation().x;
                } else {
                    immediate++; presentedX = latestPosition.position.x;
                }
                if (!Float.isFinite(presentedX)) throw new AssertionError("Non-finite presented root");
                hasPresented = true;
            }
            if (cursor != packets.size() || sampled == 0 || immediate == 0 || paused == 0 || maxError > 1e-5)
                throw new AssertionError("Incomplete or incoherent delivery: " + delivery + " " + ticks + "/" + hz + " error=" + maxError);
            data.removeEntity(entity); positions.applyChanges(); motions.applyChanges(); history.clear();
            history.synchronizeEpoch(12);
            if (!positions.isEmpty() || !motions.isEmpty() || history.isReady() || history.getHauler() != null)
                throw new AssertionError("Removed owner's presentation survived");
        } finally { positions.release(); motions.release(); data.close(); }
        String row = String.format(Locale.ROOT, "%s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%.9f",
                delivery, ticks, hz, generated, packets.size(), bytes, frames, sampled, immediate, paused, maxError);
        System.out.println("MOTION_DELIVERY " + row);
        return row;
    }

    private static Position position(double time) {
        var result = new Position((float) Math.toRadians(time < 2 ? 179 : -179),
                (float) (time + (time >= 2 ? 100 : 0)), .25f, 0);
        result.motionEpoch = time >= 2 ? 12 : 11;
        return result;
    }

    private static CreatureViewState request(double time) {
        return new CreatureViewState((short) 1, time >= 3 ? 3 : time >= 2 ? 2 : 0,
                time >= 2 ? AnimationType.RUN : AnimationType.WALK, time >= 3 ? 2 : time >= 2 ? 1 : 0);
    }

    private static byte[] encode(Object component) throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(512);
        Serializer.writeClassAndObject(buffer, component);
        return Arrays.copyOf(buffer.array(), buffer.position());
    }
}
