package net.bdew.wurm.archeotweaks;

import com.wurmonline.server.Server;
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
        Server.getInstance().broadCastAlert(String.format("Power=%.3f tier=%d", power, tier));
        if (power >= 90 && tier < ArcheoTweaksMod.minTierAtPower90) {
            return ArcheoTweaksMod.minTierAtPower90;
        } else if (power >= 70 && tier < ArcheoTweaksMod.minTierAtPower70) {
            return ArcheoTweaksMod.minTierAtPower70;
        } else if (power >= 50 && tier < ArcheoTweaksMod.minTierAtPower50) {
            return ArcheoTweaksMod.minTierAtPower50;
        }
        return tier;
    }

    public static void respawnArchaeology() {
        Zones.createInvestigatables();
    }
}
