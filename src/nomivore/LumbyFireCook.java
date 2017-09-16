package nomivore;

import org.powerbot.script.*;
import org.powerbot.script.rt4.*;
import org.powerbot.script.rt4.ClientContext;

import java.awt.*;
import java.util.concurrent.Callable;
import java.util.ArrayList;
import java.util.List;

@Script.Manifest(
        name = "LumbyFireCook", properties = "author=nomviore; client=4;",
        description = "Lights fires and cooks at lumby bank")
public class LumbyFireCook extends PollingScript<ClientContext> implements PaintListener, MessageListener {

    private final Tile destTile = new Tile(3206,3224,2);
    private final Tile bankTile = new Tile(3208,3220,2);

    private int[] toolID = {ID.TINDERBOX};
//    private int[] foodIDs = {ID.RAW_SHRIMP};
    public static List<Integer> foodIDs = new ArrayList<>();
    private int foodToCook;
    private int foodCooked;
    private int level;
    private int skill = Constants.SKILLS_COOKING;
    private int startExp;
    private String interact = "Use";
    private boolean lit = false;

    @Override
    public void start() {
        startExp = ctx.skills.experience(skill);
        final LumbyFireCookGUI gui = new LumbyFireCookGUI(ctx);

        while(!gui.done()) {
            Condition.sleep();
        }

        foodToCook = foodIDs.get(0);
    }

    @Override
    public void poll() {
        level = ctx.skills.level(skill);
        if (ctx.chat.canContinue()) {
            ctx.input.send("{VK_SPACE}");
            Condition.sleep(2000);
            ctx.input.send("{VK_SPACE}");
        }
        switch (getState()) {
            case FIRE:
                lit = false;
                GroundItem logs = ctx.groundItems.select(7).id(ID.LOGS_NORMAL).nearest().poll();
                logs.interact("Light");
                Condition.wait(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        return lit;
                    }
                }, 500, 20);
                break;
            case ACTION:
//                Condition.sleep(nap);
                final int temp = foodCooked;
                Item food = ctx.inventory.select().id(foodToCook).poll();
                GameObject fire = ctx.objects.select().id(ID.FIRE).nearest().poll();
                food.interact("Use");
                fire.interact(interact);
                Condition.sleep(1000);
                Condition.wait(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        return ctx.widgets.component(ID.WIDGET_CHATBOX, ID.WIDGET_MAKE).visible();
                    }
                },500, 5);
                if (ctx.widgets.component(ID.WIDGET_CHATBOX, ID.WIDGET_MAKE).visible()) ctx.widgets.component(ID.WIDGET_CHATBOX, ID.WIDGET_MAKE).interact("Cook all");
                Condition.wait(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        return temp != foodCooked;
                    }
                }, 500, 5);
                if (temp != foodCooked) {
                    Condition.wait(new Callable<Boolean>() {
                        @Override
                        public Boolean call() throws Exception {
                            return ctx.inventory.select().id(foodToCook).isEmpty() || ctx.chat.canContinue() || ctx.objects.select(3).id(ID.FIRE).isEmpty();
                        }
                    }, 1000, 30);
                }
                break;
            case WALK:
                if (ctx.movement.energyLevel() > 30) ctx.movement.running(true);
                if (!ctx.players.local().inMotion()) ctx.movement.step(destTile);
                break;
            case BANK:
                if (bankTile.distanceTo(ctx.players.local()) > 3) {
                    if (!ctx.players.local().inMotion()) ctx.movement.step(bankTile);
                } else {
                    if (ctx.bank.opened()) {
                        depositInventory();
                        ctx.bank.withdraw(toolID[0],1);
                        for (int selectResource : foodIDs) {
                            if (ctx.bank.select().id(selectResource).count(true) > 0) {
                                foodToCook = selectResource;
                                break;
                            }
                        }
                        if (ctx.bank.select().id(foodToCook).count(true) == 0) ctx.controller.stop();
                        ctx.bank.withdraw(foodToCook, Bank.Amount.ALL);
                        closeBank();
                    } else {
                        openNearbyBank();
                    }
                }
                break;
            case WAIT:
                break;
        }
    }

    private enum State {
        ACTION,WAIT,WALK,BANK,FIRE
    }

    private State getState() {
        if (ctx.inventory.select().id(foodToCook).count() > 0 && ctx.inventory.select().id(toolID).count() > 0 && destTile.distanceTo(ctx.players.local()) > 3) {
            return State.WALK;
        }
        if (ctx.inventory.select().id(foodToCook).count() == 0 || ctx.inventory.select().id(toolID).isEmpty()) {
            return State.BANK;
        }
        if (ctx.objects.select(7).id(ID.FIRE).isEmpty() &&
                ctx.inventory.select().id(toolID).count() > 0) {
            return State.FIRE;
        }
        if (ctx.inventory.select().id(foodToCook).count() > 0 &&
                ctx.players.local().animation() == -1 &&
                ctx.inventory.select().id(toolID).count() > 0) {
            return State.ACTION;
        }
        return State.WAIT;
    }

    @Override
    public void messaged(MessageEvent me) {
        if (me.text().contains("fire catches")) {
            lit = true;
        }
        if (me.text().contains("You ") && !me.text().contains("logs") && !me.text().contains("space")) {
            foodCooked++;
        }
    }

    public static final Font TAHOMA = new Font("Tahoma", Font.PLAIN, 12);

    public void repaint(Graphics graphics)
    {
        final Graphics2D g = (Graphics2D) graphics;
        g.setFont(TAHOMA);

        int s = (int)Math.floor(getRuntime()/1000 % 60);
        int m = (int)Math.floor(getRuntime()/60000 % 60);
        int h = (int)Math.floor(getRuntime()/3600000);

        int exp = ctx.skills.experience(skill) - startExp;
        int expHr = (int)(exp*3600000D/getRuntime());
        g.setColor(Color.WHITE);
        g.drawString(String.format("Runtime %02d:%02d:%02d", h, m, s), 10, 120);
        g.drawString(String.format("Food cooked %d", foodCooked) , 10, 140);
        g.drawString(String.format("Cooking level %d", level) , 10, 160);
        g.drawString(String.format("Exp %d/hr", expHr) , 10, 180);

        g.setColor(Color.BLACK);
        AlphaComposite alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);
        g.setComposite(alphaComposite);
        g.fillRect(5, 100, 200, 100);
    }

    public void openNearbyBank() {
        if (ctx.bank.inViewport()) {
            if (ctx.inventory.selectedItem().valid()) ctx.inventory.selectedItem().interact("Cancel");
            if (ctx.bank.open()) {
                Condition.wait(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        return ctx.bank.opened();
                    }
                }, 250, 10);
            }
        } else {
            ctx.camera.turnTo(ctx.bank.nearest());
        }
    }

    public void depositInventory() {
        if (ctx.bank.depositInventory()) {
            Condition.wait(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return ctx.inventory.select().count() == 0;
                }
            });
        }
    }

    public void closeBank() {
        if (ctx.bank.opened()) {
            Condition.wait(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
//          ctx.bank.close();
                    ctx.input.send("{VK_ESCAPE}");
                    return !ctx.bank.opened();
                }
            });
        }
    }
}
