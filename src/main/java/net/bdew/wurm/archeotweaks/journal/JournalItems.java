package net.bdew.wurm.archeotweaks.journal;

import com.wurmonline.server.items.*;
import com.wurmonline.server.skills.SkillList;
import com.wurmonline.shared.constants.ItemMaterials;
import net.bdew.wurm.archeotweaks.Utils;
import org.gotti.wurmunlimited.modsupport.ItemTemplateBuilder;

import java.io.IOException;

public class JournalItems {
    public static ItemTemplate logTemplate, journalTemplate;
    public static int logId, journalId;

    public static void register() throws IOException {

        logTemplate = new ItemTemplateBuilder("bdew.archeo.log")
                .name("report", "reports", "Archaeology report. If you're seeing this text then something is borked.")
                .itemTypes(new short[]{
                        ItemTypes.ITEM_TYPE_WOOD,
                        ItemTypes.ITEM_TYPE_HASDATA,
                        ItemTypes.ITEM_TYPE_NAMED,
                })
                .imageNumber((short) 640)
                .decayTime(Long.MAX_VALUE)
                .dimensions(1, 20, 25)
                .modelName("model.resource.sheet.paper.")
                .weightGrams(10)
                .material(ItemMaterials.MATERIAL_PAPER)
                .behaviourType((short) 1)
                .build();

        logId = logTemplate.getTemplateId();

        journalTemplate = new ItemTemplateBuilder("bdew.archeo.journal")
                .name("archaeology journal", "archaeology journals", "A binder used to keep reports on archaeological findings.")
                .itemTypes(new short[]{
                        ItemTypes.ITEM_TYPE_LEATHER,
                        ItemTypes.ITEM_TYPE_NAMED,
                        ItemTypes.ITEM_TYPE_HOLLOW,
                        ItemTypes.ITEM_TYPE_USES_SPECIFIED_CONTAINER_VOLUME,
                        ItemTypes.ITEM_TYPE_REPAIRABLE
                })
                .imageNumber((short) 1456)
                .decayTime(Long.MAX_VALUE)
                .dimensions(10, 30, 30)
                .modelName("model.container.journal.")
                .weightGrams(250)
                .difficulty(5)
                .primarySkill(SkillList.LEATHERWORKING)
                .material(ItemMaterials.MATERIAL_LEATHER)
                .behaviourType((short) 1)
                .containerSize(100, 30, 30)
                .build();

        journalId = journalTemplate.getTemplateId();

        CreationEntryCreator.createAdvancedEntry(SkillList.LEATHERWORKING, ItemList.book, ItemList.leatherStrip, journalId, false, false, 0.0F, true, false, CreationCategories.WRITING)
                .addRequirement(new CreationRequirement(1, ItemList.leatherStrip, 2, true));
    }

    public static void removeVanillaRecipe() throws NoSuchFieldException, IllegalAccessException {
        Utils.removeRecipesFor(ItemList.archaeologyJournal);
    }
}
