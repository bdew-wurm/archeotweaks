package net.bdew.wurm.archeotweaks;

import com.wurmonline.server.creatures.Communicator;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.*;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ArcheoTweaksMod implements WurmServerMod, Initable, PreInitable, Configurable, PlayerMessageListener {
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
    public static int minTierAtPower50, minTierAtPower70, minTierAtPower90;

    @Override
    public void configure(Properties properties) {
        for (int i = 0; i <= 7; i++) {
            tierLists[i] = properties.getProperty("tier" + i);
            logInfo(String.format("tier%d = %s", i, tierLists[i]));
        }

        minTierAtPower50 = Integer.parseInt(properties.getProperty("minTierAtPower50", "-1"));
        minTierAtPower70 = Integer.parseInt(properties.getProperty("minTierAtPower70", "-1"));
        minTierAtPower90 = Integer.parseInt(properties.getProperty("minTierAtPower90", "-1"));
        logInfo("minTierAtPower50 = " + minTierAtPower50);
        logInfo("minTierAtPower70 = " + minTierAtPower70);
        logInfo("minTierAtPower90 = " + minTierAtPower90);
    }

    @Override
    public void init() {
        try {
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
                                    m.replace("$_ = net.bdew.wurm.archeotweaks.Hooks.bumpTierToSkill(skill,$proceed($$));");
                                    found = true;
                                }
                            }
                        });
            }

            classPool.getCtClass("com.wurmonline.server.behaviours.TileBehaviour")
                    .getMethod("investigateTile", "(Lcom/wurmonline/server/behaviours/Action;Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;IIIIISF)Z")
                    .instrument(new ExprEditor() {
                        @Override
                        public void edit(MethodCall m) throws CannotCompileException {
                            if (m.getMethodName().equals("getRandomFragmentForSkill")) {
                                m.replace("net.bdew.wurm.archeotweaks.Hooks.logArch(performer, archSkill, negBonus, tileMax, power); $_=$proceed($1, tileMax < archSkill);");
                            }
                        }
                    });

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
}
