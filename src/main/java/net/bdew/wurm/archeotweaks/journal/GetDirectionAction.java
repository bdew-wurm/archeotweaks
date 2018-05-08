package net.bdew.wurm.archeotweaks.journal;

import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.Communicator;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.epic.EpicServerStatus;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.skills.SkillList;
import com.wurmonline.server.villages.DeadVillage;
import net.bdew.wurm.archeotweaks.Hooks;
import org.gotti.wurmunlimited.modsupport.actions.*;

import java.util.Collections;
import java.util.List;

import static org.gotti.wurmunlimited.modsupport.actions.ActionPropagation.FINISH_ACTION;
import static org.gotti.wurmunlimited.modsupport.actions.ActionPropagation.NO_ACTION_PERFORMER_PROPAGATION;
import static org.gotti.wurmunlimited.modsupport.actions.ActionPropagation.NO_SERVER_PROPAGATION;

public class GetDirectionAction implements ModAction, ActionPerformer, BehaviourProvider {
    private ActionEntry actionEntry;

    public GetDirectionAction() {
        actionEntry = new ActionEntryBuilder((short) ModActions.getNextActionId(), "Get directions", "locating", new int[]{
                48 /* ACTION_TYPE_ENEMY_ALWAYS */,
                37 /* ACTION_TYPE_NEVER_USE_ACTIVE_ITEM */
        }).build();
        ModActions.registerAction(actionEntry);
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

    public boolean canUse(Creature performer, Item target) {
        return performer.isPlayer() && target != null
                && target.getTemplateId() == JournalItems.logId
                && target.getTopParent() == performer.getInventory().getWurmId();
    }

    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item source, Item target) {
        return getBehavioursFor(performer, target);
    }

    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item target) {
        if (canUse(performer, target))
            return Collections.singletonList(actionEntry);
        else
            return null;
    }

    @Override
    public boolean action(Action action, Creature performer, Item source, Item target, short num, float counter) {
        return action(action, performer, target, num, counter);
    }

    @Override
    public boolean action(Action action, Creature performer, Item target, short num, float counter) {
        if (!canUse(performer, target)) {
            performer.getCommunicator().sendAlertServerMessage("You can't do that now.");
            return true;
        }

        Communicator comm = performer.getCommunicator();
        DeadVillage dv = Hooks.deadVillages.get(target.getData());

        if (dv == null) {
            comm.sendNormalServerMessage("You are unable to decipher the content of this report, maybe it was made in far away lands, and you would have more luck reading it there.");
            return propagate(action, ActionPropagation.FINISH_ACTION, ActionPropagation.NO_SERVER_PROPAGATION, ActionPropagation.NO_ACTION_PERFORMER_PROPAGATION);
        }

        int dist = 0;
        if (dv.getEndX() < performer.getTileX()) dist = Math.max(dist, performer.getTileX() - dv.getEndX());
        if (dv.getEndY() < performer.getTileY()) dist = Math.max(dist, performer.getTileY() - dv.getEndY());
        if (dv.getStartX() > performer.getTileX()) dist = Math.max(dist, dv.getStartX() - performer.getTileX());
        if (dv.getStartY() > performer.getTileY()) dist = Math.max(dist, dv.getStartY() - performer.getTileY());

        if (dist > performer.getSkills().getSkillOrLearn(SkillList.ARCHAEOLOGY).getKnowledge() / 2 + target.getQualityLevel()) {
            if (JournalTools.isLocationKnown(target))
                comm.sendNormalServerMessage(String.format("%s is %s to the %s.", dv.getDeedName(), dv.getDistanceFrom(performer.getTileX(), performer.getTileY()), dv.getDirectionFrom(performer.getTileX(), performer.getTileY())));
            else
                comm.sendNormalServerMessage(String.format("It looks like %s is in the %s, you might find a more accurate location when you are closer.", dv.getDeedName(), EpicServerStatus.getAreaString(dv.getStartX(), dv.getStartY())));
        } else {
            if (JournalTools.isLocationKnown(target)) {
                if (dv.getEndX() < performer.getTileX()) {
                    if (dv.getEndY() < performer.getTileY()) {
                        comm.sendNormalServerMessage(String.format("%s is %d tiles to the north and %d tiles to the west.", dv.getDeedName(), performer.getTileY() - dv.getEndY(), performer.getTileX() - dv.getEndX()));
                    } else if (dv.getStartY() > performer.getTileY()) {
                        comm.sendNormalServerMessage(String.format("%s is %d tiles to the south and %d tiles to the west.", dv.getDeedName(), dv.getStartY() - performer.getTileY(), performer.getTileX() - dv.getEndX()));
                    } else {
                        comm.sendNormalServerMessage(String.format("%s is %d tiles to the west.", dv.getDeedName(), performer.getTileX() - dv.getEndX()));
                    }
                } else if (dv.getStartX() > performer.getTileX()) {
                    if (dv.getEndY() < performer.getTileY()) {
                        comm.sendNormalServerMessage(String.format("%s is %d tiles to the north and %d tiles to the east.", dv.getDeedName(), performer.getTileY() - dv.getEndY(), dv.getStartX() - performer.getTileX()));
                    } else if (dv.getStartY() > performer.getTileY()) {
                        comm.sendNormalServerMessage(String.format("%s is %d tiles to the south and %d tiles to the east.", dv.getDeedName(), dv.getStartY() - performer.getTileY(), dv.getStartX() - performer.getTileX()));
                    } else {
                        comm.sendNormalServerMessage(String.format("%s is %d tiles to the east.", dv.getDeedName(), dv.getStartX() - performer.getTileX()));
                    }
                } else {
                    if (dv.getEndY() < performer.getTileY()) {
                        comm.sendNormalServerMessage(String.format("%s is %d tiles to the north.", dv.getDeedName(), performer.getTileY() - dv.getEndY()));
                    } else if (dv.getStartY() > performer.getTileY()) {
                        comm.sendNormalServerMessage(String.format("%s is %d tiles to the south.", dv.getDeedName(), dv.getStartY() - performer.getTileY()));
                    } else {
                        comm.sendNormalServerMessage(String.format("You are right on top of %s.", dv.getDeedName()));
                    }
                }
            } else if (dist == 0) {
                comm.sendNormalServerMessage(String.format("It looks like %s might is somewhere here, try investigating this area.", dv.getDeedName()));
            } else {
                comm.sendNormalServerMessage(String.format("It looks like %s might be %s to the %s.", dv.getDeedName(), dv.getDistanceFrom(performer.getTileX(), performer.getTileY()), dv.getDirectionFrom(performer.getTileX(), performer.getTileY())));
            }
        }

        return propagate(action, FINISH_ACTION, NO_SERVER_PROPAGATION, NO_ACTION_PERFORMER_PROPAGATION);
    }
}
