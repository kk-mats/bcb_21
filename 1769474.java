package net.sipvip.server;

import java.io.IOException;
import java.util.logging.Logger;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.xmpp.JID;
import com.google.appengine.api.xmpp.Message;
import com.google.appengine.api.xmpp.MessageBuilder;
import com.google.appengine.api.xmpp.XMPPService;
import com.google.appengine.api.xmpp.XMPPServiceFactory;

@SuppressWarnings("serial")
public class XmpptestServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(XmpptestServlet.class.getName());

    private static final String LINE = "line";

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        XMPPService xmpp = XMPPServiceFactory.getXMPPService();
        Message msg = xmpp.parseMessage(req);
        JID jid = msg.getFromJid();
        String body = msg.getBody();
        LOG.info(jid.getId() + " --> JEliza: " + body);
        Key key = KeyFactory.createKey("chatData", ":" + jid.getId());
        Entity lastLineEntity = null;
        try {
            lastLineEntity = DatastoreServiceFactory.getDatastoreService().get(key);
        } catch (EntityNotFoundException e) {
            lastLineEntity = new Entity(key);
            lastLineEntity.setProperty(LINE, "");
        }
        final String lastLine = (String) lastLineEntity.getProperty(LINE);
        final StringBuilder response = new StringBuilder();
        final ElizaParse parser = new ElizaParse() {

            @Override
            public void PRINT(String s) {
                if (lastLine.trim().length() > 0 && s.startsWith("HI! I'M ELIZA")) {
                    return;
                }
                response.append(s);
                response.append('\n');
            }
        };
        parser.lastline = lastLine;
        parser.handleLine(body);
        body = response.toString();
        LOG.info(jid.getId() + " <-- JEliza: " + body);
        msg = new MessageBuilder().withRecipientJids(jid).withBody(body).build();
        xmpp.sendMessage(msg);
        if (parser.exit) {
            lastLineEntity.setProperty(LINE, "");
        } else {
            lastLineEntity.setProperty(LINE, parser.lastline);
        }
        DatastoreServiceFactory.getDatastoreService().put(lastLineEntity);
    }
}
