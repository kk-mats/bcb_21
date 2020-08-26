package balmysundaycandy.more.low.level.logger.impl;

import balmysundaycandy.more.low.level.logger.ProtocolBufferLogger;
import balmysundaycandy.more.low.level.logger.ProtocolBufferLogContents;
import com.google.appengine.api.xmpp.JID;
import com.google.appengine.api.xmpp.Message;
import com.google.appengine.api.xmpp.MessageBuilder;
import com.google.appengine.api.xmpp.SendResponse;
import com.google.appengine.api.xmpp.XMPPService;
import com.google.appengine.api.xmpp.XMPPServiceFactory;

/**
 * logging by xmpp.
 * 
 * @author marblejenka
 *
 */
public class XmppLogger implements ProtocolBufferLogger {

    private static XMPPService xmpp = XMPPServiceFactory.getXMPPService();

    private String email;

    public XmppLogger(String email) {
        this.email = email;
    }

    public boolean log(String message) {
        String sendMessage = null;
        if (message == null) {
            sendMessage = "null";
        } else {
            sendMessage = message;
        }
        JID jid = new JID(email);
        Message msg = new MessageBuilder().withRecipientJids(jid).withBody(sendMessage).build();
        boolean messageSent = false;
        if (xmpp.getPresence(jid).isAvailable()) {
            SendResponse status = xmpp.sendMessage(msg);
            messageSent = (status.getStatusMap().get(jid) == SendResponse.Status.SUCCESS);
        }
        return messageSent;
    }

    public boolean log(ProtocolBufferLogContents logContents) {
        if (logContents == null) {
            throw new NullPointerException("logcontens is null.");
        }
        return log(logContents.toString());
    }
}
