package com.personal.authenticator;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.util.*;

public class MainActivity extends AppCompatActivity {
    private LinearLayout list; private TextView timer; private SharedPreferences prefs; private final Handler h=new Handler(Looper.getMainLooper());
    private final Runnable tick=()->{ render(); h.postDelayed(tick,1000); };
    static class Account { String label, secret; Account(String l,String s){label=l;secret=s;} }
    @Override public void onCreate(Bundle b){ super.onCreate(b); setContentView(R.layout.activity_main); list=findViewById(R.id.list); timer=findViewById(R.id.timer); prefs=getSharedPreferences("accounts",MODE_PRIVATE);
        findViewById(R.id.addButton).setOnClickListener(v->addDialog(null)); findViewById(R.id.scanButton).setOnClickListener(v->scan()); render(); h.post(tick); }
    @Override protected void onDestroy(){h.removeCallbacks(tick);super.onDestroy();}
    private ArrayList<Account> accounts(){ ArrayList<Account>a=new ArrayList<>(); String raw=prefs.getString("data",""); if(raw.isEmpty())return a; for(String x:raw.split("\\n",-1)){ if(x.trim().isEmpty())continue; int p=x.indexOf('|'); if(p>0)a.add(new Account(x.substring(0,p),x.substring(p+1))); } return a; }
    private void save(ArrayList<Account>a){StringBuilder s=new StringBuilder();for(Account x:a)s.append(x.label.replace("|"," ")).append('|').append(x.secret).append('\n');prefs.edit().putString("data",s.toString()).apply();}
    private void addDialog(String scanned){ LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(30,10,30,0); EditText label=new EditText(this);label.setHint("Account name (e.g. Gmail)"); EditText secret=new EditText(this);secret.setHint("Base32 secret key");secret.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS); if(scanned!=null){String[] q=parseOtp(scanned); if(q!=null){label.setText(q[0]);secret.setText(q[1]);}} box.addView(label);box.addView(secret); new AlertDialog.Builder(this).setTitle("Add account").setView(box).setPositiveButton("Save",(d,w)->{String l=label.getText().toString().trim();String s=secret.getText().toString().replace(" ","").replace("-","").trim().toUpperCase(Locale.US); if(!l.isEmpty()&&!s.isEmpty()){ArrayList<Account>a=accounts();a.add(new Account(l,s));save(a);render();}}).setNegativeButton("Cancel",null).show(); }
    private void scan(){ if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA},44);return;} IntentIntegrator i=new IntentIntegrator(this);i.setPrompt("Scan an authenticator QR code");i.setBeepEnabled(false);i.setOrientationLocked(false);i.initiateScan(); }
    @Override protected void onActivityResult(int r,int c,android.content.Intent data){IntentResult x=IntentIntegrator.parseActivityResult(r,c,data);if(x!=null&&x.getContents()!=null){String[] q=parseOtp(x.getContents());if(q!=null)addDialog(x.getContents());else Toast.makeText(this,"Invalid otpauth QR code",Toast.LENGTH_LONG).show();return;}super.onActivityResult(r,c,data);}
    private String[] parseOtp(String uri){try{if(!uri.startsWith("otpauth://totp/"))return null; android.net.Uri u=android.net.Uri.parse(uri);String secret=u.getQueryParameter("secret");if(secret==null)return null;String label=java.net.URLDecoder.decode(u.getPath().substring(1),"UTF-8");return new String[]{label,secret};}catch(Exception e){return null;}}
    private void render(){list.removeAllViews();long rem=30-(System.currentTimeMillis()/1000)%30;timer.setText("Refresh in "+rem+"s");for(Account a:accounts()){LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(16,16,16,16); TextView l=new TextView(this);l.setText(a.label);l.setTextSize(17); TextView code=new TextView(this);String c=totp(a.secret);code.setText(c.substring(0,3)+" "+c.substring(3));code.setTextSize(30);code.setTextIsSelectable(true);Button copy=new Button(this);copy.setText("Copy code");copy.setOnClickListener(v->{ClipboardManager cm=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);cm.setPrimaryClip(ClipData.newPlainText("TOTP",c));Toast.makeText(this,"Copied",Toast.LENGTH_SHORT).show();});Button del=new Button(this);del.setText("Delete");del.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Delete account?").setMessage(a.label).setPositiveButton("Delete",(d,w)->{ArrayList<Account>aa=accounts();aa.removeIf(z->z.label.equals(a.label)&&z.secret.equals(a.secret));save(aa);render();}).setNegativeButton("Cancel",null).show());card.addView(l);card.addView(code);card.addView(copy);card.addView(del);list.addView(card,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));}}
    private String totp(String secret){try{byte[] key=base32(secret);long counter=System.currentTimeMillis()/1000/30;byte[] msg=ByteBuffer.allocate(8).putLong(counter).array();Mac mac=Mac.getInstance("HmacSHA1");mac.init(new SecretKeySpec(key,"HmacSHA1"));byte[] h=mac.doFinal(msg);int off=h[h.length-1]&15;int bin=((h[off]&127)<<24)|((h[off+1]&255)<<16)|((h[off+2]&255)<<8)|(h[off+3]&255);return String.format(Locale.US,"%06d",bin%1000000);}catch(Exception e){return "------";}}
    private byte[] base32(String s){String abc="ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";s=s.replace("=","").toUpperCase(Locale.US);java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();int buffer=0,bits=0;for(char ch:s.toCharArray()){int v=abc.indexOf(ch);if(v<0)continue;buffer=(buffer<<5)|v;bits+=5;if(bits>=8){bits-=8;out.write((buffer>>bits)&255);}}return out.toByteArray();}
}
