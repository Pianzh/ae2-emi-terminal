package local.terminal;

import appeng.helpers.InventoryAction;
import appeng.menu.me.common.MEStorageMenu;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.stack.*;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.runtime.EmiFavorite;
import dev.emi.emi.screen.EmiScreenManager.SidebarEmiStackInteraction;
import dev.emi.emi.screen.RecipeScreen;
import local.terminal.mixin.RecipeHoverAccessor;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.ModList;

public final class TerminalOrders {
    public static boolean tryOrder(Minecraft client, int x, int y) {
        if (client.player == null || !(client.player.containerMenu instanceof MEStorageMenu menu)) return false;
        var terminal = EmiApi.getHandledScreen();
        if (terminal == null || terminal.getMenu() != menu || menu.getClientRepo() == null) return false;
        var screen = client.gui.screen();
        if (terminal != screen && !(screen instanceof RecipeScreen)) return false;

        var hovered = EmiApi.getHoveredStack(x, y, false);
        EmiStack stack;
        if (hovered instanceof SidebarEmiStackInteraction) {
            stack = displayedStack(hovered.getStack());
        } else if (screen instanceof RecipeScreen recipe && ((RecipeHoverAccessor) recipe).ae2emiterminal$hoveredWidget() instanceof SlotWidget slot) {
            var group = recipe.getGroup(slot);
            if (group == null || !slot.getBounds().contains(x - group.x(), y - group.y())) return false;
            stack = ModList.get().isLoaded("jei") && JeiPatterns.usesCustomRenderer(slot)
                    ? slot.getStack().getEmiStacks().getFirst() : displayedStack(slot.getStack());
        } else return false;

        if (stack.isEmpty()) return false;
        var target = PatternHandler.convert(stack, 1);
        if (target == null) return false;
        for (var entry : menu.getClientRepo().getAllEntries()) {
            if (entry.isCraftable() && target.what().equals(entry.getWhat())) {
                menu.handleInteraction(entry.getSerial(), InventoryAction.AUTO_CRAFT);
                return true;
            }
        }
        return false;
    }

    private static EmiStack displayedStack(EmiIngredient ingredient) {
        if (ingredient instanceof EmiFavorite favorite) return displayedStack(favorite.getStack());
        if (ingredient instanceof ListEmiIngredient list) {
            var options = list.getIngredients();
            if (options.isEmpty()) return EmiStack.EMPTY;
            int index = (int) ((System.currentTimeMillis() / 1000) % options.size());
            return displayedStack(options.get(index));
        }
        var stacks = ingredient.getEmiStacks();
        if (!stacks.isEmpty() && (stacks.size() == 1 || ingredient instanceof TagEmiIngredient)) return stacks.getFirst();
        return EmiStack.EMPTY;
    }
}
