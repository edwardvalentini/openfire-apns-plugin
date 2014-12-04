package com.wecapslabs.openfire.plugin.apns;

import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.openfire.muc.MUCRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ApnsDBHandler {

    private static final Logger Log = LoggerFactory.getLogger(ApnsPlugin.class);

    private static final String LOAD_TOKEN = "SELECT devicetoken FROM ofAPNS WHERE JID=?";
    private static final String INSERT_TOKEN = "INSERT INTO ofAPNS VALUES(?, ?)";
    private static final String UPDATE_TOKEN = "UPDATE ofAPNS SET deviceToken = ? WHERE JID = ?";
    private static final String DELETE_TOKEN = "DELETE FROM ofAPNS WHERE LOWER(devicetoken) = LOWER(?)";
    private static final String DELETE_BY_JID = "DELETE FROM ofAPNS WHERE JID = ?";
    private static final String LOAD_ROOM_MEMBER_TOKENS = "SELECT devicetoken FROM ofAPNS LEFT JOIN ofMucMember ON ofAPNS.JID = ofMucMember.jid WHERE ofMucMember.roomid = ?";
    private static final String LOAD_ROOM_OWNER_TOKENS = "SELECT devicetoken FROM ofAPNS LEFT JOIN ofMucAffiliation ON ofAPNS.JID = ofMucAffiliation.jid WHERE ofMucAffiliation.roomid = ? AND ofMucAffiliation.affiliation = ?";

    public boolean insertDeviceToken(JID targetJID, String token) {
        Connection con = null;
        PreparedStatement pstmt = null;
        boolean isCompleted = false;
        try {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(INSERT_TOKEN);
            pstmt.setString(1, targetJID.toBareJID());
            pstmt.setString(2, token);
            pstmt.executeUpdate();
            pstmt.close();

            isCompleted = true;
        } catch (SQLException sqle) {
            Log.error(sqle.getMessage(), sqle);
            isCompleted = false;
        } finally {
            DbConnectionManager.closeConnection(null, pstmt, con);
        }
        return isCompleted;
    }

    public boolean deleteDeviceTokenByJID(JID from) {
        Connection con = null;
        PreparedStatement pstmt = null;
        boolean isCompleted = false;
        try {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(DELETE_BY_JID);
            pstmt.setString(1, from.toBareJID());
            pstmt.executeUpdate();
            pstmt.close();

            isCompleted = true;
        } catch (SQLException e) {
            Log.error(e.getMessage(), e);
        } finally {
            DbConnectionManager.closeConnection(null, pstmt, con);
        }
        return isCompleted;
    }

    public boolean deleteDeviceToken(String token) {
        Connection con = null;
        PreparedStatement pstmt = null;
        boolean isCompleted = false;
        try {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(DELETE_TOKEN);
            pstmt.setString(1, token);
            pstmt.executeUpdate();
            pstmt.close();

            isCompleted = true;
        } catch (SQLException sqle) {
            Log.error(sqle.getMessage(), sqle);
            isCompleted = false;
        } finally {
            DbConnectionManager.closeConnection(null, pstmt, con);
        }
        return isCompleted;
    }

    public boolean updateDeviceToken(JID targetJID, String token) {
        Connection con = null;
        PreparedStatement pstmt = null;
        boolean isCompleted = false;
        try {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(UPDATE_TOKEN);
            pstmt.setString(1, token);
            pstmt.setString(2, targetJID.toBareJID());
            pstmt.executeUpdate();
            pstmt.close();

            isCompleted = true;
        } catch (SQLException sqle) {
            Log.error(sqle.getMessage(), sqle);
            isCompleted = false;
        } finally {
            DbConnectionManager.closeConnection(null, pstmt, con);
        }
        return isCompleted;
    }

    public String getDeviceToken(JID targetJID) {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        String returnToken = null;
        try {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(LOAD_TOKEN);
            pstmt.setString(1, targetJID.toBareJID());
            rs = pstmt.executeQuery();
            if (rs.next()) {
                returnToken = rs.getString(1);
            }
            rs.close();
            pstmt.close();
        } catch (SQLException sqle) {
            Log.error(sqle.getMessage(), sqle);
            returnToken = sqle.getMessage();
        } finally {
            DbConnectionManager.closeConnection(rs, pstmt, con);
        }
        return returnToken;
    }

    public List<String> getDeviceTokensForRoom(long roomID) {
        List<String> tokens = getRoomMemberTokens(roomID);
        tokens.addAll(getRoomOwnerTokens(roomID));
        return tokens;
    }

    private List<String> getRoomMemberTokens(long roomID) {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        List<String> returnTokens = new ArrayList<String>();
        try {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(LOAD_ROOM_MEMBER_TOKENS);
            pstmt.setLong(1, roomID);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                returnTokens.add(rs.getString(1));
            }
            rs.close();
            pstmt.close();
        } catch (SQLException sqle) {
            Log.error(sqle.getMessage(), sqle);
        } finally {
            DbConnectionManager.closeConnection(rs, pstmt, con);
        }
        return returnTokens;
    }

    private List<String> getRoomOwnerTokens(long roomID) {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        List<String> returnTokens = new ArrayList<String>();
        try {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(LOAD_ROOM_OWNER_TOKENS);
            pstmt.setLong(1, roomID);
            pstmt.setInt(2, MUCRole.Affiliation.owner.getValue());
            rs = pstmt.executeQuery();
            while (rs.next()) {
                returnTokens.add(rs.getString(1));
            }
            rs.close();
            pstmt.close();
        } catch (SQLException sqle) {
            Log.error(sqle.getMessage(), sqle);
        } finally {
            DbConnectionManager.closeConnection(rs, pstmt, con);
        }
        return returnTokens;
    }

}
