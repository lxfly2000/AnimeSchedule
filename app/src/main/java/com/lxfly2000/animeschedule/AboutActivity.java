package com.lxfly2000.animeschedule;

import android.content.ActivityNotFoundException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.lxfly2000.utilities.AndroidUtility;

public class AboutActivity extends AppCompatActivity {
    private void SetTextViewWithURL(TextView textView,String url){
        textView.setText(Html.fromHtml(String.format("<a href=\"%s\">%s</a>",url,textView.getText())));
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private String DecodeBase64(String encodedString){
        return new String(Base64.decode(encodedString,Base64.DEFAULT));
    }

    View.OnClickListener buttonClickListener=new View.OnClickListener() {
        @Override
        public void onClick(View view) {
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
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ((TextView)findViewById(R.id.textVersionInfo)).setText(getString(R.string.label_version_info,BuildConfig.VERSION_NAME,BuildConfig.BUILD_DATE));
        SetTextViewWithURL((TextView)findViewById(R.id.textViewReportBug),Values.urlReportIssue);
        SetTextViewWithURL((TextView)findViewById(R.id.textViewGotoGithub),Values.urlAuthor);
        SetTextViewWithURL((TextView)findViewById(R.id.textViewMadeBy),Values.urlAuthorGithubHome);
        ((Button)findViewById(R.id.buttonDonateQQ)).setOnClickListener(buttonClickListener);
        ((Button)findViewById(R.id.buttonDonateAlipay)).setOnClickListener(buttonClickListener);
        ((Button)findViewById(R.id.buttonDonateWechat)).setOnClickListener(buttonClickListener);
        ((Button)findViewById(R.id.buttonDonatePaypal)).setOnClickListener(buttonClickListener);
        ((Button)findViewById(R.id.buttonContactQQ)).setOnClickListener(buttonClickListener);
        ((Button)findViewById(R.id.buttonContactTwitter)).setOnClickListener(buttonClickListener);
        ((Button)findViewById(R.id.buttonContactEmail)).setOnClickListener(buttonClickListener);
    }
}
