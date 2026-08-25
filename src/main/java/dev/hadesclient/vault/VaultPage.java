package dev.hadesclient.vault;
import net.minecraft.item.ItemStack;
import java.util.*;
/** One page of a player vault with cached contents. */
public final class VaultPage {
    private final int pageNumber;
    private String customName;
    private boolean favorite;
    private final List<ItemStack> contents = new ArrayList<>();
    private long lastUpdated;
    public VaultPage(int pageNumber) { this.pageNumber = pageNumber; }
    public int pageNumber() { return pageNumber; }
    public String customName() { return customName; }
    public void customName(String name) { this.customName = name; }
    public boolean favorite() { return favorite; }
    public void favorite(boolean f) { favorite = f; }
    public List<ItemStack> contents() { return contents; }
    public long lastUpdated() { return lastUpdated; }
    public void updateContents(List<ItemStack> items) {
        contents.clear(); contents.addAll(items); lastUpdated = System.currentTimeMillis();
    }
    public String displayName() { return customName != null ? customName : "PV " + pageNumber; }
}
