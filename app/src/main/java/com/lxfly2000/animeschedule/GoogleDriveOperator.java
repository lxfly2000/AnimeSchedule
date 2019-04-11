package com.lxfly2000.animeschedule;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.drive.*;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.google.android.gms.tasks.*;
import com.lxfly2000.utilities.FileUtility;
import com.lxfly2000.utilities.StreamUtility;

import java.io.OutputStreamWriter;

public class GoogleDriveOperator {
    private Context androidContext;
    private GoogleSignInClient client=null;
    private GoogleSignInAccount googleAccount=null;
    private DriveResourceClient driveResourceClient=null;
    public GoogleDriveOperator(Context context){
        androidContext=context;
    }
    public void SignInClient(){
        GoogleSignInOptions options=new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(Drive.SCOPE_FILE)
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
        driveResourceClient=Drive.getDriveResourceClient(androidContext,googleAccount);
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

    public void UploadToDrive(final String localPath, final String appName, final String driveFileName){
        if(!IsAccountSignIn()) {
            Toast.makeText(androidContext,R.string.message_google_drive_no_login,Toast.LENGTH_LONG).show();
            return;
        }
        driveResourceClient.getRootFolder().continueWithTask(taskGetRootFolder -> {
            //判断是否存在所需文件夹
            Query query=new Query.Builder().addFilter(Filters.eq(SearchableField.TITLE,appName)).build();
            final DriveFolder rootFolder=taskGetRootFolder.getResult();
            return driveResourceClient.queryChildren(rootFolder,query)
                    .continueWithTask(taskQuerySubFolder -> {
                        if(!taskQuerySubFolder.getResult().iterator().hasNext()){//不存在则创建
                            MetadataChangeSet changeSet=new MetadataChangeSet.Builder().setTitle(appName).build();
                            driveResourceClient.createFolder(rootFolder,changeSet);
                        }
                        Query queryFile=new Query.Builder().addFilter(Filters.eq(SearchableField.TITLE,driveFileName)).build();
                        final DriveFolder appFolder=taskQuerySubFolder.getResult().get(0).getDriveId().asDriveFolder();
                        return driveResourceClient.queryChildren(appFolder,queryFile)
                                .continueWithTask(taskQuerySubFile -> {
                                    if(taskQuerySubFile.getResult().iterator().hasNext()){//把已有文件删除
                                        driveResourceClient.delete(taskQuerySubFile.getResult().get(0).getDriveId().asDriveFile());
                                    }
                                    return driveResourceClient.createContents().continueWithTask(taskCreateFile -> {
                                        MetadataChangeSet changeSet=new MetadataChangeSet.Builder()
                                                .setTitle(driveFileName)
                                                .setMimeType("application/x-javascript")
                                                .build();
                                        DriveContents contents=taskCreateFile.getResult();
                                        OutputStreamWriter writer=new OutputStreamWriter(contents.getOutputStream());
                                        writer.write(FileUtility.ReadFile(localPath));
                                        writer.flush();
                                        return driveResourceClient.createFile(appFolder,changeSet,contents)
                                                .addOnSuccessListener(driveFile -> Toast.makeText(androidContext,R.string.message_uploading_to_google_drive,Toast.LENGTH_LONG).show());
                                    });
                                });
                    });
        })
        .addOnFailureListener(e -> Toast.makeText(androidContext,androidContext.getString(R.string.message_error_upload_google_drive)+
                "\n"+e.getClass()+"\n"+e.getLocalizedMessage(),Toast.LENGTH_LONG).show());
    }
    public void DownloadFromDrive(final String appName, final String driveFileName, final String localPath){
        if(!IsAccountSignIn()) {
            Toast.makeText(androidContext,R.string.message_google_drive_no_login,Toast.LENGTH_LONG).show();
            return;
        }
        driveResourceClient.getRootFolder().continueWithTask(taskGetRootFolder -> {
            Query query=new Query.Builder().addFilter(Filters.eq(SearchableField.TITLE,appName)).build();
            DriveFolder rootFolder=taskGetRootFolder.getResult();
            return driveResourceClient.queryChildren(rootFolder,query)
                    .continueWithTask(taskQuerySubFolder -> {
                        if(!taskQuerySubFolder.getResult().iterator().hasNext()){//不存在则直接返回
                            Toast.makeText(androidContext,R.string.message_error_download_google_drive,Toast.LENGTH_LONG).show();
                            return null;
                        }
                        Query queryFile=new Query.Builder().addFilter(Filters.eq(SearchableField.TITLE,driveFileName)).build();
                        DriveFolder appFolder=taskQuerySubFolder.getResult().get(0).getDriveId().asDriveFolder();
                        return driveResourceClient.queryChildren(appFolder,queryFile)
                                .continueWithTask(taskQuerySubFile -> {
                                    if(!taskQuerySubFile.getResult().iterator().hasNext()){//不存在则直接返回
                                        Toast.makeText(androidContext,R.string.message_error_download_google_drive,Toast.LENGTH_LONG).show();
                                        return null;
                                    }
                                    return driveResourceClient.openFile(taskQuerySubFile.getResult().get(0).getDriveId().asDriveFile(),DriveFile.MODE_READ_ONLY)
                                            .continueWithTask(taskDownloadFile -> {
                                                String fileStr= StreamUtility.GetStringFromStream(taskDownloadFile.getResult().getInputStream(),false);
                                                if(fileStr.length()>0){
                                                    if(FileUtility.WriteFile(localPath,fileStr)) {
                                                        if (onGoogleDriveDownloadSuccessActions != null) {
                                                            onGoogleDriveDownloadSuccessActions.OnOperationSuccess(onGoogleDriveDownloadSuccessActions.extra);
                                                            onGoogleDriveDownloadSuccessActions = null;
                                                        }
                                                    }
                                                }
                                                return taskDownloadFile;
                                            });
                                });
                    });
        })
        .addOnFailureListener(e -> Toast.makeText(androidContext,androidContext.getString(R.string.message_error_download_google_drive)+
                "\n"+e.getClass()+"\n"+e.getLocalizedMessage(),Toast.LENGTH_LONG).show());
    }
}
