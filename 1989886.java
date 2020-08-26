package com.google.controller;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import javax.jdo.PersistenceManager;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import com.google.model.PMF;
import com.google.model.Airplane;
import javax.jdo.Query;
import com.google.appengine.api.xmpp.JID;
import com.google.appengine.api.xmpp.Message;
import com.google.appengine.api.xmpp.MessageBuilder;
import com.google.appengine.api.xmpp.SendResponse;
import com.google.appengine.api.xmpp.XMPPService;
import com.google.appengine.api.xmpp.XMPPServiceFactory;

@SuppressWarnings("serial")
public class XMPPReceiverServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        XMPPService xmpp = XMPPServiceFactory.getXMPPService();
        Message message = xmpp.parseMessage(req);
        JID fromJid = message.getFromJid();
        String body = message.getBody();
        String respMsg = null;
        if (body.equals("/list")) {
            respMsg = getPlaneList();
        } else if (body.equals("/help")) {
            respMsg = "Welcome to the Plane-Crazy Chatbot!\nThe following commands are supported: \n /list \n /help";
        } else {
            respMsg = "Command '" + body + "' not supported! \nEnter '/help' for list of commands.";
        }
        JID tojid = new JID(fromJid.getId());
        Message msg = new MessageBuilder().withRecipientJids(tojid).withBody(respMsg).build();
        boolean messageSent = false;
        xmpp = XMPPServiceFactory.getXMPPService();
        if (xmpp.getPresence(tojid).isAvailable()) {
            SendResponse status = xmpp.sendMessage(msg);
            messageSent = (status.getStatusMap().get(tojid) == SendResponse.Status.SUCCESS);
        }
    }

    public String getPlaneList() {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        String query = "";
        query = "select from " + Airplane.class.getName();
        List<Airplane> airplanes = (List<Airplane>) pm.newQuery(query).execute();
        String planelist = "Plane-Crazy plane list:";
        for (Airplane a : airplanes) {
            planelist += "\n" + a.getName() + "\n";
            planelist += "http://plane-crazy.appspot.com/app/detail.jsp?planeid=" + a.getCatalogId() + "\n";
        }
        planelist += "\n\nNote: you must be logged in at http://plane-crazy.appspot.com to click on a plane link. ";
        pm.close();
        return planelist;
    }
}
