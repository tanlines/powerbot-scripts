package scripts.Combine14;

import api.Bank;
import api.ClientContext;
import api.PollingScript;

import org.powerbot.script.*;
import org.powerbot.script.rt4.Game;
import org.powerbot.script.rt4.Item;
import scripts.ID;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.min;
//@Script.Manifest(
       //  name = "Item combiner", properties = "author=nomivore; topic=1338568; client=4;",
      //description = "Combines ITEMS, supports Make All and multi-item queuing")
public class Combine14 extends PollingScript<ClientContext> implements PaintListener, MessageListener {
    private int resourceID1;
    private int resourceID2;
    private int resourceLeft1;
    private int resourceLeft2;
    private int resourceQ1;
    private int resourceQ2;
    private int productDone;
    private boolean hasResource;
    private boolean stop;
    private long startTime;

    public static List<Combine14GUI.IDPair> itemList = new ArrayList();
    Combine14GUI gui = new Combine14GUI();

    @Override
    public void start() {
        while(!gui.done()) {
            Condition.sleep();
        }
        JOptionPane.showConfirmDialog(null,"If you still use this script, vote on the thread poll,\n otherwise it will be taken down and open sourced.","Hi",JOptionPane.OK_CANCEL_OPTION);

        startTime = getRuntime();
//        ctx.dispatcher.add(new InventoryListener() {
//            @Override
//            public void onInventoryChange(InventoryEvent ie) {
//                int id = ie.getNewItem().id();
//                System.out.print(id);
//                if (id <= 0) return;
//                if (id != resourceID1 || id != resourceID2) {
//                    productDone++;
//                }
//            }
//        });
    }

    @Override
    public void poll() {
        stop = true;
        for (Combine14GUI.IDPair i : itemList) {
            if (i.id1 != 0 && i.id2 != 0) {
                hasResource = true;
                resourceLeft1 = 1;
                resourceLeft2 = 1;
                resourceID1 = i.id1;
                resourceID2 = i.id2;
                resourceQ1 = i.id1q;
                resourceQ2 = i.id2q;
                if (activate()) execute();
                if (!hasResource) {
                    i.id1 = 0;
                    i.id2 = 0;
                    ctx.bank.openNearbyBank();
                    Utils.depositInventory();
                }
                stop = false;
                break;
            }
        }
        if (stop) ctx.controller.stop();
    }

    public boolean activate() {
        return resourceLeft1 > 0 && resourceLeft2 > 0;
    }

    public void execute() {
        if (ctx.chat.canContinue()) {
            ctx.input.send("{VK_SPACE}");
            Condition.sleep(2000);
            ctx.input.send("{VK_SPACE}");
        }
        switch (getState()) {
            case ACTION:
                action();
                break;
            case WITHDRAW:
                prologue();
                if ((resourceLeft1 == 0) || (resourceLeft2 == 0)) {
                    epilogue();
                    hasResource = false;
                }
                break;
            case WAIT:
                Condition.sleep(100);
                break;
        }
    }

    @Override
    public void messaged(MessageEvent me) {
    }

    private State getState() {
        if (ctx.inventory.select().id(resourceID1).count() == 0 || ctx.inventory.select().id(resourceID2).count() == 0) {
            return State.WITHDRAW;
        } else if (ctx.players.local().animation() == -1) {
            return State.ACTION;
        }

        return State.WAIT;
    }


    private void prologue() {
        if (ctx.bank.opened()) {
//            productDone += ctx.inventory.select().count();
            ctx.bank.depositAllExcept(233,946,1755,2347); //946 pestle,knife,chisel,hammer
            resourceLeft1 = ctx.bank.select().id(resourceID1).count(true) + ctx.inventory.select().id(resourceID1).count(true);
            resourceLeft2 = ctx.bank.select().id(resourceID2).count(true) + ctx.inventory.select().id(resourceID2).count(true);
            if (ctx.inventory.select().id(resourceID1).isEmpty()) {
                if (resourceQ1 == 0) {
                    ctx.bank.withdraw(resourceID1, Bank.Amount.ALL);
                } else if (resourceQ1 == -1) {
                    ctx.bank.withdraw(resourceID1, 14);
                } else {
                    ctx.bank.withdraw(resourceID1, resourceQ1);
                }
            }
            if (ctx.inventory.select().id(resourceID2).isEmpty()) {
                if (resourceQ2 == 0 || resourceQ2 == -1) {
                    ctx.bank.withdraw(resourceID2, Bank.Amount.ALL);
                } else {
                    ctx.bank.withdraw(resourceID2, resourceQ2);
                }
            }
            ctx.bank.close();
        } else {
            ctx.bank.openNearbyBank();
        }
    }

    private void action() {
        ctx.bank.close();
        ctx.game.tab(Game.Tab.INVENTORY);
        if (ctx.inventory.selectedItem().valid()) ctx.inventory.selectedItem().interact("Cancel");

        final Item resource1 = ctx.inventory.select().id(resourceID1).poll();
        final Item resource2 = ctx.inventory.select().id(resourceID2).poll();
        final int initialCount = ctx.inventory.select().id(resourceID2).count();

        if(resource1.interact("Use") && resource2.interact("Use")) {
            Condition.wait(() -> ctx.widgets.component(ID.WIDGET_MAKE, ID.COMPONENT_MAKE).visible(),500,6);
        }
        if (ctx.widgets.component(ID.WIDGET_MAKE, ID.COMPONENT_MAKE).visible()) {
//            ctx.widgets.component(ID.COMPONENT_MAKE, ID.COMPONENT_MAKE).interact("Make all");
            ctx.input.send("1");
            Condition.wait(() -> initialCount != ctx.inventory.select().id(resourceID2).count(), 500, 4);
            if (initialCount != ctx.inventory.select().id(resourceID2).count()) {
                Condition.wait(() -> (ctx.inventory.select().id(resourceID1).count() == 0 ||
                        ctx.inventory.select().id(resourceID2).count() == 0) ||
                        ctx.chat.canContinue(), 500, 45);
            }
        } else {
            if (initialCount-1 == ctx.inventory.select().id(resourceID2).count()) {
                Item[] inven = ctx.inventory.items();
                for (Item it : inven) {
                    if (it.id() == resource1.id()) {
                        it.interact("Use");
                        ctx.inventory.select().id(resourceID2).poll().interact("Use");
                        Condition.sleep(300);
                    }
                }
            } else if (initialCount-2 >= ctx.inventory.select().id(resourceID2).count()) {
                Condition.wait(() -> (ctx.inventory.select().id(resourceID1).count() == 0 ||
                        ctx.inventory.select().id(resourceID2).count() == 0) ||
                        ctx.chat.canContinue(), 500, 45);
            }
        }
    }

    private enum State {
        WITHDRAW,ACTION,WAIT
    }

    public static final Font TAHOMA = new Font("Tahoma", Font.PLAIN, 12);

    public void repaint(Graphics graphics)
    {
        final Graphics2D g = (Graphics2D) graphics;
        g.setFont(TAHOMA);

        g.setColor(Color.WHITE);
        g.drawString("Nomivore's Comcubiner", 10, 120);
        g.drawString(Utils.runtimeFormatted(startTime), 10, 140);
        g.drawString(String.format("Current made %d", productDone) , 10, 160);
        g.setColor(Color.BLACK);
        AlphaComposite alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);
        g.setComposite(alphaComposite);
        g.fillRect(5, 100, 200, 100);
    }

    public void epilogue() {
        if (ctx.bank.opened()) {
            if (ctx.bank.depositInventory()) {
                Condition.wait(() -> ctx.inventory.select().count() == 0);
            }
        } else {
            ctx.bank.openNearbyBank();
        }

    }
}