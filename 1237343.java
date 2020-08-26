package org.mineground.commands.player.admin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.mineground.PermissionNodes;
import org.mineground.Utilities;

/**
 * @file Resources.java (20.02.2012)
 * @author Daniel Koenen
 *
 */
public class Resources implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender cs, Command cmnd, String string, String[] strings) {
        Player player = (Player) cs;
        if (!player.hasPermission(PermissionNodes.PERMISSION_RESOURCES_COMMAND)) {
            return true;
        }
        double totalMemory = Double.valueOf((Runtime.getRuntime().totalMemory() / 1048576));
        double usedMemory = totalMemory - Double.valueOf((Runtime.getRuntime().freeMemory() / 1048576));
        double percentUsed = usedMemory / (totalMemory / 100.0);
        double percentUsedCPU = Utilities.getThreadCPUUsage();
        ChatColor criticalColorMemory = getCriticalColor(percentUsed);
        ChatColor criticalColorCPU = getCriticalColor(percentUsedCPU);
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append("Total Memory: ");
        messageBuilder.append(Math.round(totalMemory));
        messageBuilder.append(" / Used Memory: ");
        messageBuilder.append(Math.round(usedMemory));
        messageBuilder.append(" (");
        messageBuilder.append(criticalColorMemory);
        messageBuilder.append(Math.round(percentUsed));
        messageBuilder.append("%");
        messageBuilder.append(ChatColor.WHITE);
        messageBuilder.append(")");
        player.sendMessage(messageBuilder.toString());
        messageBuilder = new StringBuilder();
        messageBuilder.append("CPU Usage: ");
        messageBuilder.append(criticalColorCPU);
        messageBuilder.append(Math.round(percentUsedCPU));
        messageBuilder.append("%");
        player.sendMessage(messageBuilder.toString());
        return true;
    }

    private ChatColor getCriticalColor(double percent) {
        ChatColor criticalColor;
        if (percent < 50) {
            criticalColor = ChatColor.GREEN;
        } else if (percent >= 50 && percent < 75) {
            criticalColor = ChatColor.YELLOW;
        } else {
            criticalColor = ChatColor.RED;
        }
        return criticalColor;
    }
}
