package org.mineground.handlers;

import java.sql.Connection;
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
import org.mineground.player.PlayerLogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @file NameBanHandler.java (20.02.2012)
 * @author Daniel Koenen
 *
 */
public class NameBanHandler {

    private static final Logger ExceptionLogger = LoggerFactory.getLogger(NameBanHandler.class);

    public static final int BAN_FAIL = 0;

    public static final int BAN_CREATE = 1;

    public static final int BAN_ADD = 2;

    public static boolean checkBan(PlayerLoginEvent loginEvent, MinegroundPlayer player) {
        try {
            PreparedStatement preparedStatement;
            if (!player.isRegistered()) {
                preparedStatement = Main.getInstance().getDatabaseHandler().getConnection().prepareStatement("SELECT ban_id, reason, UNIX_TIMESTAMP(expiredate) FROM lvm_bans WHERE player_name = ?");
                preparedStatement.setString(1, player.getPlayer().getName());
            } else {
                preparedStatement = Main.getInstance().getDatabaseHandler().getConnection().prepareStatement("SELECT ban_id, reason, UNIX_TIMESTAMP(expiredate) FROM lvm_bans WHERE player_id = ?");
                preparedStatement.setInt(1, player.getProfileId());
            }
            preparedStatement.execute();
            ResultSet queryResult = preparedStatement.getResultSet();
            if (!queryResult.next()) {
                return false;
            }
            long sqlExpireDate = queryResult.getLong(3);
            int banId = queryResult.getInt(1);
            String reasonMessage = queryResult.getString(2);
            if (sqlExpireDate == 0) {
                StringBuilder kickMessageBuilder = new StringBuilder();
                kickMessageBuilder.append(ChatColor.AQUA);
                kickMessageBuilder.append("You are banned from this server / Reason: ");
                kickMessageBuilder.append(ChatColor.GREEN);
                kickMessageBuilder.append(reasonMessage);
                kickMessageBuilder.append(ChatColor.AQUA);
                kickMessageBuilder.append(" / Appeal at www.mineground.com");
                loginEvent.disallow(PlayerLoginEvent.Result.KICK_OTHER, kickMessageBuilder.toString());
                kickMessageBuilder = new StringBuilder();
                kickMessageBuilder.append(Colors.BROWN);
                kickMessageBuilder.append(Utilities.fixName(loginEvent.getPlayer()));
                kickMessageBuilder.append(Colors.BROWN);
                kickMessageBuilder.append(" attempted to join the server while being banned. (Reason: ");
                kickMessageBuilder.append(Colors.RED);
                kickMessageBuilder.append(reasonMessage);
                kickMessageBuilder.append(Colors.BROWN);
                kickMessageBuilder.append(" / Expire date: ");
                kickMessageBuilder.append(Colors.RED);
                kickMessageBuilder.append("Permanent");
                kickMessageBuilder.append(Colors.BROWN);
                kickMessageBuilder.append(")");
                Main.getInstance().getIRCHandler().sendMessage(Main.getInstance().getConfigHandler().ircDevChannel, kickMessageBuilder.toString());
                return true;
            }
            Date expireDate = new Date(sqlExpireDate * 1000);
            Date nowDate = new Date();
            if (expireDate.after(nowDate)) {
                SimpleDateFormat dateFormatter = new SimpleDateFormat("EEEEE, d MMMMM yyyy (HH:mm)", Main.DEFAULT_LOCALE);
                StringBuilder kickMessageBuilder = new StringBuilder();
                kickMessageBuilder.append(ChatColor.AQUA);
                kickMessageBuilder.append("You are banned until ");
                kickMessageBuilder.append(ChatColor.GREEN);
                kickMessageBuilder.append(dateFormatter.format(expireDate));
                kickMessageBuilder.append(" GMT + 1");
                kickMessageBuilder.append(ChatColor.AQUA);
                kickMessageBuilder.append(" / Reason: ");
                kickMessageBuilder.append(ChatColor.GREEN);
                kickMessageBuilder.append(reasonMessage);
                kickMessageBuilder.append(ChatColor.AQUA);
                kickMessageBuilder.append(" / Appeal at www.mineground.com");
                loginEvent.disallow(PlayerLoginEvent.Result.KICK_OTHER, kickMessageBuilder.toString());
                preparedStatement.close();
                kickMessageBuilder = new StringBuilder();
                kickMessageBuilder.append(Colors.RED);
                kickMessageBuilder.append("Notice: ");
                kickMessageBuilder.append(Colors.BROWN);
                kickMessageBuilder.append(Utilities.fixName(loginEvent.getPlayer()));
                kickMessageBuilder.append(Colors.BROWN);
                kickMessageBuilder.append(" attempted to join the server while being banned.  (Reason: ");
                kickMessageBuilder.append(Colors.RED);
                kickMessageBuilder.append(reasonMessage);
                kickMessageBuilder.append(Colors.BROWN);
                kickMessageBuilder.append(" / Expire date: ");
                kickMessageBuilder.append(Colors.RED);
                kickMessageBuilder.append(dateFormatter.format(expireDate));
                kickMessageBuilder.append(Colors.BROWN);
                kickMessageBuilder.append(")");
                Main.getInstance().getIRCHandler().sendMessage(Main.getInstance().getConfigHandler().ircDevChannel, kickMessageBuilder.toString());
                return true;
            }
            removeBan(banId);
            preparedStatement.close();
        } catch (Exception exception) {
            ExceptionLogger.error("Exception caught", exception);
        }
        return false;
    }

    public static void removeBan(int banId) {
        try {
            PreparedStatement preparedStatement = Main.getInstance().getDatabaseHandler().getConnection().prepareStatement("DELETE FROM lvm_bans WHERE ban_id = ?");
            preparedStatement.setInt(1, banId);
            preparedStatement.execute();
            preparedStatement.close();
        } catch (Exception exception) {
            ExceptionLogger.error("Exception caught", exception);
        }
    }

    public static int addBan(String playerName, String adminName, String reason, Date expireDate) {
        try {
            int profileId = MinegroundPlayer.INVALID_PROFILE_ID;
            PreparedStatement preparedStatement = Main.getInstance().getDatabaseHandler().getConnection().prepareStatement("SELECT player_id FROM lvm_players WHERE login_name = ?");
            preparedStatement.setString(1, playerName);
            preparedStatement.execute();
            ResultSet queryResult = preparedStatement.getResultSet();
            if (!queryResult.next()) {
                preparedStatement = Main.getInstance().getDatabaseHandler().getConnection().prepareStatement("SELECT ban_id, UNIX_TIMESTAMP(expiredate) FROM lvm_bans WHERE player_name = ?");
                preparedStatement.setString(1, playerName);
            } else {
                profileId = queryResult.getInt(1);
                preparedStatement = Main.getInstance().getDatabaseHandler().getConnection().prepareStatement("SELECT ban_id, UNIX_TIMESTAMP(expiredate) FROM lvm_bans WHERE player_id = ?");
                preparedStatement.setInt(1, profileId);
            }
            preparedStatement.execute();
            queryResult = preparedStatement.getResultSet();
            if (queryResult.next()) {
                int banId = queryResult.getInt(1);
                long expireTime = queryResult.getLong(2);
                long restTime = expireTime + ((expireDate.getTime() - new Date().getTime()) / 1000);
                preparedStatement = Main.getInstance().getDatabaseHandler().getConnection().prepareStatement("UPDATE lvm_bans SET reason = ?, expiredate = FROM_UNIXTIME(?) WHERE ban_id = ?");
                preparedStatement.setString(1, reason);
                preparedStatement.setLong(2, restTime);
                preparedStatement.setInt(3, banId);
                preparedStatement.execute();
                return NameBanHandler.BAN_ADD;
            }
            if (profileId != MinegroundPlayer.INVALID_PROFILE_ID) {
                preparedStatement = Main.getInstance().getDatabaseHandler().getConnection().prepareStatement("INSERT INTO lvm_bans (player_id, player_name, reason, expiredate) VALUES(?, ?, ?, FROM_UNIXTIME(?))");
                preparedStatement.setInt(1, profileId);
                preparedStatement.setString(2, playerName);
                preparedStatement.setString(3, reason);
                preparedStatement.setLong(4, expireDate.getTime() / 1000);
            } else {
                preparedStatement = Main.getInstance().getDatabaseHandler().getConnection().prepareStatement("INSERT INTO lvm_bans (player_id, player_name, reason, expiredate) VALUES(NULL, ?, ?, FROM_UNIXTIME(?))");
                preparedStatement.setString(1, playerName);
                preparedStatement.setString(2, reason);
                preparedStatement.setLong(3, expireDate.getTime() / 1000);
            }
            preparedStatement.execute();
            Player onlinePlayer = Main.getInstance().getServer().getPlayer(playerName);
            if (onlinePlayer != null) {
                SimpleDateFormat dateFormatter = new SimpleDateFormat("EEEEE, d MMMMM yyyy (HH:mm)", Main.DEFAULT_LOCALE);
                StringBuilder kickMessageBuilder = new StringBuilder();
                if (expireDate.getTime() > 0) {
                    kickMessageBuilder.append(ChatColor.AQUA);
                    kickMessageBuilder.append("You are banned until ");
                    kickMessageBuilder.append(ChatColor.GREEN);
                    kickMessageBuilder.append(dateFormatter.format(expireDate));
                    kickMessageBuilder.append(" GMT + 1");
                } else {
                    kickMessageBuilder.append(ChatColor.AQUA);
                    kickMessageBuilder.append("You are banned from this server");
                }
                kickMessageBuilder.append(ChatColor.AQUA);
                kickMessageBuilder.append(" / Reason: ");
                kickMessageBuilder.append(ChatColor.GREEN);
                kickMessageBuilder.append(reason);
                kickMessageBuilder.append(ChatColor.AQUA);
                kickMessageBuilder.append(" / Appeal at www.mineground.com");
                onlinePlayer.kickPlayer(kickMessageBuilder.toString());
            }
            PlayerLogManager.addLogEntry(PlayerLogManager.ACTION_ID_BAN, playerName, adminName, reason);
            preparedStatement.close();
        } catch (Exception exception) {
            ExceptionLogger.error("Exception caught", exception);
        }
        return NameBanHandler.BAN_CREATE;
    }

    public static int getBanId(String playerName) {
        Connection databaseConnection = Main.getInstance().getDatabaseHandler().getConnection();
        try {
            PreparedStatement preparedStatement = databaseConnection.prepareStatement("SELECT player_id FROM lvm_players WHERE login_name = ?");
            preparedStatement.setString(1, playerName);
            preparedStatement.execute();
            ResultSet queryResult = preparedStatement.getResultSet();
            if (!queryResult.next()) {
                preparedStatement = databaseConnection.prepareStatement("SELECT ban_id FROM lvm_bans WHERE player_name = ?");
                preparedStatement.setString(1, playerName);
            } else {
                int profileId = queryResult.getInt(1);
                preparedStatement = databaseConnection.prepareStatement("SELECT ban_id FROM lvm_bans WHERE player_id = ?");
                preparedStatement.setInt(1, profileId);
            }
            preparedStatement.execute();
            queryResult = preparedStatement.getResultSet();
            if (!queryResult.next()) {
                return -1;
            }
            int banId = queryResult.getInt(1);
            preparedStatement.close();
            return banId;
        } catch (Exception exception) {
            ExceptionLogger.error("Exception caught", exception);
        }
        return -1;
    }

    public static boolean kickPlayer(String playerName, String adminName, String reason) {
        Player onlinePlayer = Main.getInstance().getServer().getPlayer(playerName);
        if (onlinePlayer == null) {
            return false;
        }
        MinegroundPlayer playerInstance = Main.getInstance().getPlayer(onlinePlayer);
        if (playerInstance != null) {
            playerInstance.setKicked(true);
        }
        StringBuilder kickMessageBuilder = new StringBuilder();
        kickMessageBuilder.append(ChatColor.AQUA);
        kickMessageBuilder.append("You have been kicked  / Reason: ");
        kickMessageBuilder.append(ChatColor.GREEN);
        kickMessageBuilder.append(reason);
        onlinePlayer.kickPlayer(kickMessageBuilder.toString());
        PlayerLogManager.addLogEntry(PlayerLogManager.ACTION_ID_KICK, playerName, adminName, reason);
        return true;
    }
}
