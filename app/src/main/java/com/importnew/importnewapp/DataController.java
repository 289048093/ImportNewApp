package com.importnew.importnewapp;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.importnew.importnewapp.models.Post;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lzjun on 6/22/15.
 */
public class DataController {
    private static final String TAG = "DataController";
    private List<Post> repository = new ArrayList<>();
    private List<UIRespondent> mUIRespondents;

    private enum Action {INITAL, LOADMORE, REFRESH}


    public DataController() {
    }

    public final void initial() {
        String url = Uri.parse(Config.SERVER_URL).buildUpon().appendPath("items").toString();
        fetch(url, Action.INITAL);

    }

    public final void loadMore(int currentPage) {
        String queryString = (currentPage - 1) * 20 + "";
        String url = Uri.parse(Config.SERVER_URL).buildUpon().appendPath("items").appendQueryParameter("l", queryString).toString();
        fetch(url, Action.LOADMORE);
    }

    public void refresh() {
        if (!repository.isEmpty()) {
            Post firstPost = repository.get(0);
            if (firstPost != null) {
                String url = Uri.parse(Config.SERVER_URL).buildUpon().appendPath("items").appendQueryParameter("f_id", firstPost.getId() + "").toString();
                fetch(url, Action.REFRESH);
            }
        }
    }



    public final void fetch(String url, final Action action) {
        JsonObjectRequest request = new JsonObjectRequest(url.toString(), null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    Handler handler = new Handler(Looper.getMainLooper()) {
                        @Override
                        public void handleMessage(Message msg) {
                            List<Post> data = (List) msg.obj;
                            for (UIRespondent ui : mUIRespondents) {
                                switch (action){
                                    case INITAL:
                                        ui.onInitializeDone(data);
                                        break;
                                    case LOADMORE:
                                        ui.onLoadMoreDone(data);
                                        break;
                                    case REFRESH:
                                        ui.onRefreshDone(data);
                                        break;
                                }
                            }
                        }
                    };
                    Message msg = Message.obtain(handler);
                    msg.obj = parse(response);
                    ;
                    msg.sendToTarget();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
//                Log.i(TAG, error.getMessage());
            }
        });
        ApplicationController.getInstance().addToRequestQueue(request, TAG);

    }


    public int size() {
        if (repository == null)
            return 0;
        return repository.size();
    }

    public Post get(int index) {
        return repository.get(index);
    }

    private List<Post> parse(JSONObject jsonObject) throws JSONException {
        ArrayList<Post> posts = new ArrayList<>(20);
        JSONArray jsonArray = jsonObject.getJSONArray("posts");
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject postJson = (JSONObject) jsonArray.get(i);
            int id = postJson.getInt("id");
            String title = postJson.getString("title");
            String description = postJson.getString("description");
            String url = postJson.getString("url");
            String cover = postJson.getString("cover");
            String createAt = postJson.getString("create_at");
            String author = postJson.getString("author");
            posts.add(new Post(id, url, title, cover, author, createAt, description, ""));
        }
        return posts;

    }

    public void addUIRespondent(UIRespondent respondent) {
        if (mUIRespondents == null) {
            mUIRespondents = new ArrayList<>();
        }
        mUIRespondents.add(respondent);
    }

    public boolean addAll(List<Post> posts) {
        return this.repository.addAll(posts);
    }

    public void add(int index, Post post) {
        this.repository.add(index, post);
    }

}
