package local.terminal;

import mekanism.api.chemical.ChemicalStack;
import mekanism.api.recipes.SawmillRecipe;

final class MekPatternResources {
    static boolean isChemical(Object ingredient) { return ingredient instanceof ChemicalStack; }

    static boolean randomSecondary(Object recipe) {
        return recipe instanceof SawmillRecipe sawmill && sawmill.getSecondaryChance() < 1;
    }
}
