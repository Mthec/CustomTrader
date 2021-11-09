package com.wurmonline.server.questions;

import com.wurmonline.server.items.Recipe;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Base64;

public class Recipes {
    private final Recipe[] recipes = com.wurmonline.server.items.Recipes.getAllRecipes();
    final String options;

    Recipes() {
        StringBuilder sb = new StringBuilder("None");
        for (Recipe value : recipes) {
            sb.append(",");
            sb.append(value.getName().replace(",", "")).append(" - ").append(value.getRecipeId());
        }
        options = sb.toString();
    }

    Recipe getRecipe(int index) {
        if (index <= 0 || index >= recipes.length) {
            return null;
        }

        return recipes[index - 1];
    }

    public static String getInscriptionFor(Recipe recipe) throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final DataOutputStream dos = new DataOutputStream(bos);
        recipe.pack(dos);
        dos.flush();
        dos.close();
        return Base64.getEncoder().encodeToString(bos.toByteArray());
    }
}
