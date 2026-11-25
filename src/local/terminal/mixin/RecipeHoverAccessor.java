package local.terminal.mixin;

import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.screen.RecipeScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = RecipeScreen.class, remap = false)
public interface RecipeHoverAccessor {
    @Accessor("hoveredWidget") Widget ae2emiterminal$hoveredWidget();
}
