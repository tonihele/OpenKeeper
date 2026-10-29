import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.system.*;
import com.jme3.util.BufferUtils;
import de.lessvoid.nifty.render.batch.BatchRenderConfiguration;
import de.lessvoid.nifty.builder.ControlBuilder;
import de.lessvoid.nifty.elements.Element;
import toniarts.openkeeper.gui.nifty.flowlayout.CustomScroll;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.*;
public class PagingControlsRenderProbe extends SimpleApplication {
    private de.lessvoid.nifty.Nifty nifty;
    private int frame;
    private boolean passed;
    private final java.util.concurrent.CountDownLatch done=new java.util.concurrent.CountDownLatch(1);
    private final List<Element> panels=new ArrayList<>();
    private final List<CustomScroll> controls=new ArrayList<>();
    private final int[] counts={2,12,41,12};
    @Override public void simpleInitApp(){try{
        flyCam.setEnabled(false);setDisplayFps(false);setDisplayStatView(false);
        assetManager.registerLocator("assets",FileLocator.class);assetManager.registerLocator("assets/Converted",FileLocator.class);assetManager.registerLocator("/tmp",FileLocator.class);
        var config=new BatchRenderConfiguration();config.atlasWidth=2048;config.atlasHeight=2048;config.useHighQualityTextures=true;
        var display=NiftyJmeDisplay.newNiftyJmeDisplay(assetManager,inputManager,audioRenderer,guiViewPort,config);guiViewPort.addProcessor(display);nifty=display.getNifty();
        StringBuilder xml=new StringBuilder("<nifty><registerEffect name=\"simpleHintEx\" class=\"toniarts.openkeeper.gui.nifty.effect.SimpleHintEx\"/><useStyles filename=\"nifty-default-styles.xml\"/><useControls filename=\"nifty-default-controls.xml\"/><useControls filename=\"Interface/MyControls.xml\"/><screen id=\"test\" controller=\"de.lessvoid.nifty.screen.DefaultScreenController\"><layer childLayout=\"vertical\" backgroundColor=\"#203040ff\">");
        for(int i=0;i<counts.length;i++)xml.append("<panel height=\"180px\" width=\"").append(i==3?440:980).append("px\" padding=\"10px\" childLayout=\"center\"><control id=\"case-").append(i).append("\" name=\"customScroll\" maxRows=\"2\"/></panel>");
        xml.append("</layer></screen></nifty>");Files.writeString(Path.of("/tmp/paging-controls-probe.xml"),xml);nifty.fromXml("paging-controls-probe.xml","test");
        String[] images={"lair","hatchery","training","dark_library","workshop","prison"};
        for(int c=0;c<counts.length;c++){
            Element panel=nifty.getCurrentScreen().findElementById("case-"+c);panels.add(panel);
            CustomScroll control=panel.getControl(CustomScroll.class);controls.add(control);
            for(int j=0;j<counts[c];j++){
                var icon=new ControlBuilder("icon-"+c+"-"+j,"guiIcon");
                icon.parameter("image","Textures/GUI/Rooms/"+images[j%images.length]+".png");icon.parameter("hoverImage","Textures/GUI/Icons/hilight.png");icon.parameter("activeImage","Textures/GUI/Icons/selected-room.png");icon.parameter("hint","");icon.parameter("tooltip","");icon.parameter("click","");
                control.addElement(icon);
            }
        }
    }catch(Throwable e){fail(e);}}
    private void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private List<Element> rows(Element panel){return panel.findElementById("#content").getChildren().stream().filter(e->!e.getChildren().isEmpty()).toList();}
    private void save(String name)throws Exception{
        var bytes=BufferUtils.createByteBuffer(1000*720*4);renderer.readFrameBuffer(null,bytes);var out=new BufferedImage(1000,720,BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<720;y++)for(int x=0;x<1000;x++){int i=((719-y)*1000+x)*4;out.setRGB(x,y,(Byte.toUnsignedInt(bytes.get(i))<<16)|(Byte.toUnsignedInt(bytes.get(i+1))<<8)|Byte.toUnsignedInt(bytes.get(i+2)));}
        ImageIO.write(out,"png",Path.of("/tmp/"+name+".png").toFile());
    }
    private void fail(Throwable e){e.printStackTrace();done.countDown();stop();}
    @Override public void simpleUpdate(float tpf){try{
        frame++;
        if(frame==30){
            for(int c=0;c<panels.size();c++){
                Element panel=panels.get(c),forward=panel.findElementById("#forward"),back=panel.findElementById("#back"),content=panel.findElementById("#content");
                System.out.println("CASE "+c+" arrows="+forward.getX()+","+forward.getY()+" "+forward.getWidth()+"x"+forward.getHeight()+" / "+back.getX()+","+back.getY()+" rows="+rows(panel).size()+" rowWidths="+rows(panel).stream().map(Element::getWidth).toList()+" content="+content.getWidth());
                check(forward.getX()==back.getX()&&forward.getX()+forward.getWidth()<=content.getX(),"Arrows must both be left of content");
                check(forward.getY()+forward.getHeight()==back.getY(),"Arrows must stack without a gap");
                check(forward.getWidth()==32&&back.getWidth()==32,"Arrow width changed");
                check(rows(panel).size()==(c==0?1:2),"Unexpected row count");
                check(forward.isEnabled()==(c>=2)&&back.isEnabled()==(c>=2),"Both arrows should be enabled exactly when paging is needed");
                for(Element row:rows(panel)) {
                    Element previous=null;
                    for(Element item:row.getChildren()) {
                        check(item.getWidth()>0&&item.getHeight()>0,"Zero-sized icon");
                        if(previous!=null)check(item.getX()==previous.getX()+previous.getWidth()+4,"Gap or overlap between columns");
                        previous=item;
                    }
                }
            }
            save("paging-controls-before-scroll");
            var screenshot=ImageIO.read(Path.of("/tmp/paging-controls-before-scroll.png").toFile());
            var dark=panels.get(0).findElementById("#forward");var bright=panels.get(2).findElementById("#forward");
            for(int y=0;y<dark.getHeight();y++)for(int x=0;x<dark.getWidth();x++) {
                int d=screenshot.getRGB(dark.getX()+x,dark.getY()+y),b=screenshot.getRGB(bright.getX()+x,bright.getY()+y);
                for(int shift:new int[]{0,8,16})check(Math.abs(((d>>shift)&255)-Math.round(((b>>shift)&255)*100f/255f))<=2,"Disabled RGB does not match native dimming");
            }
            for(int step=0;step<40;step++)controls.forEach(CustomScroll::forward);
        }
        if(frame==40){
            for(int c=0;c<panels.size();c++){
                var rows=rows(panels.get(c));System.out.println("SCROLLED "+c+" "+rows.stream().map(Element::getX).toList());
                check(rows.stream().map(e->e.getConstraintX().getValueAsInt(1)).distinct().count()==1,"Paged rows drifted apart");
                if(c<2)for(Element row:rows)check(row.getConstraintX().getValueAsInt(1)==0,"Non-overflowing panel scrolled");
                else {
                    for(Element row:rows)check(row.getConstraintX().getValueAsInt(1)<0,"Overflowing panel did not scroll");
                    check(panels.get(c).findElementById("#forward").isEnabled()&&panels.get(c).findElementById("#back").isEnabled(),"Arrows dimmed at a paging boundary");
                }
            }
            save("paging-controls-after-scroll");for(int step=0;step<40;step++)controls.forEach(CustomScroll::back);
        }
        if(frame==50){
            for(Element panel:panels)for(Element row:rows(panel))check(row.getConstraintX().getValueAsInt(1)==0,"Back did not return to start");
            controls.get(2).removeAll();
        }
        if(frame==60){
            check(rows(panels.get(2)).isEmpty(),"removeAll left icons behind");
            var icon=new ControlBuilder("refill","guiIcon");icon.parameter("image","Textures/GUI/Rooms/lair.png");icon.parameter("hoverImage","Textures/GUI/Icons/hilight.png");icon.parameter("activeImage","Textures/GUI/Icons/selected-room.png");icon.parameter("hint","");icon.parameter("tooltip","");icon.parameter("click","");controls.get(2).addElement(icon);
        }
        if(frame==75){check(rows(panels.get(2)).size()==1,"Rebuilt tab did not return to one row");
            check(!panels.get(2).findElementById("#forward").isEnabled()&&!panels.get(2).findElementById("#back").isEnabled(),"Rebuilt sparse tab still enables paging");passed=true;System.out.println("PASS: sparse, two rows, overflow, narrow viewport, back/forward, rebuild");done.countDown();stop();}
    }catch(Throwable e){fail(e);}}
    public static void main(String[]args)throws Exception{
        var app=new PagingControlsRenderProbe();var settings=new AppSettings(true);settings.setResolution(1000,720);settings.setFrameRate(60);settings.setGammaCorrection(false);settings.setAudioRenderer(null);app.setSettings(settings);app.setShowSettings(false);app.start(JmeContext.Type.OffscreenSurface);if(!app.done.await(45,java.util.concurrent.TimeUnit.SECONDS)||!app.passed){app.stop();throw new AssertionError("Paging probe failed");}
    }
}
