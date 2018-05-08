package net.bdew.wurm.archeotweaks.journal;

import com.wurmonline.server.FailedException;
import com.wurmonline.server.Items;
import com.wurmonline.server.NoSuchItemException;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.behaviours.Actions;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemFactory;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.items.NoSuchTemplateException;
import com.wurmonline.server.skills.SkillList;
import net.bdew.wurm.archeotweaks.ArcheoTweaksMod;
import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.BehaviourProvider;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.gotti.wurmunlimited.modsupport.actions.ActionPropagation.CONTINUE_ACTION;
import static org.gotti.wurmunlimited.modsupport.actions.ActionPropagation.FINISH_ACTION;
import static org.gotti.wurmunlimited.modsupport.actions.ActionPropagation.NO_ACTION_PERFORMER_PROPAGATION;
import static org.gotti.wurmunlimited.modsupport.actions.ActionPropagation.NO_SERVER_PROPAGATION;

public class MergeJournalAction implements ModAction, ActionPerformer, BehaviourProvider {
    private class MergeInfo {
        private Map<Long, Item> sourceReports, targetReports;
        private Iterator<Long> merge, copyToSource, copyToTarget;
        private int workLeft;
    }

    private ActionEntry actionEntry;
    private WeakHashMap<Action, MergeInfo> actionData;

    public MergeJournalAction() {
        actionEntry = ActionEntry.createEntry((short) ModActions.getNextActionId(), "Merge", "merging", new int[]{
                48 /* ACTION_TYPE_ENEMY_ALWAYS */,
                36 /* ACTION_TYPE_ALWAYS_USE_ACTIVE_ITEM */
        });
        ModActions.registerAction(actionEntry);
        actionData = new WeakHashMap<>();
    }

    @Override
    public short getActionId() {
        return actionEntry.getNumber();
    }

    @Override
    public BehaviourProvider getBehaviourProvider() {
        return this;
    }

    @Override
    public ActionPerformer getActionPerformer() {
        return this;
    }

    public boolean canUse(Creature performer, Item source, Item target) {
        return (performer.isPlayer() && source != null && target != null && source != target &&
                source.getTemplateId() == JournalItems.journalId && source.getTopParent() == performer.getInventory().getWurmId() &&
                target.getTemplateId() == JournalItems.journalId && target.getTopParent() == performer.getInventory().getWurmId());
    }

    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item source, Item target) {
        if (canUse(performer, source, target))
            return Collections.singletonList(actionEntry);
        else
            return null;
    }

    private Map<Long, Item> getReports(Item journal) {
        return journal.getItems().stream()
                .filter(i -> i.getTemplateId() == JournalItems.logId)
                .collect(Collectors.toMap(Item::getData, Function.identity()));
    }

    private MergeInfo calculateActions(Item source, Item target) {
        MergeInfo data = new MergeInfo();
        data.sourceReports = getReports(source);
        data.targetReports = getReports(target);

        Set<Long> copyToTargetSet = new HashSet<>(data.sourceReports.keySet());
        Set<Long> copyToSourceSet = new HashSet<>(data.targetReports.keySet());

        Set<Long> mergeSet = new HashSet<>(copyToSourceSet);
        mergeSet.retainAll(copyToTargetSet);

        copyToSourceSet.removeAll(mergeSet);
        copyToTargetSet.removeAll(mergeSet);
        data.copyToTarget = copyToTargetSet.iterator();
        data.copyToSource = copyToSourceSet.iterator();

        mergeSet.removeIf(village -> {
            Item item1 = data.sourceReports.get(village);
            Item item2 = data.targetReports.get(village);
            return item1.getAuxData() == item2.getAuxData();
        });

        data.merge = mergeSet.iterator();
        data.workLeft = mergeSet.size() + copyToSourceSet.size() + copyToTargetSet.size();

        return data;
    }

    private Item findPaper(Creature performer) {
        for (Item i : performer.getInventory().getAllItems(false)) {
            if (i.getTemplateId() == ItemList.paperSheet) return i;
        }
        return null;
    }

    private boolean doNextAction(Creature performer, Item source, Item target, MergeInfo data) {
        if (data.merge.hasNext()) {
            Long village = data.merge.next();
            Item report1 = data.sourceReports.get(village);
            Item report2 = data.targetReports.get(village);
            byte merged;

            if (JournalTools.isLocationKnown(report1) || JournalTools.isLocationKnown(report2)) {
                merged = (byte) (report1.getAuxData() | report2.getAuxData());
            } else {
                merged = (byte) Math.max(report1.getAuxData(), report2.getAuxData());
            }

            report1.setAuxData(merged);
            report2.setAuxData(merged);
            int ql = JournalTools.fillLevel(report1);
            report1.setQualityLevel(ql);
            report2.setQualityLevel(ql);

            performer.getCommunicator().sendNormalServerMessage(String.format("You merge the information in \"%s\" from both journals", report1.getName()));

            return --data.workLeft > 0;
        } else if (data.copyToSource.hasNext()) {
            return doCopy(performer, data.targetReports.get(data.copyToSource.next()), source) && --data.workLeft > 0;
        } else if (data.copyToTarget.hasNext()) {
            return doCopy(performer, data.sourceReports.get(data.copyToTarget.next()), target) && --data.workLeft > 0;
        } else return false;
    }

    private boolean doCopy(Creature performer, Item sourceLog, Item targetJournal) {
        Item paper = findPaper(performer);
        if (paper == null) {
            performer.getCommunicator().sendNormalServerMessage("You stop merging as you're out of paper.");
            return false;
        }

        if (!targetJournal.mayCreatureInsertItem() && paper.getParentId() != targetJournal.getWurmId()) {
            performer.getCommunicator().sendNormalServerMessage(String.format("You stop merging as the %s is full.", targetJournal.getName()));
            return false;
        }

        try {
            performer.getCommunicator().sendNormalServerMessage(String.format("You grab a sheet of paper and copy the \"%s\".", sourceLog.getName()));

            Item newItem = ItemFactory.createItem(JournalItems.logId, 1f, (byte) 0, sourceLog.getCreatorName());
            newItem.setData(sourceLog.getData());
            newItem.setAuxData(sourceLog.getAuxData());
            newItem.setName(sourceLog.getName());
            newItem.setQualityLevel(sourceLog.getQualityLevel());
            targetJournal.insertItem(newItem, true);

            paper.getParent().dropItem(paper.getWurmId(), false);
            Items.destroyItem(paper.getWurmId());

            return true;
        } catch (FailedException | NoSuchTemplateException | NoSuchItemException e) {
            ArcheoTweaksMod.logException(String.format("Error copying report from %d into %d", sourceLog.getWurmId(), targetJournal.getWurmId()), e);
            performer.getCommunicator().sendAlertServerMessage("Something went wrong, try again or open a /support ticket.");
            return false;
        }
    }

    @Override
    public boolean action(Action action, Creature performer, Item source, Item target, short num, float counter) {
        if (!canUse(performer, source, target)) {
            performer.getCommunicator().sendAlertServerMessage("You can't do that now.");
            return propagate(action, FINISH_ACTION, NO_SERVER_PROPAGATION, NO_ACTION_PERFORMER_PROPAGATION);
        }

        MergeInfo data = actionData.get(action);
        if (data == null) {
            data = calculateActions(source, target);
            actionData.put(action, data);
        }

        if (counter == 1f) {
            if (data.workLeft == 0) {
                performer.getCommunicator().sendNormalServerMessage("Both journals contain the same reports, there is nothing to merge.");
                actionData.remove(action);
                return propagate(action, FINISH_ACTION, NO_SERVER_PROPAGATION, NO_ACTION_PERFORMER_PROPAGATION);
            }
            float baseTime = Actions.getQuickActionTime(performer, performer.getSkills().getSkillOrLearn(SkillList.ARCHAEOLOGY), null, 0) / 2;
            action.setData((long) (baseTime * 1000));
            float totalTime = (float) Math.ceil(data.workLeft * baseTime);
            action.setTimeLeft((int) totalTime);
            performer.sendActionControl(actionEntry.getVerbString(), true, (int) totalTime);
            action.setNextTick(1 + baseTime / 10);
            performer.getCommunicator().sendNormalServerMessage("You start merging the reports in the journals.");
        } else if (counter >= action.getNextTick()) {
            if (doNextAction(performer, source, target, data)) {
                action.incNextTick(action.getData() * 0.0001f);
            } else {
                if (data.workLeft == 0)
                    performer.getCommunicator().sendNormalServerMessage("You finish merging the journals.");
                actionData.remove(action);
                return propagate(action, FINISH_ACTION, NO_SERVER_PROPAGATION, NO_ACTION_PERFORMER_PROPAGATION);
            }
        }
        return propagate(action, CONTINUE_ACTION, NO_SERVER_PROPAGATION, NO_ACTION_PERFORMER_PROPAGATION);
    }
}