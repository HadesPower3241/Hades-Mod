package dev.hadesclient.hud.widget;
import dev.hadesclient.hud.HudCategory;
import dev.hadesclient.hud.HudWidget;
import dev.hadesclient.module.Setting;
import dev.hadesclient.render.Draw;
import dev.hadesclient.theme.Color;
import dev.hadesclient.theme.Theme;
import net.minecraft.client.gui.DrawContext;
public final class FpsWidget extends HudWidget {
    private final Setting.Bool brackets=setting(new Setting.Bool("brackets","Show Brackets",true));
    private final Setting.Bool reverse=setting(new Setting.Bool("reverse","Reverse Order",false));
    private final Setting.ColorVal textColor=setting(new Setting.ColorVal("textColor","Text Color",0x55FFFF));
    private final Setting.ColorVal bgColor=setting(new Setting.ColorVal("bgColor","Bg Color",0x121216));
    private final Setting.ColorVal borderColor=setting(new Setting.ColorVal("borderColor","Border Color",0x373741));
    public FpsWidget(){super("fps","FPS");defaults(Anchor.TOP_LEFT,8f,8f,true);}
    @Override public HudCategory category(){return HudCategory.GENERAL;}
    @Override public String description() { return "Displays your current frames per second."; }
    @Override public void render(DrawContext g,Theme theme,float x,float y){
        int fps=mc().getCurrentFps();
        String t=reverse.get()?"FPS "+fps:fps+" FPS";
        if(brackets.get())t="["+t+"]";
        float p=3f,w=Draw.textWidth(t)+p*2,h=Draw.textHeight()+p*2-2f;
        size(w,h);
        if(showBg())Draw.roundRect(g,x,y,w,h,0f,bgColor.color(bgAlpha()));
        if(showBorder())Draw.roundOutline(g,x,y,w,h,0f,borderW(),borderColor.color(borderAlpha()));
        Color c=textColor.color(txtAlpha());
        txt(g,t,x+p,y+p-1f,c);
    }
}
