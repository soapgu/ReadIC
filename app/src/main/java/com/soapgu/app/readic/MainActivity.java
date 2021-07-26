package com.soapgu.app.readic;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class MainActivity extends AppCompatActivity {

    private final CompositeDisposable disposables;
    {
        disposables = new CompositeDisposable();
    }
    private BroadcastReceiver broadcastReceiver;
    private TextView info;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.info = findViewById(R.id.tv_msg);
        Intent intent = new Intent(this,CardReadService.class);
        this.startService( intent );
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction(CardReadService.GET_CARD_NUM);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String cardNum = intent.getStringExtra("cardNum");
                info.setText( cardNum );
                disposables.clear();
                disposables.add(
                    Single.timer( 3, TimeUnit.SECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe( t-> info.setText("") ));
            }
        },filter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        disposables.clear();
    }
}