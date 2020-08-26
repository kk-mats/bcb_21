package com.addermason.monitoring.server;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import com.google.appengine.api.xmpp.MessageBuilder;
import com.google.appengine.api.xmpp.XMPPService;
import com.google.appengine.api.xmpp.XMPPServiceFactory;

public final class NotificationService {

    /**
	 * Processes all Alerts
	 * This method is mostly thread safe since
	 * Once the main loop for the Notification Cycle on an alert has been complete 
	 * The notification state on the Alert object is set & immediately persisted to the datastore
	 * The only problem with this function is that
	 * All alerts from the beginning of time are loaded into the List
	 * This can be fixed by modifying the query to filter out everything already alerted on.
	 * Also garbage cleanup should include a policy to delete Alerts older than N however that would be 
	 * An SLA & possibly a legal issue as well.
	 *  
	 */
    @SuppressWarnings("unchecked")
    public static void processAlerts() {
        List<Alert> alertList = (List<Alert>) QueryManager.findAll(Alert.class);
        for (Alert alert : alertList) {
            if (!alert.isNotified()) {
                if (alert.getCheckKey() != null) {
                    processServiceAlert(alert);
                } else {
                    if (alert.getHostKey() != null) {
                        processHostAlert(alert);
                    } else {
                        processAccountAlert(alert);
                    }
                }
            }
        }
    }

    /**
	 * An account is alerting, i.e. 
	 * balance low or some other condition that 
	 * Requires the attention of the primary contact for the account.
	 * @param alert
	 */
    private static void processAccountAlert(Alert alert) {
    }

    /**
	 * We have determined a Host is alerting and yet to be notified
	 * So lets notify the responsible party
	 * Notifications on Host Alerts are sent out according to the combined policy of the
	 * 
	 * Account
	 * Host
	 * HostGroup(s)
	 * ContactGroup(s)
	 * Contact
	 * 
	 * In that order, if false is set on any of those, it effects all descendants
	 * @param alert
	 */
    private static void processHostAlert(Alert alert) {
        boolean notified = false;
        Account account = AccountManager.lookUp(alert.getAccountKey());
        Host host = HostManager.lookUp(alert.getHostKey());
        if (account.isNotifyEnabled()) {
            if (host.isNotifyEnabled()) {
                String msgBody = generateMessageBody(alert);
                List<HostGroup> hostGroups = host.getHostGroups();
                for (HostGroup hostGroup : hostGroups) {
                    if (hostGroup.isNotifyEnabled()) {
                        List<ContactGroup> contactGroups = hostGroup.getContacts();
                        for (ContactGroup contactGroup : contactGroups) {
                            List<Contact> contacts = contactGroup.getContacts();
                            if (contactGroup.isNotifyEnabled()) {
                                for (Contact contact : contacts) {
                                    if (contact.isNotifyEnabled()) {
                                        contact.notifyContact(alert, account, "Service Alert", msgBody);
                                        notified = true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        alert.setNotified(notified);
    }

    /**
	 * We have determined a Service is alerting and yet to be notified
	 * Lets notify the responsible party
	 * Notifications on Service Alerts are sent out according to the combined policy of the
	 * 
	 * Account
	 * Host
	 * Service
	 * ServiceGroup(s)
	 * ContactGroup(s)
	 * Contact
	 * 
	 * In that order, if false is set on any of those, it effects all descendants
	 * @param alert
	 */
    private static void processServiceAlert(Alert alert) {
        boolean notified = false;
        Account account = AccountManager.lookUp(alert.getAccountKey());
        ServiceCheck service = ServiceCheckManager.lookUp(alert.getCheckKey());
        Host host = HostManager.lookUp(alert.getHostKey());
        if (account.isNotifyEnabled()) {
            if (host.isNotifyEnabled()) {
                if (service.isNotifyEnabled()) {
                    String msgBody = generateMessageBody(alert);
                    List<ServiceGroup> serviceGroups = service.getServiceGroups();
                    for (ServiceGroup serviceGroup : serviceGroups) {
                        if (serviceGroup.isNotifyEnabled()) {
                            List<ContactGroup> serviceContactGroups = serviceGroup.getContacts();
                            for (ContactGroup group : serviceContactGroups) {
                                if (group.isNotifyEnabled()) {
                                    List<Contact> contactList = group.getContacts();
                                    for (Contact contact : contactList) {
                                        if (contact.isNotifyEnabled()) {
                                            contact.notifyContact(alert, account, "Service Alert", msgBody);
                                            notified = true;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        alert.setNotified(notified);
    }

    public static void notifyStart(Account account, ProcessingRequest request, Host host) {
        if (account.isNotifyEnabled()) {
            if (host.isNotifyEnabled()) {
            }
        }
    }

    public static void notifyEnd(Account account, ProcessingRequest request, Host host) {
        if (account.isNotifyEnabled()) {
            if (host.isNotifyEnabled()) {
            }
        }
    }

    public static void sendNotification(Host host, Account account, String[] data) {
        if (account.isNotifyEnabled()) {
            if (host.isNotifyEnabled()) {
            }
        }
    }

    public static void sendNotificationByEmail(Contact contact, Alert alert, String headline, String msgBody, String address) {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);
        try {
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress("admin@addermason.com", "AIMEE Admin"));
            msg.addRecipient(Message.RecipientType.TO, new InternetAddress(address, contact.getFirstName() + " " + contact.getLastName()));
            msg.setSubject(headline);
            msg.setText(msgBody);
            Transport.send(msg);
        } catch (AddressException e) {
        } catch (MessagingException e) {
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public static void sendNotificationByXMPP(Contact contact, Alert alert, String headline, String msgBody) {
        XMPPService xmpp = XMPPServiceFactory.getXMPPService();
        com.google.appengine.api.xmpp.Message msg = new MessageBuilder().withRecipientJids(contact.getXMPP()).withBody(msgBody).build();
        try {
            xmpp.sendMessage(msg);
        } catch (Exception ex) {
            msgBody += "\nYou are receiving this by email because there was a problem sending via XMPP";
            msgBody += "\nMake sure to add aimeemonitoring@appspot.com to your friends list in your XMPP client";
            sendNotificationByEmail(contact, alert, headline, msgBody, contact.getSMS());
            sendNotificationByEmail(contact, alert, headline, msgBody, contact.getEmail().getEmail());
        }
    }

    public static void sendNotificationBySMS(Contact contact, Alert alert, String headline, String msgBody) {
        sendNotificationByEmail(contact, alert, headline, msgBody, contact.getSMS());
    }

    public static String generateMessageBody(Alert alert) {
        StringBuilder body = new StringBuilder();
        body.append("This is an alert from AIMEE!").append("\n");
        body.append("Please log in to http://aimeemonitoring.appspot.com/alerts?acknowledge=");
        body.append(alert.getKey().getId()).append(" to acknowledge this alert.\n");
        body.append("Thank you for your prompt attention to this matter.").append("\n");
        return body.toString();
    }
}
