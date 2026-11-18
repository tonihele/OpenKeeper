/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.animation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.EnumMap;
import java.util.ArrayList;
import toniarts.openkeeper.tools.convert.map.*;

/** Reports shipped metadata availability; presence never establishes a gameplay selection rule. */
public final class CreatureCatalogAudit {
    public static void main(String[] args) throws Exception {
        if(args.length != 2) throw new IllegalArgumentException("Expected DK2 root and output CSV");
        var catalog = KwdFile.load(args[0],Path.of(args[0],"Data","Editor","Maps","Alcatraz.kwd"));
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(
                Files.readAllBytes(Path.of(args[0],"Data","Editor","Creatures.kwd"))));
        var rows = new ArrayList<String>(); rows.add("creature_id,creature,action,resource,type,frames,fps,start,end,flags");
        var counts = new EnumMap<Creature.AnimationType,Integer>(Creature.AnimationType.class);
        for(var creature : catalog.getCreatureList()) for(var action : Creature.AnimationType.values()) {
            var art = creature.getAnimation(action);
            boolean available = art != null && art.getType() != ArtResource.ArtResourceType.NONE;
            if(available) counts.merge(action,1,Integer::sum);
            rows.add(creature.getCreatureId()+","+csv(creature.getName())+","+action+","+csv(art == null ? "" : art.getName())+","+
                    (art == null ? "ABSENT" : art.getType())+","+(art == null ? "" : art.getData(ArtResource.KEY_FRAMES))+","+
                    (art == null ? "" : art.getData("fps"))+","+(art != null && art.getFlags().contains(ArtResource.ArtResourceFlag.HAS_START_ANIMATION))+","+
                    (art != null && art.getFlags().contains(ArtResource.ArtResourceFlag.HAS_END_ANIMATION))+","+csv(art == null ? "" : art.getFlags().toString()));
        }
        Path output = Path.of(args[1]); Files.createDirectories(output.toAbsolutePath().getParent()); Files.write(output,rows);
        System.out.println("CATALOG map=Alcatraz creatures="+catalog.getCreatureList().size()+" Creatures.kwd.sha256="+hash+" rows="+(rows.size()-1));
        System.out.println("AVAILABLE "+counts); System.out.println("CSV "+output.toAbsolutePath());
    }
    private static String csv(String text) { return '"'+text.replace("\"","\"\"")+'"'; }
}
