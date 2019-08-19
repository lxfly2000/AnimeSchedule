package com.lxfly2000.animeschedule;

import android.content.Context;
import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;
import com.lxfly2000.utilities.AndroidDownloadFileTask;
import com.lxfly2000.utilities.StreamUtility;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLConnection;

import static org.junit.Assert.*;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class DownloadFileTest {
    String localAnimeJS,downloadJS;
    @Test
    public void DownloadFile() throws Exception{
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        localAnimeJS= StreamUtility.GetStringFromStream(appContext.getResources().openRawResource(R.raw.anime));
        AndroidDownloadFileTask task=new AndroidDownloadFileTask() {
            @Override
            public void OnReturnStream(ByteArrayInputStream stream, boolean success, int response, Object extra, URLConnection connection) {
                try{
                    downloadJS=StreamUtility.GetStringFromStream(stream);
                }catch (IOException e){
                    fail(e.getLocalizedMessage());
                }
            }
        };
        task.execute(Values.urlAuthor+"/raw/master/app/src/main/res/raw/anime.js");
        assertTrue(task.get());
        assertEquals(new JSONObject(localAnimeJS.substring(localAnimeJS.indexOf('(')+1,localAnimeJS.lastIndexOf(')'))).toString(),
                new JSONObject(downloadJS.substring(downloadJS.indexOf('(')+1,downloadJS.lastIndexOf(')'))).toString());
    }
}
