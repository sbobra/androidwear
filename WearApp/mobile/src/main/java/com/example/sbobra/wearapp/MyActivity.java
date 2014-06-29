package com.example.sbobra.wearapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.*;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItemAsset;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.apache.http.Header;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;


public class MyActivity extends ActionBarActivity implements ConnectionCallbacks, OnConnectionFailedListener, MessageApi.MessageListener {
    private static final String IMAGE_URL = "http://api.wunderground.com/api/fcf4b9a6d9a0ad4e/radar/q/CA/San_Francisco.png?newmaps=1&timelabel=1&timelabel.y=10&height=200&width=200";
    private static final String TAG = "MyActivity";
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_my);
        mGoogleApiClient = new GoogleApiClient.Builder(MyActivity.this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(MyActivity.this)
                .addOnConnectionFailedListener(MyActivity.this)
                .build();
//        mGoogleApiClient.connect();
//        Intent i= new Intent(this, DataLayerListenerService.class);
//        startService(i);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if( mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
//        mGoogleApiClient.connect();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public class DownloadGifAsyncTask extends AsyncTask<Void, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(Void... params) {
            return downloadImage(IMAGE_URL);
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (result != null) {
                Log.i("MyActivity", "download successful! " + (result.getByteCount() / 1024));
                ImageView image = (ImageView) findViewById(R.id.map);
                image.setImageBitmap(result);

                Asset asset = createAssetFromBitmap(result);
                PutDataRequest request = PutDataRequest.create("/image");
                request.putAsset("mapImage", asset);
                Wearable.DataApi.putDataItem(mGoogleApiClient, request);
                Log.i(TAG, "send data item");

            } else {
                Log.i("MyActivity", "download failed.");
            }
        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}

        public Bitmap downloadImage(String imageUrl) {
            InputStream in;
            try {
                in = (new java.net.URL(imageUrl)).openStream();
            } catch (IOException e) {
                Log.i("MyActivity", e.toString());
                return null;
            }

            Bitmap orig = BitmapFactory.decodeStream(in);
            return orig;
        }
    }

    private static Asset createAssetFromBitmap(Bitmap bitmap) {
        Log.i(TAG, "creating asset from bitmap");
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
        return Asset.createFromBytes(byteStream.toByteArray());
    }
//
//
    ////////

    @Override
    public void onConnected(Bundle connectionHint) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Connected to Google Api Service");
        }
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
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
            Wearable.MessageApi.removeListener(mGoogleApiClient, MyActivity.this);
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }
//
//    @Override
//    public void onDataChanged(DataEventBuffer dataEvents) {
//        Log.i(TAG, "onDataEventChanged");
//        for (DataEvent event : dataEvents) {
//            if (event.getType() == DataEvent.TYPE_DELETED) {
//                Log.d(TAG, "DataItem deleted: " + event.getDataItem().getUri());
//            } else if (event.getType() == DataEvent.TYPE_CHANGED && event.getDataItem().getUri().getPath().equals("/weatherrequest")) {
//                Log.i(TAG, "weather request received");
//                new DownloadGifAsyncTask().execute();
//            }
//        }
//    }


    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals("/download")) {
            Log.i(TAG, "message received");
            (new DownloadGifAsyncTask()).execute();
        }
    }

    public class DataLayerListenerService extends WearableListenerService {

        private static final String TAG = "DataLayerSample";
        private static final String START_ACTIVITY_PATH = "/start-activity";
        private static final String DATA_ITEM_RECEIVED_PATH = "/data-item-received";

        @Override
        public void onMessageReceived(MessageEvent messageEvent) {
            if (messageEvent.getPath().equals("/download")) {
                (new DownloadGifAsyncTask()).execute();
            }
        }

        @Override
        public void onDataChanged(DataEventBuffer dataEvents) {
            Log.i(TAG, "onDataChanged");
            final ArrayList<DataEvent> events = FreezableUtils
                    .freezeIterable(dataEvents);

            GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Wearable.API)
                    .build();

            ConnectionResult connectionResult =
                    googleApiClient.blockingConnect(30, TimeUnit.SECONDS);

            if (!connectionResult.isSuccess()) {
                Log.e(TAG, "Failed to connect to GoogleApiClient.");
                return;
            }

            // Loop through the events and send a message
            // to the node that created the data item.
            for (DataEvent event : events) {
                Uri uri = event.getDataItem().getUri();

                // Get the node id from the host value of the URI
                String nodeId = uri.getHost();
                // Set the data of the message to be the bytes of the URI.
                byte[] payload = uri.toString().getBytes();

                // Send the RPC
                Wearable.MessageApi.sendMessage(googleApiClient, nodeId,
                        DATA_ITEM_RECEIVED_PATH, payload);
            }
        }
    }

}
