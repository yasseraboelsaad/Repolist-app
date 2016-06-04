package com.bachelorproject.yasser.instabug.Activities;

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.bachelorproject.yasser.instabug.Models.Repo;
import com.bachelorproject.yasser.instabug.R;
import com.bachelorproject.yasser.instabug.util.InternalStorage;
import com.bachelorproject.yasser.instabug.util.JSONParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {
   //intializing variables
    static int pageno=1;
    private List<Repo> reposlist = new ArrayList<Repo>();
    ListView listView ;
    SwipeRefreshLayout mySwipeRefreshLayout;
    ArrayAdapter<Repo> adapter;
    private ProgressDialog pDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pageno=1;
        //initializing swipe refresh layout
        mySwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.menu_refresh);
        mySwipeRefreshLayout.setOnRefreshListener(MainActivity.this);
//        initializing List adapter
        adapter = new MyListAdapter();
        listView = (ListView) findViewById(R.id.lv_repos);
        listView.setAdapter(adapter);
        listView.setOnScrollListener(new EndlessScrollListener());
        //initially the array in the cahce is empty
        try {
            // Overwrites the arraylist stored in the cache with an empty one
            InternalStorage.writeObject(MainActivity.this, "KEY", reposlist);

        } catch (IOException e) {
//                Log.e(TAG, e.getMessage());
        }
        //Calling the Async task that fetches the data from the url

        new JSONLoadTask().execute("https://api.github.com/users/square/repos?page="+pageno+"&per_page=10");
    }


    //increments the page number and requests next page
    public void incpage(){
        pageno++;
        new JSONLoadTask().execute("https://api.github.com/users/square/repos?page="+pageno+"&per_page=10");
    }
//creates new repo object and adds it to the Arraylist of repos
    private void populateReposList(String name, String desc, String login,String fork, String owner_url, String repo_url) {
        Repo repo = new Repo(name,desc,login,fork,owner_url,repo_url);
        reposlist.add(repo);
    }
    //refreshes the listview content
    private void populateListView() {
        runOnUiThread(new Runnable() {
            public void run() {
                adapter.notifyDataSetChanged();
            }
        });
    }

    //Listview items long click listener
    private void registerClickCallback() {
        ListView list= (ListView)findViewById(R.id.lv_repos);
        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View viewClicked, int position, long id) {
                //create repo object instance of the clicked object and creats a popup window
                final Repo clickedRepo = reposlist.get(position);
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Website redirect")
                        .setMessage("owner's page or the repository's page?")
                        //create a button to redirect to owners page
                        .setPositiveButton("Owner's page", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                //fetch the url from the object
                                String urlString=clickedRepo.getOwner_url();
                                //create an intent containing the url
                                Intent intent=new Intent(Intent.ACTION_VIEW, Uri.parse(urlString));
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                // try to open the url using chrome
                                intent.setPackage("com.android.chrome");
                                try {
                                    MainActivity.this.startActivity(intent);
                                } catch (ActivityNotFoundException ex) {
                                    // Chrome browser presumably not installed so allow user to choose instead
                                    intent.setPackage(null);
                                    MainActivity.this.startActivity(intent);
                                }                            }
                        })
                        //creating the second button similar to the first but redirects to the repos page
                        .setNeutralButton("Repository's page", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                String urlString=clickedRepo.getRepo_url();
                                Intent intent=new Intent(Intent.ACTION_VIEW, Uri.parse(urlString));
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                intent.setPackage("com.android.chrome");
                                try {
                                    MainActivity.this.startActivity(intent);
                                } catch (ActivityNotFoundException ex) {
                                    intent.setPackage(null);
                                    MainActivity.this.startActivity(intent);
                                }
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .show();
                return true;
            }
        });
    }
    //restarts the activity
    @Override
    public void onRefresh() {
        Intent intent = getIntent();
        finish();
        startActivity(intent);
        mySwipeRefreshLayout.setRefreshing(false);
    }

    //custom list adapter
    private class MyListAdapter extends ArrayAdapter<Repo> {

        public MyListAdapter() {
            super(MainActivity.this,R.layout.list_item_repo,reposlist);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View itemView = convertView;
            if (itemView == null){
                itemView = getLayoutInflater().inflate(R.layout.list_item_repo,parent,false);
            }
            Repo currentRepo = reposlist.get(position);
            // set text view text from the repo object
            TextView nameText = (TextView)itemView.findViewById(R.id.tv_name);
            nameText.setText(currentRepo.getName());

            TextView DescText = (TextView)itemView.findViewById(R.id.tv_description);
            DescText.setText(currentRepo.getDescription());

            TextView loginText = (TextView)itemView.findViewById(R.id.tv_login);
            loginText.setText(currentRepo.getLogin());

            // check for the fork and if it isnt true set the items background to green
            if (!currentRepo.getFork().equals("true")){
                itemView.setBackgroundColor(Color.GREEN);
            }else {
                itemView.setBackgroundColor(Color.WHITE);
            }
            return itemView;
        }
    }
    //Async task that fetches the json array from the url
    public class JSONLoadTask extends AsyncTask<String, Void, JSONArray> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setMessage("Loading ..");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(true);
            pDialog.show();
        }
        @Override
        protected JSONArray doInBackground(String... params) {
            //creating an instance of the JSON parser class
            JSONParser jParser = new JSONParser();
            //recieveing the parsed JSON Object
            JSONArray object = jParser.getJsonObject(params[0]);
            return object;
        }

        @Override
        protected void onPostExecute(JSONArray result) {
            super.onPostExecute(result);
            //add retreived JSON objects to the array list with only the used information to optimize memory usage
            for(int i=0;i<result.length();i++){
                JSONObject e = null;
                try {
                    e = result.getJSONObject(i);
                } catch (JSONException e1) {
                    e1.printStackTrace();
                }
                try {
                    populateReposList(e.getString("name"),e.getString("description"),e.getJSONObject("owner").getString("login"),e.getString("fork"),e.getJSONObject("owner").getString("html_url"),e.getString("html_url"));
                } catch (JSONException e1) {
                    e1.printStackTrace();
                }
            }
            try {
                // Save the list of entries to internal storage
                InternalStorage.writeObject(MainActivity.this, "KEY", reposlist);

            } catch (IOException e) {
//                Log.e(TAG, e.getMessage());
            }
            populateListView();
            registerClickCallback();
            pDialog.dismiss();
        }

    }
// endless scroller listener that fetches the next page when the end of the list is reached
    public class EndlessScrollListener implements AbsListView.OnScrollListener {

        private int visibleThreshold = 0;
        private int currentPage = 1;
        private int previousTotal = 0;
        private boolean loading = true;

        public EndlessScrollListener() {
        }
        public EndlessScrollListener(int visibleThreshold) {
            this.visibleThreshold = visibleThreshold;
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem,
                             int visibleItemCount, int totalItemCount) {
            if (loading) {
                if (totalItemCount > previousTotal) {
                    loading = false;
                    previousTotal = totalItemCount;
                    currentPage++;
                }
            }
            if (!loading && (totalItemCount - visibleItemCount) <= (firstVisibleItem + visibleThreshold)) {
                incpage();
                loading = true;
            }
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
        }
    }

}
