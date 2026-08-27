package dev.hadesclient.render;
import dev.hadesclient.HadesClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
public final class FontManager {
    public enum FontChoice {
        MINECRAFT("minecraft","Minecraft Default",null),
        CLEAN("clean","Poppins",Identifier.of("hadesclient","clean")),
        CLEAN_MEDIUM("clean_medium","Poppins Medium",Identifier.of("hadesclient","clean_medium")),
        CLEAN_BOLD("clean_bold","Poppins Bold",Identifier.of("hadesclient","clean_bold")),
        CLEAN_LIGHT("clean_light","Poppins Light",Identifier.of("hadesclient","clean_light")),
        MODERN("modern","Noto Sans",Identifier.of("hadesclient","modern"));
        private final String id;public final String displayName;public final Identifier fontId;
        FontChoice(String id,String dn,Identifier fi){this.id=id;displayName=dn;fontId=fi;}
        public String id(){return id;}public String displayName(){return displayName;}
        public Identifier fontId(){return fontId;}
        public static FontChoice byId(String id){for(FontChoice c:values())if(c.id.equals(id))return c;return MINECRAFT;}
    }
    private FontChoice current=FontChoice.MODERN;
    private Style cachedStyle=Style.EMPTY;
    private boolean verified=false,working=true;
    public FontChoice current(){return current;}
    public void set(FontChoice c){
        if(current!=c){current=c;verified=false;working=true;
            try{cachedStyle=c.fontId==null?Style.EMPTY:Style.EMPTY.withFont(new StyleSpriteSource.Font(c.fontId));}
            catch(Throwable t){HadesClient.LOG.error("[HADES][FONT] {}",t.getMessage());cachedStyle=Style.EMPTY;working=false;}
        }
    }
    public void setById(String id){set(FontChoice.byId(id));}
    public Style style(){
        if(!verified&&current.fontId!=null&&working){verified=true;
            try{var tr=MinecraftClient.getInstance().textRenderer;
                if(tr!=null){int v=tr.getWidth("TEST"),c=tr.getWidth(Text.literal("TEST").setStyle(cachedStyle));
                    if(v==c)HadesClient.LOG.info("[HADES][FONT] '{}' width matches vanilla (may load later)",current.displayName);
                    else HadesClient.LOG.info("[HADES][FONT] '{}' OK v={}px c={}px",current.displayName,v,c);
                }
            }catch(Throwable t){HadesClient.LOG.info("[HADES][FONT] verify: {}",t.getMessage());}
        }
        return working?cachedStyle:Style.EMPTY;
    }
}
