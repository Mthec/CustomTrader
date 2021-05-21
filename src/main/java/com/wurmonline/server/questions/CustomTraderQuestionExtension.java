package com.wurmonline.server.questions;

import com.wurmonline.server.creatures.Creature;
import mod.wurmunlimited.npcs.customtrader.CustomTraderMod;

@SuppressWarnings("SameParameterValue")
public abstract class CustomTraderQuestionExtension extends Question {
    CustomTraderQuestionExtension(Creature aResponder, String aTitle, String aQuestion, int aType, long aTarget) {
        super(aResponder, aTitle, aQuestion, aType, aTarget);
    }

    boolean wasSelected(String id) {
        String val = getAnswer().getProperty(id);
        return val != null && val.equals("true");
    }

    boolean wasAnswered(String id, String desiredValue) {
        String val = getAnswer().getProperty(id);
        return val != null && val.equals(desiredValue);
    }

    float getFloatOrDefault(String id, float _default) {
        String f = getAnswer().getProperty(id);
        if (f != null) {
            try {
                return Float.parseFloat(f);
            } catch (NumberFormatException e) {
                return _default;
            }
        }
        return _default;
    }

    int getIntegerOrDefault(String id, int _default) {
        String f = getAnswer().getProperty(id);
        if (f != null) {
            try {
                return Integer.parseInt(f);
            } catch (NumberFormatException e) {
                return _default;
            }
        }
        return _default;
    }

    String getStringOrDefault(String id, String _default) {
        String f = getAnswer().getProperty(id);
        if (f != null) {
            return f;
        }
        return _default;
    }

    String getPrefix() {
        if (CustomTraderMod.namePrefix.isEmpty()) {
            return "";
        } else {
            return CustomTraderMod.namePrefix + "_";
        }
    }

    String getNameWithoutPrefix(String name) {
        if (CustomTraderMod.namePrefix.isEmpty() || name.length() < CustomTraderMod.namePrefix.length() + 1) {
            return name;
        } else {
            return name.substring(CustomTraderMod.namePrefix.length() + 1);
        }
    }
}
