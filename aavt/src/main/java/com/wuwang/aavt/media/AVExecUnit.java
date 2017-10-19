package com.wuwang.aavt.media;

/*
 * Created by Wuwang on 2017/10/18
 */
public abstract class AVExecUnit{

    protected AVExecUnit mNextUnit;
    protected AVExecUnit mLastUnit;
    private long senderId;

    public AVExecUnit(){
        senderId=(System.nanoTime()<<10)|System.currentTimeMillis();
    }

    protected AVCmd createCmd(){
        return new AVCmd(senderId);
    }

    protected boolean isMyCmd(AVCmd cmd){
        return cmd.isCreator(senderId);
    }

    public void setNextUnit(AVExecUnit unit){
        this.mNextUnit=unit;
        this.mNextUnit.mLastUnit=this;
    }

    public abstract void start();

    public final void putCmd(AVCmd cmd){
        if(isMyCmd(cmd)){
            onSelfCmdCallback(cmd);
            cmd.cmd=0;
        }else{
            if(!onUpperCmd(cmd)&&cmd.cmd!=0){
                mNextUnit.putCmd(cmd);
            }
        }
    }

    protected abstract void onSelfCmdCallback(AVCmd cmd);

    /**
     * @return 返回是否在未处理完成时，也阻止消息传递
     */
    protected abstract boolean onUpperCmd(AVCmd cmd);

    public abstract void stop();

}
