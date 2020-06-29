package com.wurmonline.server.questions;

import com.google.common.base.Joiner;
import com.wurmonline.server.behaviours.MethodsItems;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemTemplate;
import com.wurmonline.shared.constants.ItemMaterials;

import java.util.Iterator;

class EligibleMaterials {
    private final MaterialGroups materialGroup;
    private final byte specificMaterial;
    private final String specificMaterialOption;

    private static byte[] allWood;
    private static byte[] allMetal;
    private static final byte[] allMeat = new byte[] {
            ItemMaterials.MATERIAL_FLESH,
            ItemMaterials.MATERIAL_MEAT_BEAR,
            ItemMaterials.MATERIAL_MEAT_BEEF,
            ItemMaterials.MATERIAL_MEAT_CANINE,
            ItemMaterials.MATERIAL_MEAT_CAT,
            ItemMaterials.MATERIAL_MEAT_DRAGON,
            ItemMaterials.MATERIAL_MEAT_FOWL,
            ItemMaterials.MATERIAL_MEAT_GAME,
            ItemMaterials.MATERIAL_MEAT_HORSE,
            ItemMaterials.MATERIAL_MEAT_HUMAN,
            ItemMaterials.MATERIAL_MEAT_HUMANOID,
            ItemMaterials.MATERIAL_MEAT_INSECT,
            ItemMaterials.MATERIAL_MEAT_LAMB,
            ItemMaterials.MATERIAL_MEAT_PORK,
            ItemMaterials.MATERIAL_MEAT_SEAFOOD,
            ItemMaterials.MATERIAL_MEAT_SNAKE,
            ItemMaterials.MATERIAL_MEAT_TOUGH
    };
    private static String woodOptions;
    private static String metalOptions;
    private static String meatOptions;
    private static boolean loaded = false;

    private enum MaterialGroups {
        WOOD,
        METAL,
        MEAT,
        SPECIFIC
    }


    EligibleMaterials(ItemTemplate template) {
        if (!loaded) {
            init();
            loaded = true;
        }

        if (template.isWood()) {
            materialGroup = MaterialGroups.WOOD;
        } else if (template.isMetal()) {
            materialGroup = MaterialGroups.METAL;
        } else if (template.isMeat()) {
            materialGroup = MaterialGroups.MEAT;
        } else {
            materialGroup = MaterialGroups.SPECIFIC;
        }

        if (materialGroup == MaterialGroups.SPECIFIC) {
            specificMaterial = template.getMaterial();
            specificMaterialOption = Item.getMaterialString(specificMaterial);
        } else {
            specificMaterial = -1;
            specificMaterialOption = "";
        }
    }

    private void init() {
        byte[] normalWood = MethodsItems.getAllNormalWoodTypes();
        byte[] otherWood = new byte[] { ItemMaterials.MATERIAL_WOOD_ORANGE, ItemMaterials.MATERIAL_WOOD_LINGONBERRY };
        allWood = new byte[normalWood.length + otherWood.length];
        System.arraycopy(normalWood, 0, allWood, 0, normalWood.length);
        System.arraycopy(otherWood, 0, allWood, normalWood.length, otherWood.length);
        woodOptions = Joiner.on(",").join(collectNames(allWood));
        allMetal = MethodsItems.getAllMetalTypes();
        metalOptions = Joiner.on(",").join(collectNames(allMetal));
        meatOptions = Joiner.on(",").join(collectNames(allMeat));
    }

    private Iterator<String> collectNames(byte[] bytes) {
        return new Iterator<String>() {
            private int idx;

            @Override
            public boolean hasNext() {
                return idx < bytes.length;
            }

            @Override
            public String next() {
                return Item.getMaterialString(bytes[idx++]);
            }
        };
    }

    String getOptions() {
        switch(materialGroup) {
            case WOOD:
                return woodOptions;
            case METAL:
                return metalOptions;
            case MEAT:
                return meatOptions;
            default:
                return specificMaterialOption;
        }
    }

    byte getMaterial(int index) throws ArrayIndexOutOfBoundsException {
        switch(materialGroup) {
            case WOOD:
                return allWood[index];
            case METAL:
                return allMetal[index];
            case MEAT:
                return allMeat[index];
            default:
                return specificMaterial;
        }
    }

    int getIndexOf(byte material) {
        byte[] materials;
        switch(materialGroup) {
            case WOOD:
                materials = allWood;
                break;
            case METAL:
                materials = allMetal;
                break;
            case MEAT:
                materials = allMeat;
                break;
            default:
                return 0;
        }

        for (int i = 0; i < materials.length; ++i) {
            if (materials[i] == material)
                return i;
        }

        return 0;
    }
}
