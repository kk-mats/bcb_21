package com.site;

import java.io.IOException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.google.appengine.api.xmpp.JID;
import com.google.appengine.api.xmpp.Message;
import com.google.appengine.api.xmpp.MessageBuilder;
import com.google.appengine.api.xmpp.XMPPService;
import com.google.appengine.api.xmpp.XMPPServiceFactory;

@SuppressWarnings("serial")
public class CaseSiteServlet extends HttpServlet {

    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        XMPPService xmppService = XMPPServiceFactory.getXMPPService();
        Message message = xmppService.parseMessage(req);
        JID fromId = message.getFromJid();
        xmppService.sendMessage(new MessageBuilder().withBody("Your reply: " + message.getBody()).withRecipientJids(fromId).build());
    }
}
