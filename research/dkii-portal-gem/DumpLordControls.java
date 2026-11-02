import toniarts.openkeeper.tools.convert.map.*;
import toniarts.openkeeper.tools.convert.kmf.*;
import toniarts.openkeeper.tools.convert.wad.*;
import java.nio.file.*;
class DumpLordControls {
 public static void main(String[]a)throws Exception{
  var k=KwdFile.load("/home/wietse/Downloads/Data/Bullfrog/Dungeon Keeper 2/","Level1");
  var w=new WadFile(Path.of("/home/wietse/Downloads/Data/Bullfrog/Dungeon Keeper 2/Data/Meshes.WAD"));
  var c=k.getCreature((short)29);
  for(var type:new Creature.AnimationType[]{Creature.AnimationType.STAND_STILL,Creature.AnimationType.WALK,Creature.AnimationType.MELEE_ATTACK,Creature.AnimationType.DIE}){
   var art=c.getAnimation(type);if(art==null)continue;String name=w.getWadFileEntries().stream().filter(s->s.equalsIgnoreCase(art.getName()+".kmf")).findFirst().orElseThrow();
   var f=new KmfFile(w.getFileData(name));System.out.println(type+" "+name);
   if(f.getAnim()!=null) for(var control:f.getAnim().getControls())System.out.println("CTRL "+control.getUnknown1()+" "+control.getUnknown2()+" "+control.getUnknown3());
   if(f.getMesh()!=null) for(var control:f.getMesh().getControls())System.out.println("MESHCTRL "+control.getUnknown1()+" "+control.getUnknown2());
  }
 }
}
