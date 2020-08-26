package com.simconomy.magic.service;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import com.google.appengine.api.xmpp.JID;
import com.google.appengine.api.xmpp.Message;
import com.google.appengine.api.xmpp.MessageBuilder;
import com.google.appengine.api.xmpp.SendResponse;
import com.google.appengine.api.xmpp.XMPPService;
import com.google.appengine.api.xmpp.XMPPServiceFactory;
import com.simconomy.magic.exceptions.CardPriceException;
import com.simconomy.magic.jpa.Card;
import com.simconomy.magic.jpa.Decode;
import com.simconomy.magic.jpa.HistoricData;
import com.simconomy.magic.jpa.PMF;
import com.simconomy.magic.jpa.Serie;
import com.simconomy.magic.jpa.Status;
import com.simconomy.magic.model.Price;
import com.simconomy.magic.script.client.DayDate;
import com.simconomy.magic.util.RateOfChangeUtil;
import com.simconomy.magic.util.DateUtils;

public class CardPriceServiceImpl implements CardPriceService {

    private static final String ADMIN_JID = "bakker.mark@gmail.com";

    private static Logger log = Logger.getLogger(CardPriceServiceImpl.class.getName());

    private final InsertCard insertCardSQL = new InsertCard();

    private final InsertHistoricData insertHistoricDataSQL = new InsertHistoricData();

    private final SelectCard selectCardSQL = new SelectCard();

    private final SelectHistories selectHistoriesSQL = new SelectHistories();

    private final SelectDecode selectDecodeSQL = new SelectDecode();

    private final UpdateHistoricData updateHistoricData = new UpdateHistoricData();

    private final SelectCards selectCardsSQL = new SelectCards();

    private final UpdateDecode updateDecode = new UpdateDecode();

    private final UpdateCard updateCardSQL = new UpdateCard();

    public void addCardPrice(Price price) throws CardPriceException {
        Card card = selectCardSQL.exist(price.getName(), price.getSerie());
        if (card == null) {
            try {
                insertCardSQL.insert(price.getName(), price.getSerie());
                card = selectCardSQL.exist(price.getName(), price.getSerie());
                if (card == null) {
                    return;
                }
            } catch (CardPriceException e) {
                return;
            }
        }
        List<HistoricData> historicDatas = selectHistoriesSQL.retrieveHistoricData(card, 1);
        HistoricData historicData = null;
        if (!historicDatas.isEmpty()) {
            historicData = historicDatas.get(0);
            if (historicData.getRegularPrice() == 0) {
                Decode decodePrevious = selectDecodeSQL.select(historicData.getCodedPrice());
                if (decodePrevious.getValue() == 0) {
                    historicData.setRegularPrice(decodePrevious.getValue());
                    updateHistoricData.update(historicData);
                }
            }
            if (DateUtils.sameDay(historicData.getSampleDate(), price.getDate())) {
                updateCardSQL.updatePrice(card);
                return;
            }
        }
        Decode decode = selectDecodeSQL.select(price.getCodedPrice());
        if (decode.getValue() == 0) {
            sendMessage(decode.getToken());
            if (historicDatas.isEmpty() || !historicData.getCodedPrice().equals(price.getCodedPrice())) {
                try {
                    insertHistoricDataSQL.insert(card, 0, price.getDate(), price.getCodedPrice());
                } catch (CardPriceException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            double regularPrice = decode.getValue();
            if (historicDatas.isEmpty() || historicData.getRegularPrice() != regularPrice) {
                try {
                    card.setPrice(regularPrice);
                    insertHistoricDataSQL.insert(card, regularPrice, price.getDate(), price.getCodedPrice());
                } catch (CardPriceException e) {
                    throw new RuntimeException(e);
                }
            } else {
                card.setPrice(regularPrice);
                updateCardSQL.updatePrice(card);
            }
        }
    }

    private void sendMessage(String token) {
        XMPPService xmpp = XMPPServiceFactory.getXMPPService();
        JID jid = new JID(ADMIN_JID);
        String msgBody = "Recieved an unkown price code please enter a value for this code \n\r" + "http://twitterinmail.appspot.com/decode?id=" + token;
        Message msg = new MessageBuilder().withRecipientJids(jid).withBody(msgBody).build();
        boolean messageSent = false;
        if (xmpp.getPresence(jid).isAvailable()) {
            SendResponse status = xmpp.sendMessage(msg);
            messageSent = (status.getStatusMap().get(jid) == SendResponse.Status.SUCCESS);
        }
        if (!messageSent) {
            Properties props = new Properties();
            Session session = Session.getDefaultInstance(props, null);
            try {
                javax.mail.Message email = new MimeMessage(session);
                email.setFrom(new InternetAddress("admin@twitterinmail.appspot.com", "Magic Admin"));
                email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(ADMIN_JID, "Mark Bakker"));
                email.setSubject("Recieved an unkown price code please enter a value for this code");
                email.setText(msgBody);
                Transport.send(email);
            } catch (AddressException e) {
                return;
            } catch (MessagingException e) {
                return;
            } catch (UnsupportedEncodingException e) {
                return;
            }
        }
    }

    public List<Double> retrieveLastCardPrices(String cardName, Serie serie, int days, boolean weekends) {
        List<Double> result = new ArrayList<Double>();
        Date now = new Date();
        Card card = selectCardSQL.exist(cardName, serie);
        if (card != null) {
            List<HistoricData> historicDatas = selectHistoriesSQL.retrieveHistoricData(card, days);
            Date current = now;
            for (int i = 0; i < days; i++) {
                Double value = null;
                for (HistoricData historicData : historicDatas) {
                    if (DateUtils.sameDay(historicData.getSampleDate(), current)) {
                        value = historicData.getRegularPrice();
                        break;
                    } else if (historicData.getSampleDate().before(current)) {
                        value = historicData.getRegularPrice();
                        break;
                    }
                }
                if (value == null) {
                    return result;
                }
                if (weekends) {
                    if (!DateUtils.isWeekend(current)) {
                        result.add(value);
                    }
                } else {
                    result.add(value);
                }
                current = DateUtils.yesterday(current);
            }
        }
        return result;
    }

    public List<com.simconomy.magic.model.Card> retrieveCards(Serie serie, int offset, int limit) throws CardPriceException {
        List<com.simconomy.magic.model.Card> list = new ArrayList<com.simconomy.magic.model.Card>();
        List<Card> cards = selectCardsSQL.exist(serie, offset, limit);
        toCards(list, cards);
        return list;
    }

    public List<com.simconomy.magic.model.Card> retrieveTopCards(int limit) throws CardPriceException {
        List<com.simconomy.magic.model.Card> list = new ArrayList<com.simconomy.magic.model.Card>();
        List<Card> cards = selectCardsSQL.topCards(limit);
        toCards(list, cards);
        return list;
    }

    public List<com.simconomy.magic.model.CardDetail> retrieveCards(String cardName) throws CardPriceException {
        List<com.simconomy.magic.model.CardDetail> list = new ArrayList<com.simconomy.magic.model.CardDetail>();
        List<Card> cards = selectCardsSQL.select(cardName);
        for (Card card : cards) {
            com.simconomy.magic.model.CardDetail item = new com.simconomy.magic.model.CardDetail();
            item.setName(card.getName());
            item.setRarity(card.getRarity());
            item.setSerie(card.getSerie());
            item.setStatus(card.getStatus());
            item.setPrice(card.getPrice());
            item.setRoc(card.getRoc());
            List<HistoricData> historicDatas = selectHistoriesSQL.retrieveHistoricData(card, 0);
            if (!historicDatas.isEmpty()) {
                item.setPrice(historicDatas.get(0).getRegularPrice());
            }
            for (HistoricData historicData : historicDatas) {
                com.simconomy.magic.model.HistoricData data = new com.simconomy.magic.model.HistoricData();
                data.setDate(historicData.getSampleDate());
                data.setValue(historicData.getRegularPrice());
                item.getData().add(data);
            }
            list.add(item);
        }
        return list;
    }

    public com.simconomy.magic.controller.command.Decode retriveDecode(String id) throws CardPriceException {
        com.simconomy.magic.controller.command.Decode result = new com.simconomy.magic.controller.command.Decode();
        Decode decode = selectDecodeSQL.select(id);
        result.setId(id);
        result.setValue(decode.getValue());
        return result;
    }

    public void updateDecode(com.simconomy.magic.controller.command.Decode decode) throws CardPriceException {
        Decode update = selectDecodeSQL.select(decode.getId());
        update.setValue(decode.getValue());
        updateDecode.update(update);
        List<HistoricData> data = selectHistoriesSQL.retrieveHistoricData(decode.getId());
        for (HistoricData historicData : data) {
            if (historicData.getRegularPrice() == 0) {
                historicData.setRegularPrice(decode.getValue());
                updateHistoricData.update(historicData);
                Card card = selectCardSQL.selectCard(historicData);
                if (card == null) {
                    continue;
                }
                if (card.getPrice() == null || card.getPrice() == 0) {
                    List<HistoricData> list = selectHistoriesSQL.retrieveHistoricData(card, 1);
                    card.setPrice(list.get(0).getRegularPrice());
                    updateCardSQL.updatePrice(card);
                }
            }
        }
    }

    private void toCards(List<com.simconomy.magic.model.Card> list, List<Card> cards) {
        for (Card card : cards) {
            com.simconomy.magic.model.Card item = new com.simconomy.magic.model.Card();
            item.setName(card.getName());
            item.setRarity(card.getRarity());
            item.setSerie(card.getSerie());
            item.setStatus(card.getStatus());
            HistoricData historicData = selectHistoriesSQL.retrieveHistoricData(card, 1).get(0);
            if (historicData != null) {
                item.setLastUpdate(historicData.getSampleDate());
                item.setPrice(historicData.getRegularPrice());
            }
            list.add(item);
        }
    }

    protected double calcFullROC(List<HistoricData> historicData, Date date) {
        log.info("historicData size = '" + historicData.size() + "'");
        Map<String, DayDate> map = new HashMap<String, DayDate>();
        Double latestPrice = 0.0;
        Date latestDate = null;
        for (HistoricData data : historicData) {
            Date currentDate = data.getSampleDate();
            Double currentPrice = data.getRegularPrice();
            if (latestDate == null) {
                latestDate = currentDate;
                latestPrice = currentPrice;
            }
            map.put(RateOfChangeUtil.convertDate(currentDate), new DayDate(currentPrice));
            if (latestDate.before(currentDate)) {
                latestDate = currentDate;
                latestPrice = currentPrice;
            }
        }
        map.put(RateOfChangeUtil.convertDate(date), new DayDate(latestPrice));
        RateOfChangeUtil.fillGaps(map);
        RateOfChangeUtil.calcRoc(map);
        DayDate dayDate = map.get(RateOfChangeUtil.convertDate(date));
        return dayDate.getRoc();
    }

    class SelectCards {

        synchronized List<Card> exist(Serie serie, int offset, int limit) {
            PersistenceManager pm = PMF.get().getPersistenceManager();
            Query query = pm.newQuery(Card.class);
            query.setFilter("serie == serieName");
            query.declareParameters("String serieName");
            query.setRange(offset, offset + limit);
            query.setOrdering("name asc");
            List<Card> results = (List<Card>) query.executeWithArray(serie);
            return results;
        }

        synchronized List<Card> topCards(int limit) {
            PersistenceManager pm = PMF.get().getPersistenceManager();
            Query query = pm.newQuery(Card.class);
            query.setRange(0, 0 + limit);
            query.setOrdering("price desc");
            List<Card> results = (List<Card>) query.execute();
            return results;
        }

        synchronized List<Card> select(String cardName) {
            PersistenceManager pm = PMF.get().getPersistenceManager();
            Query query = pm.newQuery(Card.class);
            query.setFilter("name == cardName");
            query.declareParameters("String cardName");
            List<Card> results = (List<Card>) query.executeWithArray(cardName);
            return results;
        }
    }

    class SelectCard {

        @SuppressWarnings("unchecked")
        synchronized Card exist(final String cardName, final Serie serie) {
            PersistenceManager pm = PMF.get().getPersistenceManager();
            Query query = pm.newQuery(Card.class);
            query.setFilter("name == cardName && serie == serieName");
            query.declareParameters("String cardName, String serieName");
            List<Card> results = (List<Card>) query.executeWithArray(cardName, serie);
            if (results.size() != 1) {
                log.severe("Found cardName = '" + cardName + "' serie = '" + serie + "' '" + results.size() + "' times expected to see only once");
                for (Card result : results) {
                    if (result.getPrice() == null) {
                        pm.deletePersistent(result);
                    }
                }
                return null;
            }
            return results.get(0);
        }

        synchronized Card selectCard(HistoricData historicData) {
            PersistenceManager pm = PMF.get().getPersistenceManager();
            HistoricData current = pm.getObjectById(HistoricData.class, historicData.getId());
            Card card = current.getCard();
            return card;
        }
    }

    class SelectDecode {

        @SuppressWarnings("unchecked")
        synchronized Decode select(String code) throws CardPriceException {
            PersistenceManager pm = PMF.get().getPersistenceManager();
            Decode decode = null;
            try {
                decode = pm.getObjectById(Decode.class, code);
                return decode;
            } catch (JDOObjectNotFoundException e) {
                try {
                    decode = new Decode();
                    decode.setToken(code);
                    decode.setValue(0);
                    pm.makePersistent(decode);
                } catch (Exception e1) {
                    String message = "Oeps something wrong";
                    log.severe(message);
                    throw new CardPriceException(message, e1);
                } finally {
                    pm.close();
                }
                return select(code);
            }
        }
    }

    class SelectHistories {

        @SuppressWarnings("unchecked")
        synchronized List<HistoricData> retrieveHistoricData(final String code) {
            PersistenceManager pm = PMF.get().getPersistenceManager();
            Query query = pm.newQuery(HistoricData.class);
            query.setFilter("codedPrice == code");
            query.declareParameters("String code");
            List<HistoricData> results = (List<HistoricData>) query.executeWithArray(code);
            return results;
        }

        synchronized List<HistoricData> retrieveHistoricData(final Card card, final int days) {
            if (card == null) {
                return null;
            }
            PersistenceManager pm = PMF.get().getPersistenceManager();
            Query query = pm.newQuery(Card.class);
            query.setFilter("name == cardName && serie == serieName");
            query.declareParameters("String cardName, String serieName");
            List<Card> results = (List<Card>) query.executeWithArray(card.getName(), card.getSerie());
            if (results.size() != 1) {
                return null;
            }
            Card card2 = results.get(0);
            List<HistoricData> data = new ArrayList<HistoricData>();
            List<HistoricData> temp = card2.getHistoricData();
            int max = days;
            if (max == 0) {
                max = temp.size();
            }
            if (temp == null) {
                return data;
            }
            int i = 0;
            for (HistoricData historicData : temp) {
                if (i >= max) {
                    break;
                }
                data.add(historicData);
            }
            return data;
        }
    }

    class UpdateHistoricData {

        synchronized void update(final HistoricData historicData) throws CardPriceException {
            PersistenceManager pm = PMF.get().getPersistenceManager();
            try {
                HistoricData current = pm.getObjectById(HistoricData.class, historicData.getId());
                current.setRegularPrice(historicData.getRegularPrice());
                current.setCard(historicData.getCard());
                current.setCodedPrice(historicData.getCodedPrice());
                current.setSampleDate(historicData.getSampleDate());
                pm.makePersistent(current);
            } catch (Exception e) {
                String message = "Oeps something wrong";
                log.severe(message);
                throw new CardPriceException(message, e);
            } finally {
                pm.close();
            }
        }
    }

    class UpdateDecode {

        synchronized void update(final Decode decode) throws CardPriceException {
            PersistenceManager pm = PMF.get().getPersistenceManager();
            try {
                Decode current = pm.getObjectById(Decode.class, decode.getToken());
                current.setValue(decode.getValue());
                pm.makePersistent(current);
            } catch (Exception e) {
                String message = "Oeps something wrong";
                log.severe(message);
                throw new CardPriceException(message, e);
            } finally {
                pm.close();
            }
        }
    }

    class UpdateCard {

        synchronized void updatePrice(final Card card) throws CardPriceException {
            PersistenceManager pm = PMF.get().getPersistenceManager();
            try {
                Card current = pm.getObjectById(Card.class, card.getId());
                current.setPrice(card.getPrice());
                Double oldRoc = current.getRoc();
                double roc = calcFullROC(card.getHistoricData(), new Date());
                if (oldRoc == null) {
                    card.setStatus(Status.NONE);
                } else if (roc > oldRoc && roc > 0) {
                    card.setStatus(Status.BUY);
                } else if (roc < oldRoc && roc < 0) {
                    card.setStatus(Status.SELL);
                } else {
                    card.setStatus(Status.HOLD);
                }
                current.setRoc(roc);
                current.setStatus(card.getStatus());
                current.setLastUpdate(new Date());
                pm.makePersistent(current);
            } catch (Exception e) {
                String message = "Oeps something wrong";
                log.severe(message);
                throw new CardPriceException(message, e);
            } finally {
                pm.close();
            }
        }
    }

    class InsertHistoricData {

        synchronized void insert(final Card card, double regularPrice, Date date, String code) throws CardPriceException {
            PersistenceManager pm = PMF.get().getPersistenceManager();
            try {
                Query query = pm.newQuery(Card.class);
                query.setFilter("name == cardName && serie == serieName");
                query.declareParameters("String cardName, String serieName");
                List<Card> results = (List<Card>) query.executeWithArray(card.getName(), card.getSerie());
                if (results.size() != 1) {
                    return;
                }
                Card card2 = results.get(0);
                HistoricData historicData = new HistoricData(date, regularPrice, code);
                card2.getHistoricData().add(historicData);
                Double oldRoc = card2.getRoc();
                if (regularPrice == 0.0) {
                    card2.setRoc(0.0);
                } else if (card2.getRoc() != null && card2.getRoc() != 0.0) {
                    Double roc = RateOfChangeUtil.calcROC(regularPrice, card2.getPrice(), card2.getRoc());
                    card2.setRoc(roc);
                } else {
                    double roc = calcFullROC(card2.getHistoricData(), new Date());
                    card2.setRoc(roc);
                }
                if (regularPrice != 0.0) {
                    if (oldRoc == null) {
                        card2.setStatus(Status.NONE);
                    } else if (card2.getRoc() > oldRoc && card2.getRoc() > 0) {
                        card2.setStatus(Status.BUY);
                    } else if (card2.getRoc() < oldRoc && card2.getRoc() < 0) {
                        card2.setStatus(Status.SELL);
                    } else {
                        card2.setStatus(Status.HOLD);
                    }
                }
                card2.setPrice(regularPrice);
                card2.setLastUpdate(new Date());
                pm.makePersistent(card2);
                pm.evict(card2);
            } catch (Exception e) {
                String message = "Oeps something wrong";
                log.severe(message);
                throw new CardPriceException(message, e);
            } finally {
                pm.close();
            }
        }
    }

    class InsertCard {

        synchronized void insert(String cardName, Serie serie) throws CardPriceException {
            PersistenceManager em = PMF.get().getPersistenceManager();
            Transaction t = em.currentTransaction();
            try {
                Query query = em.newQuery(Card.class);
                query.setFilter("name == cardName && serie == serieName");
                query.declareParameters("String cardName, String serieName");
                List<Card> results = (List<Card>) query.executeWithArray(cardName, serie);
                if (results.size() != 0) {
                    return;
                }
                Card card = new Card(cardName, serie);
                em.makePersistent(card);
            } catch (Exception e) {
                String message = "Oeps something wrong";
                log.severe(message);
                throw new CardPriceException(message, e);
            } finally {
                em.close();
            }
        }
    }
}
