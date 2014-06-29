package com.example.sbobra.wearapp;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.*;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItemAsset;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

//WEAR

public class MyWearActivity extends Activity implements
        DataApi.DataListener, ConnectionCallbacks, OnConnectionFailedListener {
    private static final String TAG = "MyWearActivity";
    private TextView mTextView;
    private ImageView imageView;
    private Button button;
    private GoogleApiClient mGoogleApiClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        mGoogleApiClient = new GoogleApiClient.Builder(MyWearActivity.this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(MyWearActivity.this)
                .addOnConnectionFailedListener(MyWearActivity.this)
                .build();
        mGoogleApiClient.connect();
        //request();
        Log.i(TAG, "Attempted to connect to client");
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
                imageView = (ImageView) stub.findViewById(R.id.map);
                button = (Button) stub.findViewById(R.id.button);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.i(TAG, "Requesting");
                        request();
                    }
                });
            }
        });

    }

    public void request() {
        PutDataRequest request = PutDataRequest.create("/weatherrequest");
        Wearable.DataApi.putDataItem(mGoogleApiClient, request);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if( mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
//        mGoogleApiClient.connect();
    }

    public Bitmap loadBitmapFromAsset(DataItemAsset asset) {
        if (asset == null) {
            throw new IllegalArgumentException("Asset must be non-null");
        }
        ConnectionResult result =
                mGoogleApiClient.blockingConnect(1000, TimeUnit.MILLISECONDS);
        if (!result.isSuccess()) {
            return null;
        }
        // convert asset into a file descriptor and block until it's ready
        InputStream assetInputStream = Wearable.DataApi.getFdForAsset(
                mGoogleApiClient, asset).await().getInputStream();
        mGoogleApiClient.disconnect();

        if (assetInputStream == null) {
            Log.w(TAG, "Requested an unknown Asset.");
            return null;
        }
        // decode the stream into a bitmap
        return BitmapFactory.decodeStream(assetInputStream);
    }


    @Override
    public void onConnected(Bundle connectionHint) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Connected to Google Api Service");
        }
        Wearable.DataApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int suspended) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "Connection failed");

    }

    @Override
    protected void onStop() {
        if (null != mGoogleApiClient && mGoogleApiClient.isConnected()) {
            Wearable.DataApi.removeListener(mGoogleApiClient, MyWearActivity.this);
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.i(TAG, "onDataEventChanged");
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_DELETED) {
                Log.d(TAG, "DataItem deleted: " + event.getDataItem().getUri());
            } else if (event.getType() == DataEvent.TYPE_CHANGED &&
                    event.getDataItem().getUri().getPath().equals("/image")) {
                Log.d(TAG, "DataItem changed: " + event.getDataItem().getUri());
                DataItemAsset profileAsset = event.getDataItem().getAssets().get("mapImage");
                Bitmap bitmap = loadBitmapFromAsset(profileAsset);
                Log.i(TAG, "received bitmap");
                imageView.setImageBitmap(bitmap);
                if (bitmap == null) {
                    mTextView.setText("RECEIVED: null");

                    Log.i(TAG, "bitmap is null");
                } else {
                    mTextView.setText("RECEIVED");
                }
                // Do something with the bitmap) {
            }
        }
    }

}
