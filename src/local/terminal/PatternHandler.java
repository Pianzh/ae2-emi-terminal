package local.terminal;

import appeng.api.stacks.*;
import appeng.client.api.integrations.emi.EmiStackConverters;
import appeng.integration.modules.itemlists.EncodingHelper;
import appeng.menu.me.items.PatternEncodingTermMenu;
import dev.emi.emi.api.recipe.*;
import dev.emi.emi.api.recipe.handler.*;
import dev.emi.emi.api.stack.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.*;

final class PatternHandler<T extends PatternEncodingTermMenu> implements EmiRecipeHandler<T> {
    record Plan(List<List<GenericStack>> inputs, List<GenericStack> outputs, String error, boolean externalChemical) {
        static Plan failure(String reason) { return new Plan(List.of(), List.of(), reason, false); }
    }

    // Retain only the hovered recipe. JEI layout construction is too costly per rendered frame.
    private EmiRecipe lastRecipe;
    private Plan lastPlan;

    @Override public EmiPlayerInventory getInventory(AbstractContainerScreen<T> screen) {
        return TerminalHandler.networkInventory(screen.getMenu());
    }

    @Override public boolean alwaysDisplaySupport(EmiRecipe recipe) { return false; }
    @Override public void render(EmiRecipe recipe, EmiCraftContext<T> context, List<dev.emi.emi.api.widget.Widget> widgets,
                                 net.minecraft.client.gui.GuiGraphicsExtractor graphics) {
        MissingOverlay.draw(recipe, context.getInventory(), widgets, graphics);
    }
    @Override public boolean supportsRecipe(EmiRecipe recipe) {
        return !recipe.getInputs().isEmpty() && !recipe.getOutputs().isEmpty();
    }

    private Plan plan(EmiRecipe recipe) {
        if (lastRecipe != recipe) {
            try {
                Plan data = ModList.get().isLoaded("jei") ? JeiPatterns.read(recipe) : null;
                lastPlan = data == null ? readEmi(recipe) : data;
            } catch (RuntimeException ex) {
                com.mojang.logging.LogUtils.getLogger().warn("Could not read pattern recipe {}", recipe.getId(), ex);
                lastPlan = Plan.failure("无法读取这个配方的输入或产物");
            }
            lastRecipe = recipe;
        }
        return lastPlan;
    }

    static GenericStack convert(EmiStack stack, long count) {
        if (count <= 0) return null;
        if (stack.getKey() instanceof Item) {
            var key = AEItemKey.of(stack.getItemStack());
            return key == null ? null : new GenericStack(key, count);
        }
        if (stack.getKey() instanceof Fluid fluid) {
            var sample = new FluidStack(net.minecraft.core.registries.BuiltInRegistries.FLUID.wrapAsHolder(fluid), 1, stack.getComponentChanges());
            return new GenericStack(AEFluidKey.of(sample), count);
        }
        for (var converter : EmiStackConverters.getConverters()) {
            if (converter.getKeyType().isInstance(stack.getKey())) {
                var value = converter.toGenericStack(stack.copy().setAmount(count));
                if (value != null) return value;
            }
        }
        return null;
    }

    private static Plan readEmi(EmiRecipe recipe) {
        var inputs = new ArrayList<List<GenericStack>>();
        for (var ingredient : recipe.getInputs()) {
            if (ingredient.isEmpty()) continue;
            var choices = ingredient.getEmiStacks().stream().map(s -> convert(s, ingredient.getAmount()))
                    .filter(Objects::nonNull).distinct().toList();
            if (choices.isEmpty()) return Plan.failure("AE2 无法表示这项输入");
            inputs.add(choices);
        }
        var outputs = new ArrayList<GenericStack>();
        for (var stack : recipe.getOutputs()) {
            if (stack.getChance() < 1 || stack.isEmpty()) continue;
            var value = convert(stack, stack.getAmount());
            if (value == null) return Plan.failure("AE2 无法表示这项产物");
            outputs.add(value);
        }
        return new Plan(inputs, outputs, null, false);
    }

    private String problem(EmiRecipe recipe, T menu) {
        var p = plan(recipe);
        if (p.error() != null) return p.error();
        if (p.inputs().isEmpty() || p.outputs().isEmpty()) return "缺少可编码的输入或固定产物";
        if (p.externalChemical() && !Minecraft.getInstance().hasShiftDown()) return "若机器已单独供应化学品，按住 Shift 可填入其余材料";
        if (recipe.getCategory() == VanillaEmiRecipeCategories.CRAFTING) {
            var holder = recipe.getBackingRecipe();
            if (holder == null || !(holder.value() instanceof CraftingRecipe crafting)
                    || appeng.util.CraftingRecipeUtil.getIngredients(crafting).size() > 9
                    || crafting instanceof ShapedRecipe s && (s.getWidth() > 3 || s.getHeight() > 3)) return "无法写入 3×3 合成样板";
        } else if (p.inputs().size() > menu.getProcessingInputSlots().length || p.outputs().size() > menu.getProcessingOutputSlots().length) {
            return "配方超过处理样板的槽位数量";
        }
        return null;
    }

    @Override public boolean canCraft(EmiRecipe recipe, EmiCraftContext<T> context) {
        return context.getType() == EmiCraftContext.Type.FILL_BUTTON && problem(recipe, context.getScreenHandler()) == null;
    }

    @Override public List<ClientTooltipComponent> getTooltip(EmiRecipe recipe, EmiCraftContext<T> context) {
        String reason = problem(recipe, context.getScreenHandler());
        if (reason == null) {
            if (plan(recipe).externalChemical()) reason = "填入处理样板（化学品由机器外部供应）";
            else reason = recipe.getCategory() == VanillaEmiRecipeCategories.CRAFTING ? "填入合成样板" : "填入处理样板";
        }
        return List.of(ClientTooltipComponent.create(Component.literal(reason).getVisualOrderText()));
    }

    @Override public boolean craft(EmiRecipe recipe, EmiCraftContext<T> context) {
        if (!canCraft(recipe, context)) return false;
        var menu = context.getScreenHandler();
        var data = plan(recipe);
        if (recipe.getCategory() == VanillaEmiRecipeCategories.CRAFTING) {
            EncodingHelper.encodeCraftingRecipe(menu, recipe.getBackingRecipe(), data.inputs(), stack -> true);
        } else {
            EncodingHelper.encodeProcessingRecipe(menu, data.inputs(), data.outputs());
        }
        Minecraft.getInstance().gui.setScreen(context.getScreen());
        return true;
    }
}
