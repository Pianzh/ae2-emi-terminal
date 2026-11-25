package local.terminal;

import appeng.menu.me.items.CraftingTermMenu;
import appeng.menu.me.items.WirelessCraftingTermMenu;
import de.mari_023.ae2wtlib.wct.WCTMenu;
import de.mari_023.ae2wtlib.wet.WETMenu;
import appeng.menu.me.items.PatternEncodingTermMenu;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import net.neoforged.fml.common.Mod;

@Mod("ae2emiterminal")
@EmiEntrypoint
public final class TerminalPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        registry.addRecipeHandler(CraftingTermMenu.TYPE, new TerminalHandler<>());
        registry.addRecipeHandler(WirelessCraftingTermMenu.TYPE, new TerminalHandler<>());
        registry.addRecipeHandler(WCTMenu.TYPE, new TerminalHandler<>());
        registry.addRecipeHandler(PatternEncodingTermMenu.TYPE, new PatternHandler<>());
        registry.addRecipeHandler(WETMenu.TYPE, new PatternHandler<>());
    }
}
