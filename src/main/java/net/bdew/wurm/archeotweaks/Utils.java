package net.bdew.wurm.archeotweaks;

import com.wurmonline.server.items.*;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;

import java.util.List;
import java.util.Map;

public class Utils {
    public static void removeRecipesFor(int tpl) throws NoSuchFieldException, IllegalAccessException {
        CreationEntry ent = CreationMatrix.getInstance().getCreationEntry(tpl);

        Map<Integer, List<CreationEntry>> matrix = ReflectionUtil.getPrivateField(null, ReflectionUtil.getField(CreationMatrix.class, "matrix"));
        Map<Integer, CreationEntry> advancedEntries = ReflectionUtil.getPrivateField(null, ReflectionUtil.getField(CreationMatrix.class, "advancedEntries"));
        Map<Integer, List<CreationEntry>> simpleEntries = ReflectionUtil.getPrivateField(null, ReflectionUtil.getField(CreationMatrix.class, "simpleEntries"));

        advancedEntries.remove(ent.getObjectCreated());
        matrix.get(ent.getObjectTarget()).remove(ent);
        simpleEntries.get(ent.getObjectCreated()).remove(ent);
    }

    public static boolean isCleanPaper(Item item) {
        if (item.getTemplateId() != ItemList.paperSheet || item.getAuxData() != 0) return false;
        InscriptionData ins = item.getInscription();
        return ins == null || !ins.hasBeenInscribed();
    }
}
