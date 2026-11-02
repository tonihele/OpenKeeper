import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.*;
import com.jme3.math.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.controller.*;
import toniarts.openkeeper.game.logic.IGameLogicUpdatable;
import toniarts.openkeeper.game.state.*;
import toniarts.openkeeper.game.state.loop.GameLoopManager;
import toniarts.openkeeper.tools.convert.map.*;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.view.PlayerCameraState;

/** A local showcase using the production game controller and map renderer. */
public class ShowRoomOrder {
    static boolean capture;
    static Object field(Object o, String n) throws Exception { var f=o.getClass().getDeclaredField(n); f.setAccessible(true); return f.get(o); }
    static Object mainCall(String n,Class<?>[] types,Object...args)throws Exception {var m=Main.class.getDeclaredMethod(n,types);m.setAccessible(true);return m.invoke(null,args);}
    public static void main(String[] args)throws Exception {
        capture=Arrays.asList(args).contains("--capture");
        java.nio.file.Files.createDirectories(java.nio.file.Path.of("/tmp/openkeeper-room-showcase/frames"));
        mainCall("parseArguments",new Class[]{String[].class},(Object)new String[]{"-level","TrainMixed______","-nomovies"});
        var c=Main.class.getDeclaredConstructor();c.setAccessible(true);Main app=c.newInstance();
        mainCall("initSettings",new Class[]{Main.class},app);
        var settings=Main.getUserSettings().getAppSettings();
        settings.setResolution(1280,800);settings.setFullscreen(false);settings.setFrameRate(60);settings.setAudioRenderer("LWJGL");
        settings.setTitle("OpenKeeper - DK II room sweep showcase (#155)");
        app.setSettings(settings);app.setShowSettings(false);app.setPauseOnLostFocus(false);
        app.getStateManager().attach(new Demo(app));app.start();
    }
    static class Demo extends AbstractAppState {
        final Main app;
        final ConcurrentLinkedQueue<Runnable> commands=new ConcurrentLinkedQueue<>();
        final ScreenshotAppState shots=new ScreenshotAppState("/tmp/openkeeper-room-showcase/frames/","frame-");
        IGameController game;
        com.simsilica.es.EntityData data;
        float captureTime,nextFrame;
        boolean captureComplete;
        Object logicLoop;
        IGameWorldController world;
        BitmapText title,status,help;
        volatile String phase="Loading...";
        volatile int direction=0, actionTick=-1;
        volatile boolean ready,playing=true;
        boolean hooked,slow=true;
        long availableSince;
        int ticks;
        short roomId;
        long normalRate;
        float elapsed;
        int lastShot=-1;
        final int x1=22,y1=36,x2=26,y2=40;
        Demo(Main app){this.app=app;}
        BitmapText label(String text,int y,int size){var t=new BitmapText(app.getAssetManager().loadFont("Interface/Fonts/Default.fnt"));t.setSize(size);t.setText(text);t.setLocalTranslation(28,y,0);app.getGuiNode().attachChild(t);return t;}
        void rate() {
            try {var f=logicLoop.getClass().getDeclaredField("updateRate");f.setAccessible(true);f.setLong(logicLoop,slow?250_000_000L:normalRate);}
            catch(Exception e){throw new RuntimeException(e);}
        }
        void setup()throws Exception {
            var server=app.getStateManager().getState(GameServerState.class);
            var client=app.getStateManager().getState(GameClientState.class);
            var camera=app.getStateManager().getState(PlayerCameraState.class);
            if(server==null||client==null||camera==null||!camera.isInitialized()||client.getFogOfWarInformation()==null)return;
            var mapView=app.getStateManager().getState(toniarts.openkeeper.view.PlayerMapViewState.class);
            if(!client.isEnabled()||mapView==null||!mapView.isMapLoaded())return;
            if(availableSince==0){availableSince=System.nanoTime();return;}
            if(System.nanoTime()-availableSince<2_000_000_000L)return;
            var manager=(GameLoopManager)field(server,"game");if(manager==null)return;
            game=manager.getGameController();world=game.getGameWorldController();data=(com.simsilica.es.EntityData)field(world,"entityData");
            var loops=(List<?>)field(manager,"loops");logicLoop=loops.get(0);normalRate=(long)field(logicLoop,"updateRate");rate();
            var updatables=(IGameLogicUpdatable[])field(field(logicLoop,"gameLoopManager"),"updatables");
            var original=updatables[0];
            updatables[0]=new IGameLogicUpdatable(){
                public void start(){original.start();}public void stop(){original.stop();}
                public void processTick(float tpf){original.processTick(tpf);try{
                    for(Runnable task;(task=commands.poll())!=null;)task.run();
                    if(ready&&playing)advance();
                }catch(Throwable e){e.printStackTrace();phase="Demo error: "+e;playing=false;}}
            };
            title=label("DK II room construction / selling",700,27);
            status=label("Preparing claimed ground...",660,20);
            help=label("Space: pause / play     N: normal / slow speed     F12: screenshot",575,17);
            app.getStateManager().attach(shots);
            app.getInputManager().addMapping("room-demo-play",new KeyTrigger(KeyInput.KEY_SPACE));
            app.getInputManager().addMapping("room-demo-speed",new KeyTrigger(KeyInput.KEY_N));
            app.getInputManager().addMapping("room-demo-shot",new KeyTrigger(KeyInput.KEY_F12));
            app.getInputManager().addListener((ActionListener)(name,pressed,tpf)->{if(!pressed)return;
                if(name.equals("room-demo-play")){playing=!playing; if(playing)((toniarts.openkeeper.utils.GameLoop)logicLoop).resume();else ((toniarts.openkeeper.utils.GameLoop)logicLoop).pause();}
                if(name.equals("room-demo-speed")){slow=!slow;rate();}
                if(name.equals("room-demo-shot"))shots.takeScreenshot();
            },"room-demo-play","room-demo-speed","room-demo-shot");
            camera.setEnabled(false);
            app.getCamera().setLocation(new Vector3f(24,11,46));
            app.getCamera().lookAt(new Vector3f(24,0,38),Vector3f.UNIT_Y);
            commands.add(()->{
                var kwd=game.getLevelInfo().getLevelData();
                short path=kwd.getTerrainList().stream().filter(t->t.getName().equals("Claimed Path")).findFirst().orElseThrow().getTerrainId();
                roomId=kwd.getRooms().stream().filter(r->r.getName().equals("Training Room")).findFirst().orElseThrow().getRoomId();
                for(int y=y1-2;y<=y2+2;y++)for(int x=x1-2;x<=x2+2;x++){
                    var tile=world.getMapController().getMapData().getTile(x,y);tile.setTerrainId(path);tile.setOwnerId((short)3);
                }
                app.enqueue(() -> {
                    var view=app.getStateManager().getState(toniarts.openkeeper.view.PlayerMapViewState.class);
                    view.onFogOfWarDisabled((short)3);view.setCeilingVisible(false);
                    Main.getUserSettings().setSetting(toniarts.openkeeper.game.data.Settings.Setting.AMBIENT_LIGHT,0.5f);
                });
                world.addGold((short)3,50000);
                ready=true;phase="Ready";
                System.out.println("ROOM_SHOWCASE_READY room="+roomId+" cost="+kwd.getRoomById(roomId).getCost());
            });
            hooked=true;
        }
        void advance(){
            int cycle=52, t=ticks++%cycle;actionTick=t;
            boolean reverseX=direction==1||direction==3, reverseY=direction>=2;
            if(t==5){
                // Reset only the previous demo cycle's loose gold, before the next build.
                for(var id:data.findEntities(null,toniarts.openkeeper.game.component.Gold.class,toniarts.openkeeper.game.component.Position.class)){
                    var point=toniarts.openkeeper.utils.WorldUtils.vectorToPoint(data.getComponent(id,toniarts.openkeeper.game.component.Position.class).position);
                    if(point.x>=x1&&point.x<=x2&&point.y>=y1&&point.y<=y2)data.removeEntity(id);
                }
                world.addGold((short)3,50000);
                phase="BUILD "+(direction+1)+" / 4 - release corner first";
                Vector2f start=new Vector2f(reverseX?x2:x1,reverseY?y2:y1);
                Vector2f end=new Vector2f(reverseX?x1:x2,reverseY?y1:y2);
                world.build(start,end,(short)3,roomId);
                System.out.println("SHOWCASE_BUILD start="+start+" end="+end+" validation="+
                    RoomPlacementValidator.validate(game.getLevelInfo().getLevelData(),world.getMapController(),Set.of(),start,end,(short)3,roomId,game.getLevelInfo().getPlayers().get((short)3).getGold()).failure());
            }
            if(t==23){
                long count=0;for(int y=y1;y<=y2;y++)for(int x=x1;x<=x2;x++)if(world.getMapController().getMapData().getTile(x,y).getTerrainId()==game.getLevelInfo().getLevelData().getRoomById(roomId).getTerrainId())count++;
                System.out.println("SHOWCASE_BUILT="+count);if(count!=25)throw new IllegalStateException("Expected 25 built tiles, got "+count);
            }
            if(t==27){world.addGold((short)3,50000);phase="SELL - full storage: excess gold stays on the floor";world.sell(new Vector2f(x1,y1),new Vector2f(x2,y2),(short)3);}
            if(t==40){
                int loose=0;
                for(var id:data.findEntities(null,toniarts.openkeeper.game.component.Gold.class,toniarts.openkeeper.game.component.Position.class)){
                    var point=toniarts.openkeeper.utils.WorldUtils.vectorToPoint(data.getComponent(id,toniarts.openkeeper.game.component.Position.class).position);
                    if(point.x>=x1&&point.x<=x2&&point.y>=y1&&point.y<=y2)loose+=data.getComponent(id,toniarts.openkeeper.game.component.Gold.class).gold;
                }
                System.out.println("SHOWCASE_OVERFLOW_GOLD="+loose);
                // Creatures may collect or spend gold while the showcase runs.
            }
            if(t==51){direction=(direction+1)%4;phase="Next drag direction";}
        }
        public void update(float tpf){
            if(!hooked){try{setup();}catch(Exception e){e.printStackTrace();app.stop();}return;}
            elapsed+=tpf;
            status.setText(phase+"\n"+(slow?"Slow motion: 4 game ticks / second":"Normal game tick rate")+"  |  "+(playing?"Playing":"Paused")+"\nOne diagonal per tick; tiles, objects and gold update together");
            if(ready&&capture&&!captureComplete){
                captureTime+=tpf;
                if(captureTime>=nextFrame){nextFrame+=0.1f;shots.takeScreenshot();}
                if(captureTime>=54){captureComplete=true;System.out.println("SHOWCASE_CAPTURE_COMPLETE");}
            }
        }
    }
}
