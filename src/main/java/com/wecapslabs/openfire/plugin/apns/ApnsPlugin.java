package com.wecapslabs.openfire.plugin.apns;

import com.notnoop.apns.*;
import com.notnoop.apns.internal.Utilities;
import com.notnoop.exceptions.ApnsDeliveryErrorException;
import com.notnoop.exceptions.NetworkIOException;
import org.jivesoftware.openfire.IQRouter;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.handler.IQHandler;
import org.jivesoftware.openfire.interceptor.InterceptorManager;
import org.jivesoftware.openfire.interceptor.PacketInterceptor;
import org.jivesoftware.openfire.interceptor.PacketRejectedException;
import org.jivesoftware.openfire.muc.MUCRoom;
import org.jivesoftware.openfire.muc.MultiUserChatService;
import org.jivesoftware.openfire.session.Session;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.openfire.user.UserManager;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ApnsPlugin implements Plugin, PacketInterceptor, ApnsDelegate {

    private static final Logger Log = LoggerFactory.getLogger(ApnsPlugin.class);

    private XMPPServer xmppServer;
    private InterceptorManager interceptorManager;
    private ApnsDBHandler dbManager;
    private ApnsService apnsService;

    public ApnsPlugin() {
        xmppServer = XMPPServer.getInstance();
        interceptorManager = InterceptorManager.getInstance();
        dbManager = new ApnsDBHandler();
    }

    @SuppressWarnings("unused")
    public void setCertificatePath(String certificatePath) {
        JiveGlobals.setProperty("plugin.apns.certificatePath", certificatePath);
    }
    
    public String getCertificatePath() {
        return JiveGlobals.getProperty("plugin.apns.certificatePath", "/certificate.p12");
    }

    @SuppressWarnings("unused")
    public void setPassword(String password) {
        JiveGlobals.setProperty("plugin.apns.password", password);
    }

    public String getPassword() {
        return JiveGlobals.getProperty("plugin.apns.password", "");
    }

    @SuppressWarnings("unused")
    public void setBadge(String badge) {
        JiveGlobals.setProperty("plugin.apns.badge", badge);
    }

    public int getBadge() {
        return Integer.parseInt(JiveGlobals.getProperty("plugin.apns.badge", "1"));
    }

    @SuppressWarnings("unused")
    public void setSound(String sound) {
        JiveGlobals.setProperty("plugin.apns.sound", sound);
    }

    public String getSound() {
        return JiveGlobals.getProperty("plugin.apns.sound", "default");
    }

    @SuppressWarnings("unused")
    public void setProduction(String production) {
        JiveGlobals.setProperty("plugin.apns.production", production);
    }

    public boolean getProduction() {
        return Boolean.parseBoolean(JiveGlobals.getProperty("plugin.apns.production", "false"));
    }

    private ApnsService getApnsService() {
        if (apnsService == null) {
            try {
                apnsService = APNS.newService()
                        .withCert(getCertificatePath(), getPassword())
                        .withAppleDestination(getProduction())
                        .withDelegate(this)
                        .build();
            } catch (Exception e) {
                Log.error(e.getMessage(), e);
            }
        }
        return apnsService;
    }

    public void initializePlugin(PluginManager pManager, File pluginDirectory) {
        interceptorManager.addInterceptor(this);

        IQHandler myHandler = new ApnsIQHandler();
        IQRouter iqRouter = xmppServer.getIQRouter();
        iqRouter.addHandler(myHandler);
    }

    public void destroyPlugin() {
        if (apnsService != null) {
            apnsService.stop();
        }
        interceptorManager.removeInterceptor(this);
    }

    public void interceptPacket(Packet packet, Session session, boolean read, boolean processed) throws PacketRejectedException {
        if (isValidTargetPacket(packet, read, processed)) {
            if (packet instanceof Message) {
                Message receivedMessage = (Message) packet;
                Message.Type messageType = receivedMessage.getType();
                if (messageType != Message.Type.chat && messageType != Message.Type.groupchat) {
                    return;
                }

                String messageBody = receivedMessage.getBody();
                if (messageBody == null) {
                    return;
                }

                JID sourceJID = receivedMessage.getFrom();
                JID targetJID = receivedMessage.getTo();
                String username = sourceJID.getNode();
                String displayName = getUserDisplayName(username);
                PayloadBuilder payloadBuilder = APNS.newPayload().badge(getBadge()).sound(getSound());
                List<String> deviceTokens = new ArrayList<String>();

                if (messageType == Message.Type.chat) {
                    payloadBuilder.alertBody(displayName + ": " + messageBody);
                    payloadBuilder.customField("username", username);
                    String token = dbManager.getDeviceToken(targetJID);
                    if (token != null) deviceTokens.add(token);
                } else if (messageType == Message.Type.groupchat) {
                    MUCRoom room = getRoom(targetJID);
                    String roomDisplayName = null;
                    if (room != null) {
                        roomDisplayName = room.getNaturalLanguageName();
                        deviceTokens.addAll(dbManager.getDeviceTokensForRoom(room.getID()));
                        payloadBuilder.customField("roomname", room.getName());
                    }
                    payloadBuilder.alertBody(displayName + "@" + roomDisplayName + ": " + messageBody);
                }

                ApnsService apnsService = getApnsService();
                if (deviceTokens.isEmpty() || apnsService == null) {
                    return;
                }

                try {
                    String payloadString = payloadBuilder.shrinkBody("...").build();
                    apnsService.push(deviceTokens, payloadString);
                } catch (NetworkIOException e) {
                    Log.error(e.getMessage(), e);
                }
            }
        }
    }

    private boolean isValidTargetPacket(Packet packet, boolean read, boolean processed) {
        return !processed && read && packet instanceof Message;
    }

    private String getUserDisplayName(String username) {
        User user = null;
        try {
            user = UserManager.getInstance().getUser(username);
        } catch (UserNotFoundException e) {
            Log.error("username = " + username, e);
        }

        return user == null ? username : user.getName();
    }

    private MUCRoom getRoom(JID roomJID) {
        String roomName = roomJID.getNode();
        String roomDomain = roomJID.getDomain();
        String subdomain = roomDomain.contains(".") ? roomDomain.split("\\.")[0] : roomDomain;

        MultiUserChatService mucService = xmppServer.getMultiUserChatManager().getMultiUserChatService(subdomain);
        if (mucService != null) {
            return mucService.getChatRoom(roomName);
        }

        return null;
    }


    // ApnsDelegate methods

    public void messageSent(final ApnsNotification message, final boolean resent) {
        String deviceToken = Utilities.encodeHex(message.getDeviceToken());
        Log.info("messageSent: Id = " + message.getIdentifier() +
                ", deviceToken = " + deviceToken + ", resent: " + resent);
    }

    public void messageSendFailed(final ApnsNotification message, final Throwable e) {
        String deviceToken = Utilities.encodeHex(message.getDeviceToken());
        Log.error("messageSendFailed: Id = " + message.getIdentifier() +
                ", deviceToken = " + deviceToken, e);

        if (e instanceof ApnsDeliveryErrorException) {
            ApnsDeliveryErrorException deliveryErrorException = (ApnsDeliveryErrorException) e;
            if (deliveryErrorException.getDeliveryError() == DeliveryError.INVALID_TOKEN) {
                dbManager.deleteDeviceToken(deviceToken);
            }
        }
    }

    public void connectionClosed(final DeliveryError e, final int messageIdentifier) {
    }

    public void cacheLengthExceeded(final int newCacheLength) {
    }

    public void notificationsResent(final int resendCount) {
    }

    /*
     * TODO: We can use APNS Feedback service to get the list of inactive devices to delete them from the database.
     *       Apple suggests to do that once a day.
     *       For now we just delete deviceTokens when we receive INVALID_TOKEN(8) error after sending a message. See messageSendFailed(...).
     */
    //private void cleanInactiveDevices(ApnsService apnsService) throws NetworkIOException {
    //    final Map<String, Date> inactiveDevices = svc.getInactiveDevices();
    //    for (final Entry<String, Date> ent : inactiveDevices.entrySet()) {
    //        System.out.println("Inactive " + ent.getKey() + " at date " + ent.getValue());
    //    }
    //}
}
