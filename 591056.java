package org.dctmutils.daaf.method.email.test;

import java.util.Locale;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dctmutils.common.PropertiesReader;
import org.dctmutils.common.email.EmailMessageTokenValues;
import org.dctmutils.common.email.object.EmailMessage;
import org.dctmutils.daaf.method.email.DaafEmailMessageBuilder;
import org.dctmutils.daaf.method.email.EmailMethod;
import org.dctmutils.daaf.method.email.EmailMethodHelper;
import org.dctmutils.daaf.test.DaafTestCase;

/**
 * Unit tests for <code>EmailMethod</code>.
 * 
 * @author <a href="mailto:luther@dctmutils.org">Luther E. Birdzell</a>
 */
public class EmailMethodTest extends DaafTestCase {

    private static Log log = LogFactory.getLog(EmailMethodTest.class);

    /**
     * The 'sendEmail' property key. Tells this <code>TestCase</code> whether
     * or not to actually send the emails when testing.
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
     * Build a test <code>EmailMessage</code> send it if the sendEmail
     * property in unitTest.properties == 'true'.
     */
    public void testSendEmailDefaultMessage() {
        try {
            String messageName = TestEmailMethodHelper.TEST_MESSAGE;
            log.debug("messageName = " + messageName);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            fail("testSendEmailDefaultMessage() failed! " + e.getMessage());
        }
    }

    /**
     * Verify the values in the <code>EmailMessage</code> match the values in
     * the <code>ResourceBunele</code>.
     * 
     * @param message
     *            an <code>EmailMessage</code> value
     * @param locale
     *            a <code>Locale</code> value
     * @param messageName
     *            a <code>String</code> value
     */
    protected void verifyMessageNoFiltering(EmailMessage message, Locale locale, String messageName) {
        PropertiesReader propReader = null;
        if (locale == null) {
            propReader = new PropertiesReader(messageName);
        } else {
            propReader = new PropertiesReader(messageName, locale);
        }
        String fromName = propReader.getValue(DaafEmailMessageBuilder.FROM_NAME_BUNDLE_PROPERTY);
        assertTrue(fromName != null && message.getFromName() != null);
        assertTrue(fromName.equals(message.getFromName()));
        String fromAddress = propReader.getValue(DaafEmailMessageBuilder.FROM_ADDRESS_BUNDLE_PROPERTY);
        assertTrue(fromAddress != null && message.getFromAddress() != null);
        assertTrue(fromAddress.equals(message.getFromAddress()));
        String toName = propReader.getValue(DaafEmailMessageBuilder.TO_NAME_BUNDLE_PROPERTY);
        assertTrue(toName != null && message.getToName() != null);
        assertTrue(toName.equals(message.getToName()));
        String toAddress = propReader.getValue(DaafEmailMessageBuilder.TO_ADDRESS_BUNDLE_PROPERTY);
        assertTrue(toAddress != null && message.getToAddress() != null);
        assertTrue(toAddress.equals(message.getToAddress()));
        String body = propReader.getValue(DaafEmailMessageBuilder.BODY_BUNDLE_PROPERTY);
        assertTrue(body != null && message.getMessageText() != null);
        assertTrue(body.equals(message.getMessageText()));
        String subject = propReader.getValue(DaafEmailMessageBuilder.SUBJECT_BUNDLE_PROPERTY);
        assertTrue(message.getSubject().equals(subject));
    }

    /**
     * Describe <code>verifyMessageWithFiltering</code> method here.
     * 
     * @param message
     *            an <code>EmailMessage</code> value
     * @param locale
     *            a <code>Locale</code> value
     */
    protected void verifyMessageWithFiltering(EmailMessage message, Locale locale) {
    }

    /**
     * This method does the heavy lifting for this <code>TestCase</code>.
     * 
     * @param messageName
     *            a <code>String</code> value
     * @param locale
     *            a <code>Locale</code> value
     * @return an <code>EmailMessage</code> value
     * @exception Exception
     *                if an error occurs
     */
    protected EmailMessage getEmailMessage(String messageName, Locale locale) throws Exception {
        DaafEmailMessageBuilder messageBuilder = null;
        if (locale == null) {
            messageBuilder = new DaafEmailMessageBuilder(messageName);
        } else {
            messageBuilder = new DaafEmailMessageBuilder(messageName, locale);
        }
        assertNotNull(messageBuilder);
        EmailMessageTokenValues tokenValues = TestEmailMethodHelper.getTokenValues(getDfSession(), messageName);
        assertNotNull(tokenValues);
        return null;
    }

    /**
     * Instantiate an <code>EmailMethod</code> object using the test
     * constructor and send the message.
     *
     * @param messageName
     * @throws Exception
     */
    protected void sendMessage(String messageName) throws Exception {
        DaafEmailMessageBuilder messageBuilder = new DaafEmailMessageBuilder(messageName);
        EmailMethodHelper emailMethodHelper = new TestEmailMethodHelper();
        EmailMethod emailMethod = new EmailMethod(getDfSession(), messageName, messageBuilder, emailMethodHelper, sendEmail());
        emailMethod.runMethod();
    }

    /**
     * Reads the sendEmail property from the properties file defined in
     * <code>DaafTestBase</code>. If the value is set to 'true' the email
     * method test cases will send the messages using the SMTP server defined in
     * test-data/daaf.xml.
     * 
     * @return a <code>boolean</code> value
     */
    protected boolean sendEmail() {
        boolean sendEmail = false;
        String sendEmailStr = getPropertyValue(SEND_EMAIL);
        if (sendEmailStr != null) {
            sendEmail = Boolean.valueOf(sendEmailStr).booleanValue();
        }
        return sendEmail;
    }
}
