package com.restaurantrelay;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.telephony.*;
import java.util.*;
import org.json.*;

final class Relay {
    static void process(Context c, String sender, String body, long timestamp, int subscription) {
        var p=Store.prefs(c);
        if(!p.getBoolean("enabled",false) || !Rules.samePhone(sender,Store.source(c))) return;
        String id=UUID.nameUUIDFromBytes((Rules.phone(sender)+"|"+timestamp+"|"+body).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
        if(Store.find(c,id)!=null) return;
        JSONObject row=new JSONObject();
        Store.put(row,"id",id); Store.put(row,"time",System.currentTimeMillis()); Store.put(row,"body",body);
        Store.put(row,"sender",sender); Store.put(row,"department",Rules.department(body));
        Store.put(row,"target",p.getString(Rules.destinationKey(body),""));
        Store.put(row,"ack","Not sent"); Store.put(row,"forward","Not sent");
        Store.save(c,row);
        String target=p.getString(Rules.destinationKey(body),""), code=Rules.code(body);
        if(code==null || !Rules.validPhone(target) || Rules.samePhone(sender,target)) {
            Store.put(row,"issue","Needs attention: missing/ambiguous reply code or invalid forwarding number.");
            Store.save(c,row); alert(c,"Call needs attention. Open Restaurant Relay."); return;
        }
        // Each operation is independently tracked; a failed acknowledgment must not suppress forwarding.
        send(c,row,"ack",sender,code,subscription);
        send(c,row,"forward",target,body,subscription);
    }
    static void send(Context c, JSONObject row, String kind, String destination, String body, int subscription) {
        try {
            int sub=subscription>=0 && subscription<Integer.MAX_VALUE ? subscription : SubscriptionManager.getDefaultSmsSubscriptionId();
            if(sub<0 || sub==Integer.MAX_VALUE) throw new IllegalStateException("Choose a default SMS SIM in Android settings");
            SmsManager manager=SmsManager.getSmsManagerForSubscriptionId(sub);
            ArrayList<String> parts=manager.divideMessage(body);
            ArrayList<PendingIntent> callbacks=new ArrayList<>();
            Store.put(row,kind,"Pending carrier result"); Store.put(row,kind+"Count",parts.size());
            for(int i=0;i<parts.size();i++) {
                Intent intent=new Intent(c,SentReceiver.class).setData(Uri.parse("relay://"+row.optString("id")+"/"+kind+"/"+i));
                intent.putExtra("id",row.optString("id")).putExtra("kind",kind).putExtra("part",i);
                callbacks.add(PendingIntent.getBroadcast(c,0,intent,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT));
            }
            Store.save(c,row);
            manager.sendMultipartTextMessage(destination,null,parts,callbacks,null);
        } catch(Exception e) {
            Store.put(row,kind,"Failed: "+e.getMessage()); Store.save(c,row); alert(c,"SMS handoff failed. Open Restaurant Relay.");
        }
    }
    static void alert(Context c, String text) {
        NotificationManager nm=c.getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("attention","Calls needing attention",NotificationManager.IMPORTANCE_HIGH));
        PendingIntent open=PendingIntent.getActivity(c,0,new Intent(c,MainActivity.class),PendingIntent.FLAG_IMMUTABLE);
        try { nm.notify(1,new Notification.Builder(c,"attention").setSmallIcon(android.R.drawable.stat_notify_error).setContentTitle("SERVICE.app").setContentText(text.replace("Restaurant Relay","SERVICE.app")).setContentIntent(open).setAutoCancel(true).build()); }
        catch(SecurityException ignored) { /* History remains available if notifications are denied. */ }
    }
}
