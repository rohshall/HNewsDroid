package com.salquestfl.hnewsreader;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.net.URL;

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

    private class RssFeedTask extends AsyncTask<URL, Void, ArrayList<HashMap<String, String>>> {

        private Context context;

        public RssFeedTask(Context context) {
            this.context = context;
        }

        @Override
        protected ArrayList<HashMap<String, String>> doInBackground(URL... urls) {
            ArrayList<HashMap<String, String>> articles;
            try {
                articles = RssReader.read(urls[0]);
            } catch (Exception e) {
                articles = new ArrayList<HashMap<String, String>>();
                Log.w(TAG, e.getMessage());
                Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            Log.w(TAG, "Gotten articles: " + articles.toString());
            return articles;
        }

        @Override
        protected void onPostExecute(final ArrayList<HashMap<String, String>> articles) {
            for(HashMap<String, String> article : articles) {
                Log.i("RSS Reader", article.get("title"));
            }
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
        }
     }
     
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        try {
            URL url = new URL("https://news.ycombinator.com/rss");
            new RssFeedTask(this).execute(url);
        } catch (Exception e) {
            Log.w(TAG, e.getMessage());
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
