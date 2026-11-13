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
package toniarts.openkeeper.game.task.objective;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.creature.PartyController;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Thing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Optional integration audit of the authentic stock KWD corpus. No copyrighted
 * map data is copied into the repository; set {@code openkeeper.dk2.maps} to
 * the installed {@code Data/editor/maps} directory to run it.
 */
class StockPartyObjectiveMapTest {

    private static final Path DEFAULT_MAPS = Path.of("C:/GOG Games/Dungeon Keeper 2/Data/editor/maps");

    private static final List<ExpectedObjective> EXPECTED = List.of(
            expected("level1.kwd", 1, 1, Thing.HeroParty.Objective.WAIT),
            expected("Level14.kwd", 0, 1, Thing.HeroParty.Objective.KILL_CREATURES),
            expected("Level14.kwd", 1, 1, Thing.HeroParty.Objective.KILL_CREATURES),
            expected("Level14.kwd", 3, 1, Thing.HeroParty.Objective.STEAL_GOLD),
            expected("Level19.kwd", 0, 1, Thing.HeroParty.Objective.WAIT),
            expected("Level19.kwd", 1, 1, Thing.HeroParty.Objective.WAIT),
            expected("Level19.kwd", 2, 1, Thing.HeroParty.Objective.WAIT),
            expected("Level20.kwd", 1, 1, Thing.HeroParty.Objective.STEAL_GOLD),
            expected("Level20.kwd", 9, 1, Thing.HeroParty.Objective.WAIT),
            expected("level3.kwd", 1, 1, Thing.HeroParty.Objective.STEAL_GOLD),
            expected("level3.kwd", 3, 1, Thing.HeroParty.Objective.STEAL_GOLD),
            expected("level3.kwd", 6, 1, Thing.HeroParty.Objective.STEAL_GOLD),
            expected("level3.kwd", 7, 1, Thing.HeroParty.Objective.STEAL_GOLD),
            expected("level5.kwd", 2, 1, Thing.HeroParty.Objective.STEAL_GOLD),
            expected("Level6a.kwd", 2, 1, Thing.HeroParty.Objective.STEAL_GOLD),
            expected("level8.kwd", 6, 2, Thing.HeroParty.Objective.WAIT));

    @Test
    void stockMapsAuthorOnlyTheExpectedSixteenLeaderOrders() throws Exception {
        Path mapsDirectory = Path.of(System.getProperty("openkeeper.dk2.maps", DEFAULT_MAPS.toString()));
        Assumptions.assumeTrue(Files.isDirectory(mapsDirectory),
                "Install DK2 or set -Dopenkeeper.dk2.maps to its Data/editor/maps directory");
        // KwdFile expects the game installation root and resolves its sibling
        // Data/Editor/Maps files from there.
        Path dataDirectory = mapsDirectory.getParent().getParent().getParent();

        List<Path> mapFiles;
        try (var files = Files.list(mapsDirectory)) {
            mapFiles = files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".kwd"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)))
                    .toList();
        }

        List<ExpectedObjective> actualLeaderObjectives = new ArrayList<>();
        int partyMemberCount = 0;
        int nonLeaderCandidateCount = 0;
        int leaderKillCount = 0;
        int leaderStealCount = 0;
        int leaderWaitCount = 0;
        boolean verifiedThiefLedStealParty = false;

        for (Path mapFile : mapFiles) {
            IKwdFile map = KwdFile.load(dataDirectory.toString(), mapFile);
            for (Thing.HeroParty party : map.getThings(Thing.HeroParty.class)) {
                List<Thing.GoodCreature> members = party.getHeroPartyMembers();
                long leaderFlags = members.stream()
                        .filter(member -> member.getFlags() != null
                                && member.getFlags().contains(Thing.Creature.CreatureFlag.LEADER))
                        .count();
                assertEquals(1, leaderFlags, "Stock party " + party.getId() + " in " + mapFile.getFileName()
                        + " must define exactly one leader");
                Thing.GoodCreature leader = PartyController.selectLeaderMember(party);
                if (leader != null && leader.getObjective() == Thing.HeroParty.Objective.STEAL_GOLD) {
                    assertTrue(members.stream().map(member -> map.getCreature(member.getCreatureId()))
                                    .anyMatch(PartyObjectiveActorSelector::canStealGold),
                            "Stock STEAL_GOLD party " + party.getId() + " in " + mapFile.getFileName()
                                    + " needs at least one theft-capable member");
                }
                if (mapFile.getFileName().toString().equalsIgnoreCase("level3.kwd") && party.getId() == 6) {
                    var leaderCreature = map.getCreature(leader.getCreatureId());
                    assertNotNull(leaderCreature, "Level 3 party 6 leader creature definition");
                    assertFalse(PartyObjectiveActorSelector.canStealGold(leaderCreature),
                            "Level 3 party 6 leader delegates theft to a Thief");
                    assertTrue(members.stream().filter(member -> member != leader)
                                    .map(member -> map.getCreature(member.getCreatureId()))
                                    .anyMatch(PartyObjectiveActorSelector::canStealGold),
                            "Level 3 party 6 contains an eligible Thief/Rogue follower");
                    verifiedThiefLedStealParty = true;
                }
                partyMemberCount += members.size();
                for (int i = 0; i < members.size(); i++) {
                    Thing.GoodCreature member = members.get(i);
                    if (!isCandidate(member.getObjective())) {
                        continue;
                    }
                    if (member != leader) {
                        nonLeaderCandidateCount++;
                        continue;
                    }
                    String filename = mapFile.getFileName().toString();
                    actualLeaderObjectives.add(expected(filename, party.getId(), i + 1, member.getObjective()));
                    switch (member.getObjective()) {
                        case KILL_CREATURES -> leaderKillCount++;
                        case STEAL_GOLD -> leaderStealCount++;
                        case WAIT -> leaderWaitCount++;
                        default -> throw new IllegalStateException("Unexpected candidate objective " + member.getObjective());
                    }
                }
            }
        }

        assertEquals(66, mapFiles.size());
        assertEquals(947, partyMemberCount);
        assertEquals(42, nonLeaderCandidateCount);
        assertEquals(2, leaderKillCount);
        assertEquals(8, leaderStealCount);
        assertEquals(6, leaderWaitCount);
        assertTrue(verifiedThiefLedStealParty, "Level 3 party 6 must be included in the stock map corpus");
        assertEquals(EXPECTED.size(), actualLeaderObjectives.size());
        assertTrue(actualLeaderObjectives.containsAll(EXPECTED));
        assertTrue(EXPECTED.containsAll(actualLeaderObjectives));
    }

    private static boolean isCandidate(Thing.HeroParty.Objective objective) {
        return objective == Thing.HeroParty.Objective.KILL_CREATURES
                || objective == Thing.HeroParty.Objective.STEAL_GOLD
                || objective == Thing.HeroParty.Objective.WAIT;
    }

    private static ExpectedObjective expected(String filename, int partyId, int memberNumber,
            Thing.HeroParty.Objective objective) {
        return new ExpectedObjective(filename.toLowerCase(Locale.ROOT), (short) partyId, memberNumber, objective);
    }

    private record ExpectedObjective(String filename, short partyId, int memberNumber,
            Thing.HeroParty.Objective objective) {
    }
}
