package net.bdew.wurm.archeotweaks.journal;

import com.wurmonline.server.WurmCalendar;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.Actions;
import com.wurmonline.server.creatures.Communicator;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.villages.DeadVillage;
import net.bdew.wurm.archeotweaks.Hooks;
import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.ActionPropagation;

public class ExaminePerformer implements ActionPerformer {
    @Override
    public short getActionId() {
        return Actions.EXAMINE;
    }

    @Override
    public boolean action(Action action, Creature performer, Item target, short num, float counter) {
        if (target.getTemplateId() == JournalItems.logId) {
            Communicator comm = performer.getCommunicator();
            DeadVillage dv = Hooks.deadVillages.get(target.getData());
            if (dv == null) {
                comm.sendNormalServerMessage("You are unable to decipher the content of this report, maybe it was made in far away lands, and you would have more luck reading it there.");
                return propagate(action, ActionPropagation.FINISH_ACTION, ActionPropagation.NO_SERVER_PROPAGATION, ActionPropagation.NO_ACTION_PERFORMER_PROPAGATION);
            }

            comm.sendNormalServerMessage(String.format("This is an archaeological report on %s.", dv.getDeedName()));
            comm.sendNormalServerMessage(String.format("It was written down by %s on %s", target.creator, WurmCalendar.getDateFor(target.creationDate)));

            if (JournalTools.isLocationKnown(target)) {
                comm.sendNormalServerMessage(String.format("It contains the exact location of %s.", dv.getDeedName()));
            } else {
                comm.sendNormalServerMessage(String.format("It contains some clues to the location of %s, but not enough to pinpoint it exactly.", dv.getDeedName()));
            }

            if (JournalTools.isLastMayorKnown(target) && JournalTools.isFounderKnown(target) && dv.getMayorName().equals(dv.getFounderName())) {
                comm.sendNormalServerMessage(String.format("%s was founded by %s, who was also the last known mayor.", dv.getDeedName(), dv.getFounderName()));
            } else {
                if (JournalTools.isFounderKnown(target)) {
                    comm.sendNormalServerMessage(String.format("%s was founded by %s.", dv.getDeedName(), dv.getFounderName()));
                }
                if (JournalTools.isLastMayorKnown(target)) {
                    comm.sendNormalServerMessage(String.format("The last known mayor was %s.", dv.getFounderName()));
                }
            }

            if (JournalTools.isDisbandKnown(target)) {
                comm.sendNormalServerMessage(String.format("%s was founded %s ago.", dv.getDeedName(), DeadVillage.getTimeString(dv.getTotalAge() + dv.getTimeSinceDisband(), false)));
                if (JournalTools.isAgeKnown(target)) {
                    comm.sendNormalServerMessage(String.format("%s was abandoned for %s.", dv.getDeedName(), DeadVillage.getTimeString(dv.getTimeSinceDisband(), false)));
                }
            } else if (JournalTools.isAgeKnown(target)) {
                comm.sendNormalServerMessage(String.format("%s was inhabited for %s.", dv.getDeedName(), DeadVillage.getTimeString(dv.getTotalAge(), false)));
            }

            if (!JournalTools.isFullReport(target)) {
                comm.sendNormalServerMessage(String.format("If you discover more details about %s you can add them to the report.", dv.getDeedName()));
            }

            return propagate(action, ActionPropagation.FINISH_ACTION, ActionPropagation.NO_SERVER_PROPAGATION, ActionPropagation.NO_ACTION_PERFORMER_PROPAGATION);
        } else {
            return propagate(action, ActionPropagation.CONTINUE_ACTION, ActionPropagation.SERVER_PROPAGATION, ActionPropagation.ACTION_PERFORMER_PROPAGATION);
        }

    }
}
