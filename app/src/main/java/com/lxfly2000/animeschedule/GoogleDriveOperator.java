package com.lxfly2000.animeschedule;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.*;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GoogleDriveOperator {
    private Context androidContext;
    private GoogleSignInClient client=null;
    private GoogleSignInAccount googleAccount=null;
    private Drive driveResourceClient=null;
    private final Executor executor=Executors.newSingleThreadExecutor();
    public GoogleDriveOperator(Context context){
        androidContext=context;
    }
    public void SignInClient(){
        GoogleSignInOptions options=new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                .build();
        client= GoogleSignIn.getClient(androidContext,options);
        Task<GoogleSignInAccount> signInTask=client.silentSignIn();
        if(signInTask.isSuccessful()){
            if(GetDriveClient(signInTask.getResult()))
                OnSignInSuccess(androidContext);
        }else {
            signInTask.addOnCompleteListener(task -> {
                try {
                    if(GetDriveClient(task.getResult(ApiException.class)))
                        OnSignInSuccess(androidContext);
                }catch (ApiException e){
                    if(e.getStatusCode()== GoogleSignInStatusCodes.SIGN_IN_REQUIRED) {
                        if(!GetDriveClient(GoogleSignIn.getLastSignedInAccount(androidContext)))
                            ((MainActivity) androidContext).startActivityForResult(client.getSignInIntent(),
                                    GoogleSignInStatusCodes.SIGN_IN_REQUIRED&0xFFFF);
                    }
                    else
                        OnSignInException(androidContext,e);
                }
            });
        }
    }

    public static abstract class OnOperationSuccessActions{
        public abstract void OnOperationSuccess(Object extra);
        public OnOperationSuccessActions SetExtra(Object _extra){
            extra=_extra;
            return this;
        }
        private Object extra;
    }
    private OnOperationSuccessActions onSignInSuccessActions=null;
    private OnOperationSuccessActions onGoogleDriveDownloadSuccessActions=null;
    public void SetOnSignInSuccessActions(OnOperationSuccessActions actions){
        onSignInSuccessActions=actions;
    }
    public void SetOnGoogleDriveDownloadSuccessActions(OnOperationSuccessActions actions){
        onGoogleDriveDownloadSuccessActions=actions;
    }

    public void OnSignInResultReturn(int resultCode, Intent data){
        if(resultCode!=MainActivity.RESULT_OK)
            return;
        if(!GetDriveClient(GoogleSignIn.getLastSignedInAccount(androidContext)))
            return;
        OnSignInSuccess(androidContext);
    }

    public void OnSignInSuccess(Context context){
        Toast.makeText(context,R.string.message_login_success,Toast.LENGTH_SHORT).show();
        if(onSignInSuccessActions!=null) {
            onSignInSuccessActions.OnOperationSuccess(onSignInSuccessActions.extra);
            onSignInSuccessActions=null;
        }
    }

    public void OnSignInException(Context context,ApiException e){
        Toast.makeText(context,androidContext.getString(R.string.message_login_failed)+"\n"+e.getLocalizedMessage(),Toast.LENGTH_LONG).show();
    }

    private boolean GetDriveClient(GoogleSignInAccount taskResultAccount){
        googleAccount=taskResultAccount;
        if(googleAccount==null)
            return false;
        GoogleAccountCredential credential=GoogleAccountCredential.usingOAuth2(androidContext, Collections.singleton(DriveScopes.DRIVE_FILE));
        credential.setSelectedAccount(googleAccount.getAccount());
        driveResourceClient=new Drive.Builder(AndroidHttp.newCompatibleTransport(),new JacksonFactory(),credential)
                .setApplicationName(androidContext.getString(R.string.app_name))
                .build();
        return true;
    }

    public void SignOutClient(){
        Task<Void> signOutTask=client.signOut();
        if(signOutTask.isSuccessful()){
            AccountSignOut();
            OnSignOutSuccess(androidContext);
        }else {
            signOutTask.addOnCompleteListener(task -> {
                AccountSignOut();
                OnSignOutSuccess(androidContext);
            });
        }
    }

    public void OnSignOutSuccess(Context context){
        //Toast.makeText(context,"已注销登录。",Toast.LENGTH_LONG).show();
    }

    private void AccountSignOut(){
        googleAccount=null;
    }

    public boolean IsAccountSignIn(){
        return googleAccount!=null;
    }

    private static final String mimeDriveFolder="application/vnd.google-apps.folder";
    private static final String mimeJavaScript="application/x-javascript";

    /**
     * 获取根目录下指定项目的ID
     * @param parentId 指定父级项目的ID，若要从根目录找则指定为 root
     * @param itemName 指定要搜索的文件或文件夹的名字
     * @param itemMime 指定要搜索文件的MIME类型，如果搜索文件夹则指定 mimeDriveFolder
     * @return 返回指定项目的ID，若不存在则返回 null
     * @throws IOException list操作会产生IO异常
     */
    private String GetItemId(String parentId,String itemName,String itemMime)throws IOException {
        String pageToken=null;
        if(itemMime==null)
            itemMime="*";
        //https://developers.google.cn/drive/api/v3/ref-search-terms
        String queryStr="mimeType='"+itemMime+"'";
        if(itemName!=null)
            queryStr+=" and name='"+itemName+"'";
        //https://developers.google.cn/drive/api/v3/folder
        if(parentId!=null)
            queryStr+=" and '"+parentId+"' in parents";
        do {
            FileList result = driveResourceClient.files().list()
                    .setQ(queryStr)
                    .setSpaces("drive")
                    .setFields("nextPageToken, files(id, name)")
                    .setPageToken(pageToken)
                    .execute();
            for(File file:result.getFiles()){
                return file.getId();
            }
            pageToken=result.getNextPageToken();
        }while(pageToken!=null);
        return null;
    }

    public void UploadToDrive(final String localPath, final String appName, final String driveFileName){
        if(!IsAccountSignIn()) {
            Toast.makeText(androidContext,R.string.message_google_drive_no_login,Toast.LENGTH_LONG).show();
            return;
        }
        Tasks.call(executor,()->{
            //判断根目录是否存在所需文件夹
            //https://developers.google.cn/drive/api/v3/search-files
            String folderId=GetItemId("root",appName,mimeDriveFolder);
            if(folderId==null){
                //不存在则创建
                //https://developers.google.cn/drive/api/v3/folder
                File fileMetadata = new File();
                fileMetadata.setName(appName);
                fileMetadata.setMimeType(mimeDriveFolder);
                File file = driveResourceClient.files().create(fileMetadata)
                        .setFields("id")
                        .execute();
                folderId=file.getId();
            }
            //判断文件夹是否已有文件
            String fileId=GetItemId(folderId,driveFileName,mimeJavaScript);
            if(fileId!=null){
                //存在则先删除
                driveResourceClient.files().delete(fileId).execute();
            }
            //上传文件
            File fileUploadMeta=new File().setName(driveFileName).setParents(Collections.singletonList(folderId));
            fileId=driveResourceClient.files().create(fileUploadMeta,new FileContent(mimeJavaScript,new java.io.File(localPath)))
                    .setFields("id")
                    .execute().getId();

            return fileId;
        })
        .addOnSuccessListener(e->Toast.makeText(androidContext,R.string.message_uploading_to_google_drive,Toast.LENGTH_LONG).show())
        .addOnFailureListener(e -> Toast.makeText(androidContext,androidContext.getString(R.string.message_error_upload_google_drive)+
                "\n"+e.getClass()+"\n"+e.getLocalizedMessage(),Toast.LENGTH_LONG).show());
    }
    public void DownloadFromDrive(final String appName, final String driveFileName, final String localPath){
        if(!IsAccountSignIn()) {
            Toast.makeText(androidContext,R.string.message_google_drive_no_login,Toast.LENGTH_LONG).show();
            return;
        }
        Tasks.call(executor,()->{
            //判断文件夹是否存在
            String itemId=GetItemId("root",appName,mimeDriveFolder);
            if(itemId==null) {//不存在则直接返回
                throw new Exception(androidContext.getString(R.string.message_error_download_google_drive));
            }
            //判断文件是否存在
            itemId=GetItemId(itemId,driveFileName,mimeJavaScript);
            if(itemId==null) {//不存在则直接返回
                throw new Exception(androidContext.getString(R.string.message_error_download_google_drive));
            }
            FileOutputStream stream=new FileOutputStream(localPath);
            driveResourceClient.files().get(itemId).executeMediaAndDownloadTo(new FileOutputStream(localPath));
            stream.flush();
            stream.close();
            return 0;
        })
        .addOnSuccessListener(e->{
            if(onGoogleDriveDownloadSuccessActions!=null){
                onGoogleDriveDownloadSuccessActions.OnOperationSuccess(onGoogleDriveDownloadSuccessActions.extra);
                onGoogleDriveDownloadSuccessActions=null;
            }
        })
        .addOnFailureListener(e -> Toast.makeText(androidContext,androidContext.getString(R.string.message_error_download_google_drive)+
                "\n"+e.getClass()+"\n"+e.getLocalizedMessage(),Toast.LENGTH_LONG).show());
    }
}
