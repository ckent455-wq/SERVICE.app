package com.restaurantrelay;

import android.content.*;
import android.provider.Telephony;
import android.telephony.SmsMessage;

public final class IncomingSms extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent intent) {
        if(!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) return;
        SmsMessage[] messages=Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if(messages==null || messages.length==0) return;
        String sender=messages[0].getOriginatingAddress(); StringBuilder body=new StringBuilder();
        for(SmsMessage m:messages) { if(!Rules.samePhone(sender,m.getOriginatingAddress())) return; body.append(m.getMessageBody()); }
        Relay.process(c,sender,body.toString(),messages[0].getTimestampMillis(),intent.getIntExtra("subscription",-1));
    }
}
