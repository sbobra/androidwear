package com.example.sbobra.wearapp;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
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
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

//WEAR

public class MyWearActivity extends Activity implements
        ConnectionCallbacks, OnConnectionFailedListener, DataApi.DataListener {
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
            public void onLayoutInflated(final WatchViewStub stub) {
                runOnUiThread(new Runnable()
                {
                    public void run()
                    {
                        mTextView = (TextView) stub.findViewById(R.id.text);
                        imageView = (ImageView) stub.findViewById(R.id.map);
                        button = (Button) stub.findViewById(R.id.button);
                        button.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Log.i(TAG, "sending message");
//                        request();
                                (new SendMessageAsyncTask()).execute();
                            }
                        });
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
//
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
                final Bitmap bitmap = loadBitmapFromAsset(profileAsset);
                Log.i(TAG, "received bitmap");
                runOnUiThread(new Runnable() {
                    public void run() {
                        imageView.setImageBitmap(bitmap);
                    }
                });
                // Do something with the bitmap) {
            }
        }
    }

    public class SendMessageAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            sendMessage();
            return null;
        }

        private Collection<String> getNodes() {
            HashSet<String> results= new HashSet<String>();
            NodeApi.GetConnectedNodesResult nodes =
                    Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
            for (Node node : nodes.getNodes()) {
                results.add(node.getId());
            }
            return results;
        }

        public void sendMessage() {
            for (String node : getNodes()) {
                MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                        mGoogleApiClient, node, "/download", null).await();
                if (!result.getStatus().isSuccess()) {
                    Log.e(TAG, "ERROR: failed to send Message: " + result.getStatus());
                }
            }
        }

        @Override
        protected void onPostExecute(Void result) {

        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}
    }

//    @Override
//    public void onMessageReceived(MessageEvent messageEvent) {
//        if (messageEvent.getPath().equals("/image")) {
//            Log.i(TAG, "message received");
//
//            runOnUiThread(new Runnable()
//            {
//                public void run()
//                {
//                    mTextView.setText("message received!");
//                }
//            });
//        }
//    }
}
