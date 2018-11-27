package net.bdew.wurm.archeotweaks.journal;

import com.wurmonline.server.FailedException;
import com.wurmonline.server.Items;
import com.wurmonline.server.NoSuchItemException;
import com.wurmonline.server.Servers;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemFactory;
import com.wurmonline.server.items.NoSuchTemplateException;
import com.wurmonline.server.villages.DeadVillage;
import net.bdew.wurm.archeotweaks.ArcheoTweaksMod;
import net.bdew.wurm.archeotweaks.Utils;

public class JournalTools {
    public static Item findLog(Creature owner, DeadVillage dv, boolean create) {
        try {
            Item paper = null;

            for (Item i : owner.getInventory().getAllItems(false)) {
                if (i.getTemplateId() == JournalItems.logId && i.getData() == dv.getDeedId())
                    return i;
                else if (paper == null && Utils.isCleanPaper(i))
                    paper = i;
            }
            if (!create || paper == null) return null;

            Item parent = paper.getParent();
            parent.dropItem(paper.getWurmId(), false);
            Items.destroyItem(paper.getWurmId());

            owner.getCommunicator().sendNormalServerMessage("You grab a sheet of paper and start recording your findings...");

            Item newItem = ItemFactory.createItem(JournalItems.logId, 1f, (byte) 0, owner.getName());
            newItem.setData(dv.getDeedId());
            newItem.setAuxData((byte) 0);
            newItem.setName(String.format("Report: %s (%s)", dv.getDeedName(), Servers.getLocalServerName()));
            parent.insertItem(newItem, true);
            return newItem;
        } catch (NoSuchTemplateException | NoSuchItemException | FailedException e) {
            ArcheoTweaksMod.logException("Error in findLog", e);
            return null;
        }
    }

    public static int getLocationLevel(Item report) {
        return report.getAuxData() & 0x07;
    }

    public static void setLocationLevel(Item report, int level) {
        report.setAuxData((byte) ((report.getAuxData() & 0xF8) | level));
    }

    public static boolean isLocationKnown(Item report) {
        return (report.getAuxData() & 0x07) == 0x07;
    }

    public static boolean isLastMayorKnown(Item report) {
        return (report.getAuxData() & 0x08) != 0;
    }

    public static void setLastMayorKnown(Item report) {
        report.setAuxData((byte) (report.getAuxData() | 0x08));
    }

    public static boolean isFounderKnown(Item report) {
        return (report.getAuxData() & 0x10) != 0;
    }

    public static void setFounderKnown(Item report) {
        report.setAuxData((byte) (report.getAuxData() | 0x10));
    }

    public static boolean isDisbandKnown(Item report) {
        return (report.getAuxData() & 0x20) != 0;
    }

    public static void setDisbandKnown(Item report) {
        report.setAuxData((byte) (report.getAuxData() | 0x20));
    }

    public static boolean isAgeKnown(Item report) {
        return (report.getAuxData() & 0x40) != 0;
    }

    public static void setAgeKnown(Item report) {
        report.setAuxData((byte) (report.getAuxData() | 0x40));
    }

    public static boolean isFullReport(Item report) {
        return report.getAuxData() == 127;
    }

    public static int fillLevel(Item report) {
        if (isLocationKnown(report)) {
            int level = 60;
            if (isLastMayorKnown(report)) level += 10;
            if (isFounderKnown(report)) level += 10;
            if (isDisbandKnown(report)) level += 10;
            if (isAgeKnown(report)) level += 10;
            return level;
        } else {
            return 10 + getLocationLevel(report) * 5;
        }
    }
}
