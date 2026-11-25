package local.terminal;

import appeng.api.stacks.GenericStack;
import appeng.client.integrations.jei.GenericEntryStackHelper;
import appeng.recipes.handlers.InscriberProcessType;
import appeng.recipes.handlers.InscriberRecipe;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.jemi.JemiPlugin;
import dev.emi.emi.jemi.JemiRecipe;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.world.item.crafting.RecipeHolder;
import java.util.*;

final class JeiPatterns {
    static boolean usesCustomRenderer(dev.emi.emi.api.widget.SlotWidget slot) {
        if (!(slot instanceof dev.emi.emi.jemi.widget.JemiSlotWidget js) || js.slot.renderers == null) return false;
        return js.slot.getDisplayedIngredient().map(i -> js.slot.renderers.containsKey(i.getType())).orElse(false);
    }

    // Null means this widget needs ordinary EMI input matching (e.g. a tank widget).
    static Boolean consumes(EmiRecipe recipe, dev.emi.emi.api.widget.SlotWidget widget) {
        if (!(recipe instanceof JemiRecipe<?> imported) || !(widget instanceof dev.emi.emi.jemi.widget.JemiSlotWidget js)) return null;
        return js.slot.getRole() == RecipeIngredientRole.INPUT && consumesNamedInput(imported, js.slot.getSlotName().orElse(""));
    }

    private static boolean consumesNamedInput(JemiRecipe<?> recipe, String name) {
        Object raw = recipe.recipe instanceof RecipeHolder<?> holder ? holder.value() : recipe.recipe;
        if (raw instanceof InscriberRecipe press && (name.equals("top") || name.equals("bottom")))
            return press.getProcessType() == InscriberProcessType.PRESS;
        return !recipe.catalystSlotNames.contains(name);
    }
    static PatternHandler.Plan read(EmiRecipe recipe) {
        return recipe instanceof JemiRecipe<?> imported ? readLayout(imported) : null;
    }

    private static <R> PatternHandler.Plan readLayout(JemiRecipe<R> recipe) {
        var runtime = JemiPlugin.runtime;
        if (runtime == null) return PatternHandler.Plan.failure("JEI 配方尚未就绪");
        var layout = runtime.getRecipeManager().createRecipeLayoutDrawable(recipe.category, recipe.recipe,
                runtime.getJeiHelpers().getFocusFactory().getEmptyFocusGroup());
        if (layout.isEmpty()) return PatternHandler.Plan.failure("无法读取 JEI 配方槽位");
        Object raw = recipe.recipe instanceof RecipeHolder<?> holder ? holder.value() : recipe.recipe;
        var inputs = new ArrayList<List<GenericStack>>();
        var outputs = new ArrayList<GenericStack>();
        boolean hasMek = net.neoforged.fml.ModList.get().isLoaded("mekanism");
        boolean externalChemical = false;
        int outputIndex = 0;
        for (var slot : layout.get().getRecipeSlotsView().getSlotViews()) {
            var role = slot.getRole();
            if (role != RecipeIngredientRole.INPUT && role != RecipeIngredientRole.OUTPUT) continue;
            if (role == RecipeIngredientRole.OUTPUT && outputIndex++ == 1 && hasMek && MekPatternResources.randomSecondary(raw)) continue;
            String name = slot.getSlotName().orElse("");
            if (role == RecipeIngredientRole.INPUT && !consumesNamedInput(recipe, name)) continue;
            var values = slot.getAllIngredientsList();
            if (values.isEmpty()) continue;
            var choices = values.stream().map(GenericEntryStackHelper::ingredientToStack)
                    .filter(Objects::nonNull).filter(s -> s.amount() > 0).distinct().toList();
            if (choices.isEmpty()) {
                if (role == RecipeIngredientRole.INPUT && hasMek && values.stream().allMatch(v -> MekPatternResources.isChemical(v.getIngredient()))) {
                    externalChemical = true;
                    continue;
                }
                return PatternHandler.Plan.failure("AE2 无法编码此配方中的 " + values.getFirst().getType().getIngredientClass().getSimpleName());
            }
            if (role == RecipeIngredientRole.INPUT) inputs.add(choices);
            else if (choices.size() == 1) outputs.add(choices.getFirst());
            else return PatternHandler.Plan.failure("这个产物槽含有多个候选结果，无法确定固定产物");
        }
        return new PatternHandler.Plan(inputs, outputs, null, externalChemical);
    }
}
