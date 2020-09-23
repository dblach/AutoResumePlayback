package pl.edu.dblach.autoresumeplayback;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;
import android.widget.Toast;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.audiofx.Visualizer;
import android.net.Uri;
import java.util.Date;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.text.ParseException;

public class PlaybackService extends Service implements Visualizer.OnDataCaptureListener{
    private static SharedPreferences p;
    private Visualizer v;
    private int c=0;

    @Nullable @Override public IBinder onBind(Intent intent){return null;}

    @Override public int onStartCommand(Intent intent,int flags,int startId){
        p=this.getSharedPreferences("pl.edu.dblach.AutoResumePlayback.PREFERENCE_FILE_KEY",Context.MODE_PRIVATE);
        v=new Visualizer(0);
        v.setDataCaptureListener(this,50,true,false);
            /*
            capture interval:
            50 = 21 s
            80 = 14 s
            */
        v.setCaptureSize(2);
        v.setEnabled(true);
        return START_STICKY;
    }

    private boolean detectSilence(String s){
        for (int i=1;i<50;i+=2) if (s.charAt(i)!=s.charAt(1)) return false;
        return true;
    }

    public void startPlaying(){
        loadUrl(p.getString("t_url",""));
    }

    public void stopPlaying(){
        loadUrl("dummyurl");
    }

    public void loadUrl(String url){
        Intent i=new Intent();
        i.setAction(Intent.ACTION_VIEW);
        i.setDataAndType(Uri.parse(url),"audio/*");
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    private Boolean playbackDisabledByTimer(){
        Boolean r=false;
        if(p.getString("timer_enabled","").equals("1")){
            Date tStart=new Date();
            Date tStop=new Date();
            Date tNow=new Date();
            Date tmp=Calendar.getInstance().getTime();
            SimpleDateFormat f=new SimpleDateFormat("HH:mm");
            try{tStart=f.parse(p.getString("tpStart",""));} catch(ParseException e){}
            try{tStop=f.parse(p.getString("tpStop",""));} catch(ParseException e){}
            try{tNow=f.parse(new SimpleDateFormat("HH").format(tmp)+":"+new SimpleDateFormat("mm").format(tmp));} catch(ParseException e){}
            if(tNow.after(tStart) && tNow.before(tStop)) r=true;
            //todo : fix intervals ie 21:00-00:00
        }
        return r;
    }

    @Override public void onDestroy(){
        v.setEnabled(false);
        v.setDataCaptureListener(null,0,false,false);
        v.release();
        super.onDestroy();
    }

    @Override public void onWaveFormDataCapture(Visualizer thisVisualiser, byte[] waveform, int samplingRate) {
        String d=new String(waveform);
        if(detectSilence(d)){
            c+=1;
            if(c>3){
                c=0;
                if(!playbackDisabledByTimer()){
                    Toast.makeText(getApplicationContext(),"Restarting playback",Toast.LENGTH_SHORT).show();
                    startPlaying();
                }
            }
        }
        else{
            c=0;
            if(playbackDisabledByTimer()) stopPlaying();
        }
    }

    @Override public void onFftDataCapture(Visualizer thisVisualiser, byte[] fft, int samplingRate){}
}
