package net.bdew.wurm.archeotweaks.journal;

import com.wurmonline.server.FailedException;
import com.wurmonline.server.Items;
import com.wurmonline.server.NoSuchItemException;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemFactory;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.items.NoSuchTemplateException;
import net.bdew.wurm.archeotweaks.ArcheoTweaksMod;
import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.BehaviourProvider;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import java.util.Collections;
import java.util.List;

import static org.gotti.wurmunlimited.modsupport.actions.ActionPropagation.*;

public class CopyAction implements ModAction, ActionPerformer, BehaviourProvider {
    private ActionEntry actionEntry;

    public CopyAction() {
        actionEntry = ActionEntry.createEntry((short) ModActions.getNextActionId(), "Copy", "copying", new int[]{
                48 /* ACTION_TYPE_ENEMY_ALWAYS */,
                36 /* ACTION_TYPE_ALWAYS_USE_ACTIVE_ITEM */
        });
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

    public boolean canUse(Creature performer, Item source, Item target) {
        return (performer.isPlayer() && source != null && target != null &&
                source.getTemplateId() == JournalItems.logId && source.getTopParent() == performer.getInventory().getWurmId() &&
                target.getTemplateId() == ItemList.paperSheet && target.getTopParent() == performer.getInventory().getWurmId());
    }

    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item source, Item target) {
        if (canUse(performer, source, target))
            return Collections.singletonList(actionEntry);
        else
            return null;
    }

    @Override
    public boolean action(Action action, Creature performer, Item source, Item target, short num, float counter) {
        if (!canUse(performer, source, target)) {
            performer.getCommunicator().sendAlertServerMessage("You can't do that now.");
            return propagate(action, FINISH_ACTION, NO_SERVER_PROPAGATION, NO_ACTION_PERFORMER_PROPAGATION);
        }

        performer.getCommunicator().sendNormalServerMessage(String.format("You copy the report to a %s.", target.getName()));

        try {
            Item parent = target.getParent();
            parent.dropItem(target.getWurmId(), false);
            Items.destroyItem(target.getWurmId());

            Item newItem = ItemFactory.createItem(JournalItems.logId, source.getQualityLevel(), (byte) 0, source.getCreatorName());
            newItem.setData(source.getData());
            newItem.setAuxData(source.getAuxData());
            newItem.setName(source.getName());
            parent.insertItem(newItem, true);
        } catch (NoSuchTemplateException | NoSuchItemException | FailedException e) {
            ArcheoTweaksMod.logException("Error in copy", e);
        }

        return propagate(action, FINISH_ACTION, NO_SERVER_PROPAGATION, NO_ACTION_PERFORMER_PROPAGATION);
    }
}
