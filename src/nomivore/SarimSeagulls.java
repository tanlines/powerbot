package nomivore;


import org.powerbot.script.*;
import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.*;

import java.awt.*;
import java.util.concurrent.Callable;

@Script.Manifest(
        name = "SarimSeagulls", properties = "author=nomviore; client=4;",
        description = "Attacks seagulls, banks bones")
public class SarimSeagulls extends PollingScript<ClientContext> implements PaintListener {

    private final int[] npcs = ID.SEAGULL;
    private final int loot = ID.BONES;
    private final int depositBoxID = 26254;
    private final Tile destTile = new Tile(3028,3235);
    private final Tile bankTile = new Tile(3045,3235);
    private int hpXP;
    private int looted;
    private String state = "";

    @Override
    public void start() {
        hpXP = ctx.skills.experience(Constants.SKILLS_HITPOINTS);
    }

    @Override
    public void poll() {
        if (ctx.chat.canContinue()) {
            ctx.input.send("{VK_SPACE}");
            Condition.sleep(2000);
            ctx.input.send("{VK_SPACE}");
        }
        switch (getState()) {
            case ATTACK:
                Npc npc = ctx.npcs.select().id(npcs).nearest().select(new Filter<Npc>()
                {
                    @Override
                    public boolean accept(Npc npc)
                    {
                        return npc.healthPercent() > 0 && npc.animation() == -1;
                    }
                }).poll();
                if (!npc.inCombat()) {
                    npc.interact("Attack");
                }
                Condition.wait(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        return ctx.players.local().interacting().valid();
                    }
                },100,15);
                if (ctx.players.local().interacting().valid()) {
                    Condition.wait(new Callable<Boolean>() {
                        @Override
                        public Boolean call() throws Exception {
                            return !ctx.players.local().interacting().valid() || ctx.players.local().interacting().healthPercent() == 0;
                        }
                    }, 500, 5);
//                    hpXP = ctx.skills.experience(Constants.SKILLS_HITPOINTS);
                }
                break;
            case LOOT:
                GroundItem item = ctx.groundItems.select(2).id(loot).poll();
                item.interact("Take");
                Condition.wait(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        return !item.valid();
                    }
                }, 500, 3);
//                if (item.valid()) item.click();
                break;
            case WALK:
                if (ctx.movement.energyLevel() > 30) ctx.movement.running(true);
                if (!ctx.players.local().inMotion()) ctx.movement.step(destTile);
                break;
            case BANK:
                if (bankTile.distanceTo(ctx.players.local()) > 5) {
                    if (!ctx.players.local().inMotion()) ctx.movement.step(bankTile);
                } else {
                    if (ctx.depositBox.opened()) {
                        looted += ctx.inventory.select().id(loot).count();
//                        ctx.depositBox.depositInventory();
                        ctx.depositBox.deposit(loot, DepositBox.Amount.ALL);
                        Condition.wait(new Callable<Boolean>() {
                            @Override
                            public Boolean call() throws Exception {
                                return ctx.inventory.select().id(loot).isEmpty();
                            }
                        },500,4);
                        ctx.input.send("{VK_ESCAPE}");
                    } else {
                        ctx.objects.select().id(depositBoxID).poll().interact("Deposit");
                        Condition.wait(new Callable<Boolean>() {
                            @Override
                            public Boolean call() throws Exception {
                                return ctx.depositBox.opened();
                            }
                        },500,4);
                    }
                }
                break;
            case WAIT:
//                Condition.sleep(1000);
                break;
        }
    }

    private enum State {
        ATTACK, WAIT, WALK, BANK, LOOT
    }

    private State getState() {
        if (ctx.inventory.select().count() < 28 && !ctx.groundItems.select(2).id(loot).isEmpty() && !ctx.players.local().interacting().valid()) {
            state = "Looting";
            return State.LOOT;
        }
        if (ctx.inventory.select().count() < 28 && destTile.distanceTo(ctx.players.local()) > 7 && !ctx.players.local().interacting().valid()) {
            state = "Walking";
            return State.WALK;
        }
        if (ctx.inventory.select().count() >= 28) {
            state = "Banking";
            return State.BANK;
        }
        if (ctx.players.local().animation() == -1 && !ctx.players.local().inMotion() && !ctx.players.local().interacting().valid()) {
            state = "Attacking";
            return State.ATTACK;
        }
        state = "Waiting";
        return State.WAIT;
    }

    public static final Font TAHOMA = new Font("Tahoma", Font.PLAIN, 12);

    public void repaint(Graphics graphics)
    {
        final Graphics2D g = (Graphics2D) graphics;
        g.setFont(TAHOMA);

        int s = (int)Math.floor(getRuntime()/1000 % 60);
        int m = (int)Math.floor(getRuntime()/60000 % 60);
        int h = (int)Math.floor(getRuntime()/3600000);

        g.setColor(Color.WHITE);
        g.drawString(String.format("Runtime %02d:%02d:%02d", h, m, s), 10, 120);
        g.drawString(String.format("State %s", state), 10, 140);
        g.drawString(String.format("Banked %d", looted), 10, 160);

        g.setColor(Color.BLACK);
        AlphaComposite alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);
        g.setComposite(alphaComposite);
        g.fillRect(5, 100, 200, 100);
    }
}
