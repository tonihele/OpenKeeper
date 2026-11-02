import toniarts.openkeeper.tools.convert.map.*;
import java.lang.reflect.*;
class DumpGemDefinitions {
 static void dump(Object value) throws Exception {for(var method:value.getClass().getMethods())if(method.getName().startsWith("get")&&method.getParameterCount()==0&&!method.getName().equals("getClass")){Object v=method.invoke(value);System.out.println(method.getName()+"="+v);if(v instanceof ArtResource art){System.out.println("Art name="+art.getName()+" type="+art.getType()+" flags="+art.getFlags());for(String key:art.getDataKeys())System.out.println("Art "+key+"="+art.getData(key));}if(v instanceof toniarts.openkeeper.utils.Color c)System.out.println("RGB="+c.getRed()+","+c.getGreen()+","+c.getBlue());}}
 public static void main(String[]a)throws Exception {var k=KwdFile.load("/home/wietse/Downloads/Data/Bullfrog/Dungeon Keeper 2/","Level1");System.out.println("EFFECT257");dump(k.getEffect(257));System.out.println("ELEMENT444");dump(k.getEffectElement(444));}
}
