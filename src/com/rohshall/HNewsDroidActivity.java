package com.rohshall;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import org.apache.commons.lang3.StringEscapeUtils;

class ClickableButtonListAdapter extends SimpleAdapter { 

    private static class ViewHolder {
        TextView text;
        ImageView image;
    }
    private OnClickListener listener;

    public ClickableButtonListAdapter(final Context context, 
        List<? extends Map<String, ?>> data, int resource, String[] from, int[] to) {
        super(context, data, resource, from, to);
        listener = new OnClickListener() {
            @Override
            public void onClick(View view) {
                String item_id = (String)view.getTag();
                Intent intent = new Intent(context, HNewsCommentsActivity.class);
                intent.putExtra("item_id", item_id);
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
            holder.text = (TextView) view.findViewById(R.id.comments);
            holder.image = (ImageView) view.findViewById(R.id.arrow);
            view.setTag(holder);
            holder.text.setOnClickListener(listener);
            holder.image.setOnClickListener(listener);
        }
        final HashMap<String, String> article = (HashMap<String,String>) getItem(position);
        String item_id = article.get("item_id");
        holder.text.setTag(item_id);
        holder.image.setTag(item_id);
        return view;
    }
}

/**
 * Main Activity
 *
 */
public class HNewsDroidActivity extends Activity {

    private static final String TAG = "HNewsDroid";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        try {
            final ArrayList<HashMap<String, String>> articles = getHNewsFeed();
            final SimpleAdapter adapter = new ClickableButtonListAdapter(this, 
                    articles, R.layout.article,
                    new String[] {"title", "urlShort", "score", "comments"},
                    new int[] {R.id.title, R.id.url, R.id.score, R.id.comments}
                    );
            final ListView l = (ListView) findViewById(android.R.id.list);
            l.setAdapter(adapter);
            l.setOnItemClickListener( new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                    final HashMap<String, String> article = articles.get(position);
                    String url = article.get("url");
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                }
            });
        } catch (Exception e) {
            Log.w(TAG, e.getMessage());
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public static ArrayList<HashMap<String, String>> getHNewsFeed() 
        throws ClientProtocolException, IOException, JSONException {
        final String feed = readUrl("http://hndroidapi.appspot.com/news/format/json/page/");
        final ArrayList<HashMap<String, String>> articles = new ArrayList<HashMap<String, String>>();
        JSONObject feedJson = new JSONObject(feed);
        JSONArray jsonArray = (JSONArray)feedJson.get("items");
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            Object item_id = jsonObject.opt("item_id");
            // If there is no item_id, ignore this entry
            if (item_id != null) {
                final HashMap<String, String> article = new HashMap<String, String>();
                article.put("item_id", (String)item_id);
                article.put("title", StringEscapeUtils.unescapeHtml4(jsonObject.getString("title")));
                String url = jsonObject.getString("url");
                article.put("url", url);
                article.put("urlShort", Uri.parse(url).getHost());    
                article.put("score", jsonObject.getString("score").split(" ")[0]);
                String comments = jsonObject.getString("comments");
                article.put("comments", (comments.compareTo("discuss") != 0) ? comments.split(" ")[0] : "0");
                articles.add(article);
            }
        }
        return articles;
    }

    public static String readUrl(String url) 
        throws ClientProtocolException, IOException {
        StringBuilder builder = new StringBuilder();
        HttpClient client = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(url);
        HttpResponse response = client.execute(httpGet);
        StatusLine statusLine = response.getStatusLine();
        int statusCode = statusLine.getStatusCode();
        if (statusCode == 200) {
            HttpEntity entity = response.getEntity();
            InputStream content = entity.getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(content));
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        } else {
            throw new IOException("Server returned statusCode " + statusCode);
        }
        return builder.toString();
    }
}

