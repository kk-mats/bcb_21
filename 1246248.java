package com.google.appengine.testing.cloudcover.spi;

import com.google.appengine.api.xmpp.JID;
import com.google.appengine.api.xmpp.Message;
import com.google.appengine.api.xmpp.MessageBuilder;
import com.google.appengine.api.xmpp.SendResponse;
import com.google.appengine.api.xmpp.XMPPService;
import com.google.appengine.api.xmpp.XMPPServiceFactory;
import java.util.logging.Logger;

/**
 * {@link TestRunListener} that sends a message via XMPP when the
 * {@link TestRun} is finished.
 *
 * @author Max Ross <max.ross@gmail.com>
 */
public class XMPPTestRunListener implements TestRunListener {

    private final Logger logger = Logger.getLogger(getClass().getName());

    private final JID jid;

    public XMPPTestRunListener(JID jid) {
        this.jid = jid;
    }

    public void onTestRunCompletion(String statusURL, long runId) {
        String msgBody = buildMessageBody(statusURL, runId);
        Message msg = new MessageBuilder().withRecipientJids(jid).withBody(msgBody).build();
        XMPPService xmpp = XMPPServiceFactory.getXMPPService();
        if (xmpp.getPresence(jid).isAvailable()) {
            SendResponse status = xmpp.sendMessage(msg);
            SendResponse.Status sendStatus = status.getStatusMap().get(jid);
            logger.fine("xmpp send to " + jid + " was successful.");
            if (sendStatus != SendResponse.Status.SUCCESS) {
                logger.warning("xmpp send to " + jid + " failed with status " + sendStatus);
            }
        } else {
            logger.warning("Could not send xmpp notification to " + jid + " because user is not available.");
        }
    }

    /**
   * Subclass and override if you want to customize the content of the message. 
   */
    protected String buildMessageBody(String statusURL, long runId) {
        return String.format("Test run %d is complete.  Please visit %s to see your results.", runId, statusURL);
    }
}
