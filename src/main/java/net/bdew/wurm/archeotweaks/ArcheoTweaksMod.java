package net.bdew.wurm.archeotweaks;

import com.wurmonline.server.creatures.Communicator;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;
import net.bdew.wurm.archeotweaks.journal.*;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.*;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ArcheoTweaksMod implements WurmServerMod, Initable, PreInitable, Configurable, PlayerMessageListener, ItemTemplatesCreatedListener, ServerStartedListener {
    private static final Logger logger = Logger.getLogger("ArcheoTweaks");

    public static void logException(String msg, Throwable e) {
        if (logger != null)
            logger.log(Level.SEVERE, msg, e);
    }

    public static void logWarning(String msg) {
        if (logger != null)
            logger.log(Level.WARNING, msg);
    }

    public static void logInfo(String msg) {
        if (logger != null)
            logger.log(Level.INFO, msg);
    }

    public static String[] tierLists = new String[8];
    public static String cacheFragments;
    public static int minTierAtPower50, minTierAtPower70, minTierAtPower90;
    public static boolean lessJunkOnGoodTiles = false;
    public static boolean extraInfoLogging = false;
    public static boolean journalSystem = false;
    public static float cacheTotalPower;

    @Override
    public void configure(Properties properties) {
        for (int i = 0; i <= 7; i++) {
            tierLists[i] = properties.getProperty("tier" + i);
            logInfo(String.format("tier%d = %s", i, tierLists[i]));
        }

        cacheFragments = properties.getProperty("cache");
        logInfo(String.format("cache = %s", cacheFragments));

        minTierAtPower50 = Integer.parseInt(properties.getProperty("minTierAtPower50", "-1"));
        minTierAtPower70 = Integer.parseInt(properties.getProperty("minTierAtPower70", "-1"));
        minTierAtPower90 = Integer.parseInt(properties.getProperty("minTierAtPower90", "-1"));

        lessJunkOnGoodTiles = Boolean.parseBoolean(properties.getProperty("lessJunkOnGoodTiles", "false"));
        extraInfoLogging = Boolean.parseBoolean(properties.getProperty("extraInfoLogging", "false"));
        journalSystem = Boolean.parseBoolean(properties.getProperty("journalSystem", "false"));

        if (journalSystem)
            cacheTotalPower = Float.parseFloat(properties.getProperty("cacheTotalPower", "0"));
        else
            cacheTotalPower = 0;

        logInfo("minTierAtPower50 = " + minTierAtPower50);
        logInfo("minTierAtPower70 = " + minTierAtPower70);
        logInfo("minTierAtPower90 = " + minTierAtPower90);
        logInfo("lessJunkOnGoodTiles = " + lessJunkOnGoodTiles);
        logInfo("extraInfoLogging = " + extraInfoLogging);
        logInfo("journalSystem = " + journalSystem);
        logInfo("cacheTotalPower = " + cacheTotalPower);
    }

    @Override
    public void init() {
        try {
            ModActions.init();
            ClassPool classPool = HookManager.getInstance().getClassPool();
            CtClass ctFragUtils = classPool.getCtClass("com.wurmonline.server.items.FragmentUtilities");
            ctFragUtils.getClassInitializer()
                    .instrument(new ExprEditor() {
                        @Override
                        public void edit(FieldAccess f) throws CannotCompileException {
                            if (f.isWriter() && f.getFieldName().startsWith("diff")) {
                                logInfo(String.format("Replacing list %s creation at %d", f.getFieldName(), f.getLineNumber()));
                                if (f.getFieldName().equals("diffTrash"))
                                    f.replace("diffTrash=net.bdew.wurm.archeotweaks.Hooks.getLootList(0);");
                                else if (f.getFieldName().equals("diff0_15"))
                                    f.replace("diff0_15=net.bdew.wurm.archeotweaks.Hooks.getLootList(1);");
                                else if (f.getFieldName().equals("diff15_30"))
                                    f.replace("diff15_30=net.bdew.wurm.archeotweaks.Hooks.getLootList(2);");
                                else if (f.getFieldName().equals("diff30_40"))
                                    f.replace("diff30_40=net.bdew.wurm.archeotweaks.Hooks.getLootList(3);");
                                else if (f.getFieldName().equals("diff40_50"))
                                    f.replace("diff40_50=net.bdew.wurm.archeotweaks.Hooks.getLootList(4);");
                                else if (f.getFieldName().equals("diff50_60"))
                                    f.replace("diff50_60=net.bdew.wurm.archeotweaks.Hooks.getLootList(5);");
                                else if (f.getFieldName().equals("diff60_70"))
                                    f.replace("diff60_70=net.bdew.wurm.archeotweaks.Hooks.getLootList(6);");
                                else if (f.getFieldName().equals("diff70_80"))
                                    f.replace("diff70_80=net.bdew.wurm.archeotweaks.Hooks.getLootList(7);");
                                else if (f.getFieldName().equals("justStatues"))
                                    f.replace("diff70_80=net.bdew.wurm.archeotweaks.Hooks.getLootList(-1);");
                            }
                        }
                    });

            if (minTierAtPower50 > 0 || minTierAtPower70 > 0 || minTierAtPower90 > 0) {
                ctFragUtils.getMethod("getRandomFragmentForSkill", "(DZ)Lcom/wurmonline/server/items/FragmentUtilities$Fragment;")
                        .instrument(new ExprEditor() {
                            boolean found = false;

                            @Override
                            public void edit(MethodCall m) throws CannotCompileException {
                                if (!found && m.getMethodName().equals("nextInt")) {
                                    logInfo(String.format("Injecting bumpTierToSkill into getRandomFragmentForSkill at %d", m.getLineNumber()));
                                    m.replace("$_ = net.bdew.wurm.archeotweaks.Hooks.bumpTierToSkill(skill,$proceed($$));");
                                    found = true;
                                }
                            }
                        });
            }

            if (lessJunkOnGoodTiles || extraInfoLogging || journalSystem) {
                CtMethod mInvestigateTile = classPool.getCtClass("com.wurmonline.server.behaviours.TileBehaviour")
                        .getMethod("investigateTile", "(Lcom/wurmonline/server/behaviours/Action;Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;IIIIISF)Z");

                if (extraInfoLogging || journalSystem)
                    mInvestigateTile.insertAfter("net.bdew.wurm.archeotweaks.Hooks.endInvestigateHook(performer);");

                mInvestigateTile.instrument(new ExprEditor() {
                    int dnc = 0, tsd = 0, ta = 0, sc = 0;

                    @Override
                    public void edit(MethodCall m) throws CannotCompileException {
                        if ((journalSystem || extraInfoLogging) && m.getMethodName().equals("skillCheck") && (sc++ == 1)) {
                            String rep;

                            if (journalSystem)
                                rep = "$_ = $proceed($1 * net.bdew.wurm.archeotweaks.Hooks.diffMult(performer, possibleTargets), $2, $3, $4, $5);";
                            else
                                rep = "$_ = $proceed($$);";

                            if (cacheTotalPower > 0)
                                rep += "net.bdew.wurm.archeotweaks.Hooks.cachePowerHook($_);";

                            if (extraInfoLogging)
                                rep += "net.bdew.wurm.archeotweaks.Hooks.logArch(performer, archSkill, negBonus, tileMax, $_);";

                            m.replace(rep);
                            logInfo(String.format("Hooking skillCheck in investigateTile at %d", m.getLineNumber()));
                            return;
                        }

                        if (lessJunkOnGoodTiles && m.getMethodName().equals("getRandomFragmentForSkill")) {
                            m.replace("$_=$proceed($1, tileMax < archSkill);");
                            logInfo(String.format("Hooking getRandomFragmentForSkill in investigateTile at %d", m.getLineNumber()));
                            return;
                        }

                        if (journalSystem) {
                            switch (m.getMethodName()) {
                                case "getDeedName":
                                    if (dnc == 0) {
                                        m.replace("$_=$proceed(); net.bdew.wurm.archeotweaks.Hooks.journalHook(performer, $0, 0);");
                                        logInfo(String.format("Hooking getDeedName (partial) in investigateTile at %d", m.getLineNumber()));
                                    } else if (dnc < 3) {
                                        m.replace("$_=$proceed(); net.bdew.wurm.archeotweaks.Hooks.journalHook(performer, $0, 1);");
                                        logInfo(String.format("Hooking getDeedName (full) in investigateTile at %d", m.getLineNumber()));
                                    }
                                    dnc++;
                                    break;
                                case "getMayorName":
                                    m.replace("$_=$proceed(); net.bdew.wurm.archeotweaks.Hooks.journalHook(performer, $0, 2);");
                                    logInfo(String.format("Hooking getMayorName in investigateTile at %d", m.getLineNumber()));
                                    break;
                                case "getFounderName":
                                    m.replace("$_=$proceed(); net.bdew.wurm.archeotweaks.Hooks.journalHook(performer, $0, 3);");
                                    logInfo(String.format("Hooking getFounderName in investigateTile at %d", m.getLineNumber()));
                                    break;
                                case "getTimeSinceDisband":
                                    if (tsd++ == 1) { //only second call
                                        m.replace("$_=$proceed(); net.bdew.wurm.archeotweaks.Hooks.journalHook(performer, $0, 4);");
                                        logInfo(String.format("Hooking getTimeSinceDisband in investigateTile at %d", m.getLineNumber()));
                                    }
                                    break;
                                case "getTotalAge":
                                    if (ta++ == 1) { //only second call
                                        m.replace("$_=$proceed(); net.bdew.wurm.archeotweaks.Hooks.journalHook(performer, $0, 5);");
                                        logInfo(String.format("Hooking getTotalAge in investigateTile at %d", m.getLineNumber()));
                                    }
                                    break;
                                case "toString":
                                    m.replace("$_=$proceed().replace(\"If you had an archaeology journal with a blank report in it you could record your findings.\",\"\");");
                                    logInfo(String.format("Hooking message toString in investigateTile at %d", m.getLineNumber()));
                                    break;
                            }
                        }

                    }
                });

                if (journalSystem) {
                    classPool.getCtClass("com.wurmonline.server.items.Item")
                            .getMethod("moveToItem", "(Lcom/wurmonline/server/creatures/Creature;JZ)Z")
                            .instrument(new ExprEditor() {
                                boolean patched = false;

                                @Override
                                public void edit(MethodCall m) throws CannotCompileException {
                                    if (!patched && m.getMethodName().equals("getItem")) {
                                        m.replace("$_=$proceed($$); if (net.bdew.wurm.archeotweaks.Hooks.blockMove(this, $_, mover)) return false;");
                                        logInfo(String.format("Hooking Item.moveToItem at %d", m.getLineNumber()));
                                        patched = true;
                                    }
                                }
                            });
                }

                mInvestigateTile.getMethodInfo().rebuildStackMap(classPool);

                classPool.getCtClass("com.wurmonline.server.behaviours.TileBehaviour").debugWriteFile();
            }

        } catch (Throwable e) {
            logException("Error loading mod", e);
            throw new RuntimeException(e);
        }
    }


    @Override
    public void preInit() {
    }

    @Override
    public MessagePolicy onPlayerMessage(Communicator communicator, String message, String title) {
        if (communicator.getPlayer().getPower() == 5 && message.equals("#respawnArchaeology")) {
            Hooks.respawnArchaeology();
            communicator.sendNormalServerMessage("Done.");
            return MessagePolicy.DISCARD;
        }
        return MessagePolicy.PASS;
    }

    @Override
    @Deprecated
    public boolean onPlayerMessage(Communicator communicator, String message) {
        return false;
    }


    @Override
    public void onItemTemplatesCreated() {
        try {
            if (journalSystem) {
                JournalItems.register();
                ModActions.registerActionPerformer(new ExaminePerformer());
                ModActions.registerAction(new CopyAction());
                ModActions.registerAction(new GetDirectionAction());
                ModActions.registerAction(new MergeJournalAction());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onServerStarted() {
        if (journalSystem) {
            try {
                JournalItems.removeVanillaRecipe();
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
