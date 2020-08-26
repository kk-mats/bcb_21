package de.knup.jedi.jayshare;

import java.net.*;
import java.io.*;
import java.util.*;
import org.w3c.tools.crypt.*;
import org.jabber.jabberbeans.*;
import org.jabber.jabberbeans.util.*;
import org.jabber.jabberbeans.Extension.*;
import de.knup.jedi.jayshare.Fileshare.*;

/**
 * Handles a connection to a conference room
 *
 * @author <a href="mailto:berni@knup.de">B. Pietsch</a>
 * @version 1.0 $Revision: 1.26 $
 */
public class Connection {

    /**
     * Reference to the Status instance.
     */
    protected Status status;

    /**
     * JabberBeans connection bean to control connection to the server.
     */
    protected ConnectionBean connBean;

    /**
     * FileshareListener listens to incoming packets and extracts usable input.
     */
    protected FileshareListener listener;

    /**
     * Provides background communication intelligence.
     * It automatically answers to fileshare requests, sends list modification timestamps to new users
     * and updates file lists with incoming new ones.
     */
    protected CommunicationThread comm;

    /**
     * JID of the conference. This is something like "jayshare@conference.jabber.org".
     */
    protected JID confJID;

    /**
     * Create a connection instance. This does <i>not</i> connect to the server. Use the connect () method.
     * @param user User name
     * @param nick Nick name
     * @param password Jabber password
     * @param chatroom jayShare conference room (e.g. "jayshare")
     * @param jabberServer Jabber server to connect to (e.g. "jabber.org")
     * @param confServer Conference server which provides the group chat (e.g. "conferene.jabber.org")
     * @param port Jabber server port (usually 5222)
     */
    public Connection(Status status) {
        this.status = status;
        status.setConnection(this);
        confJID = new JID(status.getChatroom(), status.getConferenceServer(), null);
        connBean = new ConnectionBean();
    }

    /**
     * Add another PacketListener (e.g. for debugging, for integration of a chat client)
     */
    public void addPacketListener(PacketListener listener) {
        connBean.addPacketListener(listener);
    }

    /**
     * Add a ConnectionListener (e.g. for debugging)
     */
    public void addConnectionListener(ConnectionListener listener) {
        connBean.addConnectionListener(listener);
    }

    /**
     * Create a ListModificationBuilder to send a packet.
     * @param date Time of the last change of the export list.
     */
    public FileshareComponentBuilder makeListModification(MDate date) {
        ListModificationBuilder builder = new ListModificationBuilder();
        builder.setDate(date);
        return builder;
    }

    /**
     * Create a CapabilityBuilder to send a packet.
     * @param versions is a Vector of Version objects that define the clients version capabilities.
     * @param langs is a space-seperated String that defines the supported languages.
     * @param xtags is a Vector of XTag objects that contain additional capabilities (e.g. '<x-tag key="needs_push" value="true"').
     */
    public FileshareComponentBuilder makeCapability(Vector versions, String langs, Vector xtags) {
        CapabilityBuilder builder = new CapabilityBuilder();
        builder.setVersions(versions);
        builder.setLanguages(langs);
        builder.setXTags(xtags);
        return builder;
    }

    /**
     * Create a ListRequestBuilder to send a packet.
     */
    public FileshareComponentBuilder makeListRequest() {
        ListRequestBuilder builder = new ListRequestBuilder();
        return builder;
    }

    /**
     * Create a ListreplyBuilder to send a packet.
     * @param myList is the export list.
     */
    public FileshareComponentBuilder makeListReply(FileList myList) {
        ListReplyBuilder builder = new ListReplyBuilder();
        builder.setFileList(myList);
        return builder;
    }

    /**
     * Create a ServiceRequestBuilder to send a packet.
     */
    public FileshareComponentBuilder makeServiceRequest() {
        ServiceRequestBuilder builder = new ServiceRequestBuilder();
        return builder;
    }

    /**
     * Create a ServiceReplyBuilder to send a packet.
     * @param proto defines the Service protocol (usually "http").
     * @param host defines the DNS server name or IP address of the download server.
     * @param port defines the download server port.
     */
    public FileshareComponentBuilder makeServiceReply(String proto, String host, int port) {
        ServiceReplyBuilder builder = new ServiceReplyBuilder();
        builder.setProtocol(proto);
        builder.setHost(host);
        builder.setPort(port);
        return builder;
    }

    /**
     * Sends a jayShare packet.
     * @param toAddress is the packets target (either a single user in a conference).
     * @param builder is the packet builder (@see makeListModification (), makeCapability (), makeListRequest (), makeListReply (), makeServiceRequest (), makeServiceReply ()).
     */
    public void sendMessage(JID toAddress, FileshareComponentBuilder builder) {
        MessageBuilder mb = new MessageBuilder();
        mb.setToAddress(toAddress);
        mb.setType("chat");
        builder.setHashedID(status.getHashedID());
        try {
            mb.addExtension(builder.build());
            connBean.send(mb.build());
        } catch (InstantiationException e) {
            System.err.println("*EE* Could not send message: " + e);
            return;
        }
    }

    /**
     * Sends a jayShare packet.
     * @param builder is the packet builder (@see makeListModification ()).
     */
    public void broadcastMessage(FileshareComponentBuilder builder) {
        MessageBuilder mb = new MessageBuilder();
        mb.setToAddress(confJID);
        mb.setType("groupchat");
        builder.setHashedID(status.getHashedID());
        try {
            mb.addExtension(builder.build());
            connBean.send(mb.build());
        } catch (InstantiationException e) {
            System.err.println("*EE* Could not send message: " + e);
            return;
        }
    }

    /**
    * Broadcasts a list modification to the whole conference.
    * This should be done when the export list has changed and on the entrance to the jayShare room.
    */
    public void broadcastListModification(MDate date) {
        broadcastMessage(makeListModification(date));
    }

    /**
     * Hum, we have to leave now.
     */
    public void disconnect() {
        connBean.disconnect();
    }

    /**
     * Connect to the server and log in.
     * @exception UnknownHostException, IOException, NoUserListException
     */
    public synchronized void connect() throws UnknownHostException, IOException, NoUserListException {
        if (status.getUserList() == null) throw new NoUserListException();
        comm = new CommunicationThread(this, status);
        listener = new FileshareListener(comm, status);
        connBean.addPacketListener(listener);
        connBean.connect(InetAddress.getByName(status.getJabberServer()), status.getJabberPort());
        InfoQueryBuilder iqb = new InfoQueryBuilder();
        iqb.setType("set");
        IQAuthBuilder iab = new IQAuthBuilder();
        iab.setUsername(status.getUsername());
        iab.setPassword(status.getPassword());
        iab.setResource("jayShare");
        try {
            iqb.addExtension(iab.build());
            connBean.send((InfoQuery) iqb.build());
        } catch (InstantiationException e) {
            System.err.println("*EE* Authentication failed: " + e);
            return;
        }
        PresenceBuilder pb = new PresenceBuilder();
        try {
            connBean.send(pb.build());
            pb.setToAddress(new JID(status.getChatroom(), status.getConferenceServer(), status.getNickname()));
            pb.setStatus(status.getHashedID());
            connBean.send(pb.build());
        } catch (InstantiationException e) {
            System.err.println("*EE* Could not send presence: " + e);
            return;
        }
    }
}
