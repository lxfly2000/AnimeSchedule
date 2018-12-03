package com.lxfly2000.animeschedule;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.drive.*;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.google.android.gms.tasks.*;
import com.lxfly2000.utilities.FileUtility;

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
            signInTask.addOnCompleteListener(new OnCompleteListener<GoogleSignInAccount>() {
                @Override
                public void onComplete(@NonNull Task<GoogleSignInAccount> task) {
                    try {
                        if(GetDriveClient(task.getResult(ApiException.class)))
                            OnSignInSuccess(androidContext);
                    }catch (ApiException e){
                        if(e.getStatusCode()== GoogleSignInStatusCodes.SIGN_IN_REQUIRED) {
                            if(!GetDriveClient(GoogleSignIn.getLastSignedInAccount(androidContext)))
                                ((AppCompatActivity) androidContext).startActivityForResult(client.getSignInIntent(),
                                        GoogleSignInStatusCodes.SIGN_IN_REQUIRED&0xFFFF);
                        }
                        else
                            OnSignInException(androidContext,e);
                    }
                }
            });
        }
    }

    public static abstract class OnSignedInSuccessActions{
        public abstract void OnSignedInSuccess(Object extra);
        public OnSignedInSuccessActions SetExtra(Object _extra){
            extra=_extra;
            return this;
        }
        private Object extra;
    }
    private OnSignedInSuccessActions onSignedInSuccessActions=null;
    public void SetOnSignedInSuccessActions(OnSignedInSuccessActions actions){
        onSignedInSuccessActions=actions;
    }

    public void OnSignInResultReturn(int resultCode, Intent data){
        if(resultCode!=AppCompatActivity.RESULT_OK)
            return;
        if(!GetDriveClient(GoogleSignIn.getLastSignedInAccount(androidContext)))
            return;
        OnSignInSuccess(androidContext);
    }

    public void OnSignInSuccess(Context context){
        Toast.makeText(context,R.string.message_login_success,Toast.LENGTH_LONG).show();
        if(onSignedInSuccessActions!=null) {
            onSignedInSuccessActions.OnSignedInSuccess(onSignedInSuccessActions.extra);
            onSignedInSuccessActions=null;
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
            signOutTask.addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    AccountSignOut();
                    OnSignOutSuccess(androidContext);
                }
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
        if(!IsAccountSignIn())
            return;
        driveResourceClient.getRootFolder().continueWithTask(new Continuation<DriveFolder, Task<DriveFile>>() {
            @Override
            public Task<DriveFile> then(@NonNull final Task<DriveFolder> task) throws Exception {
                //判断是否存在所需文件夹
                Query query=new Query.Builder().addFilter(Filters.eq(SearchableField.TITLE,appName)).build();
                final DriveFolder rootFolder=task.getResult();
                return driveResourceClient.queryChildren(rootFolder,query)
                        .continueWithTask(new Continuation<MetadataBuffer, Task<DriveFile>>() {
                            @Override
                            public Task<DriveFile> then(@NonNull Task<MetadataBuffer> task) throws Exception {
                                if(!task.getResult().iterator().hasNext()){//不存在则创建
                                    MetadataChangeSet changeSet=new MetadataChangeSet.Builder().setTitle(appName).build();
                                    driveResourceClient.createFolder(rootFolder,changeSet);
                                }
                                Query queryFile=new Query.Builder().addFilter(Filters.eq(SearchableField.TITLE,driveFileName)).build();
                                final DriveFolder appFolder=task.getResult().get(0).getDriveId().asDriveFolder();
                                return driveResourceClient.queryChildren(appFolder,queryFile)
                                        .continueWithTask(new Continuation<MetadataBuffer, Task<DriveFile>>() {
                                            @Override
                                            public Task<DriveFile> then(@NonNull Task<MetadataBuffer> task) throws Exception {
                                                if(task.getResult().iterator().hasNext()){//把已有文件删除
                                                    driveResourceClient.delete(task.getResult().get(0).getDriveId().asDriveFile());
                                                }
                                                return driveResourceClient.createContents().continueWithTask(new Continuation<DriveContents, Task<DriveFile>>() {
                                                    @Override
                                                    public Task<DriveFile> then(@NonNull Task<DriveContents> task) throws Exception {
                                                        MetadataChangeSet changeSet=new MetadataChangeSet.Builder()
                                                                .setTitle(driveFileName)
                                                                .setMimeType("application/x-javascript")
                                                                .build();
                                                        DriveContents contents=task.getResult();
                                                        OutputStreamWriter writer=new OutputStreamWriter(contents.getOutputStream());
                                                        writer.write(FileUtility.ReadFile(localPath));
                                                        writer.flush();
                                                        return driveResourceClient.createFile(appFolder,changeSet,contents)
                                                                .addOnSuccessListener(new OnSuccessListener<DriveFile>() {
                                                                    @Override
                                                                    public void onSuccess(DriveFile driveFile) {
                                                                        Toast.makeText(androidContext,R.string.message_uploading_to_google_drive,Toast.LENGTH_LONG).show();
                                                                    }
                                                                });
                                                    }
                                                });
                                            }
                                        });
                            }
                        });
            }
        })
        .addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(androidContext,androidContext.getString(R.string.message_error_upload_google_drive)+
                        "\n"+e.getClass()+"\n"+e.getLocalizedMessage(),Toast.LENGTH_LONG).show();
            }
        });
    }
    public void DownloadFromDrive(String appName, final String driveFileName, String localPath){
        if(!IsAccountSignIn())
            return;
        driveResourceClient.getRootFolder().continueWithTask(new Continuation<DriveFolder, Task<DriveFolder>>() {
            @Override
            public Task<DriveFolder> then(@NonNull Task<DriveFolder> task) throws Exception {
                Query query=new Query.Builder()
                        .addFilter(Filters.eq(SearchableField.TITLE,driveFileName))
                        .build();
                driveResourceClient.queryChildren(task.getResult(),query)
                        .addOnSuccessListener(new OnSuccessListener<MetadataBuffer>() {
                            @Override
                            public void onSuccess(MetadataBuffer metadata) {
                                //TODO：怎么读取到那个文件？？
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(androidContext,"无法从Google Drive读取文件。",Toast.LENGTH_LONG).show();
                            }
                        })
                        .addOnSuccessListener(new OnSuccessListener<MetadataBuffer>() {
                            @Override
                            public void onSuccess(MetadataBuffer metadata) {

                            }
                        });
                return null;//TODO：怎么返回次级目录？？
            }
        })
        .addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(androidContext,"无法读取Google Drive的目录。",Toast.LENGTH_LONG).show();
            }
        });
    }
}
