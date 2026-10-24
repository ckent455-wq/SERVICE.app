package com.restaurantrelay;
import android.Manifest;
import android.app.*;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.text.InputType;
import org.json.*;

public final class MainActivity extends Activity {
 final int BG=0xff0c121b,CARD=0xff172230,INK=0xfff2f6fb,MUTED=0xff99aabe,TEAL=0xff66e2c1;
 LinearLayout content; int page=0;
 @Override public void onCreate(Bundle b){super.onCreate(b);if(b!=null)page=b.getInt("page",0);show();}
 @Override public void onResume(){super.onResume();show();}
 @Override public void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putInt("page",page);}
 int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
 boolean smsGranted(){return checkSelfPermission(Manifest.permission.RECEIVE_SMS)==PackageManager.PERMISSION_GRANTED&&checkSelfPermission(Manifest.permission.SEND_SMS)==PackageManager.PERMISSION_GRANTED;}
 GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
 LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
 TextView text(LinearLayout p,String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));t.setLineSpacing(dp(3),1);p.addView(t);return t;}
 void gap(LinearLayout p,int h){p.addView(new View(this),new LinearLayout.LayoutParams(1,dp(h)));}
 LinearLayout card(){LinearLayout c=col();c.setPadding(dp(22),dp(22),dp(22),dp(22));c.setBackground(bg(CARD,22));content.addView(c,new LinearLayout.LayoutParams(-1,-2));gap(content,16);return c;}
 void button(LinearLayout p,String title,boolean primary,Runnable action){Button b=new Button(this);b.setText(title);b.setTextSize(15);b.setAllCaps(false);b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));b.setTextColor(primary?BG:INK);b.setBackground(bg(primary?TEAL:0xff243447,14));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(52));lp.topMargin=dp(12);p.addView(b,lp);b.setOnClickListener(v->action.run());}
 String number(String key){String d=Rules.phone(("source".equals(key)?Store.ANSWERING_SERVICE:Store.prefs(this).getString(key,"")));return d.length()==10?"("+d.substring(0,3)+") "+d.substring(3,6)+"-"+d.substring(6):"Not configured";}
 void show(){
  LinearLayout root=col();root.setBackgroundColor(BG);setContentView(root);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);getWindow().getDecorView().setSystemUiVisibility(0);
  if(Build.VERSION.SDK_INT>=30)root.setOnApplyWindowInsetsListener((v,i)->{android.graphics.Insets b=i.getInsets(android.view.WindowInsets.Type.systemBars());v.setPadding(b.left,b.top,b.right,b.bottom);return i;});
  LinearLayout h=col();h.setPadding(dp(24),dp(22),dp(24),dp(18));root.addView(h);text(h,"SERVICE.app",27,INK,true);text(h,"RESTAURANT AND GROCERY SUPPORT",10,TEAL,true).setLetterSpacing(.18f);
  ScrollView scroll=new ScrollView(this);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));content=col();content.setPadding(dp(24),dp(10),dp(24),dp(24));scroll.addView(content);
  if(page==0)dashboard();else if(page==1)history();else settings();
  LinearLayout nav=new LinearLayout(this);nav.setPadding(dp(16),dp(10),dp(16),dp(14));root.addView(nav);String[] names={"Overview","Activity","Settings"};
  for(int i=0;i<3;i++){final int index=i;Button b=new Button(this);b.setText(names[i]);b.setAllCaps(false);b.setTextSize(13);b.setTextColor(page==i?BG:MUTED);b.setBackground(bg(page==i?TEAL:BG,14));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(48),1);lp.setMargins(dp(3),0,dp(3),0);nav.addView(b,lp);b.setOnClickListener(v->{page=index;show();});}
 }
 void dashboard(){
  boolean active=Store.prefs(this).getBoolean("enabled",false)&&smsGranted();
  text(content,"your on call, handled",25,INK,true);gap(content,8);text(content,"you’re welcome",14,MUTED,false);gap(content,24);
  LinearLayout c=card();text(c,active?"●  ROUTING ACTIVE":"●  ROUTING PAUSED",12,active?TEAL:0xffffcc80,true);gap(c,12);text(c,active?"Ready for the next call":"You’re in control",23,INK,true);gap(c,8);text(c,active?"New service texts are acknowledged and forwarded automatically.":"Set your numbers, then turn on automatic routing.",14,MUTED,false);button(c,active?"Pause routing":"Enable routing",!active,()->toggle(!active));
  departmentCard("BRINK","brink","SLIM CHICKENS calls"); departmentCard("ALOHA","aloha","All other restaurant calls");
  c=card();text(c,"THE HANDOFF",10,MUTED,true).setLetterSpacing(.1f);gap(c,16);text(c,"01   Answering service",16,INK,true);text(c,"        "+number("source"),14,MUTED,false);gap(c,14);text(c,"02   Receipt acknowledged",16,INK,true);text(c,"        Reply code sent back automatically",13,MUTED,false);gap(c,14);text(c,"03   Technician notified",16,INK,true);text(c,"        Original message forwarded in full",13,MUTED,false);
  text(content,"Recent activity",20,INK,true);gap(content,12);JSONArray rows=Store.history(this);if(rows.length()==0)empty();else{callCard(rows.optJSONObject(0));button(content,"View all activity",false,()->{page=1;show();});}
 }
 void toggle(boolean on){var p=Store.prefs(this);if(on){for(String key:new String[]{"brink","aloha"}){String n=p.getString(key,"");if(!Rules.validPhone(n)||Rules.samePhone(n,Store.ANSWERING_SERVICE)){dialog("Finish setup","Set both BRINK and ALOHA forwarding numbers before enabling routing.");return;}}if(!smsGranted()){permissions();return;}}p.edit().putBoolean("enabled",on).commit();show();}
 void history(){text(content,"Call activity",27,INK,true);gap(content,6);text(content,"Your latest 100 handoffs, stored on this phone.",14,MUTED,false);gap(content,12);button(content,"Refresh activity",false,this::show);gap(content,16);JSONArray r=Store.history(this);if(r.length()==0)empty();for(int i=0;i<r.length();i++)callCard(r.optJSONObject(i));text(content,"Sent confirms the phone sent the text. It does not confirm receipt or reading. Check pending calls manually.",12,MUTED,false);}
 void empty(){LinearLayout c=card();text(c,"No calls yet",20,INK,true);gap(c,8);text(c,"New handoffs will appear here with acknowledgment and forwarding status.",14,MUTED,false);}
 void callCard(JSONObject r){if(r==null)return;LinearLayout c=card();String body=r.optString("body"),preview=body.replaceAll("\\s+"," ");if(preview.length()>95)preview=preview.substring(0,95)+"…";text(c,java.text.DateFormat.getDateTimeInstance(2,3).format(new java.util.Date(r.optLong("time"))),12,MUTED,false);gap(c,10);text(c,preview,16,INK,true);gap(c,14);text(c,r.optString("department","Previous handoff")+"  ·  "+r.optString("target"),13,MUTED,false);gap(c,10);text(c,"Acknowledgment · "+r.optString("ack"),13,statusColor(r.optString("ack")),false);gap(c,5);text(c,"Forwarding · "+r.optString("forward"),13,statusColor(r.optString("forward")),false);if(r.has("issue")){gap(c,10);text(c,r.optString("issue"),13,0xffffcc80,false);}button(c,"Open message",false,()->dialog("Original message",body));}
 int statusColor(String s){return s.startsWith("Sent")?TEAL:s.startsWith("Failed")?0xffffa2a2:0xffffcc80;}
 void settings(){
  text(content,"Settings",27,INK,true);gap(content,6);text(content,"Keep your handoff details up to date.",14,MUTED,false);gap(content,24);
  departmentCard("BRINK","brink","SLIM CHICKENS calls"); departmentCard("ALOHA","aloha","All other restaurant calls"); LinearLayout c;
  c=card();text(c,"Answering service",18,INK,true);gap(c,6);text(c,number("source"),20,INK,false);gap(c,6);text(c,"Only texts from this number trigger a handoff.",13,MUTED,false);text(c,"Fixed service number",12,MUTED,false);
  c=card();text(c,"Phone permissions",18,INK,true);gap(c,8);text(c,smsGranted()?"SMS access granted":"SMS access required",14,smsGranted()?TEAL:0xffffcc80,false);if(Build.VERSION.SDK_INT>=33){gap(c,5);text(c,checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED?"Failure notifications allowed":"Allow notifications for failure alerts",13,MUTED,false);}button(c,"Manage permissions",false,this::permissions);
  c=card();text(c,"Try a sample",18,INK,true);gap(c,8);text(c,"Preview a handoff without sending a text.",14,MUTED,false);button(c,"Open test preview",true,this::preview);text(content,"SERVICE.app / 1.0.0",12,MUTED,false);
 }
 void editNumber(String key){var p=Store.prefs(this);LinearLayout wrap=col();wrap.setPadding(dp(24),dp(8),dp(24),0);EditText f=new EditText(this);f.setText(p.getString(key,""));f.setHint("(555) 123-4567");f.setInputType(InputType.TYPE_CLASS_PHONE);f.setSingleLine();wrap.addView(f);AlertDialog d=new AlertDialog.Builder(this).setTitle(key.equals("brink")?"BRINK on-call technician":"ALOHA on-call technician").setMessage("Calls for this department forward here after saving.").setView(wrap).setPositiveButton("Save number",null).setNegativeButton("Cancel",null).create();d.setOnShowListener(x->d.getButton(-1).setOnClickListener(v->{String n=f.getText().toString();if(!Rules.validPhone(n)){f.setError("Enter a US phone number with area code");return;}if(Rules.samePhone(n,Store.ANSWERING_SERVICE)){f.setError("Use two different numbers");return;}p.edit().putString(key,n).commit();d.dismiss();show();}));d.show();}
 void departmentCard(String title,String key,String description){LinearLayout c=card();text(c,title+" ON CALL",12,TEAL,true).setLetterSpacing(.1f);gap(c,10);text(c,number(key),26,INK,true);gap(c,6);text(c,description,14,MUTED,false);button(c,"Change "+title+" number",true,()->editNumber(key));}
 void permissions(){requestPermissions(Build.VERSION.SDK_INT>=33?new String[]{Manifest.permission.RECEIVE_SMS,Manifest.permission.SEND_SMS,Manifest.permission.POST_NOTIFICATIONS}:new String[]{Manifest.permission.RECEIVE_SMS,Manifest.permission.SEND_SMS},10);}
 void preview(){EditText s=new EditText(this);s.setTextSize(15);s.setPadding(dp(24),dp(12),dp(24),dp(12));s.setMinLines(6);s.setText("Office/On Call\nAlex 202-555-0142\nExample Restaurant\nER Service\nCard reader unavailable. Please call.\nReply with 83");new AlertDialog.Builder(this).setTitle("Test a message").setView(s).setPositiveButton("Preview",(d,w)->{String code=Rules.code(s.getText().toString());dialog("Preview · no texts sent",code==null?"No single reply code found. This message needs manual attention.":"Reply to service: "+code+"\n\nDepartment: "+Rules.department(s.getText().toString())+"\nForward to: "+number(Rules.destinationKey(s.getText().toString()))+"\n\n"+s.getText());}).setNegativeButton("Cancel",null).show();}
 void dialog(String t,String s){new AlertDialog.Builder(this).setTitle(t).setMessage(s).setPositiveButton("Done",null).show();}
 @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);show();}
}
