package com.tht.k3pler.handler;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;

/** Start/stop service **/
public class ServiceController {
    private Activity activity;
    private Class service;
    public ServiceController(Activity activity, Class service){
        this.activity = activity;
        this.service = service;
    }
    public Intent getIntent(){
        return new Intent(activity, service);
    }
    public void startServiceWithBind(ServiceConnection serviceConnection) throws Exception {
        startService();
        bindService(serviceConnection);
    }
    public void bindService(ServiceConnection serviceConnection){
        activity.bindService(getIntent(), serviceConnection, Context.BIND_AUTO_CREATE);
    }
    public void unbindService(ServiceConnection serviceConnection){
        activity.unbindService(serviceConnection);
    }
    public void startService() throws Exception{
        activity.startService(getIntent());
    }
    public void stopService() throws Exception {
        activity.stopService(getIntent());
    }

}
