package com.salquestfl.hnewsreader;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.net.HttpURLConnection;

import android.app.Activity;
import android.content.Intent;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.view.View.OnClickListener;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import android.os.Bundle;
import android.util.Log;
import android.net.Uri;
import android.os.AsyncTask;




class ClickableButtonListAdapter extends SimpleAdapter { 

    private static class ViewHolder {
        TextView text;
        ImageView image;
    }
    private OnClickListener listener;

    public ClickableButtonListAdapter(final Context context, List<? extends Map<String, ?>> data, int resource, String[] from, int[] to) {
        super(context, data, resource, from, to);
        listener = new OnClickListener() {
            @Override
            public void onClick(View view) {
                String url = (String)view.getTag();
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                context.startActivity(intent);
            }
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final View view = super.getView(position, convertView, parent);
        ViewHolder holder = (ViewHolder) view.getTag();
        if(holder == null) {
            holder = new ViewHolder();
            holder.image = (ImageView) view.findViewById(R.id.arrow);
            holder.image.setOnClickListener(listener);
            view.setTag(holder);
        }
        final HashMap<String, String> article = (HashMap<String, String>) getItem(position);
        String commentsUrl = article.get("comments");
        holder.image.setTag(commentsUrl);
        return view;
    }
}

/**
 * Main Activity
 *
 */
public class HNewsDroidActivity extends Activity {

    private static final String TAG = "HNewsDroid";

    private class RssFeedTask extends AsyncTask<String, Void, String> {

        private Context context;

        public RssFeedTask(Context context) {
            this.context = context;
        }

        private String readData(BufferedReader in) throws IOException {
            StringBuilder chars = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                chars.append(line);
            }
            return chars.toString();
        }

        @Override
        protected String doInBackground(String... urls) {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(urls[0]);
                conn = (HttpURLConnection) url.openConnection();
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                String data = readData(in);
                return data;
            } catch (Exception e) {
                Log.w(TAG, e.getMessage());
                Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                return null;
            }
            finally {
                if (conn != null) {
                      conn.disconnect();
                }
            }
        }

        @Override
        protected void onPostExecute(String data) {
            try {
                final ArrayList<HashMap<String, String>> articles = RssReader.read(new StringReader(data));
                final SimpleAdapter adapter = new ClickableButtonListAdapter(
                        context, 
                        articles, R.layout.article,
                        new String[] {"title", "link"},
                        new int[] {R.id.title, R.id.url}
                        );
                final ListView l = (ListView) findViewById(android.R.id.list);
                l.setAdapter(adapter);
                l.setOnItemClickListener( new OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                        final HashMap<String, String> article = articles.get(position);
                        String url = article.get("link");
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                    }
                });
            } catch (Exception e) {
                Log.w(TAG, e.getMessage());
                Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
     }
     
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        // Get the RSS feed asynchronously
        String url = "https://news.ycombinator.com/rss";
        new RssFeedTask(this).execute(url);
    }
}
