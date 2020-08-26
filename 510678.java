package com.app.servlet;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.google.appengine.api.xmpp.JID;
import com.google.appengine.api.xmpp.Message;
import com.google.appengine.api.xmpp.MessageBuilder;
import com.google.appengine.api.xmpp.SendResponse;
import com.google.appengine.api.xmpp.XMPPService;
import com.google.appengine.api.xmpp.XMPPServiceFactory;

@SuppressWarnings("serial")
public class XMPPAgentServlet extends HttpServlet {

    public static final Logger log = Logger.getLogger(XMPPAgentServlet.class.getName());

    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String strStatus = "";
            XMPPService xmpp = XMPPServiceFactory.getXMPPService();
            Message msg = xmpp.parseMessage(req);
            JID fromJid = msg.getFromJid();
            String body = msg.getBody();
            log.info("Received a message from " + fromJid + " and body = " + body);
            String msgBody = "You sent me : " + body;
            Message replyMessage = new MessageBuilder().withRecipientJids(fromJid).withBody(msgBody).build();
            boolean messageSent = false;
            if (xmpp.getPresence(fromJid).isAvailable()) {
                SendResponse status = xmpp.sendMessage(replyMessage);
                messageSent = (status.getStatusMap().get(fromJid) == SendResponse.Status.SUCCESS);
            }
            if (messageSent) {
                strStatus = "Message has been sent successfully";
            } else {
                strStatus = "Message could not be sent";
            }
            log.info(strStatus);
        } catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage());
        }
    }
}
