package com.lxfly2000.animeschedule;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import com.lxfly2000.utilities.AndroidDownloadFileTask;
import com.lxfly2000.utilities.AndroidUtility;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class TestAvailabilityActivity extends AppCompatActivity {
    AnimeJson json;
    SharedPreferences preferences;
    ListView listAvailability;
    SimpleAdapter listAdapter;
    ArrayList<Integer>jsonSortTable;
    TextView textBilibiliAvailable,textIQiyiAvailable,textQQVideoAvailable,textYoukuAvailable,textAcFunAvailable,textEtcAvailable,textTotalAvailable;
    TextView textBilibiliUnavailable,textIQiyiUnavailable,textQQVideoUnavailable,textYoukuUnavailable,textAcFunUnavailable,textEtcUnavailable,textTotalUnavailable;
    TextView textBilibiliTotal,textIQiyiTotal,textQQVideoTotal,textYoukuTotal,textAcFunTotal,textEtcTotal,textTotalTotal;
    int countBilibiliAvailable,countIQiyiAvailable,countQQVideoAvailable,countYoukuAvailable,countAcFunAvailable,countEtcAvailable;
    int countBilibiliUnavailable,countIQiyiUnavailable,countQQVideoUnavailable,countYoukuUnavailable,countAcFunUnavailable,countEtcUnavailable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_availability);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        preferences=Values.GetPreference(this);

        listAvailability=findViewById(R.id.listAvailability);
        textBilibiliAvailable=findViewById(R.id.textAvailabilityBilibiliTrue);
        textIQiyiAvailable=findViewById(R.id.textAvailabilityIQiyiTrue);
        textQQVideoAvailable=findViewById(R.id.textAvailabilityQQVideoTrue);
        textYoukuAvailable=findViewById(R.id.textAvailabilityYoukuTrue);
        textAcFunAvailable=findViewById(R.id.textAvailabilityAcFunTrue);
        textEtcAvailable=findViewById(R.id.textAvailabilityEtcTrue);
        textTotalAvailable=findViewById(R.id.textAvailabilityTotalTrue);
        textBilibiliUnavailable=findViewById(R.id.textAvailabilityBilibiliFalse);
        textIQiyiUnavailable=findViewById(R.id.textAvailabilityIQiyiFalse);
        textQQVideoUnavailable=findViewById(R.id.textAvailabilityQQVideoFalse);
        textYoukuUnavailable=findViewById(R.id.textAvailabilityYoukuFalse);
        textAcFunUnavailable=findViewById(R.id.textAvailabilityAcFunFalse);
        textEtcUnavailable=findViewById(R.id.textAvailabilityEtcFalse);
        textTotalUnavailable=findViewById(R.id.textAvailabilityTotalFalse);
        textBilibiliTotal=findViewById(R.id.textAvailabilityBilibiliTotal);
        textIQiyiTotal=findViewById(R.id.textAvailabilityIQiyiTotal);
        textQQVideoTotal=findViewById(R.id.textAvailabilityQQVideoTotal);
        textYoukuTotal=findViewById(R.id.textAvailabilityYoukuTotal);
        textAcFunTotal=findViewById(R.id.textAvailabilityAcFunTotal);
        textEtcTotal=findViewById(R.id.textAvailabilityEtcTotal);
        textTotalTotal=findViewById(R.id.textAvailabilityTotalTotal);

        listAdapter=new SimpleAdapter(this,filteredResults,R.layout.item_anime_availability,
                new String[]{FilteredResult.keyTitle,FilteredResult.keyWatchUrl,FilteredResult.keyResponse},
                new int[]{R.id.textAvailabilityTitle,R.id.textAvailabilityURL,R.id.textAvailabilityStatus});
        listAdapter.setViewBinder(testResultViewBinder);
        listAvailability.setAdapter(listAdapter);
        listAvailability.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                AndroidUtility.OpenUri(getBaseContext(),testResults.get(filteredResults.get(i).get(FilteredResult.keyWatchUrl)).GetWatchUrl());
            }
        });

        String src=getIntent().getStringExtra("src");
        if(src==null)
            src=Values.GetJsonDataFullPath();
        json=new AnimeJson(src);
        jsonSortTable=new ArrayList<>();
        for(int i=0;i<json.GetAnimeCount();i++)
            jsonSortTable.add(i);
    }

    @Override
    protected void onDestroy() {
        ClearResults(true);
        super.onDestroy();
    }

    MenuItem menuItemRetest,menuItemStop,menuItemStart,menuItemHelp;
    MenuItem menuItemBilibili,menuItemIQiyi,menuItemQQVideo,menuItemYouku,menuItemAcFun,menuItemEtc;
    MenuItem menuItemShowFollowing,menuItemShowAbandoned,menuItemShowAvailable,menuItemShowUnavailable;

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_test_availability,menu);

        menuItemRetest=menu.findItem(R.id.action_retest);
        menuItemStop=menu.findItem(R.id.action_stop);
        menuItemStart=menu.findItem(R.id.action_start);
        menuItemHelp=menu.findItem(R.id.action_help);
        menuItemBilibili=menu.findItem(R.id.action_show_bilibili);
        menuItemIQiyi=menu.findItem(R.id.action_show_iqiyi);
        menuItemQQVideo=menu.findItem(R.id.action_show_qqvideo);
        menuItemYouku=menu.findItem(R.id.action_show_youku);
        menuItemAcFun=menu.findItem(R.id.action_show_acfun);
        menuItemEtc=menu.findItem(R.id.action_show_etc);
        menuItemShowFollowing=menu.findItem(R.id.action_show_following);
        menuItemShowAbandoned=menu.findItem(R.id.action_show_abandoned);
        menuItemShowAvailable=menu.findItem(R.id.action_show_available);
        menuItemShowUnavailable=menu.findItem(R.id.action_show_unavailable);

        menuItemRetest.getIcon().setTint(Color.WHITE);
        menuItemStop.getIcon().setTint(Color.WHITE);
        menuItemStart.getIcon().setTint(Color.WHITE);
        menuItemBilibili.setChecked(true);
        menuItemIQiyi.setChecked(true);
        menuItemQQVideo.setChecked(true);
        menuItemYouku.setChecked(true);
        menuItemAcFun.setChecked(true);
        menuItemEtc.setChecked(true);
        menuItemShowFollowing.setChecked(true);
        menuItemShowAbandoned.setChecked(true);
        menuItemShowAvailable.setChecked(true);
        menuItemShowUnavailable.setChecked(true);

        ClearResults(false);
        return true;
    }

    AndroidDownloadFileTask currentTask;
    int runningStatus=0;//0=Stop, 1=Running, 2=Paused
    int currentIndex=0;

    private void TestAvailability(int index){
        currentIndex=index;
        if(currentIndex>=json.GetAnimeCount()){
            OnFinishedTest();
            return;
        }
        setTitle(String.format("%d/%d %s...",currentIndex+1,json.GetAnimeCount(),json.GetTitle(currentIndex)));
        currentTask=new AndroidDownloadFileTask() {
            @Override
            public void OnReturnStream(ByteArrayInputStream stream, boolean success, int response, Object extra,Object additionalReturned) {
                AddResult((int)extra,response);
                if(runningStatus==1)
                    TestAvailability(index+1);
            }
        };
        currentTask.SetDownloadFile(false);
        currentTask.SetExtra(jsonSortTable.get(currentIndex));
        currentTask.SetConnectTimeOut(preferences.getInt(getString(R.string.key_test_connection_timeout),Values.vDefaultTestConnectionTimeout));
        currentTask.SetReadTimeOut(preferences.getInt(getString(R.string.key_test_read_timeout),Values.vDefaultTestReadTimeout));
        currentTask.execute(json.GetWatchUrl(jsonSortTable.get(currentIndex)));
    }

    class TestResult{
        int response;
        int jsonIndex;
        public String GetTitle(){
            return json.GetTitle(jsonIndex);
        }
        public String GetWatchUrl(){
            return json.GetWatchUrl(jsonIndex);
        }
        public boolean GetAbandoned(){
            return json.GetAbandoned(jsonIndex);
        }
        public TestResult(int index,int _response){
            jsonIndex=index;
            response=_response;
        }
        public boolean IsAvailable(){
            return response/100==2;
        }
    }

    ArrayList<TestResult>testResults=new ArrayList<>();

    class FilteredResult extends HashMap<String,Integer>{
        static final String keyTitle="title";
        static final String keyWatchUrl="url";
        static final String keyResponse="code";
        public FilteredResult(int index){
            put(keyTitle,index);
            put(keyWatchUrl,index);
            put(keyResponse,index);
        }
    }

    ArrayList<FilteredResult>filteredResults=new ArrayList<>();

    private void OnFinishedTest(){
        StopTest();
        setTitle(getString(R.string.title_test_availability_result,
                countBilibiliAvailable+countIQiyiAvailable+countQQVideoAvailable+countYoukuAvailable+countEtcAvailable,
                countBilibiliUnavailable+countIQiyiUnavailable+countQQVideoUnavailable+countYoukuUnavailable+countEtcUnavailable));
        menuItemStart.setVisible(false);
        runningStatus=0;
    }

    private void StartTest(){
        menuItemRetest.setVisible(false);
        menuItemStop.setVisible(true);
        menuItemStart.setVisible(false);
        runningStatus=1;
        TestAvailability(currentIndex);
    }

    private void StopTest(){
        menuItemRetest.setVisible(true);
        menuItemStop.setVisible(false);
        menuItemStart.setVisible(true);
        if(currentTask!=null)
            currentTask.cancel(true);
        runningStatus=2;
    }

    private void ClearResults(boolean stopTest){
        if(stopTest)
            StopTest();
        menuItemRetest.setVisible(false);
        //Test: com.lxfly2000.javatest.TestArrayList.main
        testResults.clear();
        FilteredDataClear();
        NotifyUpdateDataView();
    }

    private void ClearAndRetest(){
        ClearResults(true);
        currentIndex=0;
        StartTest();
    }

    private void NotifyUpdateDataView(){
        listAdapter.notifyDataSetChanged();
    }

    private boolean IsDataAcceptableByFilter(TestResult result){
        String url=result.GetWatchUrl();
        if(result.IsAvailable()&&!menuItemShowAvailable.isChecked())
            return false;
        else if(!result.IsAvailable()&&!menuItemShowUnavailable.isChecked())
            return false;
        if(result.GetAbandoned()&&!menuItemShowAbandoned.isChecked())
            return false;
        else if(!result.GetAbandoned()&&!menuItemShowFollowing.isChecked())
            return false;
        if((URLUtility.IsBilibiliVideoLink(url)||URLUtility.IsBilibiliBangumiLink(url))&&!menuItemBilibili.isChecked())
            return false;
        else if(URLUtility.IsIQiyiLink(url)&&!menuItemIQiyi.isChecked())
            return false;
        else if(URLUtility.IsQQVideoLink(url)&&!menuItemQQVideo.isChecked())
            return false;
        else if(URLUtility.IsYoukuLink(url)&&!menuItemYouku.isChecked())
            return false;
        else if(URLUtility.IsAcFunLink(url)&&!menuItemAcFun.isChecked())
            return false;
        else if(!(URLUtility.IsBilibiliVideoLink(url)||URLUtility.IsBilibiliBangumiLink(url))&&
                !URLUtility.IsIQiyiLink(url)&&!URLUtility.IsQQVideoLink(url)&&!URLUtility.IsYoukuLink(url)&&
                !menuItemEtc.isChecked())
            return false;
        return true;
    }

    private void UpdateDataFilter(){
        FilteredDataClear();
        for(int i=0;i<testResults.size();i++){
            if(IsDataAcceptableByFilter(testResults.get(i)))
                FilteredDataAdd(new FilteredResult(i));
        }
        NotifyUpdateDataView();
    }

    private void FilteredDataAdd(FilteredResult result){
        filteredResults.add(result);
        boolean available=testResults.get(result.get(FilteredResult.keyResponse)).IsAvailable();
        String url=testResults.get(result.get(FilteredResult.keyWatchUrl)).GetWatchUrl();
        if(URLUtility.IsBilibiliVideoLink(url)||URLUtility.IsBilibiliBangumiLink(url)){
            if(available) {
                countBilibiliAvailable++;
                textBilibiliAvailable.setText(String.valueOf(countBilibiliAvailable));
            }else {
                countBilibiliUnavailable++;
                textBilibiliUnavailable.setText(String.valueOf(countBilibiliUnavailable));
            }
            textBilibiliTotal.setText(String.valueOf(countBilibiliAvailable+countBilibiliUnavailable));
        }else if(URLUtility.IsIQiyiLink(url)){
            if(available) {
                countIQiyiAvailable++;
                textIQiyiAvailable.setText(String.valueOf(countIQiyiAvailable));
            }else {
                countIQiyiUnavailable++;
                textIQiyiUnavailable.setText(String.valueOf(countIQiyiUnavailable));
            }
            textIQiyiTotal.setText(String.valueOf(countIQiyiAvailable+countIQiyiUnavailable));
        }else if(URLUtility.IsQQVideoLink(url)){
            if(available) {
                countQQVideoAvailable++;
                textQQVideoAvailable.setText(String.valueOf(countQQVideoAvailable));
            }else {
                countQQVideoUnavailable++;
                textQQVideoUnavailable.setText(String.valueOf(countQQVideoUnavailable));
            }
            textQQVideoTotal.setText(String.valueOf(countQQVideoAvailable+countQQVideoUnavailable));
        }else if(URLUtility.IsYoukuLink(url)) {
            if (available) {
                countYoukuAvailable++;
                textYoukuAvailable.setText(String.valueOf(countYoukuAvailable));
            } else {
                countYoukuUnavailable++;
                textYoukuUnavailable.setText(String.valueOf(countYoukuUnavailable));
            }
            textYoukuTotal.setText(String.valueOf(countYoukuAvailable + countYoukuUnavailable));
        }else if(URLUtility.IsAcFunLink(url)){
            if(available){
                countAcFunAvailable++;
                textAcFunAvailable.setText(String.valueOf(countAcFunAvailable));
            }else{
                countAcFunUnavailable++;
                textAcFunUnavailable.setText(String.valueOf(countAcFunUnavailable));
            }
            textAcFunTotal.setText(String.valueOf(countAcFunAvailable+countAcFunUnavailable));
        }else {
            if(available) {
                countEtcAvailable++;
                textEtcAvailable.setText(String.valueOf(countEtcAvailable));
            }else {
                countEtcUnavailable++;
                textEtcUnavailable.setText(String.valueOf(countEtcUnavailable));
            }
            textEtcTotal.setText(String.valueOf(countEtcAvailable+countEtcUnavailable));
        }
        int totalAvailable=countBilibiliAvailable+countIQiyiAvailable+countQQVideoAvailable+countYoukuAvailable+countAcFunAvailable+countEtcAvailable;
        int totalUnavailable=countBilibiliUnavailable+countIQiyiUnavailable+countQQVideoUnavailable+countYoukuUnavailable+countAcFunUnavailable+countEtcUnavailable;
        if(available)
            textTotalAvailable.setText(String.valueOf(totalAvailable));
        else
            textTotalUnavailable.setText(String.valueOf(totalUnavailable));
        textTotalTotal.setText(String.valueOf(totalAvailable+totalUnavailable));
    }

    private void FilteredDataClear(){
        filteredResults.clear();
        textBilibiliAvailable.setText(String.valueOf(countBilibiliAvailable=0));
        textIQiyiAvailable.setText(String.valueOf(countIQiyiAvailable=0));
        textQQVideoAvailable.setText(String.valueOf(countQQVideoAvailable=0));
        textYoukuAvailable.setText(String.valueOf(countYoukuAvailable=0));
        textAcFunAvailable.setText(String.valueOf(countAcFunAvailable=0));
        textEtcAvailable.setText(String.valueOf(countEtcAvailable=0));
        int totalAvailable=countBilibiliAvailable+countIQiyiAvailable+countQQVideoAvailable+countYoukuAvailable+countAcFunAvailable+countEtcAvailable;
        textTotalAvailable.setText(String.valueOf(totalAvailable));
        textBilibiliUnavailable.setText(String.valueOf(countBilibiliUnavailable=0));
        textIQiyiUnavailable.setText(String.valueOf(countIQiyiUnavailable=0));
        textQQVideoUnavailable.setText(String.valueOf(countQQVideoUnavailable=0));
        textYoukuUnavailable.setText(String.valueOf(countYoukuUnavailable=0));
        textAcFunUnavailable.setText(String.valueOf(countAcFunUnavailable=0));
        textEtcUnavailable.setText(String.valueOf(countEtcUnavailable=0));
        int totalUnavailable=countBilibiliUnavailable+countIQiyiUnavailable+countQQVideoUnavailable+countYoukuUnavailable+countAcFunUnavailable+countEtcUnavailable;
        textTotalUnavailable.setText(String.valueOf(totalUnavailable));
        textBilibiliTotal.setText(String.valueOf(countBilibiliAvailable+countBilibiliUnavailable));
        textIQiyiTotal.setText(String.valueOf(countIQiyiAvailable+countIQiyiUnavailable));
        textQQVideoTotal.setText(String.valueOf(countQQVideoAvailable+countQQVideoUnavailable));
        textYoukuTotal.setText(String.valueOf(countYoukuAvailable+countYoukuUnavailable));
        textAcFunTotal.setText(String.valueOf(countAcFunAvailable+countAcFunUnavailable));
        textEtcTotal.setText(String.valueOf(countEtcAvailable+countEtcUnavailable));
        textTotalTotal.setText(String.valueOf(totalAvailable+totalUnavailable));
    }

    private void AddResult(int index,int response){
        //注意此处的index已是JSON的索引，无需再用jsonSortTable.
        TestResult result=new TestResult(index,response);
        testResults.add(result);
        if(IsDataAcceptableByFilter(result)) {
            FilteredDataAdd(new FilteredResult(testResults.size()-1));
            NotifyUpdateDataView();
            listAvailability.setSelection(filteredResults.size()-1);
        }
    }

    SimpleAdapter.ViewBinder testResultViewBinder=((view, o, s) -> {
        switch (view.getId()){
            case R.id.textAvailabilityTitle:
                ((TextView)view).setText(testResults.get((int)o).GetTitle());
                break;
            case R.id.textAvailabilityURL:
                ((TextView)view).setText(testResults.get((int)o).GetWatchUrl());
                break;
            case R.id.textAvailabilityStatus:
                ((TextView)view).setText(String.valueOf(testResults.get((int)o).response));
                switch (testResults.get((int)o).response/100){
                    default:((TextView)view).setTextColor(getResources().getColor(R.color.colorHttpTimeOut));break;
                    case 1:((TextView)view).setTextColor(getResources().getColor(R.color.colorHttpNotify));break;
                    case 2:((TextView)view).setTextColor(getResources().getColor(R.color.colorHttpOk));break;
                    case 3:((TextView)view).setTextColor(getResources().getColor(R.color.colorHttpRedirect));break;
                    case 4:((TextView)view).setTextColor(getResources().getColor(R.color.colorHttpClient));break;
                    case 5:((TextView)view).setTextColor(getResources().getColor(R.color.colorHttpServer));break;
                }
                break;
        }
        return true;
    });

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.action_show_bilibili:
                menuItemBilibili.setChecked(!menuItemBilibili.isChecked());
                break;
            case R.id.action_show_iqiyi:
                menuItemIQiyi.setChecked(!menuItemIQiyi.isChecked());
                break;
            case R.id.action_show_qqvideo:
                menuItemQQVideo.setChecked(!menuItemQQVideo.isChecked());
                break;
            case R.id.action_show_youku:
                menuItemYouku.setChecked(!menuItemYouku.isChecked());
                break;
            case R.id.action_show_acfun:
                menuItemAcFun.setChecked(!menuItemAcFun.isChecked());
                break;
            case R.id.action_show_etc:
                menuItemEtc.setChecked(!menuItemEtc.isChecked());
                break;
            case R.id.action_show_following:
                menuItemShowFollowing.setChecked(!menuItemShowFollowing.isChecked());
                break;
            case R.id.action_show_abandoned:
                menuItemShowAbandoned.setChecked(!menuItemShowAbandoned.isChecked());
                break;
            case R.id.action_show_available:
                menuItemShowAvailable.setChecked(!menuItemShowAvailable.isChecked());
                break;
            case R.id.action_show_unavailable:
                menuItemShowUnavailable.setChecked(!menuItemShowUnavailable.isChecked());
                break;
            case R.id.action_retest:
                ClearAndRetest();
                return true;
            case R.id.action_start:
                StartTest();
                return true;
            case R.id.action_stop:
                StopTest();
                return true;
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_help:
                ShowHelp();
                return true;
        }
        UpdateDataFilter();
        return super.onOptionsItemSelected(item);
    }

    private void ShowHelp(){
        AlertDialog dlg=new AlertDialog.Builder(this)
                .setTitle(R.string.help)
                .setPositiveButton(android.R.string.ok,null)
                .setNeutralButton(R.string.button_show_more_help, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        AndroidUtility.OpenUri(getBaseContext(),httpResponseIntroductionURL);
                    }
                })
                .setMessage(R.string.message_http_response_help)
                .show();
    }

    static final String httpResponseIntroductionURL="https://developer.mozilla.org/docs/Web/HTTP/Status";
}
