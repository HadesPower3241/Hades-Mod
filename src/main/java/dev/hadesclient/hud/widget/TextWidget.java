package dev.hadesclient.hud.widget;
import dev.hadesclient.hud.HudWidget;
import dev.hadesclient.render.Draw;
import dev.hadesclient.theme.Color;
import dev.hadesclient.theme.Theme;
import net.minecraft.client.gui.DrawContext;
public abstract class TextWidget extends HudWidget {
    private static final float PX=6f,PY=3f,GAP=4f;
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
