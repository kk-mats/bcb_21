package uk.ac.lkl.server;

import java.io.IOException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.google.appengine.api.xmpp.Message;
import com.google.appengine.api.xmpp.MessageBuilder;
import com.google.appengine.api.xmpp.MessageType;
import com.google.appengine.api.xmpp.XMPPService;
import com.google.appengine.api.xmpp.XMPPServiceFactory;

public class XMPPServlet extends HttpServlet {

    private static final XMPPService xmppService = XMPPServiceFactory.getXMPPService();

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Message message = xmppService.parseMessage(request);
        ServerUtils.warn(message.getBody());
        replyToMessage(message, "Hello");
    }

    private void replyToMessage(Message message, String body) {
        Message reply = new MessageBuilder().withRecipientJids(message.getFromJid()).withMessageType(MessageType.NORMAL).withBody(body).build();
        xmppService.sendMessage(reply);
    }
}
