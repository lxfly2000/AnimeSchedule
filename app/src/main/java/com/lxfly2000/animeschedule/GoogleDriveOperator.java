package com.lxfly2000.animeschedule;

import android.content.Context;
import android.support.annotation.NonNull;
import android.widget.Toast;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.drive.*;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.google.android.gms.tasks.*;
import com.lxfly2000.utilities.FileUtility;

import java.io.OutputStreamWriter;
import java.io.Writer;

public abstract class GoogleDriveOperator {
    private Context androidContext;
    private GoogleSignInClient client=null;
    private GoogleSignInAccount googleAccount=null;
    private DriveClient driveClient=null;
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
            GetDriveClient(signInTask.getResult());
            OnSignInSuccess(androidContext,googleAccount);
        }else {
            signInTask.addOnCompleteListener(new OnCompleteListener<GoogleSignInAccount>() {
                @Override
                public void onComplete(@NonNull Task<GoogleSignInAccount> task) {
                    try {
                        GetDriveClient(task.getResult(ApiException.class));
                        OnSignInSuccess(androidContext,googleAccount);
                    }catch (ApiException e){
                        OnSignInException(androidContext,e);
                    }
                }
            });
        }
    }
    public abstract void OnSignInSuccess(Context context,GoogleSignInAccount account);
    public abstract void OnSignInException(Context context,ApiException e);
    private void GetDriveClient(GoogleSignInAccount taskResultAccount){
        googleAccount=taskResultAccount;
        driveClient=Drive.getDriveClient(androidContext.getApplicationContext(),googleAccount);
        driveResourceClient=Drive.getDriveResourceClient(androidContext.getApplicationContext(),googleAccount);
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
    public abstract void OnSignOutSuccess(Context context);
    public abstract void OnTransferComplete(Context context);
    private void AccountSignOut(){
        googleAccount=null;
    }

    public boolean IsAccountSignIn(){
        return googleAccount!=null;
    }

    public void UploadToDrive(final String localPath, final String appName, final String driveFileName){
        if(!IsAccountSignIn())
            return;
        driveResourceClient.getRootFolder().continueWithTask(new Continuation<DriveFolder, Task<DriveFolder>>() {
            @Override
            public Task<DriveFolder> then(@NonNull Task<DriveFolder> task) throws Exception {
                DriveFolder parentFolder=task.getResult();
                MetadataChangeSet changeSet=new MetadataChangeSet.Builder()
                        .setTitle(appName)
                        .setMimeType(DriveFolder.MIME_TYPE)
                        .build();
                return driveResourceClient.createFolder(parentFolder,changeSet);
            }
        })
        .addOnSuccessListener(new OnSuccessListener<DriveFolder>() {
            @Override
            public void onSuccess(final DriveFolder driveFolder) {
                Task<DriveContents>createContentsTask=driveResourceClient.createContents();
                createContentsTask.continueWithTask(new Continuation<DriveContents, Task<DriveFile>>() {
                    @Override
                    public Task<DriveFile> then(@NonNull Task<DriveContents> task) throws Exception {
                        MetadataChangeSet changeSet=new MetadataChangeSet.Builder()
                                .setTitle(driveFileName)
                                .setMimeType("application/json")
                                .build();
                        DriveContents contents=task.getResult();
                        Writer writer=new OutputStreamWriter(contents.getOutputStream());
                        writer.write(FileUtility.ReadFile(localPath));
                        return driveResourceClient.createFile(driveFolder,changeSet,contents);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(androidContext,"无法在Google Drive上创建文件。\n"+e.getLocalizedMessage(),Toast.LENGTH_LONG).show();
                    }
                })
                .addOnSuccessListener(new OnSuccessListener<DriveFile>() {
                    @Override
                    public void onSuccess(DriveFile driveFile) {
                        OnTransferComplete(androidContext);
                    }
                });
            }
        })
        .addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(androidContext,"无法在Google Drive上创建文件夹。\n"+e.getLocalizedMessage(),Toast.LENGTH_LONG).show();
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
                                Toast.makeText(androidContext,"无法从Google Drive读取文件。\n"+e.getLocalizedMessage(),Toast.LENGTH_LONG).show();
                            }
                        })
                        .addOnSuccessListener(new OnSuccessListener<MetadataBuffer>() {
                            @Override
                            public void onSuccess(MetadataBuffer metadata) {
                                OnTransferComplete(androidContext);
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
