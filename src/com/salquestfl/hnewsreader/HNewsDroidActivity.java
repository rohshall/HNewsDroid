package com.salquestfl.hnewsreader;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.URL;
import java.net.HttpURLConnection;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.Toast;
import android.os.Bundle;
import android.util.Log;
import android.net.Uri;
import android.os.AsyncTask;



class RssFeedTask extends AsyncTask<String, Void, ArrayList<HashMap<String, String>>> {

    private static final String TAG = "HNewsDroid";
    private Activity activity;

    public RssFeedTask(Activity activity) {
        this.activity = activity;
    }

    // This executes in non-UI thread. No UI calls from here (including Toast)
    @Override
    protected ArrayList<HashMap<String, String>> doInBackground(String... urls) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urls[0]);
            conn = (HttpURLConnection) url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            return RssReader.read(in);
        } catch (Exception e) {
            Log.w(TAG, e.toString());
            return null;
        }
        finally {
            if (conn != null) {
                  conn.disconnect();
            }
        }
    }

    // This executes in UI thread
    @Override
    protected void onPostExecute(final ArrayList<HashMap<String, String>> articles) {
        if (articles == null) {
            String msg = "Could not connect to the server. Please try again after some time.";
            Log.w(TAG, msg);
            Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
        } else {
            final BaseAdapter adapter = new ClickableButtonListAdapter(activity, articles);
            final ListView l = (ListView) activity.findViewById(android.R.id.list);
            l.setAdapter(adapter);
            l.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                    final HashMap<String, String> article = articles.get(position);
                    String url = article.get("link");
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    activity.startActivity(intent);
                }
            });
        }
    }
 }
 
/**
 * Main Activity
 *
 */
public class HNewsDroidActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        // Get the RSS feed asynchronously
        String url = "https://news.ycombinator.com/rss";
        new RssFeedTask(this).execute(url);
    }
}
