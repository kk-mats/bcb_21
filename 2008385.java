package org.sf.daaf.workflow.method.email.test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import java.util.Locale;
import org.apache.log4j.Logger;
import org.sf.daaf.common.email.EmailException;
import org.sf.daaf.common.email.EmailMessage;
import org.sf.daaf.test.DAAFTestBase;
import org.sf.daaf.workflow.core.WorkflowConfig;
import org.sf.daaf.workflow.method.email.EmailMethod;
import org.sf.daaf.workflow.method.email.EmailMethodHelper;
import org.sf.daaf.workflow.method.email.EmailMessageBuilder;
import org.sf.daaf.workflow.method.email.EmailMessageTokenValues;
import org.sf.daaf.common.util.PropertiesReader;

/**
 * Unit tests for <code>EmailMethod</code>.
 *
 * @author <a href="mailto:luther@lebsvcs.com">Luther E. Birdzell</a>
 * @version 1.0
 */
public class EmailMethodTest extends DAAFTestBase {

    private static Logger logger = Logger.getLogger(EmailMethodTest.class);

    /**
     * The 'sendEmail' property key.  Tells this <code>TestCase</code>
     * whether or not to actually send the emails when testing.
     */
    public static final String SEND_EMAIL = "sendEmail";

    /**
     * Set up the TestSuite.
     *
     * @return a <code>Test</code> value
     */
    public static Test suite() {
        return new TestSuite(EmailMethodTest.class);
    }

    /**
     * Test WorkflowConfig.init().
     * Parse the configuration and build the corresponding dataobjects.
     *
     */
    protected void setUp() {
        WorkflowConfig.init();
    }

    /**
     * Build a test <code>EmailMessage</code> send it if the 
     * sendEmail property in unitTest.properties == 'true'.
     */
    public void testSendEmailDefaultMessage() {
        try {
            String messageName = TestEmailMethodHelper.TEST_MESSAGE;
            EmailMessage message = getEmailMessage(messageName, null);
            verifyMessageNoFiltering(message, null, messageName);
            sendMessage(messageName);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            fail("testSendEmail() failed! " + e.getMessage());
        }
    }

    /**
     * Build a test <code>EmailMessage</code> send it if the 
     * sendEmail property in unitTest.properties == 'true'.
     */
    public void testSendEmailSpanishMessage() {
        try {
            Locale locale = new Locale("es");
            EmailMessage message = getEmailMessage(TestEmailMethodHelper.TEST_MESSAGE, locale);
            verifyMessageNoFiltering(message, locale, TestEmailMethodHelper.TEST_MESSAGE);
            sendMessage(TestEmailMethodHelper.TEST_MESSAGE);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            fail("testSendEmail() failed! " + e.getMessage());
        }
    }

    /**
     * Build a test <code>EmailMessage</code> send it if the 
     * sendEmail property in unitTest.properties == 'true'.
     */
    public void testSendEmailSpanishUSMessage() {
        try {
            Locale locale = new Locale("es", "US");
            EmailMessage message = getEmailMessage(TestEmailMethodHelper.TEST_MESSAGE, locale);
            verifyMessageNoFiltering(message, locale, TestEmailMethodHelper.TEST_MESSAGE);
            sendMessage(TestEmailMethodHelper.TEST_MESSAGE);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            fail("testSendEmail() failed! " + e.getMessage());
        }
    }

    /**
     * Build a test <code>EmailMessage</code> send it if the 
     * sendEmail property in unitTest.properties == 'true'.
     */
    public void testSendEmailFilteredMessage() {
        try {
            EmailMessage message = getEmailMessage(TestEmailMethodHelper.TEST_FILTER_MESSAGE, null);
            verifyMessageWithFiltering(message, null);
            sendMessage(TestEmailMethodHelper.TEST_FILTER_MESSAGE);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            fail("testSendEmail() failed! " + e.getMessage());
        }
    }

    /**
     * Verify the values in the <code>EmailMessage</code> match
     * the values in the <code>ResourceBunele</code>.
     *
     * @param message an <code>EmailMessage</code> value
     * @param locale a <code>Locale</code> value
     * @param messageName a <code>String</code> value
     */
    protected void verifyMessageNoFiltering(EmailMessage message, Locale locale, String messageName) {
        PropertiesReader propReader = null;
        if (locale == null) {
            propReader = new PropertiesReader(messageName);
        } else {
            propReader = new PropertiesReader(messageName, locale);
        }
        String fromName = propReader.getValue(EmailMessageBuilder.FROM_NAME_BUNDLE_PROPERTY);
        assertTrue(fromName != null && message.getFromName() != null);
        assertTrue(fromName.equals(message.getFromName()));
        String fromAddress = propReader.getValue(EmailMessageBuilder.FROM_ADDRESS_BUNDLE_PROPERTY);
        assertTrue(fromAddress != null && message.getFromAddress() != null);
        assertTrue(fromAddress.equals(message.getFromAddress()));
        String toName = propReader.getValue(EmailMessageBuilder.TO_NAME_BUNDLE_PROPERTY);
        assertTrue(toName != null && message.getToName() != null);
        assertTrue(toName.equals(message.getToName()));
        String toAddress = propReader.getValue(EmailMessageBuilder.TO_ADDRESS_BUNDLE_PROPERTY);
        assertTrue(toAddress != null && message.getToAddress() != null);
        assertTrue(toAddress.equals(message.getToAddress()));
        String body = propReader.getValue(EmailMessageBuilder.BODY_BUNDLE_PROPERTY);
        assertTrue(body != null && message.getMessageText() != null);
        assertTrue(body.equals(message.getMessageText()));
        String subject = propReader.getValue(EmailMessageBuilder.SUBJECT_BUNDLE_PROPERTY);
        assertTrue(message.getSubject().equals(subject));
    }

    /**
     * Describe <code>verifyMessageWithFiltering</code> method here.
     *
     * @param message an <code>EmailMessage</code> value
     * @param locale a <code>Locale</code> value
     */
    protected void verifyMessageWithFiltering(EmailMessage message, Locale locale) {
    }

    /**
     * This method does the heavy lifting for this <code>TestCase</code>.
     *
     * @param messageName a <code>String</code> value
     * @param locale a <code>Locale</code> value
     * @return an <code>EmailMessage</code> value
     * @exception Exception if an error occurs
     */
    protected EmailMessage getEmailMessage(String messageName, Locale locale) throws Exception {
        EmailMessageBuilder messageBuilder = null;
        if (locale == null) {
            messageBuilder = new EmailMessageBuilder(messageName);
        } else {
            messageBuilder = new EmailMessageBuilder(messageName, locale);
        }
        EmailMessageTokenValues tokenValues = TestEmailMethodHelper.getTokenValues(getDfSession(), messageName);
        EmailMessage message = messageBuilder.buildMessage(tokenValues);
        logger.debug("message from MessageBuilder = " + message.toString());
        return message;
    }

    /**
     * Instantiate an <code>EmailMethod</code> object using the 
     * test constructor and send the message.
     */
    protected void sendMessage(String messageName) throws Exception {
        EmailMessageBuilder messageBuilder = new EmailMessageBuilder(messageName);
        EmailMethodHelper emailMethodHelper = new TestEmailMethodHelper();
        EmailMethod emailMethod = new EmailMethod(getDfSession(), messageName, messageBuilder, emailMethodHelper, sendEmail());
        emailMethod.exec();
    }

    /**
     * Reads the sendEmail property from the properties file
     * defined in <code>DAAFTestBase</code>.  If the value is set 
     * to 'true' the email method test cases will send the messages
     * using the SMTP server defined in test-data/DAAFConfig.xml.
     *
     * @return a <code>boolean</code> value
     */
    protected static boolean sendEmail() {
        boolean sendEmail = false;
        if (getPropReader() != null) {
            String sendEmailStr = getPropReader().getValue(SEND_EMAIL);
            if (sendEmailStr != null) {
                sendEmail = Boolean.valueOf(sendEmailStr).booleanValue();
            }
        }
        return sendEmail;
    }
}
