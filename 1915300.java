package org.mineground.commands.irc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.jibble.pircbot.Colors;
import org.jibble.pircbot.User;
import org.mineground.Main;
import org.mineground.handlers.IPBanHandler;
import org.mineground.handlers.irc.CommandExecutor;
import org.mineground.handlers.irc.UserLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @name UnbanIP.java
 * @author Daniel Koenen (2012)
 */
public class UnbanIP implements CommandExecutor {

    private static final Logger ExceptionLogger = LoggerFactory.getLogger(UnbanIP.class);

    @Override
    public void onCommand(User sender, UserLevel level, String channel, String command, String arguments[]) {
        if (level.compareTo(UserLevel.IRC_OP) < 0) {
            return;
        }
        if (arguments.length < 1) {
            Main.getInstance().getIRCHandler().sendMessage(channel, Colors.RED + "* Usage:" + Colors.NORMAL + " !unbanip [ip-address]");
            return;
        }
        String ipAddress = arguments[0];
        if (!ipAddress.matches("(\\d{1,3}|\\*)\\.(\\d{1,3}|\\*)\\.(\\d{1,3}|\\*)\\.(\\d{1,3}|\\*)")) {
            Main.getInstance().getIRCHandler().sendMessage(channel, Colors.RED + "* Error: Invalid ip-address format. Use: *.*.*.* (where * can be a number from 0 to 255, or a wildcard chracter).");
            return;
        }
        long rangeStart = IPBanHandler.getRangeStart(ipAddress);
        long rangeEnd = IPBanHandler.getRangeEnd(ipAddress);
        try {
            PreparedStatement queryStatement = Main.getInstance().getDatabaseHandler().getConnection().prepareStatement("SELECT ban_id FROM lvm_ip_bans WHERE start_range = ? AND end_range = ?");
            queryStatement.setLong(1, rangeStart);
            queryStatement.setLong(2, rangeEnd);
            queryStatement.execute();
            ResultSet queryResult = queryStatement.getResultSet();
            int banId = -1;
            if (queryResult.next()) {
                banId = queryResult.getInt(1);
            }
            if (banId == -1) {
                Main.getInstance().getIRCHandler().sendMessage(channel, Colors.RED + "* Error: That ip-ban doesn't exist");
                return;
            }
            IPBanHandler.removeBan(banId);
            StringBuilder unbanMessageBuilder = new StringBuilder();
            unbanMessageBuilder.append(Colors.TEAL);
            unbanMessageBuilder.append("IP-address ");
            unbanMessageBuilder.append(Colors.DARK_GREEN);
            unbanMessageBuilder.append(ipAddress);
            unbanMessageBuilder.append(Colors.TEAL);
            unbanMessageBuilder.append(" has been unbanned.");
            Main.getInstance().getIRCHandler().sendMessage(channel, unbanMessageBuilder.toString());
            queryStatement.close();
        } catch (Exception exception) {
            ExceptionLogger.error("Exception caught", exception);
        }
    }
}
