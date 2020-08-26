package org.mineground.player;

import java.text.DecimalFormat;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jibble.pircbot.Colors;
import org.mineground.Main;
import org.mineground.Utilities;

/**
 * @file ReactionTest.java (18.02.2012)
 * @author Daniel Koenen
 *
 */
public class ReactionTest {

    public static final short REACTION_TYPE_CALCULATION = 0;

    public static final short REACTION_TYPE_CHARACTERS = 1;

    private long testStart;

    private int[] testValue = new int[3];

    private int testMode;

    private int testResult;

    private int priceId;

    private boolean isRunning;

    private String testString;

    private Timer reactionTimer = null;

    public ReactionTest() {
        callTimer();
    }

    private void callTimer() {
        if (reactionTimer != null) {
            reactionTimer.cancel();
        }
        reactionTimer = new Timer();
        reactionTimer.schedule(new Reaction(), Main.getInstance().getConfigHandler().reactionTestDelay * 60000L);
    }

    public void killTimer() {
        if (reactionTimer == null) {
            return;
        }
        reactionTimer.cancel();
        reactionTimer = null;
    }

    public void checkText(PlayerChatEvent event) {
        if (!isRunning) return;
        Player player = event.getPlayer();
        String message = event.getMessage();
        if (testMode == REACTION_TYPE_CALCULATION && Utilities.isNumeric(message) && Integer.parseInt(message) == testResult) {
            win(player);
        } else if (testMode == REACTION_TYPE_CHARACTERS && message.equals(testString)) {
            win(player);
        }
    }

    public void generateTest() {
        if (Main.getInstance().getServer().getOnlinePlayers().length < 2 && Main.getInstance().getConfigHandler().liveVersion) return;
        Random rand = new Random();
        if (rand.nextInt(2) == REACTION_TYPE_CALCULATION) {
            testValue[0] = rand.nextInt(150);
            testValue[1] = rand.nextInt(150);
            testValue[2] = rand.nextInt(150);
            char[] parameter = new char[2];
            parameter[0] = '-';
            parameter[1] = '+';
            char[] randomParameter = new char[2];
            randomParameter[0] = parameter[rand.nextInt(2)];
            randomParameter[1] = parameter[rand.nextInt(2)];
            StringBuilder testStringBuilder = new StringBuilder();
            testStringBuilder.append(testValue[0]);
            testStringBuilder.append(randomParameter[0]);
            testStringBuilder.append(testValue[1]);
            testStringBuilder.append(randomParameter[1]);
            testStringBuilder.append(testValue[2]);
            testString = testStringBuilder.toString();
            if (randomParameter[0] == '+' && randomParameter[1] == '-') testResult = testValue[0] + testValue[1] - testValue[2]; else if (randomParameter[0] == '-' && randomParameter[1] == '+') testResult = testValue[0] - testValue[1] + testValue[2]; else if (randomParameter[0] == '+' && randomParameter[1] == '+') testResult = testValue[0] + testValue[1] + testValue[2]; else if (randomParameter[0] == '-' && randomParameter[1] == '-') testResult = testValue[0] - testValue[1] - testValue[2];
            testMode = REACTION_TYPE_CALCULATION;
            isRunning = true;
            priceId = rand.nextInt(Main.getInstance().getConfigHandler().reactionBlockId.size());
            StringBuilder broadcastMessage = new StringBuilder();
            broadcastMessage.append(ChatColor.YELLOW);
            broadcastMessage.append("First player to solve ");
            broadcastMessage.append(testString);
            broadcastMessage.append(" wins ");
            broadcastMessage.append(Main.getInstance().getConfigHandler().reactionBlockAmount.get(priceId));
            broadcastMessage.append(" ");
            broadcastMessage.append(Main.getInstance().getConfigHandler().reactionBlockName.get(priceId));
            broadcastMessage.append(".");
            Utilities.sendMessageToAll(broadcastMessage.toString());
            broadcastMessage = new StringBuilder();
            broadcastMessage.append(Colors.RED);
            broadcastMessage.append("*** First player to solve ");
            broadcastMessage.append(Colors.BOLD);
            broadcastMessage.append(testString);
            broadcastMessage.append(Colors.NORMAL);
            broadcastMessage.append(Colors.RED);
            broadcastMessage.append(" wins ");
            broadcastMessage.append(Main.getInstance().getConfigHandler().reactionBlockAmount.get(priceId));
            broadcastMessage.append(" ");
            broadcastMessage.append(Main.getInstance().getConfigHandler().reactionBlockName.get(priceId));
            broadcastMessage.append(".");
            Main.getInstance().getIRCHandler().sendEchoMessage(broadcastMessage.toString());
            if (!Main.getInstance().getConfigHandler().liveVersion) Utilities.sendMessageToAll("Result: " + testResult);
        } else {
            String table = "ABCDEFGHIJKLMNPQRTUVWXYZabzdefghijklmnpqrstuvwxyz0123456789";
            testString = "";
            for (int i = 0; i < 8; i++) {
                testString += table.charAt(rand.nextInt(table.length()));
            }
            testMode = REACTION_TYPE_CHARACTERS;
            isRunning = true;
            priceId = rand.nextInt(Main.getInstance().getConfigHandler().reactionBlockId.size());
            StringBuilder broadcastMessage = new StringBuilder();
            broadcastMessage.append(ChatColor.YELLOW);
            broadcastMessage.append("First player to type ");
            broadcastMessage.append(testString);
            broadcastMessage.append(" wins ");
            broadcastMessage.append(Main.getInstance().getConfigHandler().reactionBlockAmount.get(priceId));
            broadcastMessage.append(" ");
            broadcastMessage.append(Main.getInstance().getConfigHandler().reactionBlockName.get(priceId));
            broadcastMessage.append(".");
            Utilities.sendMessageToAll(broadcastMessage.toString());
            broadcastMessage = new StringBuilder();
            broadcastMessage.append(Colors.RED);
            broadcastMessage.append("*** First player to type ");
            broadcastMessage.append(Colors.BOLD);
            broadcastMessage.append(testString);
            broadcastMessage.append(Colors.NORMAL);
            broadcastMessage.append(Colors.RED);
            broadcastMessage.append(" wins ");
            broadcastMessage.append(Main.getInstance().getConfigHandler().reactionBlockAmount.get(priceId));
            broadcastMessage.append(" ");
            broadcastMessage.append(Main.getInstance().getConfigHandler().reactionBlockName.get(priceId));
            broadcastMessage.append(".");
            Main.getInstance().getIRCHandler().sendEchoMessage(broadcastMessage.toString());
        }
        testStart = System.currentTimeMillis();
    }

    public void givePrice(Player player) {
        PlayerInventory inventory;
        int blockId;
        int blockAmount;
        blockId = Main.getInstance().getConfigHandler().reactionBlockId.get(priceId);
        blockAmount = Main.getInstance().getConfigHandler().reactionBlockAmount.get(priceId);
        if (Material.getMaterial(blockId) == null) {
            System.out.println("* Error: Invalid item, visit http://www.minecraftwiki.net/wiki/Data_values#Block_IDs_.28Minecraft_Beta.29 for more information.");
            return;
        }
        inventory = player.getInventory();
        ItemStack newItem = new ItemStack(blockId);
        newItem.setAmount(blockAmount);
        inventory.addItem(newItem);
        StringBuilder winMessageBuilder = new StringBuilder();
        winMessageBuilder.append(ChatColor.GREEN);
        winMessageBuilder.append("You got ");
        winMessageBuilder.append(blockAmount);
        winMessageBuilder.append(" ");
        winMessageBuilder.append(Main.getInstance().getConfigHandler().reactionBlockName.get(priceId));
        winMessageBuilder.append(".");
        player.sendMessage(winMessageBuilder.toString());
    }

    private void win(Player player) {
        long difference = System.currentTimeMillis() - testStart;
        double seconds = difference / 1000;
        DecimalFormat decimalFormatter = new DecimalFormat("#0.00");
        StringBuilder winMessageBuilder = new StringBuilder();
        winMessageBuilder.append(ChatColor.YELLOW);
        winMessageBuilder.append(player.getDisplayName());
        winMessageBuilder.append(" has won the reactiontest in ");
        winMessageBuilder.append(decimalFormatter.format(seconds));
        winMessageBuilder.append(" seconds.");
        Utilities.sendMessageToAll(winMessageBuilder.toString());
        winMessageBuilder = new StringBuilder();
        winMessageBuilder.append(Colors.RED);
        winMessageBuilder.append("*** ");
        winMessageBuilder.append(Utilities.fixName(player));
        winMessageBuilder.append(" has won the reactiontest in ");
        winMessageBuilder.append(decimalFormatter.format(seconds));
        winMessageBuilder.append(" seconds.");
        Main.getInstance().getIRCHandler().sendEchoMessage(winMessageBuilder.toString());
        givePrice(player);
        isRunning = false;
        Main.getInstance().getPlayer(player).addReactiontest();
        callTimer();
    }

    class Reaction extends TimerTask {

        @Override
        public void run() {
            generateTest();
            callTimer();
        }
    }
}
