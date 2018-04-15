package net.bdew.wurm.archeotweaks;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.zones.Zones;

public class Hooks {
    static private AdvancedItemIdParser idParser;

    public static int[] getLootList(int tier) {
        if (idParser == null) idParser = new AdvancedItemIdParser();
        int[] list = idParser.parseListSafe(ArcheoTweaksMod.tierLists[tier]);
        ArcheoTweaksMod.logInfo(String.format("Prepared tier %d list - %d entries", tier, list.length));
        return list;
    }

    public static int bumpTierToSkill(double power, int tier) {
        if (lastArch != null) lastArch += String.format(" - fragPower=%.1f tier=%d", power, tier);
        if (power >= 90 && tier < ArcheoTweaksMod.minTierAtPower90) {
            if (lastArch != null) ArcheoTweaksMod.logInfo(lastArch + " - BUMP TO " + ArcheoTweaksMod.minTierAtPower90);
            lastArch = null;
            return ArcheoTweaksMod.minTierAtPower90;
        } else if (power >= 70 && tier < ArcheoTweaksMod.minTierAtPower70) {
            if (lastArch != null) ArcheoTweaksMod.logInfo(lastArch + " - BUMP TO " + ArcheoTweaksMod.minTierAtPower70);
            lastArch = null;
            return ArcheoTweaksMod.minTierAtPower70;
        } else if (power >= 50 && tier < ArcheoTweaksMod.minTierAtPower50) {
            if (lastArch != null) ArcheoTweaksMod.logInfo(lastArch + " - BUMP TO " + ArcheoTweaksMod.minTierAtPower50);
            lastArch = null;
            return ArcheoTweaksMod.minTierAtPower50;
        }
        if (lastArch != null) ArcheoTweaksMod.logInfo(lastArch + " - NO BUMP");
        lastArch = null;
        return tier;
    }

    private static String lastArch;

    public static void logArch(Creature perfomer, double archSkill, double negBonus, double tileMax, double power2) {
        double diff = Math.max(tileMax, archSkill / 5.0);
        lastArch = String.format("Archeology for %s - skill=%.1f negBonus=%.1f tileMax=%.1f power2=%.1f diff=%.1f"
                , perfomer.getName(), archSkill, negBonus, tileMax, power2, diff);
    }

    public static void respawnArchaeology() {
        Zones.createInvestigatables();
    }
}
