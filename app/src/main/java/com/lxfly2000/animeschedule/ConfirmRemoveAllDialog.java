package com.lxfly2000.animeschedule;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

public class ConfirmRemoveAllDialog {
    private Context ctx;
    private String answer;
    public ConfirmRemoveAllDialog(@NonNull Context context){
        ctx=context;
    }

    public void SetAnswer(String a){
        answer=a;
    }

    private EditText editInput;
    private DialogInterface.OnClickListener okListener;

    public void SetOnOkListener(DialogInterface.OnClickListener listener){
        okListener=listener;
    }

    public void Show(){
        final AlertDialog dlg=new AlertDialog.Builder(ctx)
                .setTitle(R.string.message_confirm_your_operation)
                .setView(R.layout.dialog_with_input)
                .setPositiveButton(android.R.string.ok, okListener)
                .setNegativeButton(android.R.string.cancel,null)
                .show();
        editInput=dlg.findViewById(R.id.editInputBox);
        editInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //Nothing
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                dlg.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(answer.contentEquals(charSequence));
            }

            @Override
            public void afterTextChanged(Editable editable) {
                //Nothing
            }
        });
        editInput.setText("");
    }
}
