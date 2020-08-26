package net.sipvip.server;

import java.util.logging.Logger;
import net.sipvip.client.GreetingService;
import net.sipvip.shared.FieldVerifier;
import com.google.appengine.api.xmpp.JID;
import com.google.appengine.api.xmpp.MessageBuilder;
import com.google.appengine.api.xmpp.Message;
import com.google.appengine.api.xmpp.SendResponse;
import com.google.appengine.api.xmpp.XMPPService;
import com.google.appengine.api.xmpp.XMPPServiceFactory;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class GreetingServiceImpl extends RemoteServiceServlet implements GreetingService {

    private static final Logger LOG = Logger.getLogger(GreetingServiceImpl.class.getName());

    public String greetServer(String input) throws IllegalArgumentException {
        if (!FieldVerifier.isValidName(input)) {
            throw new IllegalArgumentException("Name must be at least 4 characters long");
        }
        LOG.info("Start GreetingServiceImpl");
        String serverInfo = getServletContext().getServerInfo();
        String userAgent = getThreadLocalRequest().getHeader("User-Agent");
        JID jid = new JID("wwwpillume@appspot.com");
        String msgBody = "Someone has sent you a gift";
        Message msg = new MessageBuilder().withRecipientJids(jid).withBody(msgBody).build();
        boolean messageSent = false;
        XMPPService xmpp = XMPPServiceFactory.getXMPPService();
        SendResponse status = xmpp.sendMessage(msg);
        messageSent = (status.getStatusMap().get(jid) == SendResponse.Status.SUCCESS);
        if (!messageSent) {
            LOG.info("CANT SEND MESSAGE");
        }
        input = escapeHtml(input);
        userAgent = escapeHtml(userAgent);
        return "Hello, " + input + "!<br><br>I am running " + serverInfo + ".<br><br>It looks like you are using:<br>" + userAgent;
    }

    /**
	 * Escape an html string. Escaping data received from the client helps to
	 * prevent cross-site script vulnerabilities.
	 * 
	 * @param html the html string to escape
	 * @return the escaped string
	 */
    private String escapeHtml(String html) {
        if (html == null) {
            return null;
        }
        return html.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
    }
}
