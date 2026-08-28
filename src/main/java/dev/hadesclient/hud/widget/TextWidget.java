package dev.hadesclient.hud.widget;
import dev.hadesclient.module.Setting;
import dev.hadesclient.hud.HudWidget;
import dev.hadesclient.render.Draw;
import dev.hadesclient.theme.Color;
import dev.hadesclient.theme.Theme;
import net.minecraft.client.gui.DrawContext;
public abstract class TextWidget extends HudWidget {
    private static final float PX=4f,PY=2f,GAP=3f;
    private final Setting.ColorVal textColor=setting(new Setting.ColorVal("textColor","Text Color",0xFFFFFF));
    private final Setting.ColorVal labelColor=setting(new Setting.ColorVal("labelColor","Label Color",0x55FFFF));
    private final Setting.ColorVal bgColor=setting(new Setting.ColorVal("bgColor","Bg Color",0x121216));
    private final Setting.ColorVal borderColor=setting(new Setting.ColorVal("borderColor","Border Color",0x373741));
    protected TextWidget(String id,String name){super(id,name);}
    protected abstract String label();
    protected abstract String value();
    protected Color valueColor(Theme theme){return theme.text();}
    @Override public void render(DrawContext g,Theme theme,float x,float y){
        String l=label(),v=value();
        float lw=l==null?0f:Draw.textWidth(l)+GAP;
        float w=PX*2+lw+Draw.textWidth(v),h=PY*2+Draw.textHeight();
        size(w,h);chrome(g,x,y,w,h,0f);
        float ty=y+PY;
        if(l!=null)txt(g,l,x+PX,ty,theme.accent());
        txt(g,v,x+PX+lw,ty,valueColor(theme));
    }
}
