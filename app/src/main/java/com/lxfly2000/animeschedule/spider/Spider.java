package com.lxfly2000.animeschedule.spider;

import android.content.Context;
import com.lxfly2000.animeschedule.data.AnimeItem;

public abstract class Spider {
    protected Context context;
    public Spider(Context ctx){
        context=ctx;
    }
    public static final int STATUS_OK=0,STATUS_ONGOING=1,STATUS_FAILED=-1;
    public static abstract class OnReturnDataFunction{
        public abstract void OnReturnData(AnimeItem data,int status,String resultMessage);
    }
    protected OnReturnDataFunction onReturnDataFunction=null;
    public void SetOnReturnDataFunction(OnReturnDataFunction f){
        onReturnDataFunction=f;
    }
    public abstract void Execute(String url);
}
