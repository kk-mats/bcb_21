package com.hyk.proxy.server.gae.rpc.channel;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import com.google.appengine.api.xmpp.JID;
import com.google.appengine.api.xmpp.Message;
import com.google.appengine.api.xmpp.MessageBuilder;
import com.google.appengine.api.xmpp.MessageType;
import com.google.appengine.api.xmpp.SendResponse;
import com.google.appengine.api.xmpp.XMPPService;
import com.google.appengine.api.xmpp.XMPPServiceFactory;
import com.hyk.proxy.common.xmpp.XmppAddress;
import com.hyk.rpc.core.address.Address;
import com.hyk.rpc.core.transport.RpcChannelData;
import com.hyk.codec.Base64;
import com.hyk.io.buffer.ChannelDataBuffer;

/**
 * @author yinqiwen
 * 
 */
public class XmppServletRpcChannel extends AbstractAppEngineRpcChannel {

    XMPPService xmpp = XMPPServiceFactory.getXMPPService();

    private static final int RETRY = 3;

    private XmppAddress address;

    public XmppServletRpcChannel(String jid) {
        super();
        address = new XmppAddress(jid);
    }

    @Override
    public Address getRpcChannelAddress() {
        return address;
    }

    @Override
    protected void send(RpcChannelData data) throws IOException {
        if (logger.isInfoEnabled()) {
            logger.info("Send message to " + data.address.toPrintableString());
        }
        XmppAddress dataaddress = (XmppAddress) data.address;
        JID jid = new JID(dataaddress.getJid());
        Message msg = new MessageBuilder().withRecipientJids(jid).withMessageType(MessageType.CHAT).withBody(Base64.encodeToString(ChannelDataBuffer.asByteArray(data.content), false)).build();
        {
            int retry = RETRY;
            while (SendResponse.Status.SUCCESS != xmpp.sendMessage(msg).getStatusMap().get(jid) && retry-- > 0) {
                logger.error("Failed to send response, try again!");
            }
        }
    }

    public void processXmppMessage(Message msg) {
        JID fromJid = msg.getFromJid();
        String jid = fromJid.getId();
        try {
            byte[] raw = Base64.decodeFast(msg.getBody());
            ChannelDataBuffer buffer = ChannelDataBuffer.wrap(raw);
            RpcChannelData recv = new RpcChannelData(buffer, new XmppAddress(jid));
            processIncomingData(recv);
        } catch (Exception e) {
            CharArrayWriter writer = new CharArrayWriter();
            e.printStackTrace(new PrintWriter(writer));
            msg = new MessageBuilder().withRecipientJids(fromJid).withBody(writer.toString()).build();
            xmpp.sendMessage(msg);
        }
    }

    @Override
    public boolean isReliable() {
        return true;
    }
}
