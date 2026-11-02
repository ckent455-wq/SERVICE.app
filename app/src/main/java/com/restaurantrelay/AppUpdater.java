package com.restaurantrelay;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.core.content.FileProvider;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** User-initiated GitHub updates. Never receives call data or sends SMS. */
final class AppUpdater {
    private static final String API="https://api.github.com/repos/ckent455-wq/SERVICE.app/releases/latest";
    private static final long MAX_APK=100L*1024*1024;
    private static final AtomicBoolean BUSY=new AtomicBoolean(false);
    private static volatile String status="Check GitHub for the latest signed release.";
    static final int INSTALL_PERMISSION=420;
    private final Activity activity;
    private final Runnable refresh;
    AppUpdater(Activity activity,Runnable refresh) { this.activity=activity; this.refresh=refresh; }
    String status() { return status; }
    String installedVersion() {
        try { return activity.getPackageManager().getPackageInfo(activity.getPackageName(),0).versionName; }
        catch(PackageManager.NameNotFoundException e) { throw new IllegalStateException(e); }
    }
    private File directory() { return new File(activity.getCacheDir(),"updates"); }
    private File apk() { return new File(directory(),"update.apk"); }
    private File metadata() { return new File(directory(),"release.json"); }
    private JSONObject prepared() throws Exception {
        return new JSONObject(new String(Files.readAllBytes(metadata().toPath()),StandardCharsets.UTF_8));
    }
    boolean hasPreparedUpdate() {
        try { return apk().isFile() && metadata().isFile() && UpdateRules.newer(prepared().getString("version"),installedVersion()); }
        catch(Exception e) { return false; }
    }
    private void ui(Runnable action) {
        activity.runOnUiThread(()->{ if(!activity.isFinishing() && !activity.isDestroyed()) action.run(); });
    }
    private interface Work { Runnable run() throws Exception; }
    private void task(String message,Work work) {
        if(!BUSY.compareAndSet(false,true)) { android.widget.Toast.makeText(activity,"An update check or download is already running.",android.widget.Toast.LENGTH_SHORT).show(); return; }
        status=message; refresh.run();
        new Thread(()->{
            Runnable next;
            try { next=work.run(); }
            catch(Exception e) {
                status="Update failed. Your installed app and settings are unchanged.";
                String reason=e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();
                next=()->new AlertDialog.Builder(activity).setTitle("Couldn’t update").setMessage(reason+"\n\nTry again when you have a connection.").setPositiveButton("OK",null).show();
            }
            BUSY.set(false); final Runnable result=next;
            ui(()->{ refresh.run(); if(result!=null)result.run(); });
        },"SERVICE-app-update").start();
    }
    void check() {
        task("Checking for updates…",()->{
            JSONObject release=new JSONObject(readText(API,1024*1024));
            if(release.optBoolean("draft") || release.optBoolean("prerelease")) throw new IOException("No stable release is available.");
            String tag=release.getString("tag_name"),version=UpdateRules.version(tag);
            if(!UpdateRules.newer(version,installedVersion())) {
                status="You’re up to date · "+installedVersion();
                return ()->new AlertDialog.Builder(activity).setTitle("You’re up to date").setMessage("SERVICE.app "+installedVersion()+" is the latest available version.").setPositiveButton("Done",null).show();
            }
            String filename=UpdateRules.apkName(tag); JSONObject asset=null,checksums=null; JSONArray assets=release.getJSONArray("assets");
            for(int i=0;i<assets.length();i++) {
                JSONObject item=assets.getJSONObject(i);
                if(filename.equals(item.optString("name")))asset=item;
                if("SHA256SUMS.txt".equals(item.optString("name")))checksums=item;
            }
            if(asset==null)throw new IOException("The new release does not contain an installable SERVICE.app APK yet. Try again shortly.");
            String url=asset.getString("browser_download_url");
            if(!UpdateRules.releaseAssetUrl(url,tag,filename))throw new IOException("Unexpected APK download location.");
            long size=asset.getLong("size");
            if(size<=0 || size>MAX_APK)throw new IOException("Unexpected APK size.");
            String hash;
            if(!asset.isNull("digest")) hash=UpdateRules.digest(asset.getString("digest"));
            else {
                if(checksums==null)throw new IOException("The release checksum is not available yet. Try again shortly.");
                String checksumUrl=checksums.getString("browser_download_url");
                if(!UpdateRules.releaseAssetUrl(checksumUrl,tag,"SHA256SUMS.txt"))throw new IOException("Unexpected checksum location.");
                hash=UpdateRules.checksum(readText(checksumUrl,65536),filename);
            }
            final String expectedHash=hash;
            status="Version "+version+" is available.";
            String notes=release.optString("body",""); if(notes.length()>1600)notes=notes.substring(0,1600)+"…";
            final String details=notes;
            return ()->new AlertDialog.Builder(activity).setTitle("SERVICE.app "+version+" available")
                .setMessage("Download and install this update? Your on-call numbers and history will be kept. Android will ask you to approve installation.\n\n"+details)
                .setPositiveButton("Download update",(d,w)->download(version,url,expectedHash,size)).setNegativeButton("Later",null).show();
        });
    }
    private void download(String version,String url,String hash,long expectedSize) {
        task("Downloading and verifying "+version+"…",()->{
            File dir=directory(); if(!dir.isDirectory() && !dir.mkdirs())throw new IOException("Could not create update storage.");
            File part=new File(dir,"download.part"); Files.deleteIfExists(metadata().toPath());
            try {
                HttpURLConnection connection=open(url);
                try(InputStream input=connection.getInputStream(); OutputStream output=new FileOutputStream(part)) {
                    byte[] buffer=new byte[32768]; long size=0; int count;
                    while((count=input.read(buffer))!=-1) {
                        size+=count; if(size>MAX_APK || size>expectedSize)throw new IOException("The download size does not match the release.");
                        output.write(buffer,0,count);
                    }
                    if(size!=expectedSize)throw new IOException("The download was incomplete. Please try again.");
                } finally { connection.disconnect(); }
                verify(part,version,hash);
                Files.move(part.toPath(),apk().toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                JSONObject info=new JSONObject(); info.put("version",version); info.put("hash",hash);
                Files.write(metadata().toPath(),info.toString().getBytes(StandardCharsets.UTF_8));
                status="Update verified and ready to install · "+version;
                return this::install;
            } catch(Exception e) { Files.deleteIfExists(part.toPath()); throw e; }
        });
    }
    void install() {
        if(!hasPreparedUpdate()) { status="No downloaded update is ready. Check for updates again."; refresh.run(); return; }
        if(!activity.getPackageManager().canRequestPackageInstalls()) {
            new AlertDialog.Builder(activity).setTitle("Allow SERVICE.app updates")
                .setMessage("On the next screen, enable Allow from this source for SERVICE.app. Then return here to approve this update.")
                .setPositiveButton("Open settings",(d,w)->{
                    try { activity.startActivityForResult(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+activity.getPackageName())),INSTALL_PERMISSION); }
                    catch(ActivityNotFoundException e) { status="Open Android Settings and allow SERVICE.app to install updates, then tap Install downloaded update."; refresh.run(); }
                }).setNegativeButton("Later",null).show(); return;
        }
        task("Verifying downloaded update…",()->{
            JSONObject info=prepared(); verify(apk(),info.getString("version"),info.getString("hash"));
            status="Ready for Android installation approval.";
            return ()->{
                Uri uri=FileProvider.getUriForFile(activity,activity.getPackageName()+".updates",apk());
                Intent intent=new Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setClipData(ClipData.newRawUri("SERVICE.app update",uri));
                try { activity.startActivity(intent); }
                catch(ActivityNotFoundException e) { status="Android’s installer is unavailable on this phone."; refresh.run(); }
            };
        });
    }
    void returnedFromPermission() { if(activity.getPackageManager().canRequestPackageInstalls() && hasPreparedUpdate())install(); }
    private void verify(File file,String version,String hash) throws Exception {
        MessageDigest digest=MessageDigest.getInstance("SHA-256");
        try(InputStream input=new FileInputStream(file)) { byte[] buffer=new byte[32768]; int n; while((n=input.read(buffer))!=-1)digest.update(buffer,0,n); }
        if(!hex(digest.digest()).equals(UpdateRules.digest(hash)))throw new IOException("APK checksum mismatch. The update was not installed.");
        PackageManager pm=activity.getPackageManager(); int flags=Build.VERSION.SDK_INT>=28?PackageManager.GET_SIGNING_CERTIFICATES:PackageManager.GET_SIGNATURES;
        PackageInfo current=pm.getPackageInfo(activity.getPackageName(),flags),candidate=pm.getPackageArchiveInfo(file.getAbsolutePath(),flags);
        if(candidate==null || !UpdateRules.matchesIdentity(candidate.packageName,code(candidate),code(current),version,candidate.versionName))
            throw new IOException("This APK is not a newer SERVICE.app release.");
        Set<String> installedSigners=signers(current),updateSigners=signers(candidate);
        if(installedSigners.isEmpty() || !installedSigners.equals(updateSigners))
            throw new IOException("This APK does not have the installed app’s signing key. It was not installed.");
    }
    private static long code(PackageInfo info) { return Build.VERSION.SDK_INT>=28?info.getLongVersionCode():info.versionCode; }
    private static Set<String> signers(PackageInfo info) throws Exception {
        Signature[] signatures=Build.VERSION.SDK_INT>=28?(info.signingInfo==null?null:info.signingInfo.getApkContentsSigners()):info.signatures;
        Set<String> result=new HashSet<>(); if(signatures!=null) for(Signature signature:signatures)result.add(hex(MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()))); return result;
    }
    private static String hex(byte[] bytes) { StringBuilder result=new StringBuilder(); for(byte b:bytes)result.append(String.format(Locale.ROOT,"%02x",b&255)); return result.toString(); }
    private static HttpURLConnection open(String url) throws IOException {
        for(int redirects=0;redirects<6;redirects++) {
            if(!UpdateRules.trustedDownloadUrl(url))throw new IOException("Untrusted update download location.");
            HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection(); c.setInstanceFollowRedirects(false);
            c.setConnectTimeout(15000); c.setReadTimeout(30000); c.setRequestProperty("User-Agent","SERVICE.app Android updater");
            c.setRequestProperty("Accept","application/vnd.github+json, application/octet-stream");
            int status;
            try { status=c.getResponseCode(); } catch(IOException e) { c.disconnect(); throw e; }
            if(status==200)return c;
            if(status==301 || status==302 || status==303 || status==307 || status==308) {
                String location=c.getHeaderField("Location"); c.disconnect();
                if(location==null)throw new IOException("Update redirect has no location.");
                url=new URL(new URL(url),location).toString(); continue;
            }
            c.disconnect();
            if(status==403 || status==429)throw new IOException("GitHub is limiting update checks. Please try again later.");
            throw new IOException("GitHub update request failed (HTTP "+status+").");
        }
        throw new IOException("Too many update redirects.");
    }
    private static String readText(String url,int limit) throws IOException {
        HttpURLConnection c=open(url);
        try(InputStream input=c.getInputStream(); ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            byte[] buffer=new byte[8192]; int n,total=0;
            while((n=input.read(buffer))!=-1) { total+=n; if(total>limit)throw new IOException("Update response is too large."); out.write(buffer,0,n); }
            return out.toString(StandardCharsets.UTF_8.name());
        } finally { c.disconnect(); }
    }
}
