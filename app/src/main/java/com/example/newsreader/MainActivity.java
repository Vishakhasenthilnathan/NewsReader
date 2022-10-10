package com.example.newsreader;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.icu.text.CaseMap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    ListView newsListView;
    ArrayList<String> titles = new ArrayList<>();
    ArrayList<String> newUrls = new ArrayList<>();
    ArrayAdapter arrayAdapter;
    SQLiteDatabase articleDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        articleDB = this.openOrCreateDatabase("News", MODE_PRIVATE, null);
        articleDB.execSQL("CREATE TABLE IF NOT EXISTS article (Id INTEGER PRIMARY KEY,ArticleId INTEGER, Title VARCHAR, url VARCHAR ) ");


        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, titles);
        newsListView = findViewById(R.id.newslistView);
        newsListView.setAdapter(arrayAdapter);
        newsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(getApplicationContext(),NewsActivity.class);
                intent.putExtra("content url" ,newUrls.get(i));
                startActivity(intent);
            }
        });

        DownloadTask downloadTask = new DownloadTask();
        try {
            downloadTask.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty").get();
        } catch (Exception e) {
            e.printStackTrace();
        }

        updateListView();
    }

    public void updateListView(){
        Cursor c = articleDB.rawQuery("SELECT * FROM Article",null);
        int contentIndex = c.getColumnIndex("url");
        int titleIndex = c.getColumnIndex("Title");

        if(c.moveToFirst()){
            titles.clear();
            newUrls.clear();
            do {
                titles.add(c.getString(titleIndex));
                newUrls.add(c.getString(contentIndex));
                Log.i("content str", c.getString(contentIndex));
            }while (c.moveToNext());
            arrayAdapter.notifyDataSetChanged();
        }

    }

    public class DownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {
            String result = "";
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                InputStream inputStream = httpURLConnection.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                int data = inputStreamReader.read();

                StringBuilder stringBuilder = new StringBuilder();
                while (data != -1) {
                    char current = (char) data;
                    stringBuilder.append(current);
                    data = inputStreamReader.read();
                }
                result = stringBuilder.toString();
                JSONArray jsonArray = new JSONArray(result);

                int numberOfItems = 3;
                if (jsonArray.length() < 10) {
                    numberOfItems = jsonArray.length();
                }

                articleDB.execSQL("DELETE FROM Article");

                String content = "";
                for (int i = 0; i < numberOfItems; i++) {
                    String articleId = jsonArray.getString(i);
                    url = new URL("https://hacker-news.firebaseio.com/v0/item/" + articleId + ".json?print=pretty");
                    Log.i("newsurl", url.toString());
                    httpURLConnection = (HttpURLConnection) url.openConnection();
                    inputStream = httpURLConnection.getInputStream();
                    inputStreamReader = new InputStreamReader(inputStream);
                    data = inputStreamReader.read();

                    StringBuilder articlestringBuilder = new StringBuilder();
                    while (data != -1) {
                        char current = (char) data;
                        articlestringBuilder.append(current);
                        data = inputStreamReader.read();
                    }
                    content = articlestringBuilder.toString();
                    Log.i("content", content);
                    String title="";
                    String urlx="";
                    JSONObject jsonObject = new JSONObject(content);
                    if (!jsonObject.isNull("title") && !jsonObject.isNull("url")) {
                       title  = jsonObject.getString("title");
                        urlx = jsonObject.getString("url");
                        titles.add(title);
                        newUrls.add(urlx);
                        Log.i("title", title);
                        Log.i("urls", urlx);
                    }
                    String sql = "INSERT INTO article (ArticleId, Title, url) VALUES (?,?,?)";
                    SQLiteStatement statement = articleDB.compileStatement(sql);
                    statement.bindString(1, articleId);
                    statement.bindString(2, title);
                    statement.bindString(3, urlx);
                    statement.execute();
                }
                return result;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

        }
    }
}