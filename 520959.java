package org.jabber.jabberbeans;

import org.jabber.jabberbeans.*;
import org.jabber.jabberbeans.util.JID;
import org.jabber.jabberbeans.Extension.*;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;
import java.util.*;
import java.io.*;
import java.lang.InstantiationException;

public class GroupChatBean implements Serializable {

    private JID address = null;

    private boolean debug = false;

    private ConnectionBean cb = null;

    private Vector chatListeners = new Vector();

    private Hashtable participantList = new Hashtable();

    private InternalPacketListener listener = null;

    private PresenceBean pb = null;

    public GroupChatBean(ConnectionBean cb, JID address, PresenceBean pb) {
        reset(cb, address, pb);
    }

    public void reset(ConnectionBean cb, JID address, PresenceBean pb) {
        try {
            cb.delPacketListener(listener);
        } catch (Exception e) {
            e.printStackTrace();
        }
        listener = new InternalPacketListener();
        cb.addPacketListener(listener);
        this.cb = cb;
        this.address = address;
        this.pb = pb;
        participantList.clear();
        pb.setBlacklistEnable(true);
        pb.addToBlacklist(new JID(address.getUsername(), address.getServer(), null));
        sendPresence(null, "available", "available");
    }

    public void shutdown() {
        pb.removeFromBlacklist(address);
        sendPresence("unavailable", "unavailable", "unavailable");
    }

    public void setAddress(JID jid) {
        if (jid != null) address = jid;
        sendPresence(null, "available", "available");
    }

    public JID getAddress() {
        return address;
    }

    public void setNickName(String nickName) {
        if (nickName == null || "".equals(nickName)) return;
        address = new JID(address.getUsername(), address.getServer(), nickName);
        sendPresence(null, "available", "available");
    }

    public String getNickName() {
        return address.getResource();
    }

    public boolean sendPresence(String type, String state, String status) {
        if (state == null || status == null) return false;
        try {
            PresenceBuilder builder = new PresenceBuilder();
            builder.reset();
            if (type != null) builder.setType(type);
            builder.setStateShow(state);
            builder.setStatus(status);
            builder.setToAddress(address);
            cb.send(builder.build());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean sendMessage(String subject, String body) {
        if (body == null) return false;
        try {
            MessageBuilder builder = new MessageBuilder();
            builder.reset();
            builder.setToAddress(new JID(address.getUsername(), address.getServer(), null));
            builder.setType("groupchat");
            if (subject != null) builder.setSubject(subject);
            builder.setBody(body);
            cb.send(builder.build());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    class InternalPacketListener implements PacketListener {

        public void receivedPacket(PacketEvent pe) {
            try {
                if (debug) System.err.println(pe.getPacket());
                if (pe.getPacket() instanceof Presence) {
                    Presence temp = (Presence) pe.getPacket();
                    if (temp.getFromAddress().toSimpleString().equals(address.toSimpleString())) PresenceHandler(temp);
                    return;
                }
                if (pe.getPacket() instanceof Message) {
                    Message temp = (Message) pe.getPacket();
                    if (temp.getFromAddress().toSimpleString().equals(address.toSimpleString())) MessageHandler((Message) pe.getPacket());
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void sentPacket(PacketEvent pe) {
        }

        public void sendFailed(PacketEvent pe) {
        }
    }

    public void PresenceHandler(Presence p) {
        String type = p.getType();
        JID addy = p.getFromAddress();
        String state = p.getStateShow();
        String status = p.getStatus();
        JID full_address = addy;
        JID room_address = JID.fromString(addy.toSimpleString());
        boolean room_contained = participantList.containsKey(room_address);
        if (type == (null) || "available".equals(type)) {
            if (!room_contained) {
                if (debug) System.err.println("Adding user the first time.");
                PresenceUserNode node = new PresenceUserNode(addy, state, status);
                if (debug) System.err.println("Node: " + node);
                participantList.put(room_address, node);
                fireChangedPresence(participantList, p, node, "online");
                if (debug) System.err.println("Fired changed presence event.");
            } else {
                PresenceUserNode node = (PresenceUserNode) participantList.get(room_address);
                if (node.containsResource(full_address)) {
                    if (debug) System.err.println(full_address + " adjusted for state info.");
                    node.setState(addy, state);
                    node.setStatus(addy, status);
                    fireChangedPresence(participantList, p, node, "status");
                } else {
                    if (debug) System.err.println(addy + " added as a resource to the user node.");
                    node.addResource(addy, state, status);
                    if (debug) System.err.println("Adding resource " + addy + ".\nResources: " + node.getNumResources());
                    fireChangedPresence(participantList, p, node, "online");
                }
                if (debug) System.err.println("Fired changed presence event.");
            }
        } else if ("unavailable".equals(type)) {
            if (room_contained) {
                PresenceUserNode node = (PresenceUserNode) participantList.get(room_address);
                if (node.getNumResources() <= 1) {
                    participantList.remove(room_address);
                    if (debug) System.err.println("Removing room " + room_address);
                } else {
                    if (debug) System.err.println("Removing resource " + full_address + ".\nResources: " + node.getNumResources());
                    node.removeResource(full_address);
                    if (debug) System.err.println("Removing resource " + full_address + ".\nResources: " + node.getNumResources());
                }
                fireChangedPresence(participantList, p, null, "offline");
            }
        } else {
            if (debug) System.err.println("Error presence packet recieved");
            fireError(p);
        }
    }

    public void MessageHandler(Message m) {
        if ("groupchat".equals(m.getType())) fireMessageReceived(m);
    }

    public ConnectionBean getConnBean() {
        return this.cb;
    }

    public Hashtable getParticipantList() {
        return this.participantList;
    }

    public boolean getDebug() {
        return this.debug;
    }

    public void setDebug(boolean b) {
        this.debug = b;
    }

    public String toString() {
        String temp = new String();
        Collection collection = participantList.values();
        Iterator iterator = collection.iterator();
        while (iterator.hasNext()) {
            PresenceUserNode node = (PresenceUserNode) iterator.next();
            temp += node.toString();
        }
        return temp;
    }

    public void fireChangedPresence(Hashtable t, Presence p, PresenceUserNode n, String s) {
        try {
            Vector broadcast = (Vector) chatListeners.clone();
            for (Enumeration e = broadcast.elements(); e.hasMoreElements(); ) ((GroupChatListener) e.nextElement()).ChangedPresence(t, p, n, s);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void fireError(Presence p) {
        try {
            Vector broadcast = (Vector) chatListeners.clone();
            for (Enumeration e = broadcast.elements(); e.hasMoreElements(); ) ((GroupChatListener) e.nextElement()).Error(p);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void fireMessageReceived(Message m) {
        try {
            Vector broadcast = (Vector) chatListeners.clone();
            for (Enumeration e = broadcast.elements(); e.hasMoreElements(); ) ((GroupChatListener) e.nextElement()).ReceivedMessage(m);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void addGroupChatListener(GroupChatListener l) {
        if (!chatListeners.contains(l)) chatListeners.addElement(l);
    }

    public synchronized void delGroupChatListener(GroupChatListener l) {
        if (chatListeners.contains(l)) chatListeners.removeElement(l);
    }
}
