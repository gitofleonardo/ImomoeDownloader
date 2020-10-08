package cn.huangchengxi.imomoedown;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class DownloadHelper {
    private DownloadHelper(){ }
    private static volatile DownloadHelper helper;
    private ExecutorService service;
    private int DOWNLOAD_THREAD_COUNT = 5;
    private ConcurrentHashMap<String,Download> downloads=new ConcurrentHashMap<>();
    private ObservableList<Download> downloadObservableList=FXCollections.observableArrayList();

    public static DownloadHelper instance(){
        if (helper==null){
            synchronized (DownloadHelper.class){
                if (helper==null){
                    helper=new DownloadHelper();
                    helper.service=Executors.newFixedThreadPool(helper.DOWNLOAD_THREAD_COUNT);
                }
            }
        }
        return helper;
    }
    public ObservableList<Download> mapAsList(){
        return downloadObservableList;
    }

    public synchronized void setDownloadThreadCount(int newValue){
        DOWNLOAD_THREAD_COUNT=newValue;
        ((ThreadPoolExecutor) service).setCorePoolSize(newValue);
    }
    public int getDownloadThreadCount(){
        return DOWNLOAD_THREAD_COUNT;
    }
    public synchronized void commitDownload(Download download){
        if (download.stateNow == Download.STATE.DONE){
            throw new IllegalArgumentException("Download already finished.");
        }
        if (downloads.get(download.dID)!=null){
            throw new IllegalArgumentException("Download already exists.");
        }
        downloads.put(download.dID,download);
        downloadObservableList.add(download);
        service.submit(download);
    }
    public synchronized void removeDownload(Download download){
        download.stop();
        downloads.remove(download.dID);
        //downloadObservableList.remove(download);
    }
    public synchronized void pauseDownload(Download download){
        download.pause();
    }
    public synchronized void resumeDownload(Download download){
        if (download.stateNow== Download.STATE.STOPPED || download.stateNow== Download.STATE.PAUSED){
            service.submit(download);
        }
    }
    public synchronized void shutdown(){
        for (Download d:downloads.values()){
            d.stop();
        }
        service.shutdownNow();
    }

    public static class Download implements Runnable{
        private String url;
        private String location;
        private String filename;
        private DCallback callback;
        private String dID;
        private ObservableLong totalSize;
        private ObservableLong downloadedSize;
        private ObservableFloat percent;
        private STATE stateNow=STATE.PREPARING;

        public Download(String url,String location,String filename,DCallback callback){
            this.url=url;
            this.location=location;
            this.callback=callback;
            this.filename=filename;
            dID=md5Sum(url);

            this.callback.onWaiting(this);

            totalSize=new ObservableLong();
            downloadedSize=new ObservableLong();
            percent=new ObservableFloat();
        }
        public String getId(){
            return dID;
        }

        public String getUrl() {
            return url;
        }

        public String getLocation() {
            return location;
        }

        public DCallback getCallback() {
            return callback;
        }

        public ObservableLong getTotalSize() {
            return totalSize;
        }

        public ObservableLong getDownloadedSize() {
            return downloadedSize;
        }

        public ObservableFloat getPercent() {
            return percent;
        }

        public String getFilename() {
            return filename;
        }

        private void pause(){
            stateNow=STATE.PAUSED;
        }
        private void stop(){
            stateNow=STATE.STOPPED;
        }

        public STATE getStateNow() {
            return stateNow;
        }

        @Override
        public void run() {
            //todo download file from url
            File file=null;
            URLConnection connection=null;
            RandomAccessFile raf=null;
            try{
                stateNow=STATE.STARTING;
                Platform.runLater(() -> callback.onStarting(this));

                URL url=new URL(getUrl());

                URLConnection testConnection=new URL(getUrl()).openConnection();
                totalSize.setValue(testConnection.getContentLengthLong());

                connection=url.openConnection();
                connection.setDoInput(true);

                file=new File(location+"/"+filename);

                downloadedSize.setValue(file.length());
                connection.setRequestProperty("Range","bytes="+downloadedSize.getValue()+"-"+totalSize.getValue());

                if (file.exists() && file.length()>=totalSize.getValue()){
                    downloadedSize.setValue(totalSize.getValue());
                    percent.setValue(1.0f);
                    Platform.runLater(()->callback.onDone(this));
                    Platform.runLater(()->helper.removeDownload(this));
                    return;
                }

                if (!file.exists() && !file.createNewFile()){
                    Platform.runLater(()->callback.onError(this));
                }

                stateNow=STATE.STARTED;
                if (downloadedSize.getValue()>0)
                    Platform.runLater(()->callback.onRestart(this));
                else
                    Platform.runLater(()->callback.onStart(this));

                byte[] buffer=new byte[8192];
                int len;
                InputStream is=connection.getInputStream();
                raf=new RandomAccessFile(location+"/"+filename,"rwd");
                raf.seek(downloadedSize.getValue());

                while ((len=is.read(buffer))!=-1){
                    raf.write(buffer,0,len);
                    downloadedSize.setValue(downloadedSize.getValue()+len);
                    percent.setValue(((float)downloadedSize.getValue()/(float)totalSize.getValue()));
                    Platform.runLater(()->callback.onProgressUpdated(this));

                    if (stateNow==STATE.PAUSED || stateNow==STATE.STOPPED){
                        if (stateNow==STATE.PAUSED)
                            Platform.runLater(()->callback.onPause(this));
                        else
                            Platform.runLater(()->callback.onStop(this));
                        is.close();
                        raf.close();
                        return;
                    }
                }

                Platform.runLater(()->callback.onDone(this));
                Platform.runLater(()->helper.removeDownload(this));
                is.close();
                raf.close();
            }catch (Exception e){
                e.printStackTrace();

                stateNow=STATE.ERROR;
                Platform.runLater(()->callback.onError(this));
            }finally {
                try{
                    if (raf!=null){
                        raf.close();
                    }
                }catch (Exception ignored){}
            }
        }

        public String md5Sum(String str){
            try {
                MessageDigest md=MessageDigest.getInstance("MD5");
                byte[] bs=md.digest(str.getBytes());
                StringBuilder sb=new StringBuilder();
                for (byte b:bs){
                    if ((b & 0xff)>>4 ==0){
                        sb.append("0").append(Integer.toHexString(b & 0xff));
                    }else{
                        sb.append(Integer.toHexString(b & 0xff));
                    }
                }
                return sb.toString();
            }catch (Exception e){
                return str;
            }
        }
        public enum STATE{
            PREPARING,
            STARTING,
            STARTED,
            PAUSED,
            STOPPED,
            ERROR,
            DONE
        }
    }
    public static class ObservableLong implements ObservableValue<Long> {
        private ArrayList<WeakReference<ChangeListener<? super Long>>> listeners=new ArrayList<>();
        private Long val=0L;

        @Override
        public void addListener(ChangeListener<? super Long> changeListener) {
            listeners.add(new WeakReference<>(changeListener));
        }

        @Override
        public void removeListener(ChangeListener<? super Long> changeListener) {
            for (WeakReference<ChangeListener<? super Long>> l:listeners){
                if (l.get()==changeListener){
                    listeners.remove(l);
                    return;
                }
            }
        }

        @Override
        public Long getValue() {
            return val;
        }
        public void setValue(Long newVal){
            Long old=val;
            this.val=newVal;

            for (WeakReference<ChangeListener<? super Long>> l:listeners){
                if (l!=null && l.get()!=null){
                    l.get().changed(this,old,newVal);
                }
            }
        }

        @Override
        public void addListener(InvalidationListener invalidationListener) {

        }

        @Override
        public void removeListener(InvalidationListener invalidationListener) {

        }
    }
    public static class ObservableFloat implements ObservableValue<Float> {
        private ArrayList<WeakReference<ChangeListener<? super Float>>> listeners=new ArrayList<>();
        private Float val=0F;

        @Override
        public void addListener(ChangeListener<? super Float> changeListener) {
            listeners.add(new WeakReference<>(changeListener));
        }

        @Override
        public void removeListener(ChangeListener<? super Float> changeListener) {
            for (WeakReference<ChangeListener<? super Float>> l:listeners){
                if (l.get()==changeListener){
                    listeners.remove(l);
                    return;
                }
            }
        }

        @Override
        public Float getValue() {
            return val;
        }
        public void setValue(Float newVal){
            Float old=val;
            this.val=newVal;

            for (WeakReference<ChangeListener<? super Float>> l:listeners){
                if (l!=null && l.get()!=null){
                    l.get().changed(this,old,newVal);
                }
            }
        }


        @Override
        public void addListener(InvalidationListener invalidationListener) {

        }

        @Override
        public void removeListener(InvalidationListener invalidationListener) {

        }
    }
    public interface DCallback{
        void onStart(Download download);
        void onPause(Download download);
        void onStop(Download download);
        void onRestart(Download download);
        void onWaiting(Download download);
        void onStarting(Download download);
        void onProgressUpdated(Download download);
        void onError(Download download);
        void onDone(Download download);
    }
}
