package com.lxfly2000.animeschedule;

import android.content.ActivityNotFoundException;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.lxfly2000.utilities.AndroidUtility;

public class AboutActivity extends AppCompatActivity {
    private void SetTextViewWithURL(TextView textView,String url){
        if(url!=null)
            textView.setText(Html.fromHtml(String.format("<a href=\"%s\">%s</a>",url,textView.getText())));
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private String DecodeBase64(String encodedString){
        return new String(Base64.decode(encodedString,Base64.DEFAULT));
    }

    private View.OnClickListener buttonClickListener= view -> {
        try {
            switch (view.getId()) {
                case R.id.buttonDonateAlipay:AndroidUtility.OpenUri(getBaseContext(), DecodeBase64(Values.urlDonateAlipayBase64));break;
                case R.id.buttonDonateQQ:AndroidUtility.OpenUri(getBaseContext(), DecodeBase64(Values.urlDonateQQBase64));break;
                case R.id.buttonDonateWechat:AndroidUtility.OpenUri(getBaseContext(), DecodeBase64(Values.urlDonateWechatBase64));break;
                case R.id.buttonDonatePaypal:AndroidUtility.OpenUri(getBaseContext(), DecodeBase64(Values.urlDonatePaypalBase64));break;
                case R.id.buttonContactQQ:AndroidUtility.OpenUri(getBaseContext(), DecodeBase64(Values.urlContactQQBase64));break;
                case R.id.buttonContactTwitter:AndroidUtility.OpenUri(getBaseContext(), Values.urlContactTwitter);break;
                case R.id.buttonContactEmail:AndroidUtility.OpenUri(getBaseContext(), DecodeBase64(Values.urlAuthorEmailBase64));break;
            }
        }catch (ActivityNotFoundException e){
            Toast.makeText(getBaseContext(),getString(R.string.message_exception_no_activity_available)+
                    "\n"+e.getLocalizedMessage(),Toast.LENGTH_LONG).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ((TextView)findViewById(R.id.textVersionInfo)).setText(getString(R.string.label_version_info,BuildConfig.VERSION_NAME,BuildConfig.BUILD_DATE));
        SetTextViewWithURL(findViewById(R.id.textViewReportBug),Values.urlReportIssue);
        SetTextViewWithURL(findViewById(R.id.textViewGotoGithub),Values.urlAuthor);
        SetTextViewWithURL(findViewById(R.id.textViewMadeBy),Values.urlAuthorGithubHome);
        SetTextViewWithURL(findViewById(R.id.textViewThanksXiaoyaocz),null);
        SetTextViewWithURL(findViewById(R.id.textViewThanksNl),null);
        SetTextViewWithURL(findViewById(R.id.textViewUsingFilepicker),null);
        findViewById(R.id.buttonDonateQQ).setOnClickListener(buttonClickListener);
        findViewById(R.id.buttonDonateAlipay).setOnClickListener(buttonClickListener);
        findViewById(R.id.buttonDonateWechat).setOnClickListener(buttonClickListener);
        findViewById(R.id.buttonDonatePaypal).setOnClickListener(buttonClickListener);
        findViewById(R.id.buttonContactQQ).setOnClickListener(buttonClickListener);
        findViewById(R.id.buttonContactTwitter).setOnClickListener(buttonClickListener);
        findViewById(R.id.buttonContactEmail).setOnClickListener(buttonClickListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_about,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_check_update:
                new UpdateChecker(this).CheckForUpdate(false);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
