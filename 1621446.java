package com.professionalintellectualdevelopment.gae.logging.xmpp;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import com.google.appengine.api.quota.QuotaService;
import com.google.appengine.api.quota.QuotaServiceFactory;
import com.google.appengine.api.xmpp.JID;
import com.google.appengine.api.xmpp.Message;
import com.google.appengine.api.xmpp.MessageBuilder;
import com.google.appengine.api.xmpp.XMPPService;
import com.google.appengine.api.xmpp.XMPPServiceFactory;

public class XMPPHandler extends Handler {

    private static String _instanceId;

    private JID _sender;

    private JID[] _recipients;

    private Level _thresholdLevel;

    private long _thresholdLatencyMillis;

    private long _thresholdTotalMillis;

    private ThreadLocal<String> _message = new ThreadLocal<String>() {

        protected synchronized String initialValue() {
            return "";
        }
    };

    private ThreadLocal<Long> _startTime = new ThreadLocal<Long>() {

        protected synchronized Long initialValue() {
            return 0L;
        }
    };

    private ThreadLocal<Boolean> _loggingBegun = new ThreadLocal<Boolean>() {

        protected synchronized Boolean initialValue() {
            return false;
        }
    };

    private ThreadLocal<Boolean> _thresholdReached = new ThreadLocal<Boolean>() {

        protected synchronized Boolean initialValue() {
            return false;
        }
    };

    private ThreadLocal<Boolean> _atLeastOneLogRecord = new ThreadLocal<Boolean>() {

        protected synchronized Boolean initialValue() {
            return false;
        }
    };

    private ThreadLocal<DateFormat> _timeFormatter = new ThreadLocal<DateFormat>() {

        protected synchronized DateFormat initialValue() {
            return DateFormat.getTimeInstance(DateFormat.MEDIUM);
        }
    };

    private ThreadLocal<DateFormat> _dateFormatter = new ThreadLocal<DateFormat>() {

        protected synchronized DateFormat initialValue() {
            return DateFormat.getDateInstance(DateFormat.MEDIUM);
        }
    };

    public XMPPHandler(Level thresholdLevel, long thresholdLatencyMillis, long thresholdTotalMillis, String sender, String... recipients) {
        _instanceId = "" + Runtime.getRuntime().hashCode();
        _thresholdLevel = thresholdLevel;
        _thresholdLatencyMillis = thresholdLatencyMillis;
        _thresholdTotalMillis = thresholdTotalMillis;
        if (recipients == null || recipients.length == 0) throw new IllegalArgumentException("At least one recipient must be specified");
        if (sender != null && sender.trim().length() > 0) _sender = new JID(sender);
        List<JID> tRecipients = new ArrayList<JID>(recipients.length);
        for (String recipient : recipients) {
            if (recipient != null && recipient.trim().length() > 0) {
                tRecipients.add(new JID(recipient));
            }
        }
        if (tRecipients.size() == 0) throw new IllegalArgumentException("At least one recipient must be specified");
        _recipients = tRecipients.toArray(new JID[tRecipients.size()]);
    }

    public void begin(String uri, String url, String queryString, String remoteAddress, String method, String userAgent) {
        _atLeastOneLogRecord.set(false);
        _loggingBegun.set(true);
        Date begin = new Date();
        _startTime.set(begin.getTime());
        if (_thresholdLevel == null && _thresholdLatencyMillis <= 0 && _thresholdTotalMillis <= 0) {
            _thresholdReached.set(true);
        } else {
            _thresholdReached.set(false);
        }
        String message = String.format("\n**********************************************************************************************\n" + "Request: %1$s%2$s\nURL: %3$s%2$s\nBegan: %4$s %5$s %6$d\nBy: %7$s using %8$s via %9$s\nInstance: %10$s\nThread Id: %11$d\n", uri, queryString != null ? ("?" + queryString) : "", url, _dateFormatter.get().format(begin), _timeFormatter.get().format(begin), (begin.getTime() % 1000), remoteAddress, method, userAgent, _instanceId, Thread.currentThread().getId());
        _message.set(message);
    }

    public void end() {
        if (!_loggingBegun.get()) return;
        Date end = new Date();
        QuotaService qs = QuotaServiceFactory.getQuotaService();
        long cpuTimeMegaCycles = qs.getCpuTimeInMegaCycles();
        long apiTimeMegaCycles = qs.getApiTimeInMegaCycles();
        double cpuMillis = qs.convertMegacyclesToCpuSeconds(cpuTimeMegaCycles) * 1000;
        double apiMillis = qs.convertMegacyclesToCpuSeconds(apiTimeMegaCycles) * 1000;
        double totalMillis = cpuMillis + apiMillis;
        long latencyMillis = end.getTime() - _startTime.get();
        if ((_thresholdLatencyMillis > 0 && latencyMillis >= _thresholdLatencyMillis) || (_thresholdTotalMillis > 0 && totalMillis >= _thresholdTotalMillis)) {
            _thresholdReached.set(true);
        }
        String message = String.format("%1$s\nFinished: %2$s %3$s %4$d\nStats: %5$dlatency_ms %6$.2fcpu_ms %7$.2fapi_ms %8$.2ftotal_ms\n" + "**********************************************************************************************\n", _message.get(), _dateFormatter.get().format(end), _timeFormatter.get().format(end), (end.getTime() % 1000), latencyMillis, cpuMillis, apiMillis, totalMillis);
        _message.set(message);
        flush();
        _loggingBegun.set(false);
        _message.set("");
        _startTime.set(0L);
        _thresholdReached.set(false);
    }

    private String format(LogRecord record) {
        Date time = new Date(record.getMillis());
        String message = String.format("%1$s %2$s %3$d %4$s %5$s\n%6$s: %7$s\n", _dateFormatter.get().format(time), _timeFormatter.get().format(time), (time.getTime() % 1000), record.getSourceClassName(), record.getSourceMethodName(), record.getLevel(), record.getMessage());
        return message;
    }

    @Override
    public void publish(LogRecord record) {
        if (!_loggingBegun.get() || !isLoggable(record)) return;
        _atLeastOneLogRecord.set(true);
        if (_thresholdLevel != null && record.getLevel().intValue() >= _thresholdLevel.intValue()) _thresholdReached.set(true);
        String message = String.format("%1$s\n%2$s", _message.get(), format(record));
        _message.set(message);
    }

    @Override
    public void flush() {
        String message = _message.get();
        if (message != null && message.length() > 0 && _thresholdReached.get() && _atLeastOneLogRecord.get()) {
            XMPPService xmpp = XMPPServiceFactory.getXMPPService();
            MessageBuilder msgBuilder = new MessageBuilder();
            if (_sender != null) msgBuilder.withFromJid(_sender);
            Message msg = msgBuilder.withRecipientJids(_recipients).withBody(_message.get()).build();
            xmpp.sendMessage(msg);
        }
        _message.set("");
        _atLeastOneLogRecord.set(false);
    }

    @Override
    public void close() throws SecurityException {
        flush();
    }
}
