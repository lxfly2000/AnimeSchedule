package com.lxfly2000.youget;

import android.content.Context;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.lxfly2000.animeschedule.R;
import com.lxfly2000.animeschedule.Values;
import com.lxfly2000.utilities.AndroidDownloadFileTask;
import com.lxfly2000.utilities.AndroidSysDownload;
import com.lxfly2000.utilities.FileUtility;
import com.lxfly2000.utilities.StreamUtility;
import com.lxfly2000.youget.joiner.Joiner;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public class QQGet extends YouGet {
    private String paramPlayUrl,paramSavePath, fileNameWithoutExt;
    private String htmlString,title;
    private String videoId;
    String cookiePath=Values.GetRepositoryPathOnLocal()+"/cookie_qqvideo.txt";
    public QQGet(@NonNull Context context) {
        super(context);
    }

    @Override
    public void DownloadBangumi(String url, int episodeToDownload_fromZero, int quality, String saveDirPath) {
        paramPlayUrl=url;
        paramSavePath=saveDirPath;
        AndroidDownloadFileTask taskGetContent=new AndroidDownloadFileTask() {
            @Override
            public void OnReturnStream(ByteArrayInputStream stream, boolean success, int response, Object extra, URLConnection connection) {
                if(onFinishFunction ==null)
                    return;
                if(!success){
                    onFinishFunction.OnFinish(false,paramPlayUrl,null,ctx.getString(R.string.message_unable_to_fetch_anime_info));
                    return;
                }
                try{
                    String enc=connection.getHeaderField("Content-Encoding");
                    InputStream iStream=stream;
                    if("gzip".equals(enc))//判断输入流是否是压缩的，并获取压缩算法
                        iStream=new GZIPInputStream(stream);
                    else if("deflate".equals(enc))
                        iStream=new InflaterInputStream(stream,new Inflater(true));
                    htmlString=StreamUtility.GetStringFromStream(iStream,false);
                    String rUrl=Common.Match1(htmlString,"<link.*?rel\\s*=\\s*\"canonical\".*?href\\s*=\"(.+?)\".*?>");
                    videoId="";
                    if(rUrl!=null){
                        String[]sp=rUrl.split("/");
                        videoId=sp[sp.length-1].split("\\.")[0];
                        if(videoId.equals("undefined")||videoId.equals("index"))
                            videoId="";
                    }
                    if(videoId==null||videoId.equals("")){
                        String[]sp=url.split("/");
                        videoId=sp[sp.length-1].split("\\.")[0];
                    }
                    if(videoId==null||videoId.equals(""))
                        videoId=Common.Match1(htmlString,"vid\"*\\s*:\\s*\"\\s*([^\"]+)\"");
                    if(videoId==null||videoId.equals(""))
                        videoId=Common.Match1(htmlString,"id\"*\\s*:\\s*\"(.+?)\"");
                    title=Common.Match1(htmlString,String.format("<a.*?id\\s*=\\s*\"%s\".*?title\\s*=\\s*\"(.+?)\".*?>",videoId));
                    if(title==null||title.equals(""))
                        title=Common.Match1(htmlString,"title\">([^\"]+)</p>");
                    if(title==null||title.equals(""))
                        title=Common.Match1(htmlString,"\"title\":\"([^\"]+)\"");
                    if(title==null||title.equals(""))
                        title=videoId;
                    fileNameWithoutExt=title+" "+(episodeToDownload_fromZero+1);
                    QQDownloadByVid();
                }catch (IOException e){
                    onFinishFunction.OnFinish(false,paramPlayUrl,null,ctx.getString(R.string.message_error_on_reading_stream,e.getLocalizedMessage()));
                }
            }
        };
        Common.SetYouGetHttpHeader(taskGetContent);
        if(FileUtility.IsFileExists(cookiePath))
            taskGetContent.SetCookie(FileUtility.ReadFile(cookiePath));
        taskGetContent.execute(url);
    }

    private void QQDownloadByVid(){
        QQDownloadByVid_forPlatforms(0);
    }

    private static class DownloadParams{
        public String originalName,url,ext,downloadFullPath;
        public int partNumber;
        public boolean downloaded;
    }

    private void QQDownloadByVid_forPlatforms(int i){
        int[]platforms={4100201, 11};
        String infoApi=String.format("http://vv.video.qq.com/getinfo?otype=json&appver=3.2.19.333&platform=%s&defnpayver=1&defn=shd&vid=%s",platforms[i],videoId);
        AndroidDownloadFileTask task=new AndroidDownloadFileTask() {
            @Override
            public void OnReturnStream(ByteArrayInputStream stream, boolean success, int response, Object extra, URLConnection connection) {
                if(onFinishFunction ==null)
                    return;
                if(!success){
                    onFinishFunction.OnFinish(false,paramPlayUrl,null,ctx.getString(R.string.message_unable_to_fetch_anime_info));
                    return;
                }
                try{
                    String enc=connection.getHeaderField("Content-Encoding");
                    InputStream iStream=stream;
                    if("gzip".equals(enc))//判断输入流是否是压缩的，并获取压缩算法
                        iStream=new GZIPInputStream(stream);
                    else if("deflate".equals(enc))
                        iStream=new InflaterInputStream(stream,new Inflater(true));
                    String info=StreamUtility.GetStringFromStream(iStream,false);
                    String matchStr=Common.Match1(info,"QZOutputJson=(.*)");
                    JSONObject videoJson=new JSONObject(matchStr.substring(0,matchStr.length()-1));
                    if(videoJson.has("msg")&&videoJson.getString("msg").equals("cannot play outside")&&i+1<platforms.length){
                        QQDownloadByVid_forPlatforms(i+1);
                        return;
                    }
                    String fnPre=videoJson.getJSONObject("vl").getJSONArray("vi").getJSONObject(0).getString("lnk");
                    title=videoJson.getJSONObject("vl").getJSONArray("vi").getJSONObject(0).getString("ti");
                    String host=videoJson.getJSONObject("vl").getJSONArray("vi").getJSONObject(0).getJSONObject("ul").getJSONArray("ui").getJSONObject(0).getString("url");
                    int segCnt;
                    int fcCnt=segCnt=videoJson.getJSONObject("vl").getJSONArray("vi").getJSONObject(0).getJSONObject("cl").getInt("fc");
                    String downloadFileName=videoJson.getJSONObject("vl").getJSONArray("vi").getJSONObject(0).getString("fn");
                    String magicStr="",videoType="";
                    if(segCnt==0){
                        segCnt=1;
                    }else{
                        String[]sp=downloadFileName.split("\\.");
                        fnPre=sp[0];
                        magicStr=sp[1];
                        videoType=sp[2];
                    }
                    QQDownloadByVid_forSegCnt(new ArrayList<>(),1,segCnt,fcCnt,videoJson,fnPre,magicStr,videoType,host);
                }catch (IOException e){
                    onFinishFunction.OnFinish(false,paramPlayUrl,null,ctx.getString(R.string.message_error_on_reading_stream,e.getLocalizedMessage()));
                }catch (JSONException e){
                    onFinishFunction.OnFinish(false,paramPlayUrl,null,ctx.getString(R.string.message_json_exception,e.getLocalizedMessage()));
                }
            }
        };
        Common.SetYouGetHttpHeader(task);
        if(FileUtility.IsFileExists(cookiePath))
            task.SetCookie(FileUtility.ReadFile(cookiePath));
        task.execute(infoApi);
    }

    private void QQDownloadByVid_forSegCnt(ArrayList<DownloadParams>partParams,int part,int segCnt,int fcCnt,JSONObject videoJson,String fnPre,String magicStr,String videoType,String host) throws JSONException{
        if(part<=segCnt+1) {
            String partFormatId;
            DownloadParams dp=new DownloadParams();
            if (fcCnt == 0) {
                String[] sp = videoJson.getJSONObject("vl").getJSONArray("vi").getJSONObject(0).getJSONObject("cl").getString("keyid").split("\\.");
                partFormatId = sp[sp.length - 1];
            } else {
                String[] sp = videoJson.getJSONObject("vl").getJSONArray("vi").getJSONObject(0).getJSONObject("cl").getJSONArray("ci").getJSONObject(part - 1).getString("keyid").split("\\.");
                partFormatId = sp[1];
                dp.originalName = fnPre;
                if (magicStr.length() > 0)
                    dp.originalName += "." + magicStr;
                dp.originalName += "." + part;
                if (videoType.length() > 0) {
                    dp.originalName += "." + videoType;
                    dp.ext=videoType;
                }
                dp.partNumber=part;
            }
            String keyApi = String.format("http://vv.video.qq.com/getkey?otype=json&platform=11&format=%s&vid=%s&filename=%s&appver=3.2.19.333",partFormatId,videoId,dp.originalName);
            AndroidDownloadFileTask task=new AndroidDownloadFileTask() {
                @Override
                public void OnReturnStream(ByteArrayInputStream stream, boolean success, int response, Object extra, URLConnection connection) {
                    if(onFinishFunction ==null)
                        return;
                    if(!success){
                        onFinishFunction.OnFinish(false,paramPlayUrl,null,ctx.getString(R.string.message_unable_to_fetch_anime_info));
                        return;
                    }
                    try{
                        DownloadParams downloadParams=(DownloadParams) extra;
                        String enc=connection.getHeaderField("Content-Encoding");
                        InputStream iStream=stream;
                        if("gzip".equals(enc))//判断输入流是否是压缩的，并获取压缩算法
                            iStream=new GZIPInputStream(stream);
                        else if("deflate".equals(enc))
                            iStream=new InflaterInputStream(stream,new Inflater(true));
                        String jsonStr=Common.Match1(StreamUtility.GetStringFromStream(iStream,false),"QZOutputJson=(.*)");
                        JSONObject keyJson=new JSONObject(jsonStr.substring(0,jsonStr.length()-1));
                        String vKey,url;
                        if(!keyJson.has("key")){
                            vKey=videoJson.getJSONObject("vl").getJSONArray("vi").getJSONObject(0).getString("fvkey");
                            url=String.format("%s%s?vkey=%s",videoJson.getJSONObject("vl").getJSONArray("vi").getJSONObject(0).getJSONObject("ul").getJSONArray("ui").getJSONObject(0).getString("url"),fnPre+".mp4",vKey);
                        }else{
                            vKey=keyJson.getString("key");
                            url=String.format("%s%s?vkey=%s",host,downloadParams.originalName,vKey);
                        }
                        if(vKey==null||vKey.equals("")){
                            if(part==1)
                                Toast.makeText(ctx,String.format("WTF: %s",keyJson.getString("msg")),Toast.LENGTH_SHORT).show();
                            else
                                Toast.makeText(ctx,String.format("Warning: %s",keyJson.getString("msg")),Toast.LENGTH_SHORT).show();
                            QQDownloadByVid_forSegCnt(partParams,segCnt+2, segCnt, fcCnt, videoJson, fnPre, magicStr, videoType, host);
                            return;
                        }
                        if(!keyJson.has("filename")||keyJson.getString("filename")==null||keyJson.getString("filename").equals("")) {
                            Toast.makeText(ctx, String.format("Warning: %s", keyJson.getString("msg")), Toast.LENGTH_SHORT).show();
                            QQDownloadByVid_forSegCnt(partParams,segCnt+2, segCnt, fcCnt, videoJson, fnPre, magicStr, videoType, host);
                            return;
                        }
                        downloadParams.url=url;
                        partParams.add(downloadParams);
                        QQDownloadByVid_forSegCnt(partParams,part+1, segCnt, fcCnt, videoJson, fnPre, magicStr, videoType, host);
                    }catch (IOException e){
                        onFinishFunction.OnFinish(false,paramPlayUrl,null,ctx.getString(R.string.message_error_on_reading_stream,e.getLocalizedMessage()));
                    }catch (JSONException e){
                        onFinishFunction.OnFinish(false,paramPlayUrl,null,ctx.getString(R.string.message_json_exception,e.getLocalizedMessage()));
                    }
                }
            };
            Common.SetYouGetHttpHeader(task);
            task.SetExtra(dp);
            if(FileUtility.IsFileExists(cookiePath))
                task.SetCookie(FileUtility.ReadFile(cookiePath));
            task.execute(keyApi);
            return;
        }
        for (int i=0;i<partParams.size();i++) {
            String url=partParams.get(i).url;
            String videoSavePath=paramSavePath+"/"+fileNameWithoutExt;
            if(partParams.size()>1)
                videoSavePath+=" ["+partParams.get(i).partNumber+"]";
            videoSavePath+="."+partParams.get(i).ext;
            partParams.get(i).downloadFullPath=videoSavePath;
            partParams.get(i).downloaded=false;
            AndroidSysDownload sysDownload = new AndroidSysDownload(ctx);
            if(FileUtility.IsFileExists(cookiePath))
                sysDownload.SetCookie(FileUtility.ReadFile(cookiePath));
            sysDownload.SetOnDownloadFinishReceiver(new AndroidSysDownload.OnDownloadCompleteFunction() {
                @Override
                public void OnDownloadComplete(long downloadId, boolean success, int downloadedSize, int returnedFileSize, Object extra) {
                    Toast.makeText(ctx, ctx.getString(R.string.message_download_finish, partParams.get((int)extra).downloadFullPath), Toast.LENGTH_LONG).show();
                    partParams.get((int)extra).downloaded=true;
                    for (DownloadParams dp : partParams) {
                        if (!dp.downloaded)
                            return;
                    }
                    ArrayList<String>si=new ArrayList<>();
                    for (DownloadParams dp : partParams) {
                        si.add(dp.downloadFullPath);
                    }
                    String[]a=new String[si.size()];
                    Joiner joiner=Joiner.AutoChooseJoiner(si.toArray(a));
                    if(joiner!=null) {
                        String output=paramSavePath+"/"+fileNameWithoutExt+"."+joiner.getExt();
                        if(joiner.join(a, output)==0) {
                            for(String dPath:a){
                                FileUtility.DeleteFile(dPath);
                            }
                            Toast.makeText(ctx, ctx.getString(R.string.message_merged_videos) + "\n" + output, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            },i);
            sysDownload.StartDownloadFile(url,videoSavePath);
        }
    }

    @Override
    public void QueryQualities(String url, int episodeToDownload_fromZero, OnReturnVideoQualityFunction f) {
        ArrayList<VideoQuality>vqs=new ArrayList<>();
        vqs.add(new VideoQuality(0,""));
        f.OnReturnVideoQuality(true,vqs);
    }
}
