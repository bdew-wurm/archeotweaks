package net.bdew.wurm.archeotweaks;

import com.wurmonline.server.NoSuchItemException;
import com.wurmonline.server.NoSuchPlayerException;
import com.wurmonline.server.Server;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.NoSuchCreatureException;
import com.wurmonline.server.items.FragmentUtilities;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.players.AchievementList;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.skills.SkillList;
import com.wurmonline.server.villages.DeadVillage;
import com.wurmonline.server.villages.Villages;
import com.wurmonline.server.zones.NoSuchZoneException;
import com.wurmonline.server.zones.Zones;
import net.bdew.wurm.archeotweaks.journal.JournalItems;
import net.bdew.wurm.archeotweaks.journal.JournalTools;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.gotti.wurmunlimited.modsupport.properties.ModPlayerProperties;
import org.gotti.wurmunlimited.modsupport.properties.Property;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Hooks {
    static private AdvancedItemIdParser idParser;

    static private final String CACHE_POWER_PROP = "bdew.archeotweaks.cachepower";

    private static String archLog;
    private static LinkedList<String> journalMessages = new LinkedList<>();
    private static boolean noPaper = false;
    private static double lastDiffMult = 1;
    private static double cachePowerAdd = -1;

    public static final ConcurrentHashMap<Long, DeadVillage> deadVillages;


    static {
        try {
            deadVillages = ReflectionUtil.getPrivateField(null, ReflectionUtil.getField(Villages.class, "deadVillages"));
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException("unable to get deadvillages", e);
        }
    }


    public static int[] getLootList(int tier) {
        if (idParser == null) idParser = new AdvancedItemIdParser();
        int[] list = idParser.parseListSafe(tier == -1 ? ArcheoTweaksMod.cacheFragments : ArcheoTweaksMod.tierLists[tier]);
        ArcheoTweaksMod.logInfo(String.format("Prepared tier %d list - %d entries", tier, list.length));
        return list;
    }

    public static int bumpTierToSkill(double power, int tier) {
        if (archLog != null) archLog += String.format(" - fragPower=%.1f tier=%d", power, tier);
        if (power >= 90 && tier < ArcheoTweaksMod.minTierAtPower90) {
            if (archLog != null) ArcheoTweaksMod.logInfo(archLog + " - BUMP TO " + ArcheoTweaksMod.minTierAtPower90);
            archLog = null;
            return ArcheoTweaksMod.minTierAtPower90;
        } else if (power >= 70 && tier < ArcheoTweaksMod.minTierAtPower70) {
            if (archLog != null) ArcheoTweaksMod.logInfo(archLog + " - BUMP TO " + ArcheoTweaksMod.minTierAtPower70);
            archLog = null;
            return ArcheoTweaksMod.minTierAtPower70;
        } else if (power >= 50 && tier < ArcheoTweaksMod.minTierAtPower50) {
            if (archLog != null) ArcheoTweaksMod.logInfo(archLog + " - BUMP TO " + ArcheoTweaksMod.minTierAtPower50);
            archLog = null;
            return ArcheoTweaksMod.minTierAtPower50;
        }
        if (archLog != null) ArcheoTweaksMod.logInfo(archLog + " - NO BUMP");
        archLog = null;
        return tier;
    }

    public static void logArch(Creature perfomer, double archSkill, double negBonus, double tileMax, double power2) {
        double diff = Math.max(tileMax, archSkill / 5.0) * lastDiffMult;
        archLog = String.format("Archeology for %s - skill=%.1f negBonus=%.1f tileMax=%.1f power2=%.1f diff=%.1f diffMult=%.3f"
                , perfomer.getName(), archSkill, negBonus, tileMax, power2, diff, lastDiffMult);
        lastDiffMult = 1;
    }

    public static void respawnArchaeology() {
        Zones.createInvestigatables();
    }

    // 0 - location partial
    // 1 - location full
    // 2 - getMayorName
    // 3 - getFounderName
    // 4 - getTimeSinceDisband
    // 5 - getTotalAge

    public static void journalHook(Creature performer, DeadVillage deed, int mode) {
        Item report = JournalTools.findLog(performer, deed, true);

        if (report == null) {
            noPaper = true;
            return;
        }

        boolean added = false;

        switch (mode) {
            case 0:
                int locLevel = JournalTools.getLocationLevel(report);
                if (locLevel == 0) {
                    locLevel = (int) (performer.getSkills().getSkillOrLearn(SkillList.ARCHAEOLOGY).getKnowledge() / 40) + 1;
                    journalMessages.add(String.format("You note the clues to the location of %s in the report, finding more will let you narrow it down further.", deed.getDeedName()));
                    added = true;
                    JournalTools.setLocationLevel(report, locLevel);
                } else if (locLevel < 7) {
                    int bonus = (int) (performer.getSkills().getSkillOrLearn(SkillList.ARCHAEOLOGY).getKnowledge() / 40);
                    locLevel = Math.min(locLevel + 1 + bonus, 7);
                    if (locLevel < 4) {
                        journalMessages.add(String.format("You note some clues to the location of %s in the report, finding more will let you narrow it down further.", deed.getDeedName()));
                    } else if (locLevel < 7) {
                        journalMessages.add(String.format("You note more clues to the location of %s in the report, you think you are close to finding it's true location.", deed.getDeedName()));
                    } else {
                        journalMessages.add(String.format("With the clues you gathered, you now know the exact location of %s, which you write down in the report.", deed.getDeedName()));
                    }
                    added = true;
                    JournalTools.setLocationLevel(report, locLevel);
                }
                break;
            case 1:
                if (JournalTools.getLocationLevel(report) < 7) {
                    journalMessages.add(String.format("You add the location of %s to the report.", deed.getDeedName()));
                    JournalTools.setLocationLevel(report, 7);
                    added = true;
                }
                break;
            case 2:
                if (!JournalTools.isLastMayorKnown(report)) {
                    journalMessages.add(String.format("You add the name of %s, the last mayor of %s to the report.", deed.getMayorName(), deed.getDeedName()));
                    JournalTools.setLastMayorKnown(report);
                    added = true;
                }
                break;
            case 3:
                if (!JournalTools.isFounderKnown(report)) {
                    journalMessages.add(String.format("You add the name of %s, the founder of %s to the report.", deed.getFounderName(), deed.getDeedName()));
                    JournalTools.setFounderKnown(report);
                    added = true;
                }
                break;
            case 4:
                if (!JournalTools.isDisbandKnown(report)) {
                    journalMessages.add(String.format("You add the disband date of %s to the report.", deed.getDeedName()));
                    JournalTools.setDisbandKnown(report);
                    added = true;
                }
                break;
            case 5:
                if (!JournalTools.isAgeKnown(report)) {
                    journalMessages.add(String.format("You add the age of %s to the report.", deed.getDeedName()));
                    JournalTools.setAgeKnown(report);
                    added = true;
                }
                break;
            default:
                ArcheoTweaksMod.logWarning(String.format("Unknown mode %d in journalHook", mode));
        }

        if (added) {
            report.setQualityLevel(JournalTools.fillLevel(report));
            if (report.getAuxData() == 127)
                journalMessages.add(String.format("The report on %s is now complete!", deed.getDeedName()));
        }
    }

    public static void cachePowerHook(double power) {
        cachePowerAdd = power;
    }

    public static void endInvestigateHook(Creature performer) {
        if (archLog != null) {
            ArcheoTweaksMod.logInfo(archLog + " - FAIL");
            archLog = null;
        }
        if (!journalMessages.isEmpty()) {
            journalMessages.forEach(performer.getCommunicator()::sendNormalServerMessage);
            journalMessages.clear();
        }
        if (noPaper) {
            performer.getCommunicator().sendNormalServerMessage("You would be able to record some findings if you had a sheet of paper.");
            noPaper = false;
        }
        if (cachePowerAdd > 0 && ArcheoTweaksMod.cacheTotalPower > 0) {
            List<DeadVillage> hasReports =
                    Villages.getDeadVillagesFor(performer.getTileX(), performer.getTileY()).stream()
                            .filter(i -> {
                                Item log = JournalTools.findLog(performer, i, false);
                                return log != null && JournalTools.isFullReport(log);
                            }).collect(Collectors.toList());
            if (!hasReports.isEmpty()) {
                DeadVillage dv = hasReports.get(Server.rand.nextInt(hasReports.size()));
                List<Property> powers = ModPlayerProperties.getInstance().getPlayerProperties(CACHE_POWER_PROP, performer.getWurmId());
                double power = powers.stream().mapToDouble(Property::getNumValue).max().orElse(0) + cachePowerAdd;

                if (!powers.isEmpty())
                    ModPlayerProperties.getInstance().deletePlayerProperties(CACHE_POWER_PROP, performer.getWurmId());

                if (ArcheoTweaksMod.extraInfoLogging)
                    ArcheoTweaksMod.logInfo(String.format("Cache power for %s - added: %.1f - now: %.1f - needed: %.1f", performer.getName(), cachePowerAdd, power, ArcheoTweaksMod.cacheTotalPower));

                if (power >= ArcheoTweaksMod.cacheTotalPower) {
                    if (ArcheoTweaksMod.extraInfoLogging)
                        ArcheoTweaksMod.logInfo(String.format("Spawning cache for %s", performer.getName()));
                    final Item cache = FragmentUtilities.createVillageCache((Player) performer, JournalTools.findLog(performer, dv, false), dv, performer.getSkills().getSkillOrLearn(10069));
                    if (cache != null) {
                        performer.getCommunicator().sendAlertServerMessage(String.format("You pull a %s out of the ground!", cache.getName()), (byte) 2);
                        Server.getInstance().broadCastAction(performer.getName() + " pulls a " + cache.getName() + " from the ground.", performer, 5);
                        performer.achievement(AchievementList.ACH_ARCH_CACHE);
                        try {
                            cache.putItemInfrontof(performer);
                        } catch (NoSuchCreatureException | NoSuchZoneException | NoSuchPlayerException | NoSuchItemException e) {
                            ArcheoTweaksMod.logException("Error placing cache", e);
                            performer.getCommunicator().sendNormalServerMessage("An error occurred. Please try again later or contact /support.", (byte) 2);
                            ModPlayerProperties.getInstance().setPlayerProperty(CACHE_POWER_PROP, performer.getWurmId(), (float) power);
                        }
                    } else {
                        performer.getCommunicator().sendNormalServerMessage("An error occurred. Please try again later or contact /support.", (byte) 2);
                        ModPlayerProperties.getInstance().setPlayerProperty(CACHE_POWER_PROP, performer.getWurmId(), (float) power);
                    }
                } else {
                    if (power >= ArcheoTweaksMod.cacheTotalPower * 0.75f) {
                        performer.getCommunicator().sendNormalServerMessage("You feel like you're getting close to finding a cache!");
                    } else if (power >= ArcheoTweaksMod.cacheTotalPower * 0.333f) {
                        performer.getCommunicator().sendNormalServerMessage("You feel like you're making progress to finding a cache!");
                    } else {
                        performer.getCommunicator().sendNormalServerMessage("You feel like you're on the right path to finding a cache!");
                    }
                    ModPlayerProperties.getInstance().setPlayerProperty(CACHE_POWER_PROP, performer.getWurmId(), (float) power);
                }

                cachePowerAdd = -1f;
            }
        }
    }

    public static boolean blockMove(Item source, Item target, Creature performer) {
        if (target.getTemplateId() == JournalItems.journalId) {
            if (!Utils.isCleanPaper(source) && source.getTemplateId() != JournalItems.logId) {
                performer.getCommunicator().sendNormalServerMessage("Only reports and clean paper sheets go into the journal.");
                return true;
            }
        }
        return false;
    }

    public static double diffMult(Creature performer, List<DeadVillage> targets) {
        double maxKnown = 0;
        for (DeadVillage dv : targets) {
            Item rep = JournalTools.findLog(performer, dv, false);
            if (rep != null && rep.getQualityLevel() > maxKnown) maxKnown = rep.getQualityLevel();
        }
        lastDiffMult = (1.0 - (maxKnown / 200.0));
        return lastDiffMult;
    }
}
