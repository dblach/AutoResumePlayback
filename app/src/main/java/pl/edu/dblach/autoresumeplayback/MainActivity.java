package pl.edu.dblach.autoresumeplayback;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.widget.TextView;
import android.widget.CheckBox;
import android.net.Uri;
import android.view.View;
//import android.media.audiofx.Visualizer;
import android.content.SharedPreferences;
import android.content.Context;
import android.widget.Toast;
import android.text.TextWatcher;
import android.text.Editable;
import android.app.ActivityManager;
//import java.util.Date;
//import java.util.Calendar;
//import java.text.SimpleDateFormat;
//import java.text.ParseException;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;

public class MainActivity extends AppCompatActivity{
//public class MainActivity extends AppCompatActivity implements Visualizer.OnDataCaptureListener{
    public static final int PICKFILE_RESULT_CODE=1;
    //private Visualizer v;
    //private int c=0;
    private TextView label_monitoring;
    private TextView t_filename;
    private TextView t_url;
    private CheckBox cbTime;
    private TextView tpStart;
    private TextView tpStop;
    private SharedPreferences p;

    @Override protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //v=new Visualizer(0);
        p=this.getSharedPreferences("pl.edu.dblach.AutoResumePlayback.PREFERENCE_FILE_KEY",Context.MODE_PRIVATE);
        label_monitoring=(TextView)findViewById(R.id.label_monitoring);
        t_filename=(TextView)findViewById(R.id.label_loadedfile);
        t_url=(TextView)findViewById(R.id.label_url);
        cbTime=(CheckBox)findViewById(R.id.cbTime);
        tpStart=(TextView)findViewById(R.id.tpStart);
        tpStop=(TextView)findViewById(R.id.tpStop);
        //label_monitoring.setText(p.getString("monitoring","OFF"));
        label_monitoring.setText(isServiceRunning());
        t_filename.setText(p.getString("t_filename",""));
        t_url.setText(getStreamUrl(t_filename.getText().toString()));
        if(p.getString("timer_enabled","0").equals("1")) cbTime.setChecked(true);
        tpStart.setText(p.getString("tpStart",""));
        tpStop.setText(p.getString("tpStop",""));
        tpStart.addTextChangedListener(new TextWatcher(){
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            @Override public void afterTextChanged(Editable s){}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count){
                saveSetting("tpStart",s.toString());
            }
        });
        tpStop.addTextChangedListener(new TextWatcher(){
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            @Override public void afterTextChanged(Editable s){}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count){
                saveSetting("tpStop",s.toString());
            }
        });
    }

    public void switchVisualizer(View a){
        if(isServiceRunning().equals("OFF")){
        //if(label_monitoring.getText().toString().equals("OFF")){
            //v.setDataCaptureListener(this,50,true,false);
            /*
            capture interval:
            50 = 21 s
            80 = 14 s
            */
            //v.setCaptureSize(2);
            //v.setEnabled(true);
            //label_monitoring.setText("ON");
            //saveSetting("monitoring","ON");
            startService(new Intent(this,PlaybackService.class));
        }
        else{
            //v.setEnabled(false);
            //v.setDataCaptureListener(null, 0, false, false);
            //v.release();
            //label_monitoring.setText("OFF");
            //saveSetting("monitoring","OFF");
            stopService(new Intent(this,PlaybackService.class));
        }
        label_monitoring.setText(isServiceRunning());
    }

    public void btnOpenClick(View v){
        Intent chooseFile=new Intent(Intent.ACTION_GET_CONTENT);
        chooseFile.setType("*/*");
        chooseFile=Intent.createChooser(chooseFile,"Choose playlist file");
        startActivityForResult(chooseFile,PICKFILE_RESULT_CODE);
    }

    public void btnPlayClick(View v){
        startPlaying();
    }

    public void btnStopClick(View v){
        stopPlaying();
    }

    public void startPlaying(){
        loadUrl(t_url.getText().toString());
    }

    public void stopPlaying(){
        loadUrl("dummyurl");
    }

    public void cbTimeClick(View v){
        if(cbTime.isChecked()) saveSetting("timer_enabled","1");
        else saveSetting("timer_enabled","0");
    }

    public void loadUrl(String url){
        Intent i=new Intent();
        i.setAction(Intent.ACTION_VIEW);
        i.setDataAndType(Uri.parse(url),"audio/*");
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==PICKFILE_RESULT_CODE && resultCode==-1){
            Uri fileUri=data.getData();
            String filePath=fileUri.getPath();
            t_filename.setText(filePath);
            t_url.setText(getStreamUrl(filePath));
            saveSetting("t_filename",filePath);
            saveSetting("t_url",t_url.getText().toString());
        }
    }
/*
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

    private boolean detectSilence(String s){
        for (int i=1;i<50;i+=2) if (s.charAt(i)!=s.charAt(1)) return false;
        return true;
    }
*/
    private void saveSetting(String n,String v){
        SharedPreferences.Editor e=p.edit();
        e.putString(n,v);
        e.commit();
    }

    private String getStreamUrl(String f){
        try{
            BufferedReader b=new BufferedReader(new FileReader(new File(f)));
            String l;
            while((l=b.readLine())!=null){
                int p=l.indexOf("http");
                if(p>0) return l.substring(p);
            }
            b.close();
        }
        catch(Exception e){}
        return "";
    }
/*
    private Boolean playbackDisabledByTimer(){
        Boolean r=false;
        if(cbTime.isChecked()){
            Date tStart=new Date();
            Date tStop=new Date();
            Date tNow=new Date();
            Date tmp=Calendar.getInstance().getTime();
            SimpleDateFormat f=new SimpleDateFormat("HH:mm");
            try{tStart=f.parse(tpStart.getText().toString());} catch(ParseException e){}
            try{tStop=f.parse(tpStop.getText().toString());} catch(ParseException e){}
            try{tNow=f.parse(new SimpleDateFormat("HH").format(tmp)+":"+new SimpleDateFormat("mm").format(tmp));} catch(ParseException e){}
            if(tNow.after(tStart) && tNow.before(tStop)) r=true;
        }
        return r;
    }
*/
    private String isServiceRunning(){
        ActivityManager m=(ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        for(ActivityManager.RunningServiceInfo service:m.getRunningServices(Integer.MAX_VALUE)) if(PlaybackService.class.getName().equals(service.service.getClassName())) return "ON";
        return "OFF";
    }
}