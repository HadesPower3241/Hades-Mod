package dev.hadesclient.vault;
import dev.hadesclient.HadesClient;
import net.minecraft.item.ItemStack;
import java.util.*;
import java.util.stream.Collectors;
/** Local-only vault data manager. Stores vault page contents per context. */
public final class VaultManager {
    private final Map<String, List<VaultPage>> vaults = new LinkedHashMap<>();
    private String currentContext = "default";
    public void setContext(String ctx) { currentContext = ctx != null ? ctx : "default"; }
    public String context() { return currentContext; }
    private List<VaultPage> pages() { return vaults.computeIfAbsent(currentContext, k -> new ArrayList<>()); }
    public VaultPage getOrCreate(int pageNum) {
        for (VaultPage p : pages()) if (p.pageNumber() == pageNum) return p;
        VaultPage p = new VaultPage(pageNum); pages().add(p);
        pages().sort(Comparator.comparingInt(VaultPage::pageNumber)); return p;
    }
    public List<VaultPage> allPages() { return Collections.unmodifiableList(pages()); }
    public List<VaultPage> favorites() { return pages().stream().filter(VaultPage::favorite).collect(Collectors.toList()); }
    /** Search across all pages for items matching a query string. */
    public List<SearchResult> search(String query) {
        String q = query.toLowerCase(Locale.ROOT);
        List<SearchResult> results = new ArrayList<>();
        for (VaultPage page : pages()) {
            for (int i = 0; i < page.contents().size(); i++) {
                ItemStack stack = page.contents().get(i);
                if (!stack.isEmpty() && stack.getName().getString().toLowerCase(Locale.ROOT).contains(q)) {
                    results.add(new SearchResult(page, i, stack));
                }
            }
        }
        return results;
    }
    public record SearchResult(VaultPage page, int slot, ItemStack stack) {}
}
