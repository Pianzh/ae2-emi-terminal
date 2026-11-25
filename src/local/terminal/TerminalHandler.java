package local.terminal;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.integration.modules.itemlists.CraftingHelper;
import appeng.integration.modules.itemlists.TransferHelper;
import appeng.menu.SlotSemantics;
import appeng.menu.me.items.CraftingTermMenu;
import appeng.util.CraftingRecipeUtil;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.EmiRecipeHandler;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.crafting.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

final class TerminalHandler<T extends CraftingTermMenu> implements EmiRecipeHandler<T> {
    @Override
    public EmiPlayerInventory getInventory(AbstractContainerScreen<T> screen) {
        return networkInventory(screen.getMenu());
    }

    static EmiPlayerInventory networkInventory(appeng.menu.me.common.MEStorageMenu menu) {
        var stacks = new ArrayList<EmiStack>();
        for (var group : List.of(SlotSemantics.PLAYER_INVENTORY, SlotSemantics.PLAYER_HOTBAR, SlotSemantics.CRAFTING_GRID)) {
            if (group == SlotSemantics.CRAFTING_GRID && !(menu instanceof CraftingTermMenu)) continue;
            for (var slot : menu.getSlots(group)) {
                if (slot.hasItem()) stacks.add(EmiStack.of(slot.getItem()));
            }
        }
        var repo = menu.getClientRepo();
        if (repo != null) {
            for (var entry : repo.getAllEntries()) {
                long count = Math.max(entry.getStoredAmount(), entry.getRequestableAmount());
                if (count <= 0) continue;
                if (entry.getWhat() instanceof AEItemKey item) {
                    stacks.add(EmiStack.of(item.getReadOnlyStack(), count));
                } else if (entry.getWhat() instanceof AEFluidKey fluid) {
                    stacks.add(EmiStack.of(fluid.getFluid(), fluid.toStack(1).getComponentsPatch(), count));
                }
            }
        }
        return new EmiPlayerInventory(stacks);
    }

    @Override
    public boolean supportsRecipe(EmiRecipe recipe) {
        if (recipe.getCategory() != VanillaEmiRecipeCategories.CRAFTING) return false;
        var holder = recipe.getBackingRecipe();
        if (holder == null || !(holder.value() instanceof CraftingRecipe crafting)) return false;
        if (crafting instanceof ShapedRecipe s && (s.getWidth() > 3 || s.getHeight() > 3)) return false;
        var inputs = CraftingRecipeUtil.getIngredients(crafting);
        return inputs.size() <= 9 && inputs.stream().anyMatch(java.util.Optional::isPresent);
    }

    private record Availability(int slots, CraftingTermMenu.MissingIngredientSlots missing) {
        boolean canFill() { return slots > missing.missingSlots().size(); }
    }

    private Availability availability(T menu, Recipe<?> recipe) {
        var grid = CraftingRecipeUtil.ensure3by3CraftingMatrix(recipe);
        var inputs = new HashMap<Integer, Ingredient>();
        for (int i = 0; i < grid.size(); i++) {
            if (grid.get(i).isPresent()) inputs.put(i, grid.get(i).get());
        }
        return new Availability(inputs.size(), menu.findMissingIngredients(inputs));
    }

    @Override
    public void render(EmiRecipe recipe, EmiCraftContext<T> context, List<dev.emi.emi.api.widget.Widget> widgets,
                       net.minecraft.client.gui.GuiGraphicsExtractor graphics) {
        MissingOverlay.draw(recipe, context.getInventory(), widgets, graphics);
    }

    @Override
    public boolean canCraft(EmiRecipe recipe, EmiCraftContext<T> context) {
        if (!supportsRecipe(recipe)) return false;
        if (context.getType() == EmiCraftContext.Type.CRAFTABLE) return context.getInventory().canCraft(recipe);
        return availability(context.getScreenHandler(), recipe.getBackingRecipe().value()).canFill();
    }

    @Override
    public List<ClientTooltipComponent> getTooltip(EmiRecipe recipe, EmiCraftContext<T> context) {
        if (!supportsRecipe(recipe)) return List.of();
        var missing = availability(context.getScreenHandler(), recipe.getBackingRecipe().value()).missing();
        if (!missing.anyMissingOrCraftable()) return List.of();
        return TransferHelper.createCraftingTooltip(missing, Minecraft.getInstance().hasControlDown(), false)
                .stream().map(line -> ClientTooltipComponent.create(line.getVisualOrderText())).toList();
    }

    @Override
    public boolean craft(EmiRecipe recipe, EmiCraftContext<T> context) {
        if (!supportsRecipe(recipe)) return false;
        var holder = recipe.getBackingRecipe();
        var menu = context.getScreenHandler();
        if (!availability(menu, holder.value()).canFill()) return false;
        var client = Minecraft.getInstance();
        CraftingHelper.performTransfer(menu, holder.id(), holder.value(), client.hasControlDown());
        client.gui.setScreen(context.getScreen());
        return true;
    }
}
