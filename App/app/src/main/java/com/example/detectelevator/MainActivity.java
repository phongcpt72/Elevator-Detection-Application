package com.example.detectelevator;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;

import smile.math.distance.DynamicTimeWarping;
import smile.regression.LinearModel;

//import com.chaquo.python.PyObject;
//import com.chaquo.python.Python;
//import com.chaquo.python.android.AndroidPlatform;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import com.example.detectelevator.LiRe;
import com.example.detectelevator.DTW;

public class MainActivity extends AppCompatActivity implements SensorEventListener {


    public Button startButton;
    public Button stopButton;
    public Button touchButton;
    public TextView Time, MovingTime, Go, Des, DetailedDes, RmsAcc, PValue, Notiinfo;

    public SensorManager mSensorManager;
    public Sensor mPressure;
    public Sensor mAccelerometer;
    public Sensor mMagnetic;
    public Sensor mGravity;

    public int Rate = 40000; //25hz
    public long starttime;
    public double rmsacc, rmsacclpf,valuetmp;
    public double pacc;

    public int i,j ,noti, value= 0, detect = 0, desno = 0;
    public int windowtimeNP = 50;
    public int windowtimeN8P= 150;
    public double threshold = 0.12;
    public int ovelap = 25;
    public boolean flag = true;
    public boolean checkelevator = false;
    public boolean totalelevator = false;
    public boolean flagonetime   = false;

    public int countcheck = 0;
    public int checklocation1st;

    public int level;
    public String des;
    public Double timemaxpoint,timeminpoint,minpoint,maxpoint, beginrealtime, endrealtime;
    public Double begintimeP, endtimeP;

    public List<Double> tmprmsaccnolimit = new ArrayList<Double>();
    public List<Integer> elevatorarr = new ArrayList<Integer>();
    public List<Double> tmp2s = new ArrayList<Double>();
    public List<Double> tmpp = new ArrayList<Double>();
    public List<Integer> tmpid = new ArrayList<Integer>();
    public List<Integer> tmpidnolimit = new ArrayList<Integer>();
    public List<Double> tmp8s = new ArrayList<Double>();
    public List<Double> tmptimestampnolimit = new ArrayList<Double>();

    public List<Integer> movingindex = new ArrayList<Integer>();
    public List<Double> maxzarr = new ArrayList<Double>();
    public List<Double> minzarr = new ArrayList<Double>();
    public List<Double> maxminzarr = new ArrayList<Double>();
    //public List startz = new ArrayList<>();
    //public List endz = new ArrayList<>();
    public List<Double> startz = new ArrayList<Double>();
    public List<Double> endz = new ArrayList<Double>();

    public List<Double> AxLPFlist = new ArrayList<Double>();
    public List<Double> AyLPFlist = new ArrayList<Double>();
    public List<Double> AzLPFlist = new ArrayList<Double>();
    public List<Double> RMSAccLPFlist = new ArrayList<Double>();
    public List<Double> RMSAccLPFlistorder2 = new ArrayList<Double>();
    public List<Double> PLPFlist = new ArrayList<Double>();
    public List<Double> PLPFlistorder2 = new ArrayList<Double>();
    public List<Integer> indexes = new ArrayList<Integer>();
    public List<Integer> indexesreverse = new ArrayList<Integer>();
    private float[] gravityValues = null;
    private float[] magneticValues = null;
    static final float ALPHA = 0.06f;
    static final float ALPHAP = 0.06f;
    public double Ax,Ay,Az,P,AxLPF, AyLPF, AzLPF,PLPF;
    public int iforacc;
    public int iforp;

    public String result;
    public double countime;
    public double countimess;
    public int countelevator =0;
    public int sum=0;
    public double timeendelevator=0;

    public static final String FileName = "Sensors.csv";
    public StringBuilder dataBuffer;
    public static final int REC_STARTED = 1;
    public static final int REC_STOPPED = 2;
    public int status;
    public String delim = ",";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkAndRequestPermissions();
        isExternalStorageReadable();

        startButton = (Button) findViewById(R.id.startButton);
        stopButton  = (Button) findViewById(R.id.stopButton);
        touchButton = (Button) findViewById(R.id.touchButton);

        Time = (TextView) findViewById(R.id.Time);
        MovingTime = (TextView) findViewById(R.id.MovingTime);
        Go = (TextView) findViewById(R.id.Go);
        Des = (TextView) findViewById(R.id.Des);
        DetailedDes = (TextView) findViewById(R.id.DetailedDes);
        RmsAcc = (TextView) findViewById(R.id.rmsacc);
        PValue = (TextView) findViewById(R.id.pvalue);
        Notiinfo = (TextView) findViewById(R.id.notinfo);

        RmsAcc.setText("0");
        PValue.setText("0");

        indexesreverse = createoverlap(indexesreverse);
        Collections.reverse(indexesreverse);
        indexes = createoverlap(indexes);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mPressure = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        mMagnetic = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mGravity  = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

        mSensorManager.registerListener(this, mAccelerometer, Rate);
        mSensorManager.registerListener(this, mPressure, Rate);
        mSensorManager.registerListener(this, mMagnetic, Rate);
        mSensorManager.registerListener(this, mGravity, Rate);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dataBuffer = new StringBuilder();
                starttime = System.currentTimeMillis();
                status = REC_STARTED;
                clearList();
                countime = 0;
                iforacc = 0;
                iforp = 0;
                sum = 0;
                countelevator =0;
                i=0;
                countcheck = 0;
                flagonetime = false;
                String col = "Timestamp" + delim + "Ax" + delim + "Ay" +delim + "Az" + delim +"P"
                        +delim + "LPFRMSAcc"+ delim + "LPFPValue"+ delim+"Value"+delim +"Detection"+delim+"Type"+"\n";
                dataBuffer.append(col);

                //RMSAcc.setText("Nothing");
            }
        });
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (status != REC_STARTED) {
                    return;
                }
                status = REC_STOPPED;
                writeFile();
                String msg = "Saved file";
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();

            }
        });
        touchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                noti++;
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(noti == 1){
                            Toast.makeText(MainActivity.this,"Single Click", Toast.LENGTH_SHORT).show();
                            value = 0;
                            Notiinfo.setText("0");
                        }
                        else if (noti == 2)
                        {
                            Toast.makeText(MainActivity.this,"Double Click", Toast.LENGTH_SHORT).show();
                            value = 1;
                            Notiinfo.setText("1");
                        }
                        noti =0;

                    }
                },500);
            }
        });
    }

    protected void onResume() {

        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, Rate);
        mSensorManager.registerListener(this, mPressure, Rate);
        mSensorManager.registerListener(this, mMagnetic, Rate);
        mSensorManager.registerListener(this, mGravity, Rate);
    }
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }
    @Override
    protected void onStop() {
        super.onStop();
    }


    void clearList()
    {
        AxLPFlist.clear();
        AyLPFlist.clear();
        AzLPFlist.clear();
        RMSAccLPFlist.clear();
        PLPFlist.clear();

        tmprmsaccnolimit.clear();
        elevatorarr.clear();
        tmp2s.clear();
        tmpid.clear();
        tmpidnolimit.clear();
        tmp8s.clear();
        tmptimestampnolimit.clear();
        RMSAccLPFlistorder2.clear();
        PLPFlistorder2.clear();
    }

    List<Integer> createoverlap(List<Integer> index)
    {
        for(int i = 0; i<ovelap;i++)
        {
            index.add(i);
        }
        return index;
    }
    int dnf(double n)
    {
        //Log.d("dnf",String.valueOf(n));
        int count = 1;
        int a = 3;
        int b = 7;
        if ( n < 3)
        {
            return count;
        }
        while (true) {
            if (a <= n && n < b) {
                break;
            } else {
                count+= 1;
                a += 3;
                b += 3;
            }
        }
        return count;
    }

    double movingtime (List<Double> rmstmp, List<Double> changetmp)
    {
        minpoint = Collections.min(rmstmp);
        maxpoint = Collections.max(rmstmp);
        for (int i = 0; i < changetmp.size(); i++)
        {
            if (minpoint == rmstmp.get(i))
            {
                timeminpoint = changetmp.get(i);
            }
            else if(maxpoint == rmstmp.get(i))
            {
                timemaxpoint = changetmp.get(i);
            }
        }
        double range = roundTwoDecimals(timemaxpoint - timeminpoint);
        return range;
    }

    double movingtime1(List<Double> rmstmp, List<Double> timetmp, List<Integer> idtmp, Integer checklocation)
    {
        Integer itmp = checklocation - 100;
        List<Double> tmpacc = new ArrayList<Double>();
        List<Double> tmptime = new ArrayList<Double>();
        Log.d("itmp", String.valueOf(itmp));
        Log.d("Size idtmp", String.valueOf(idtmp));

        while( itmp < idtmp.size() )
        {
            tmpacc.add(rmstmp.get(itmp));
            tmptime.add(timetmp.get(itmp));
            itmp+=1;
        }

        minpoint = Collections.min(tmpacc);
        maxpoint = Collections.max(tmpacc);
        for (int i = 0; i < tmpacc.size(); i++)
        {
            if (minpoint == tmpacc.get(i))
            {
                timeminpoint = tmptime.get(i);
            }
            else if (maxpoint == tmpacc.get(i))
            {
                timemaxpoint = tmptime.get(i);
            }
        }
        Log.d("timeminpoint", String.valueOf(timeminpoint));
        Log.d("timemaxpoint", String.valueOf(timemaxpoint));
        double range = roundTwoDecimals(timemaxpoint - timeminpoint);
        return range;
    }

    double elevatorendtime (List<Double> rmstmp, List<Double> timetmp, List<Integer> idtmp, Integer checklocation)
    {
        Integer itmp = checklocation - 100;
        List<Double> tmpacc = new ArrayList<Double>();
        List<Double> tmptime = new ArrayList<Double>();
        Log.d("itmp", String.valueOf(itmp));
        Log.d("Size idtmp", String.valueOf(idtmp));
        while( itmp < idtmp.size() )
        {
            tmpacc.add(rmstmp.get(itmp));
            tmptime.add(timetmp.get(itmp));
            itmp+=1;
        }
        minpoint = Collections.min(tmpacc);
        maxpoint = Collections.max(tmpacc);
        for (int i = 0; i < tmpacc.size(); i++)
        {
            if (minpoint == tmpacc.get(i))
            {
                timeminpoint = tmptime.get(i);
            }
            else if (maxpoint == tmpacc.get(i))
            {
                timemaxpoint = tmptime.get(i);
            }
        }
        if (timeminpoint > timemaxpoint)
        {
            return timeminpoint;
        }
        else {
            return timemaxpoint;
        }
    }

    String FindMinMax(List<Double> rmstmp, List<Double> changetmp)
    {
        minpoint = Collections.min(rmstmp);
        maxpoint = Collections.max(rmstmp);
        for (int i= 0; i < changetmp.size();i++)
        {
            if (minpoint == rmstmp.get(i))
            {
                timeminpoint = changetmp.get(i);
            }
            else if(maxpoint == rmstmp.get(i))
            {
                timemaxpoint = changetmp.get(i);
            }
        }
        double range = roundTwoDecimals(timemaxpoint - timeminpoint);


        MovingTime.setText("Moving time: "+ String.valueOf(Math.abs(range)));
        if (range > 0)
        {
            level = dnf(range);
            des = "Go down " + String.valueOf(level) + " level";
        }
        else
        {
            range = Math.abs(range);
            level = dnf(range);
            des = "Go up " + String.valueOf(level) + " level";
        }
        return des;
    }

    public static double CalMean(List<Double> list)
    {
        int n = 10;
        double firstsum = 0;
        for(int i = 0; i < n; i++) {
            firstsum = list.get(i) + firstsum;
        }
        double lastsum = 0;
        int lastindex = list.size()-1;
        int breakva = lastindex - n ;
        for(int i = lastindex; i > breakva; i--) {
            lastsum = list.get(i) + lastsum;
        }
        double mean = lastsum/10 - firstsum/10;

        return mean;
    }

    void checkelevator(double rmsacc, double pacc, double countimess)
    {
        tmp2s.add(pacc);
        tmpp.add(pacc);

        tmpid.add(i);
        tmpidnolimit.add(i);

        tmptimestampnolimit.add(countimess);
        tmprmsaccnolimit.add(rmsacc);

        tmp8s.add(countimess);
        if (tmp2s.size() == windowtimeNP)
        {

            //Log.d("Tmpid: ", String.valueOf(tmpid));
            double a = CalMean(tmp2s);
            //double a = tmp2s.get(tmp2s.size()-1) - tmp2s.get(0);
            Log.d("Mean: ", String.valueOf(a));
            if (Math.abs(a) > threshold)
            {
                checkelevator = true;
                movingindex.add(tmpid.get(0));
                movingindex.add(tmpid.get(tmpid.size()-1));
                Log.d("Moving index: ", String.valueOf(movingindex));
                if (a < 0)
                {
                    des = " Go up ";
                    //Log.d("Des: ", des);
                    Go.setText("Detect: " + "Go up");
                    detect = 1;
                    desno = 1;
                }
                else if (a > 0)
                {
                    Go.setText("Detect: " + "Go down");
                    des = " Go down ";
                    detect = 1;
                    desno = 2;
                    //Log.d("Des: ", des);
                }
                if (flagonetime == false)
                {
                    Log.d("tmpid", String.valueOf(tmpid.get(0)));
                    checklocation1st = tmpid.get(0);
                    flagonetime = true;
                    Log.d("flagonetime", String.valueOf(flagonetime));
                }
            }
            else if (tmp8s.size() == windowtimeN8P && checkelevator == false)
            {

                //Log.d("time", String.valueOf(tmp8s.size()));
                Des.setText("Des: " + "Walking");
                Go.setText("Detect: ");
                DetailedDes.setText("Detailed: ");
                MovingTime.setText("Moving time: ");
                double time = tmp8s.get(tmp8s.size()-1) - timeendelevator;
                if (totalelevator == true && time > 30.0 )
                {
                    Log.d("total time",String.valueOf(time));
                    for(int i = 0; i< elevatorarr.size(); i++)
                    {
                        sum+=elevatorarr.get(i);
                    }
                    Log.d("sum",String.valueOf(sum));
                    DetailedDes.setText("Detailed: Total " + des + String.valueOf(sum) + " level");
                    elevatorarr.clear();
                    totalelevator = false;
                    sum = 0;
                }
                tmp8s.clear();
            }
            else if (Math.abs(a) < threshold && checkelevator == true)
            {
                if (countcheck > 3)
                {
                    detect = 0;
                    checkelevator = false;
                    flagonetime = false;

                    if (checklocation1st > 1.0)
                    {
                        int begintime = Collections.min(movingindex);
                        int endtime = Collections.max(movingindex);

                        int beforebegintime = begintime - 50;
                        int afterbegintime = begintime + 50;

                        int beforeendtime = endtime - 50;
                        int afterendtime = endtime + 50;


                        List<Double> ptest = new ArrayList<Double>();
                        List<Integer> idtest = new ArrayList<Integer>();
                        for(int i = 0; i<tmpidnolimit.size();i++) {
                            if (begintime <= tmpidnolimit.get(i) && tmpidnolimit.get(i) <= endtime)
                            {
                                ptest.add(tmpp.get(i));
                                idtest.add(i);
                            }
                        }

                        LiRe lire = new LiRe(ptest,idtest);
                        double slopes = lire.predictValue();
                        //Des.setText(String.valueOf(slopes));

                        if (desno == 1)
                        {
                            for(int i = 0; i < tmpidnolimit.size(); i++) {
                                if (beforebegintime <= tmpidnolimit.get(i) && tmpidnolimit.get(i) <= afterbegintime) {
                                    maxzarr.add(tmprmsaccnolimit.get(i));
                                } else if (beforeendtime <= tmpidnolimit.get(i) && tmpidnolimit.get(i) <= afterendtime) {
                                    minzarr.add(tmprmsaccnolimit.get(i));
                                }
                            }
                            maxminzarr.add(Collections.max(maxzarr));
                            maxminzarr.add(Collections.min(minzarr));
                        }

                        else if (desno == 2)
                        {
                            for(int i = 0; i < tmpidnolimit.size(); i++) {
                                if (beforebegintime <= tmpidnolimit.get(i) && tmpidnolimit.get(i) <= afterbegintime) {
                                    minzarr.add(tmprmsaccnolimit.get(i));
                                } else if (beforeendtime <= tmpidnolimit.get(i) && tmpidnolimit.get(i) <= afterendtime) {
                                    maxzarr.add(tmprmsaccnolimit.get(i));
                                }
                            }
                            maxminzarr.add(Collections.min(minzarr));
                            maxminzarr.add(Collections.max(maxzarr));
                        }

                        for(int i = 0; i < tmprmsaccnolimit.size(); i++)
                        {
                            if (tmprmsaccnolimit.get(i) == maxminzarr.get(0))
                            {
                                beginrealtime = tmptimestampnolimit.get(i);
                            }
                            else if (tmprmsaccnolimit.get(i) == maxminzarr.get(1))
                            {
                                endrealtime = tmptimestampnolimit.get(i);
                            }
                        }
                        for(int i = 0 ; i < tmpidnolimit.size(); i++)
                        {
                            if (tmpidnolimit.get(i) == begintime)
                            {
                                begintimeP = tmptimestampnolimit.get(i);
                            }
                            else if (tmpidnolimit.get(i) == endtime)
                            {
                                endtimeP = tmptimestampnolimit.get(i);
                            }
                        }
                        double rangesss = roundTwoDecimals(endtimeP - begintimeP);
                        Des.setText(String.valueOf(rangesss));

                        //Des.setText("Des"+String.valueOf(beginrealtime)+ " "+ String.valueOf(endrealtime));

                        double beforebeginrealtime = beginrealtime - 1.0;
                        double afterbeginrealtime = beginrealtime + 1.0;

                        double beforeendrealtime = endrealtime - 1.0;
                        double afterendrealtime = endrealtime + 1.52;

                        for (int i = 0; i < tmptimestampnolimit.size();i++)
                        {
                            if (beforebeginrealtime <= tmptimestampnolimit.get(i) && tmptimestampnolimit.get(i) <= afterbeginrealtime)
                            {
                                startz.add(tmprmsaccnolimit.get(i));
                            }
                            else if (beforeendrealtime <= tmptimestampnolimit.get(i) && tmptimestampnolimit.get(i) <= afterendrealtime)
                            {
                                endz.add(tmprmsaccnolimit.get(i));
                            }
                        }
                        DTW dtw = new DTW(startz,endz,desno);
                        List<Double> dtwlist = new ArrayList<Double>();
                        dtwlist = dtw.calculateDTW();
                        MovingTime.setText(String.valueOf(dtwlist.get(0)));


                        double range = roundTwoDecimals(endrealtime - beginrealtime);
                        //double range = movingtime1(tmprmsaccnolimit, tmptimestampnolimit,tmpidnolimit,checklocation1st);
                        //MovingTime.setText("Moving time: " + String.valueOf(Math.abs(range)));
                        level = dnf(Math.abs(range));

                        //Log.d("level", String.valueOf(level));
                        DetailedDes.setText("Detailed: " + des + String.valueOf(level) + " level");
                        elevatorarr.add(level);
                        timeendelevator = elevatorendtime(tmprmsaccnolimit, tmptimestampnolimit, tmpidnolimit,checklocation1st);

                    }
                    totalelevator = true;
                    tmp8s.clear();
                    Log.d("flagonetime1", String.valueOf(flagonetime));
                    countcheck = 0;
                    movingindex.clear();
                    minzarr.clear();
                    maxzarr.clear();
                    maxminzarr.clear();
                }
                countcheck+=1;
            }
            tmp2s = filterreverse(tmp2s,indexesreverse);
            tmpid = filterINT(tmpid,indexesreverse);

            //Log.d("tmpid", String.valueOf(tmpid));


            //Log.d("tmp2s", String.valueOf(tmp2s.size()));
            //Log.d("tmp2soverlap", String.valueOf(tmp2soverlap.size()));

            //Log.d("tmp2s", String.valueOf(tmp2s.size()));
        }
        i+=1;
    }

    private List<Integer> filterint(List<Integer> tmpid, List<Integer> indexes, List<Integer> tmpidoverlap) {
        for(int i = 0; i < indexes.size(); i++)
        {
            tmpidoverlap.add(tmpid.get(i));
        }
        return tmpidoverlap;
    }

    double roundTwoDecimals(double d)
    {
        DecimalFormat twoDForm = new DecimalFormat("#.##");
        return Double.valueOf(twoDForm.format(d));
    }

    double RMS(double x, double y, double z)
    {
        return Math.sqrt(x * x + y * y + z * z);
    }

    double LPF(double value, int i, List<Double> rmstmp, float alpha)
    {
        if (rmstmp.size() == 0)
        {
            valuetmp = alpha * value;
            rmstmp.add(valuetmp);
        }
        else
        {
            valuetmp = alpha * value + (1-alpha) * rmstmp.get(i-1);
            rmstmp.add(valuetmp);
        }
        return valuetmp;
    }
    public static List<Double> filterreverse(List<Double> list, List<Integer> indexesToRemove){
        for (Integer indexToRemove : indexesToRemove) {
            list.remove((int)indexToRemove);
        }
        return list;
    }
    public static List<Double> filter(List<Double> list, List<Integer> indexesToRemove, List<Double> overlap)
    {
        for (int i = 0; i < indexesToRemove.size(); i++)
        {
            overlap.add(list.get(indexesToRemove.get(i)));
        }
        return overlap;
    }
    public static List<Integer> filterINT(List<Integer> list, List<Integer> indexesToRemove){
        for (Integer indexToRemove : indexesToRemove) {
            list.remove((int)indexToRemove);
        }
        return list;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        if (status == REC_STARTED) {

            if ((gravityValues != null) && (magneticValues != null)
                    && (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)) {
                countimess = roundTwoDecimals(countime);
                Time.setText("Real Time: " + String.valueOf(countimess));
                float[] deviceRelativeAcceleration = new float[4];
                deviceRelativeAcceleration[0] = event.values[0];
                deviceRelativeAcceleration[1] = event.values[1];
                deviceRelativeAcceleration[2] = event.values[2];
                deviceRelativeAcceleration[3] = 0;

                // Change the device relative acceleration values to earth relative values
                // X axis -> East
                // Y axis -> North Pole
                // Z axis -> Sky

                float[] R = new float[16], I = new float[16], earthAcc = new float[16];

                SensorManager.getRotationMatrix(R, I, gravityValues, magneticValues);

                float[] inv = new float[16];

                android.opengl.Matrix.invertM(inv, 0, R, 0);
                android.opengl.Matrix.multiplyMV(earthAcc, 0, inv, 0, deviceRelativeAcceleration, 0);
                Log.d("Acceleration", "Values: (" + earthAcc[0] + ", " + earthAcc[1] + ", " + earthAcc[2] + ")");

                float x  = earthAcc[0];
                float y  = earthAcc[1];
                float z  = earthAcc[2];

                rmsacclpf = LPF(z, iforacc,RMSAccLPFlist, ALPHA);
                rmsacclpf = LPF(rmsacclpf, iforacc,RMSAccLPFlistorder2, ALPHA);
                RmsAcc.setText(Double.toString(rmsacclpf));
                iforacc+=1;

            } else if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
                gravityValues = event.values;
            } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                magneticValues = event.values;
            }
            else if(sensor.getType() == Sensor.TYPE_PRESSURE)
            {
                P = event.values[0];
                PLPF = LPF(P, iforp, PLPFlist, ALPHAP);
                PLPF = LPF(PLPF, iforp,PLPFlistorder2, ALPHAP);
                PValue.setText(Double.toString(PLPF));
                //Az.setText(String.valueOf(pacc));
                //Log.d("pacc", String.valueOf(pacc));
                String line = countimess + delim + Ax + delim + Ay + delim + Az +
                        delim + P + delim + rmsacclpf + delim + PLPF + delim + value + delim + detect +"\n";

                double px = Double.parseDouble(PValue.getText().toString());
                double rmsacc = Double.parseDouble(RmsAcc.getText().toString());
                checkelevator(rmsacc, px, countimess);

                //checkelevator(rmsacclpf, PLPF, countimess);
                dataBuffer.append(line);
                countime +=0.04;
                iforp+=1;
            }
        }
    }
    @Override
    public void onAccuracyChanged(Sensor sensor,int accuracy){

    }
    private void writeFile() {

        String state;
        state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state))
        {
            File Root = Environment.getExternalStorageDirectory();
            File Dir = new File(Root.getAbsolutePath());
            if (Dir.exists())
            {
                Dir.mkdir();
            }
            File file = new File(Dir,FileName);
            try {
                file.createNewFile();
                FileOutputStream fos;
                byte[] data = dataBuffer.toString().getBytes();
                fos = new FileOutputStream(file);
                fos.write(data);
                fos.flush();
                fos.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    private void checkAndRequestPermissions() {
        String[] permissions = new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        };
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(permission);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), 1);
        }
    }
}
