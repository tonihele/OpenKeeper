import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.anim.AnimComposer;
import com.jme3.scene.*;
import com.simsilica.es.base.DefaultEntityData;
import toniarts.openkeeper.tools.convert.map.*;
import toniarts.openkeeper.view.control.CreatureViewControl;
import toniarts.openkeeper.game.component.*;
import toniarts.openkeeper.game.controller.creature.CreatureState;
public class GoblinRunSequenceProbe {
 public static void main(String[] args) {
  var level=KwdFile.load("/home/wietse/Downloads/Data/Bullfrog/Dungeon Keeper 2/","Level1");
  var goblin=level.getCreatureList().stream().filter(c->c.getName().equalsIgnoreCase("Goblin")).findFirst().orElseThrow();
  var assets=new DesktopAssetManager(true);assets.registerLocator("assets",FileLocator.class);assets.registerLocator("assets/Converted",FileLocator.class);
  var data=new DefaultEntityData();var id=data.createEntity();data.setComponents(id,new Owner((short)3,(short)3),new CreatureAi(0,CreatureState.FIGHT,goblin.getCreatureId()));
  var root=new Node("Goblin");var control=new CreatureViewControl(id,data,goblin,Creature.AnimationType.RUN,assets,null);root.addControl(control);
  var start=root.getChild("Start");var run=root.getChild("Goblin-Run");
  System.out.println("Nodes="+root.getChildren().stream().map(s->s.getName()+":"+s.getLocalCullHint()).toList()+" flags="+goblin.getAnimation(Creature.AnimationType.RUN).getFlags());
  if(start==null||run==null||start.getLocalCullHint()!=Spatial.CullHint.Inherit)throw new AssertionError("RUN must begin with its start sequence");
  for(int frame=0;frame<600;frame++)root.updateLogicalState(1f/60);
  if(start.getLocalCullHint()!=Spatial.CullHint.Always||run.getLocalCullHint()!=Spatial.CullHint.Inherit)throw new AssertionError("Start did not hand off to RUN");
  var composer=run.getControl(AnimComposer.class);
  if(!composer.isEnabled()||composer.getCurrentAction()==null||control.isStopAnimation())throw new AssertionError("RUN stopped instead of looping");
  System.out.println("PASS: actual Goblin-RunStart hands off to Goblin-Run and remains looping after 10 seconds");
  control.cleanup();data.close();
 }
}
