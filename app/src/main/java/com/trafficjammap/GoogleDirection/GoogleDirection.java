package com.trafficjammap.GoogleDirection;

import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;

public class GoogleDirection {
    ApiCallback apiCallback;

    public GoogleDirection(ApiCallback callback) {
        this.apiCallback = callback;
    }

    public void foo(String origin, String dest) {
        new ApiAsyncTask().execute(origin, dest);
    }

    class ApiAsyncTask extends AsyncTask<String, Void, Directions> {
        private ArrayList<ArrayList<Double>> data;

        @Override
        protected Directions doInBackground(String... params) {
            try {
                URL url = new URL(URLBuilder.getURL(params[0],params[1]));

                Log.d("mylog", url.toString());

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                InputStreamReader streamReader = new InputStreamReader(conn.getInputStream());
                BufferedReader reader = new BufferedReader(streamReader);
                StringBuffer buff = new StringBuffer();
                String line;

                while ((line = reader.readLine()) != null) {
                    buff.append(line);
                }

                Log.d("mylog", buff.toString());
                Directions result = new Gson().fromJson(buff.toString(), Directions.class);

                buff.delete(0, buff.length());

                return result;
            }
            catch (Exception ex) {
                ex.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Directions result) {
            super.onPostExecute(result);

            apiCallback.onApiResult(result);
        }
    }
}
