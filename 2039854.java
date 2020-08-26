package net.sipvip.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.xmpp.JID;
import com.google.appengine.api.xmpp.Message;
import com.google.appengine.api.xmpp.MessageBuilder;
import com.google.appengine.api.xmpp.SendResponse;
import com.google.appengine.api.xmpp.XMPPService;
import com.google.appengine.api.xmpp.XMPPServiceFactory;

public class LastLine extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(LastLine.class.getName());

    private static final String PHRASETYPE = "phrasetype";

    private static final String LINE = "line";

    public void process(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String text = req.getParameter("text");
        String clientuuid = req.getParameter("clientuuid");
        String UrlStr = req.getRequestURL().toString();
        URL domainurl = new URL(UrlStr);
        String domain = domainurl.getHost();
        String chatdomain = null;
        if (domain.equals("www.tussu.info")) {
            chatdomain = "wwwtussuinfo";
        } else if (domain.equals("www.verkossa.info")) {
            chatdomain = "wwwverkossainfo";
        }
        if (clientuuid != null && chatdomain != null) {
            String lastline = text;
            String phrasetype = null;
            Key key = KeyFactory.createKey("chatData", clientuuid + "@" + chatdomain + ".appspotchat.com/bot");
            Entity lastLineEntity = null;
            try {
                lastLineEntity = DatastoreServiceFactory.getDatastoreService().get(key);
                String msgBody;
                JID jid = new JID("wwwpillume@appspot.com");
                JID jidr = new JID(clientuuid + "@" + chatdomain + ".appspotchat.com");
                if (lastline == null) {
                    lastline = (String) lastLineEntity.getProperty(LINE);
                    phrasetype = (String) lastLineEntity.getProperty(PHRASETYPE);
                    if (phrasetype.equals("/askme")) {
                        msgBody = phrasetype;
                        Message msg = new MessageBuilder().withFromJid(jidr).withRecipientJids(jid).withBody(msgBody).build();
                        boolean messageSent = false;
                        XMPPService xmpp = XMPPServiceFactory.getXMPPService();
                        SendResponse status = xmpp.sendMessage(msg);
                        messageSent = (status.getStatusMap().get(jid) == SendResponse.Status.SUCCESS);
                        if (!messageSent) {
                            LOG.info("CANT SEND MESSAGE");
                        }
                    }
                } else {
                    phrasetype = (String) lastLineEntity.getProperty(PHRASETYPE);
                    lastLineEntity.setProperty(LINE, text);
                    if (phrasetype.equals("/tellme")) {
                        msgBody = phrasetype + " " + text;
                        ;
                    } else {
                        msgBody = text;
                    }
                    Message msg = new MessageBuilder().withFromJid(jidr).withRecipientJids(jid).withBody(msgBody).build();
                    boolean messageSent = false;
                    XMPPService xmpp = XMPPServiceFactory.getXMPPService();
                    SendResponse status = xmpp.sendMessage(msg);
                    messageSent = (status.getStatusMap().get(jid) == SendResponse.Status.SUCCESS);
                    if (!messageSent) {
                        LOG.info("CANT SEND MESSAGE");
                    }
                }
            } catch (EntityNotFoundException e) {
                LOG.info("CANT FIND ENTIty!!");
                lastLineEntity = new Entity(key);
                lastLineEntity.setProperty(LINE, "Moi!");
                lastLineEntity.setProperty(PHRASETYPE, "/askme");
                DatastoreServiceFactory.getDatastoreService().put(lastLineEntity);
            }
            LOG.info("LastLine: " + lastline);
            PrintWriter out = resp.getWriter();
            out.print("{");
            out.print("\"line\": \"");
            out.print(lastline);
            out.print("\",");
            out.print("\"phrasetype\": \"");
            out.print(phrasetype);
            out.print("\"}");
            out.flush();
        } else {
            LOG.severe(" no clientuuid!!");
        }
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        process(req, resp);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        process(req, resp);
    }
}
