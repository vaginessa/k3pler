package com.tht.k3pler.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.tht.k3pler.adapter.BlacklistAdapter;
import com.tht.k3pler.frag.BlacklistPageInflater;
import com.tht.k3pler.handler.SqliteDBHelper;
import com.tht.k3pler.sub.ProxyNotifier;
import com.tht.k3pler.R;
import com.tht.k3pler.handler.RequestDialog;
import com.tht.k3pler.sub.SQLiteBL;
import com.tht.k3pler.sub.TextViewEFX;
import com.tht.k3pler.adapter.LayoutPagerAdapter;
import com.tht.k3pler.adapter.RequestAdapter;
import com.tht.k3pler.frag.MainPageInflater;
import com.tht.k3pler.handler.NotificationHandler;
import com.tht.k3pler.sub.FilteredResponse;
import com.tht.k3pler.sub.HTTPReq;

import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersSource;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Locale;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;

public class ProxyService extends Service {

    public HttpProxyServer httpProxyServer;
    public static final int PORT_NUMBER = 8090;
    private static final int MAX_BUFFER = 10 * 1024 * 1024;
    private NotificationHandler notificationHandler;
    private final IBinder mBinder = new Binder();
    private Intent currentIntent;
    private ArrayList<HTTPReq> httpReqs = new ArrayList<>();
    private Dialog guiDialog;
    private String decoderResult = "", arrowChar = " > ";
    private Handler mainHandler;
    private LayoutPagerAdapter layoutPagerAdapter;
    private BlacklistAdapter blacklistAdapter;
    private Boolean pageBackwards = false;
    private SqliteDBHelper sqliteDBHelper;
    private ArrayList<String> blackListArr;
    // ** //
    private TextView txvPage, txvNum;
    private RecyclerView mRecyclerView;
    private ListView lstBlacklist;
    private ViewPager viewPager;
    private RelativeLayout rlMain;
    private MainPageInflater mainPageInflater;
    private BlacklistPageInflater blacklistPageInflater;

    public interface IProxyStatus {
        void onReceive(HttpRequest httpRequest);
        void onNotify(NotificationHandler notificationHandler);
        void onError(Exception e);
    }

    public ProxyService() {}

    @Override
    public IBinder onBind(Intent intent) { return mBinder; }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent currentIntent, int flags, int startId) {
        this.currentIntent = currentIntent;
        this.checkExtras();
        return START_NOT_STICKY;
    }
    private void checkExtras() {
        if (currentIntent != null) {
            try {
                if (currentIntent.getBooleanExtra(getString(R.string.show_gui), false)) {
                    showGuiDialog();
                } else if (currentIntent.getBooleanExtra(getString(R.string.proxy_stop), false)) {
                    stopSelf();
                } else{
                    showGUI();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else{
            showGUI();
        }
    }
    @SuppressWarnings("deprecation")
    private void setBlacklistLstView(final ListView listView){
        sqliteDBHelper = new SqliteDBHelper(getApplicationContext(),
                new SQLiteBL(getApplicationContext()).getWritableDatabase(),
                SQLiteBL.BLACKLIST_DATA, SQLiteBL.TABLE_NAME);
        blackListArr = new ArrayList<>();
        String[] blackList = sqliteDBHelper.getAll().split("~");
        sqliteDBHelper.close();
        for(String item:blackList) {
            if (item.length() > 3) {
                blackListArr.add(item);
            }
        }
        if(blackListArr.size() > 0) {
            blacklistAdapter = new BlacklistAdapter(getApplicationContext(), blackListArr);
            listView.setAdapter(blacklistAdapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, final int i, long l) {
                    final String[] options = new String[]{getString(R.string.remove_blacklist)};
                    AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext(), android.R.style.Theme_DeviceDefault_Dialog);
                    builder.setTitle(blackListArr.get(i));
                    builder.setItems(options, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(which == 0){
                                sqliteDBHelper = new SqliteDBHelper(getApplicationContext(),
                                        new SQLiteBL(getApplicationContext()).getWritableDatabase(),
                                        SQLiteBL.BLACKLIST_DATA, SQLiteBL.TABLE_NAME);
                                sqliteDBHelper.delVal(blackListArr.get(i));
                                sqliteDBHelper.close();
                                setBlacklistLstView(listView);
                            }
                        }
                    });
                    AlertDialog alertDialog = builder.create();
                    alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                    alertDialog.show();
                }
            });
        }
    }
    @SuppressWarnings("deprecation")
    private void initGUI(Dialog dialog){
        txvPage = dialog.findViewById(R.id.txvPage);
        txvNum = dialog.findViewById(R.id.txvNum);
        viewPager = dialog.findViewById(R.id.viewPager);
        rlMain = dialog.findViewById(R.id.rlMain);
        layoutPagerAdapter = new LayoutPagerAdapter(getApplicationContext(), new LayoutPagerAdapter.IViewPager() {
            @Override
            public void onViewsAdded(ArrayList<ViewGroup> layouts) {
                try {
                    mainPageInflater = new MainPageInflater(getApplicationContext(), layouts.get(0));
                    mainPageInflater.init(new MainPageInflater.IRecylerView() {
                        @Override
                        public void onInit(RecyclerView recyclerView) {
                            mRecyclerView = recyclerView;
                        }
                    });
                }catch (Exception e){
                    e.printStackTrace();
                }
                try {
                    blacklistPageInflater = new BlacklistPageInflater(getApplicationContext(), layouts.get(1));
                    blacklistPageInflater.init(new BlacklistPageInflater.IListView() {
                        @Override
                        public void onInit(ListView listView) {
                            lstBlacklist = listView;
                            setBlacklistLstView(listView);
                        }
                    });
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
        viewPager.setAdapter(layoutPagerAdapter);
        new TextViewEFX().useFX(txvPage, arrowChar + getString(LayoutPagerAdapter.PagerEnum.MainPage.getTitleResId()));
        onViewPager_select(0);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}
            @Override
            public void onPageScrollStateChanged(int state) {}
            @Override
            public void onPageSelected(int position) {
                new TextViewEFX().useFX(txvPage, arrowChar + getString(LayoutPagerAdapter.PagerEnum.values()[position].getTitleResId()));
                onViewPager_select(position);
            }
        });
        txvNum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if(viewPager.getCurrentItem() + 1 != layoutPagerAdapter.getCount()
                            && !pageBackwards || viewPager.getCurrentItem() - 1 == -1) {
                        pageBackwards = false;
                        viewPager.setCurrentItem(viewPager.getCurrentItem() + 1, true);
                    }else{
                        pageBackwards = true;
                        viewPager.setCurrentItem(viewPager.getCurrentItem() - 1, true);
                    }
                }catch (Exception e1){
                    e1.printStackTrace();
                }
            }
        });
    }
    @SuppressWarnings("deprecation")
    private void showGUI(){
        try {
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            guiDialog = new Dialog(getApplicationContext(), android.R.style.Theme_Black);
            guiDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            guiDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            guiDialog.setContentView(inflater.inflate(R.layout.layout_main, null));
            initGUI(guiDialog);
            mainHandler = new Handler(getApplicationContext().getMainLooper());
            guiDialog.show();

            startLocalProxy(new IProxyStatus() {
                @Override
                public void onReceive(HttpRequest httpRequest) {
                    if(httpRequest.getDecoderResult().isSuccess())
                        decoderResult = "S";
                    else if(httpRequest.getDecoderResult().isFinished())
                        decoderResult = "F";
                    else if(httpRequest.getDecoderResult().isFailure())
                        decoderResult = "X";
                    httpReqs.add(new HTTPReq(httpRequest.getUri(),
                            String.valueOf(httpRequest.getMethod().name().charAt(0)),
                            httpRequest.getProtocolVersion().text().replace("HTTP", "H"),
                            decoderResult,
                            getTime()));
                    final ArrayList<HTTPReq> tmpHttpReqs = new ArrayList<>(httpReqs);
                    Collections.reverse(tmpHttpReqs);
                    Runnable setAdapterRunnable = new Runnable() {
                        @Override
                        public void run() {
                            try {
                                mRecyclerView.setAdapter(new RequestAdapter(getApplicationContext(), tmpHttpReqs, new RequestAdapter.OnItemClickListener() {
                                    @Override
                                    public void onItemClick(HTTPReq item, int i) {
                                       onRecycleItemClick(item);
                                    }
                                }));
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    };
                    mainHandler.post(setAdapterRunnable);
                }

                @Override
                public void onNotify(NotificationHandler notificationHandler) {}

                @Override
                public void onError(Exception e) {
                    Log.d(getString(R.string.app_name), e.toString());
                }
                private String getTime() {
                    DateFormat df = new SimpleDateFormat("{HH:mm:ss}", Locale.getDefault());
                    return df.format(Calendar.getInstance().getTime());
                }
            });

        }catch (Exception e){
            e.printStackTrace();
            Log.d(getString(R.string.app_name), "GUI start error.");
            stopSelf();
        }
    }
    private void onRecycleItemClick(HTTPReq item){
        new RequestDialog(getApplicationContext(), item).show(new RequestDialog.IBtnBlackList() {
            @Override
            public void onInit(Button btnReqBlackList, final Dialog dialog, final String uri) {
                btnReqBlackList.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        sqliteDBHelper = new SqliteDBHelper(getApplicationContext(),
                                new SQLiteBL(getApplicationContext()).getWritableDatabase(),
                                SQLiteBL.BLACKLIST_DATA, SQLiteBL.TABLE_NAME);
                        if(!sqliteDBHelper.getAll().contains(uri)) {
                            sqliteDBHelper.insert(uri);
                        }
                        sqliteDBHelper.close();
                        dialog.cancel();
                        setBlacklistLstView(lstBlacklist);
                        // TODO: 9/12/2018 Toast
                    }
                });
            }
        });
    }
    @SuppressWarnings("deprecation")
    private void onViewPager_select(int position){
        String pageNumHTML = "";
        int color;
        for(int i = 0; i < LayoutPagerAdapter.PagerEnum.values().length; i++){
            if(i == position)
                color = ContextCompat.getColor(getApplicationContext(), android.R.color.white);
            else
                color = ContextCompat.getColor(getApplicationContext(), R.color.color2);
            pageNumHTML += "<font color=\"" + color + "\">" + String.valueOf(i+1) + " " + "</font>";
        }
        txvNum.setText(Html.fromHtml(pageNumHTML));
    }
    private void startLocalProxy(final IProxyStatus proxyStatus){
        try {
            httpProxyServer = DefaultHttpProxyServer.bootstrap()
                    .withPort(PORT_NUMBER)
                    .withFiltersSource(new HttpFiltersSource() {
                        @Override
                        public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
                            try {
                                proxyStatus.onReceive(originalRequest);
                            }catch (Exception e){ e.printStackTrace(); }
                            return new FilteredResponse(originalRequest);
                        }
                        @Override
                        public int getMaximumRequestBufferSizeInBytes() {
                            return MAX_BUFFER;
                        }

                        @Override
                        public int getMaximumResponseBufferSizeInBytes() {
                            return MAX_BUFFER;
                        }
                    }).start();
            notificationHandler = new NotificationHandler(1, getApplicationContext(), ProxyService.class);
            if(httpProxyServer != null){
                new ProxyNotifier(getApplicationContext(), httpProxyServer, notificationHandler).execute();
            }else{
                throw new Exception("Failed to start proxy.");
            }
            if(proxyStatus!=null){
                proxyStatus.onNotify(notificationHandler);
            }
        }catch (Exception e){
            if(proxyStatus!=null){
                proxyStatus.onError(e);
            }
            e.printStackTrace();
        }
    }
    private void showGuiDialog(){
        try{
            if(guiDialog != null && !guiDialog.isShowing()){
                guiDialog.show();
            }else if (guiDialog == null){
                cancelNotifications();
                showGUI();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void cancelNotifications(){
        try {
            notificationHandler.getNotificationManager().cancelAll();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void stopProxy(){
        if(guiDialog != null && guiDialog.isShowing()) {
            guiDialog.cancel();
            guiDialog = null;
        }
        try{
            httpProxyServer.stop();
        }catch (Exception e){
            e.printStackTrace();
        }
        try{
            httpProxyServer.abort();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        cancelNotifications();
        stopProxy();
    }
}