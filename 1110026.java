package org.mineground.commands.irc;

import org.jibble.pircbot.Colors;
import org.jibble.pircbot.User;
import org.mineground.Main;
import org.mineground.handlers.NameBanHandler;
import org.mineground.handlers.irc.CommandExecutor;
import org.mineground.handlers.irc.UserLevel;
import org.mineground.player.PlayerLogManager;

public class Unban implements CommandExecutor {

    @Override
    public void onCommand(User sender, UserLevel level, String channel, String command, String args[]) {
        if (level.compareTo(UserLevel.IRC_OP) < 0) {
            return;
        }
        if (args.length < 1) {
            Main.getInstance().getIRCHandler().sendMessage(channel, Colors.RED + "* Usage:" + Colors.NORMAL + " !unban [exact player name]");
            return;
        }
        String playerName = args[0];
        int banId = NameBanHandler.getBanId(playerName);
        if (banId == -1) {
            Main.getInstance().getIRCHandler().sendMessage(channel, Colors.RED + "* Error:" + Colors.NORMAL + " That player hasn't been banned.");
            return;
        }
        NameBanHandler.removeBan(banId);
        StringBuilder unbanMessageBuilder = new StringBuilder();
        unbanMessageBuilder.append(Colors.DARK_GREEN);
        unbanMessageBuilder.append(playerName);
        unbanMessageBuilder.append(Colors.TEAL);
        unbanMessageBuilder.append(" has been unbanned.");
        Main.getInstance().getIRCHandler().sendMessage(channel, unbanMessageBuilder.toString());
        PlayerLogManager.addLogEntry(PlayerLogManager.ACTION_ID_UNBAN, playerName, sender.getNick(), "Unbanned");
    }
}
