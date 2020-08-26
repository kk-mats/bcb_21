package org.mineground.commands.irc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.jibble.pircbot.Colors;
import org.jibble.pircbot.User;
import org.mineground.Main;
import org.mineground.handlers.IPBanHandler;
import org.mineground.handlers.irc.CommandExecutor;
import org.mineground.handlers.irc.UserLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @file IsBanned.java (21.02.2012)
 * @author Daniel Koenen
 *
 */
public class IsBanned implements CommandExecutor {

    private static final Logger ExceptionLogger = LoggerFactory.getLogger(IsBanned.class);

    @Override
    public void onCommand(User sender, UserLevel level, String channel, String command, String[] args) {
        if (level.compareTo(UserLevel.IRC_OP) < 0) {
            return;
        }
        if (args.length < 1) {
            Main.getInstance().getIRCHandler().sendMessage(channel, Colors.RED + "* Usage:" + Colors.NORMAL + " !isbanned [exact player name/ip-address]");
            return;
        }
        if (args[0].matches("(\\d{1,3}|\\*)\\.(\\d{1,3}|\\*)\\.(\\d{1,3}|\\*)\\.(\\d{1,3}|\\*)")) {
            isIPBanned(args[0], channel);
            return;
        }
        isNameBanned(args[0], channel);
    }

    private void isNameBanned(String playerName, String channel) {
        try {
            PreparedStatement preparedStatement = Main.getInstance().getDatabaseHandler().getConnection().prepareStatement("SELECT player_id FROM lvm_players WHERE login_name = ?");
            preparedStatement.setString(1, playerName);
            preparedStatement.execute();
            ResultSet queryResult = preparedStatement.getResultSet();
            if (!queryResult.next()) {
                Main.getInstance().getIRCHandler().sendMessage(channel, Colors.RED + "* Error: Invalid player name / ip-address.");
                return;
            }
            int profileId = queryResult.getInt(1);
            preparedStatement = Main.getInstance().getDatabaseHandler().getConnection().prepareStatement("SELECT reason, UNIX_TIMESTAMP(expiredate) FROM lvm_bans WHERE player_id = ?");
            preparedStatement.setInt(1, profileId);
            preparedStatement.execute();
            queryResult = preparedStatement.getResultSet();
            if (!queryResult.next()) {
                Main.getInstance().getIRCHandler().sendMessage(channel, Colors.DARK_GREEN + playerName + " is not banned.");
                return;
            }
            String reason = queryResult.getString(1);
            long expireDate = queryResult.getLong(2);
            StringBuilder messageBuilder = new StringBuilder();
            SimpleDateFormat dateFormatter = new SimpleDateFormat("EEEEE, d MMMMM yyyy, HH:mm", Main.DEFAULT_LOCALE);
            messageBuilder.append(Colors.DARK_GREEN);
            messageBuilder.append(playerName);
            messageBuilder.append(Colors.TEAL);
            messageBuilder.append(" was banned for ");
            messageBuilder.append(Colors.DARK_GREEN);
            messageBuilder.append(reason);
            if (expireDate > 0) {
                messageBuilder.append(Colors.TEAL);
                messageBuilder.append(" and won't be able to play until ");
                messageBuilder.append(Colors.DARK_GREEN);
                messageBuilder.append(dateFormatter.format(new Date(expireDate * 1000)));
            }
            messageBuilder.append(Colors.TEAL);
            messageBuilder.append(".");
            Main.getInstance().getIRCHandler().sendMessage(channel, messageBuilder.toString());
            Main.getInstance().getIRCHandler().sendMessage(channel, Colors.BROWN + "Use '!why " + playerName + "' for further information.");
            preparedStatement.close();
        } catch (Exception exception) {
            ExceptionLogger.error("Exception caught", exception);
        }
    }

    private void isIPBanned(String ipAddress, String channel) {
        try {
            long rangeStart = IPBanHandler.getRangeStart(ipAddress);
            long rangeEnd = IPBanHandler.getRangeEnd(ipAddress);
            PreparedStatement preparedStatement = Main.getInstance().getDatabaseHandler().getConnection().prepareStatement("SELECT reason, UNIX_TIMESTAMP(expire_date) FROM lvm_ip_bans WHERE ? >= start_range AND ? <= end_range");
            preparedStatement.setLong(1, rangeStart);
            preparedStatement.setLong(2, rangeEnd);
            preparedStatement.execute();
            ResultSet queryResult = preparedStatement.getResultSet();
            if (!queryResult.next()) {
                Main.getInstance().getIRCHandler().sendMessage(channel, Colors.DARK_GREEN + ipAddress + " is not banned.");
                return;
            }
            String reason = queryResult.getString(1);
            long expireDate = queryResult.getLong(2);
            StringBuilder messageBuilder = new StringBuilder();
            SimpleDateFormat dateFormatter = new SimpleDateFormat("EEEEE, d MMMMM yyyy, HH:mm", Main.DEFAULT_LOCALE);
            messageBuilder.append(Colors.TEAL);
            messageBuilder.append("IP ");
            messageBuilder.append(Colors.DARK_GREEN);
            messageBuilder.append(ipAddress);
            messageBuilder.append(Colors.TEAL);
            messageBuilder.append(" was banned for ");
            messageBuilder.append(Colors.DARK_GREEN);
            messageBuilder.append(reason);
            if (expireDate > 0) {
                messageBuilder.append(Colors.TEAL);
                messageBuilder.append(" and no one using it will be able to connect until ");
                messageBuilder.append(Colors.DARK_GREEN);
                messageBuilder.append(dateFormatter.format(new Date(expireDate * 1000)));
            }
            messageBuilder.append(Colors.TEAL);
            messageBuilder.append(".");
            Main.getInstance().getIRCHandler().sendMessage(channel, messageBuilder.toString());
            preparedStatement.close();
        } catch (Exception exception) {
            ExceptionLogger.error("Exception caught", exception);
        }
    }
}
