package com.restaurantrelay;

import android.app.Activity;
import android.content.*;
import org.json.*;

public final class SentReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent intent) {
        synchronized(Store.class) {
            JSONObject row=Store.find(c,intent.getStringExtra("id")); if(row==null) return;
            String kind=intent.getStringExtra("kind"); if(!"ack".equals(kind) && !"forward".equals(kind)) return;
            Store.put(row,kind+"Part"+intent.getIntExtra("part",0),getResultCode());
            int count=row.optInt(kind+"Count"), done=0; boolean failed=false;
            for(int i=0;i<count;i++) if(row.has(kind+"Part"+i)) { done++; if(row.optInt(kind+"Part"+i)!=Activity.RESULT_OK) failed=true; }
            Store.put(row,kind,failed ? "Failed — check call manually" : done==count ? "Sent (receipt not confirmed)" : "Pending carrier result");
            Store.save(c,row); if(failed) Relay.alert(c,"SMS handoff failed. Open Restaurant Relay.");
        }
    }
}
