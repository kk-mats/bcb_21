package com.simconomy.magic.servlet;

import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.google.appengine.api.xmpp.JID;
import com.google.appengine.api.xmpp.Message;
import com.google.appengine.api.xmpp.MessageBuilder;
import com.google.appengine.api.xmpp.SendResponse;
import com.google.appengine.api.xmpp.XMPPService;
import com.google.appengine.api.xmpp.XMPPServiceFactory;
import com.simconomy.magic.exceptions.CardPriceException;
import com.simconomy.magic.model.Card;
import com.simconomy.magic.model.CardDetail;
import com.simconomy.magic.service.CardPriceService;

@SuppressWarnings("serial")
public class XMPPReceiverServlet extends HttpServlet {

    private static final String HELP = "help";

    private static final String CARD = "card";

    private static final String TOP = "top";

    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        XMPPService xmpp = XMPPServiceFactory.getXMPPService();
        Message message = xmpp.parseMessage(req);
        JID fromJid = message.getFromJid();
        String body = message.getBody();
        if (body.toLowerCase().equals(HELP)) {
            String msgBody = "usage guide:\n\r" + TOP + " [Number] - return top [Number] cards (higest price)\n\r" + CARD + " [Card Name] - return current value of this card\n\r";
            send(fromJid.getId(), msgBody);
        } else if (body.toLowerCase().startsWith(CARD)) {
            String cardName = body.substring(CARD.length() + 1).trim();
            CardPriceService cardPriceService = CardPriceService.Util.getInstance();
            String msgBody = null;
            try {
                List<CardDetail> cards = cardPriceService.retrieveCards(cardName);
                StringBuffer sb = new StringBuffer();
                sb.append("Found " + cards.size() + " result(s)\n\r");
                for (CardDetail cardDetail : cards) {
                    sb.append("price: " + cardDetail.getPrice() + " serie: " + cardDetail.getSerie() + " status: " + cardDetail.getStatus() + "\n");
                }
                msgBody = sb.toString();
                if (cards.isEmpty()) {
                    msgBody = "Card '" + cardName + "' was not found";
                }
            } catch (Exception e) {
                msgBody = "Oeps something went wrong when retrieving card '" + cardName + "'";
            }
            send(fromJid.getId(), msgBody);
        } else if (body.toLowerCase().startsWith(TOP)) {
            CardPriceService cardPriceService = CardPriceService.Util.getInstance();
            String cardName = body.substring(TOP.length() + 1).trim();
            String msgBody = null;
            try {
                int limit = Integer.parseInt(cardName);
                List<Card> cards = cardPriceService.retrieveTopCards(limit);
                StringBuffer sb = new StringBuffer();
                sb.append("Top " + cardName + ":\n\r");
                for (Card cardDetail : cards) {
                    sb.append("price: " + cardDetail.getPrice() + " name: " + cardDetail.getName() + " status: " + cardDetail.getStatus() + "\n");
                }
                msgBody = sb.toString();
                if (cards.isEmpty()) {
                    msgBody = "No results found";
                }
            } catch (Exception e) {
                msgBody = "Oeps something went wrong when retrieving card '" + cardName + "'";
            }
            send(fromJid.getId(), msgBody);
        } else {
            String msgBody = "Could not understand message '" + body + "', send '" + HELP + "' for a usage guide";
            send(fromJid.getId(), msgBody);
        }
    }

    private void send(String fromJid, String msgBody) {
        XMPPService xmpp = XMPPServiceFactory.getXMPPService();
        JID jid = new JID(fromJid);
        Message msg = new MessageBuilder().withRecipientJids(jid).withBody(msgBody).build();
        if (xmpp.getPresence(jid).isAvailable()) {
            SendResponse status = xmpp.sendMessage(msg);
            boolean messageSent = (status.getStatusMap().get(jid) == SendResponse.Status.SUCCESS);
            if (!messageSent) {
                status = xmpp.sendMessage(msg);
            }
        }
    }
}
