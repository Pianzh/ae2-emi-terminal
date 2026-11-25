package local.terminal;

import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.Widget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.neoforged.fml.ModList;
import java.util.*;

final class MissingOverlay {
    private record Input(SlotWidget slot, EmiIngredient ingredient) {}

    static void draw(EmiRecipe recipe, EmiPlayerInventory inventory, List<Widget> widgets, GuiGraphicsExtractor graphics) {
        var remaining = new ArrayList<>(recipe.getInputs());
        var visible = new ArrayList<Input>();
        boolean jei = ModList.get().isLoaded("jei");
        for (var widget : widgets) {
            if (!(widget instanceof SlotWidget slot) || slot.getRecipe() != null || slot.getStack().isEmpty()) continue;
            var shown = slot.getStack();
            Boolean consumed = jei ? JeiPatterns.consumes(recipe, slot) : null;
            if (Boolean.FALSE.equals(consumed)) continue;
            int match = find(remaining, shown);
            if (match >= 0) visible.add(new Input(slot, remaining.remove(match)));
            else if (Boolean.TRUE.equals(consumed)) visible.add(new Input(slot, shown));
        }

        // Prefer a specific ingredient over a tag when they compete for the same stack.
        // Stable ordering makes shortages of repeated ingredients appear on the later slots.
        visible.sort(Comparator.comparingInt(i -> i.ingredient().getEmiStacks().size()));
        var reserved = new IdentityHashMap<EmiStack, Long>();
        for (var input : visible) {
            boolean available = false;
            for (var option : input.ingredient().getEmiStacks()) {
                var stock = inventory.inventory.get(option);
                if (stock == null) continue;
                long count = Math.max(input.ingredient().getAmount(), option.getAmount());
                long used = reserved.getOrDefault(stock, 0L);
                if (stock.getAmount() - used >= count) {
                    reserved.put(stock, used + count);
                    available = true;
                    break;
                }
            }
            if (!available) {
                // RecipeScreen has already translated the pose to this recipe's origin.
                var b = input.slot().getBounds();
                graphics.fill(b.left(), b.top(), b.right(), b.bottom(), 0x66FF2020);
            }
        }
    }

    private static int find(List<EmiIngredient> inputs, EmiIngredient shown) {
        for (int i = 0; i < inputs.size(); i++) if (inputs.get(i) == shown) return i;
        for (int i = 0; i < inputs.size(); i++) {
            var input = inputs.get(i);
            if (input.getAmount() == shown.getAmount() && EmiIngredient.areEqual(input, shown)) return i;
        }
        return -1;
    }
}
