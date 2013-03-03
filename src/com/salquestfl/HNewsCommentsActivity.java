package com.salquestfl;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;
import org.apache.http.client.ClientProtocolException;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import org.apache.commons.lang3.StringEscapeUtils;

class IndentedListAdapter extends SimpleAdapter { 

    public IndentedListAdapter(Context context, 
        List<? extends Map<String, ?>> data, int resource, String[] from, int[] to) {
        super(context, data, resource, from, to);
    }

    @SuppressWarnings("unchecked")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final View view = super.getView(position, convertView, parent);
        RelativeLayout child = (RelativeLayout) view.getTag();
        if(child == null) {
            // Add padding to the only child - RelativeLayout, dependent on comment's level
            child = (RelativeLayout) ((LinearLayout)view).getChildAt(0);
            view.setTag(child);
        }
        final HashMap<String, String> comment = (HashMap<String,String>) getItem(position);
        final int leftPadding = Integer.parseInt(comment.get("level")) * 8;
        child.setPadding(leftPadding, 0, 0, 0);
        return view;
    }
}
/**
 * Main Activity
 *
 */
public class HNewsCommentsActivity extends Activity {

    private static final String TAG = "HNewsComments";

    private String item_id;
    private ArrayList<HashMap<String, String>> comments;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        item_id = this.getIntent().getStringExtra("item_id");
        try {
            comments = getComments(item_id);
            SimpleAdapter adapter = new IndentedListAdapter(this, comments, R.layout.comment,
                    new String[] {"username", "time", "comment"},
                    new int[] {R.id.username, R.id.time, R.id.comment}
                    );
            final ListView l = (ListView) findViewById(android.R.id.list);
            l.setAdapter(adapter);
        } catch (Exception e) {
            Log.w(TAG, e.getMessage());
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    public static ArrayList<HashMap<String, String>> getCommentsFromArray(final JSONArray jsonArray, final int level)
        throws JSONException {
        ArrayList<HashMap<String, String>> comments = new ArrayList<HashMap<String, String>>(jsonArray.length());
        // skip the last element which points to the next page
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            Object id = jsonObject.opt("id");
            if (id != null) {
                HashMap<String, String> comment = new HashMap<String, String>();
                comment.put("id", (String)id);
                comment.put("username", jsonObject.getString("username"));
                comment.put("time", jsonObject.getString("time"));
                String content = jsonObject.getString("comment").replaceAll("__BR__", "\n");
                comment.put("comment", StringEscapeUtils.unescapeHtml4(content));
                comment.put("level", Integer.toString(level)); // Add level as a comment attribute
                comments.add(comment);
                comments.addAll(getCommentsFromArray(jsonObject.getJSONArray("children"), level+1));
            }
        }
        return comments;
    }

    public static ArrayList<HashMap<String, String>> getComments(final String item_id) 
        throws ClientProtocolException, IOException, JSONException {
        String feed = HNewsDroidActivity.readUrl("http://hndroidapi.appspot.com/nestedcomments/format/json/id/" + item_id);
        JSONObject feedJson = new JSONObject(feed);
        JSONArray jsonArray = (JSONArray)feedJson.get("items");
        return getCommentsFromArray(jsonArray, 0);
    }

}

