import toniarts.openkeeper.tools.convert.map.*;
import java.util.*;
class DumpLavaAssets {
 static void art(String role, ArtResource a) {
  if(a!=null) System.out.printf("%s name=%s type=%s flags=%s%n",role,a.getName(),a.getType(),a.getFlags());
  if(a!=null) for(var key:a.getDataKeys()) System.out.printf("  %s=%s%n",key,a.getData(key));
 }
 static String rgb(toniarts.openkeeper.utils.Color c) { return c.getRed()+","+c.getGreen()+","+c.getBlue(); }
 public static void main(String[] args) {
  var k=KwdFile.load(args[0], "Level1");
  for(var t:k.getTerrainList()) if(t.getFlags().contains(Terrain.TerrainFlag.LAVA)||t.getFlags().contains(Terrain.TerrainFlag.WATER)) {
   System.out.printf("TERRAIN %d %s flags=%s depth=%f wibbleH=%d leanH=%s wibbleV=%d leanV=%s frames=%d ambient=%s terrainLight=%s%n",t.getTerrainId(),t.getName(),t.getFlags(),t.getDepth(),t.getWibbleH(),Arrays.toString(t.getLeanH()),t.getWibbleV(),Arrays.toString(t.getLeanV()),t.getTextureFrames(),rgb(t.getAmbientLight()),rgb(t.getTerrainLight()));
   art("complete",t.getCompleteResource());art("top",t.getTopResource());art("side",t.getSideResource());
  }
 }
}
