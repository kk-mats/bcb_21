package org.mineground.handlers;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerLoginEvent;
import org.jibble.pircbot.Colors;
import org.mineground.Main;
import org.mineground.Utilities;
import org.mineground.player.MinegroundPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @name IPBanHandler.java
 * @author Daniel Koenen (2012)
 */
public class IPBanHandler {

    private static final Logger ExceptionLogger = LoggerFactory.getLogger(IPBanHandler.class);

    public static final int BAN_FAIL = 0;

    public static final int BAN_CREATE = 1;

    public static final int BAN_ADD = 2;

    public static boolean isInRange(Player player, String rangeAddress) {
        long playerAddress = Utilities.ipToLong(player.getAddress().getAddress().getHostAddress());
        long rangeStart = getRangeStart(rangeAddress);
        long rangeEnd = getRangeEnd(rangeAddress);
        return (playerAddress >= rangeStart && playerAddress <= rangeEnd) ? (true) : (false);
    }

    public static boolean checkBan(String playerAddress, PlayerLoginEvent loginEvent, Player player) {
        try {
            long playerIp = Utilities.ipToLong(playerAddress);
            PreparedStatement queryStatement = Main.getInstance().getDatabaseHandler().getConnection().prepareStatement("SELECT ban_id, start_range, end_range, reason, UNIX_TIMESTAMP(expire_date) FROM lvm_ip_bans WHERE (? >= start_range AND ? <= end_range) OR (? = start_range AND ? = end_range)");
            queryStatement.setLong(1, playerIp);
            queryStatement.setLong(2, playerIp);
            queryStatement.setLong(3, playerIp);
            queryStatement.setLong(4, playerIp);
            queryStatement.execute();
            ResultSet queryResult = queryStatement.getResultSet();
            if (!queryResult.next()) {
                return false;
            }
            boolean isPermanent = false;
            int banId = queryResult.getInt(1);
            long rangeStart = queryResult.getLong(2);
            long rangeEnd = queryResult.getLong(3);
            String banReason = queryResult.getString(4);
            Date expireDate = new Date(queryResult.getLong(5) * 1000);
            MinegroundPlayer playerInstance = Main.getInstance().getPlayer(player);
            if (!expireDate.after(new Date()) && queryResult.getLong(5) != 0L) {
                removeBan(banId);
                return false;
            }
            if (queryResult.getLong(5) == 0) {
                isPermanent = true;
            }
            if (playerInstance != null) {
                int profileId = playerInstance.getProfileId();
                queryStatement = Main.getInstance().getDatabaseHandler().getConnection().prepareStatement("SELECT ban_id FROM lvm_ip_ban_exceptions WHERE ban_id = ? AND player_id = ?");
                queryStatement.setInt(1, banId);
                queryStatement.setInt(2, profileId);
                queryStatement.execute();
                queryResult = queryStatement.getResultSet();
                if (queryResult.next()) {
                    StringBuilder crewInformBuilder = new StringBuilder();
                    crewInformBuilder.append(Colors.RED);
                    crewInformBuilder.append("Notice: ");
                    crewInformBuilder.append(Colors.NORMAL);
                    crewInformBuilder.append("IP ");
                    crewInformBuilder.append(playerAddress);
                    crewInformBuilder.append(" (");
                    crewInformBuilder.append(player.getName());
                    crewInformBuilder.append(") matched with banned address [");
                    crewInformBuilder.append(banId);
                    crewInformBuilder.append("] ");
                    crewInformBuilder.append(getRangeAsString(rangeStart, rangeEnd));
                    crewInformBuilder.append(". Nick is on the exceptions list.");
                    Main.getInstance().getIRCHandler().sendMessage(Main.getInstance().getConfigHandler().ircDevChannel, crewInformBuilder.toString());
                    return false;
                }
            }
            SimpleDateFormat dateFormatter = new SimpleDateFormat("EEEEE, d MMMMM yyyy (HH:mm)", Main.DEFAULT_LOCALE);
            StringBuilder kickMessageBuilder = new StringBuilder();
            if (!isPermanent) {
                kickMessageBuilder.append(ChatColor.AQUA);
                kickMessageBuilder.append("Your ip-address is banned until ");
                kickMessageBuilder.append(ChatColor.GREEN);
                kickMessageBuilder.append(dateFormatter.format(expireDate));
                kickMessageBuilder.append(" GMT + 1");
                kickMessageBuilder.append(ChatColor.AQUA);
                kickMessageBuilder.append(" / Reason: ");
                kickMessageBuilder.append(ChatColor.GREEN);
                kickMessageBuilder.append(banReason);
                kickMessageBuilder.append(ChatColor.AQUA);
                kickMessageBuilder.append(" / Appeal at www.mineground.com");
            } else {
                kickMessageBuilder.append(ChatColor.AQUA);
                kickMessageBuilder.append("Your ip-address is permanently banned");
                kickMessageBuilder.append(ChatColor.AQUA);
                kickMessageBuilder.append(" / Reason: ");
                kickMessageBuilder.append(ChatColor.GREEN);
                kickMessageBuilder.append(banReason);
                kickMessageBuilder.append(ChatColor.AQUA);
                kickMessageBuilder.append(" / Appeal at www.mineground.com");
            }
            loginEvent.disallow(PlayerLoginEvent.Result.KICK_OTHER, kickMessageBuilder.toString());
            StringBuilder crewInformBuilder = new StringBuilder();
            crewInformBuilder.append(Colors.RED);
            crewInformBuilder.append("Notice: ");
            crewInformBuilder.append(Colors.NORMAL);
            crewInformBuilder.append("IP ");
            crewInformBuilder.append(playerAddress);
            crewInformBuilder.append(" (");
            crewInformBuilder.append(player.getName());
            crewInformBuilder.append(") matched with banned address [");
            crewInformBuilder.append(banId);
            crewInformBuilder.append("] ");
            crewInformBuilder.append(getRangeAsString(rangeStart, rangeEnd));
            crewInformBuilder.append(". Nick is not on the exceptions list. Banning ");
            crewInformBuilder.append(player.getName());
            crewInformBuilder.append("...");
            Main.getInstance().getIRCHandler().sendMessage(Main.getInstance().getConfigHandler().ircDevChannel, crewInformBuilder.toString());
            queryStatement.close();
            return true;
        } catch (Exception exception) {
            ExceptionLogger.error("Exception caught", exception);
        }
        return false;
    }

    public static String getRangeAsString(long rangeStart, long rangeEnd) {
        String[] rangeStartBytes = Utilities.longToIp(rangeStart).split("\\.");
        String[] rangeEndBytes = Utilities.longToIp(rangeEnd).split("\\.");
        StringBuilder outputAddress = new StringBuilder();
        for (short byteIndex = 0; byteIndex < 4; byteIndex++) {
            if (rangeStartBytes[byteIndex].equals("0") && rangeEndBytes[byteIndex].equals("255")) {
                outputAddress.append(".*");
                continue;
            }
            outputAddress.append(".");
            outputAddress.append(rangeStartBytes[byteIndex]);
        }
        return outputAddress.toString().substring(1);
    }

    public static long getRangeStart(String rangeFormat) {
        StringBuilder rangeStartStringBuilder = new StringBuilder();
        String[] rangeBytes = rangeFormat.split("\\.");
        for (short byteIndex = 0; byteIndex < 4; byteIndex++) {
            if (rangeBytes[byteIndex].equals("*")) {
                rangeStartStringBuilder.append(".0");
                continue;
            }
            rangeStartStringBuilder.append(".");
            rangeStartStringBuilder.append(rangeBytes[byteIndex]);
        }
        return Utilities.ipToLong(rangeStartStringBuilder.toString().substring(1));
    }

    public static long getRangeEnd(String rangeFormat) {
        StringBuilder rangeEndStringBuilder = new StringBuilder();
        String[] rangeBytes = rangeFormat.split("\\.");
        for (short byteIndex = 0; byteIndex < 4; byteIndex++) {
            if (rangeBytes[byteIndex].equals("*")) {
                rangeEndStringBuilder.append(".255");
                continue;
            }
            rangeEndStringBuilder.append(".");
            rangeEndStringBuilder.append(rangeBytes[byteIndex]);
        }
        return Utilities.ipToLong(rangeEndStringBuilder.toString().substring(1));
    }

    public static int addBan(String rangeFormat, String reason, Date expireDate) {
        try {
            int banId = -1;
            long rangeStart = getRangeStart(rangeFormat);
            long rangeEnd = getRangeEnd(rangeFormat);
            PreparedStatement queryStatement = Main.getInstance().getDatabaseHandler().getConnection().prepareStatement("SELECT ban_id, UNIX_TIMESTAMP(expire_date) FROM lvm_ip_bans WHERE start_range = ? AND end_range = ?");
            queryStatement.setLong(1, rangeStart);
            queryStatement.setLong(2, rangeEnd);
            queryStatement.execute();
            ResultSet queryResult = queryStatement.getResultSet();
            if (queryResult.next()) {
                banId = queryResult.getInt(1);
            }
            if (banId != -1) {
                long newExpireTime = queryResult.getLong(2) + ((expireDate.getTime() - new Date().getTime()) / 1000);
                queryStatement = Main.getInstance().getDatabaseHandler().getConnection().prepareStatement("UPDATE lvm_ip_bans SET reason = ?, expire_date = FROM_UNIXTIME(?) WHERE ban_id = ?");
                queryStatement.setString(1, reason);
                queryStatement.setLong(2, newExpireTime);
                queryStatement.setInt(3, banId);
                queryStatement.execute();
                queryStatement.close();
                return BAN_ADD;
            }
            queryStatement = Main.getInstance().getDatabaseHandler().getConnection().prepareStatement("INSERT INTO lvm_ip_bans (start_range, end_range, reason, expire_date) VALUES (?, ?, ?, FROM_UNIXTIME(?))");
            queryStatement.setLong(1, rangeStart);
            queryStatement.setLong(2, rangeEnd);
            queryStatement.setString(3, reason);
            queryStatement.setLong(4, (expireDate.getTime()) / 1000L);
            queryStatement.execute();
            StringBuilder crewInformBuilder;
            for (Player player : Main.getInstance().getServer().getOnlinePlayers()) {
                if (player.getAddress() == null) {
                    continue;
                }
                if (isInRange(player, rangeFormat)) {
                    crewInformBuilder = new StringBuilder();
                    crewInformBuilder.append(ChatColor.AQUA);
                    crewInformBuilder.append("Your ip-address has been banned from this server / Reason: ");
                    crewInformBuilder.append(ChatColor.GREEN);
                    crewInformBuilder.append(reason);
                    crewInformBuilder.append(ChatColor.AQUA);
                    crewInformBuilder.append(" / Appeal at www.mineground.com");
                    player.kickPlayer(crewInformBuilder.toString());
                }
            }
            queryStatement.close();
            return BAN_CREATE;
        } catch (Exception exception) {
            ExceptionLogger.error("Exception caught", exception);
        }
        return BAN_FAIL;
    }

    public static void removeBan(int banId) {
        try {
            PreparedStatement preparedStatement = Main.getInstance().getDatabaseHandler().getConnection().prepareStatement("DELETE FROM lvm_ip_bans WHERE ban_id = ?");
            preparedStatement.setInt(1, banId);
            preparedStatement.execute();
            preparedStatement.close();
        } catch (Exception exception) {
            ExceptionLogger.error("Exception caught", exception);
        }
    }
}
