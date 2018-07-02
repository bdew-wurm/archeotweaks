package net.bdew.wurm.archeotweaks;

import com.wurmonline.server.items.CreationEntry;
import com.wurmonline.server.items.CreationMatrix;
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
}
