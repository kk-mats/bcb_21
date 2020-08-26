package net.czechit.paradox.mail.server;

import net.czechit.paradox.mail.server.ParadoxEvent;
import net.czechit.paradox.mail.server.PMF;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import javax.jdo.PersistenceManager;
import com.google.appengine.api.xmpp.*;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MailHandlerServlet extends HttpServlet {

    /**
	 * 
	 */
    private static final long serialVersionUID = -3242780683575352937L;

    private static final Logger log = Logger.getLogger(MailHandlerServlet.class.getName());

    private static final String[] MONTHS = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };

    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);
        log.info("Begin doPost");
        MimeMessage message;
        String msg;
        try {
            message = new MimeMessage(session, req.getInputStream());
            parseMessage(message.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
            log.warning(e.toString());
            return;
        }
        ParadoxEvent p = new ParadoxEvent(new Date());
        PersistenceManager pm = PMF.get().getPersistenceManager();
        try {
            pm.makePersistent(p);
        } finally {
            pm.close();
        }
    }

    private void parseMessage(InputStream inputStream) throws IOException {
        Pattern timePattern = Pattern.compile("Time:\\s+((\\d+)\\s+(\\w+)\\s+(\\d+)\\s+(\\d+):(\\d+))");
        Pattern messagePattern = Pattern.compile("Message:\\s+(\\w+)");
        Pattern userPattern = Pattern.compile("By:\\s+(.*)");
        Date paradoxDate = null;
        String paradoxMessage = null;
        String paradoxUser = null;
        Matcher m;
        String line;
        BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
        while ((line = r.readLine()) != null) {
            log.info(line);
            m = timePattern.matcher(line);
            if (m.find()) {
                List<String> months = Arrays.asList(MONTHS);
                int month = months.indexOf(m.group(3));
                if (month >= 0) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.set(Integer.parseInt(m.group(4)), month, Integer.parseInt(m.group(2)), Integer.parseInt(m.group(5)), Integer.parseInt(m.group(6)), 0);
                    paradoxDate = calendar.getTime();
                }
            }
            m = messagePattern.matcher(line);
            if (m.find()) {
                paradoxMessage = m.group(1);
            }
            m = userPattern.matcher(line);
            if (m.find()) {
                paradoxUser = m.group(1);
            }
        }
        r.close();
        if (paradoxMessage != null && paradoxUser != null && paradoxDate != null) {
            sendJabber(String.format("%s: %s at %s", paradoxMessage, paradoxUser, paradoxDate));
        }
    }

    private void sendJabber(String msgBody) {
        log.info("Zacatek sendJabber");
        JID jid = new JID("tonda.kmoch@gmail.com");
        Message msg2 = new MessageBuilder().withRecipientJids(jid).withBody(msgBody).build();
        boolean messageSent = false;
        XMPPService xmpp = XMPPServiceFactory.getXMPPService();
        if (xmpp.getPresence(jid).isAvailable()) {
            SendResponse status = xmpp.sendMessage(msg2);
            messageSent = (status.getStatusMap().get(jid) == SendResponse.Status.SUCCESS);
        } else {
            log.info("Uzivatel neni dostupny");
        }
        if (messageSent) {
            log.info("Uspesne odeslano");
        }
    }
}
