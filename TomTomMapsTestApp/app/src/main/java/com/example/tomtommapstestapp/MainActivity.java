package com.example.tomtommapstestapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ServiceConnection;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.google.common.collect.ImmutableList;
import com.tomtom.online.sdk.common.location.LatLng;
import com.tomtom.online.sdk.map.Icon;
import com.tomtom.online.sdk.map.ApiKeyType;
import com.tomtom.online.sdk.map.MapFragment;
import com.tomtom.online.sdk.map.MapProperties;
import com.tomtom.online.sdk.map.MarkerAnchor;
import com.tomtom.online.sdk.map.MarkerBuilder;
import com.tomtom.online.sdk.map.OnMapReadyCallback;
import com.tomtom.online.sdk.map.SimpleMarkerBalloon;
import com.tomtom.online.sdk.map.TomtomMap;
import com.tomtom.online.sdk.search.api.SearchError;
import com.tomtom.online.sdk.search.api.fuzzy.FuzzySearchResultListener;
import com.tomtom.online.sdk.search.data.common.Poi;
import com.tomtom.online.sdk.search.data.fuzzy.FuzzySearchQuery;
import com.tomtom.online.sdk.search.data.fuzzy.FuzzySearchQueryBuilder;
import com.tomtom.online.sdk.search.data.fuzzy.FuzzySearchResponse;
import com.tomtom.online.sdk.search.data.fuzzy.FuzzySearchResult;
import com.tomtom.online.sdk.search.extensions.SearchService;
import com.tomtom.online.sdk.search.extensions.SearchServiceConnectionCallback;
import com.tomtom.online.sdk.search.extensions.SearchServiceManager;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "TomTomSampleApp";
    TomtomMap tomtomMap;
    SearchService tomtomSearch;
    ImmutableList<FuzzySearchResult> lastSearchResult;
    ResultListAdapter adapter;

    class ResultListAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            if(lastSearchResult == null)
                return 0;
            return lastSearchResult.size();
        }

        @Override
        public Object getItem(int position) {
            return lastSearchResult.get(position);
        }

        @Override
        public long getItemId(int position) {
            return lastSearchResult.get(position).hashCode();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup container) {
            if(convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.search_list_item, container, false);
            }

            TextView itemNameTextView = convertView.findViewById(R.id.result_name);
            FuzzySearchResult item = (FuzzySearchResult)getItem(position);
            itemNameTextView.setText(item.getPoi().getName());
            return convertView;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "starting");

        initTomTomServices();

        EditText searchEditText = findViewById(R.id.searchText);
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            LatLng mapCenter = tomtomMap.getCenterOfMap();

            FuzzySearchQuery searchQuery = FuzzySearchQueryBuilder.create(v.getText().toString())
                    .withPosition(mapCenter)
                    .build();
            tomtomSearch.search(searchQuery, new FuzzySearchResultListener() {
                @Override
                public void onSearchResult(FuzzySearchResponse fuzzySearchResponse) {
                    ImmutableList<FuzzySearchResult> results = fuzzySearchResponse.getResults();
                    showSearchResults(results);
                }
                @Override
                public void onSearchError(SearchError searchError) {
                }
            });




            return false;
        });

        Log.d(TAG, "Requesting search service");

        // To use the search API with the SearchServiceManager, you need to specify the API KEY in the AndroidManifest.xml
        ServiceConnection serviceConnection = SearchServiceManager.createAndBind(getBaseContext(),
                new SearchServiceConnectionCallback() {
                    @Override
                    public void onBindSearchService(SearchService searchService) {
                        Log.d(TAG,"Search service retrieved");
                        tomtomSearch = searchService;
                    }
                });

        adapter = new ResultListAdapter();
        ListView searchResultList = (ListView)findViewById(R.id.search_list);
        searchResultList.setClickable(true);
        searchResultList.setAdapter(adapter);
        searchResultList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                FuzzySearchResult result = (FuzzySearchResult)searchResultList.getItemAtPosition(position);
                LatLng geoposition = result.getPosition();
                tomtomMap.centerOn(geoposition);

            }
        });


    }

    void showSearchResults(ImmutableList<FuzzySearchResult> resultList)
    {
        Log.i(TAG, resultList.toString());
        this.lastSearchResult = resultList;
        adapter.notifyDataSetChanged();

        tomtomMap.clear();
        for(int i=0;i<lastSearchResult.size();++i)
        {
            LatLng geoposition = lastSearchResult.get(i).getPosition();
            Poi poi = lastSearchResult.get(i).getPoi();
            MarkerBuilder markerBuilder = new MarkerBuilder(geoposition)
                    .icon(Icon.Factory.fromResources(getBaseContext(), R.drawable.ic_favourites))
                    .markerBalloon(new SimpleMarkerBalloon(poi.getName()))
                    .tag(lastSearchResult.get(i).getAddress())
                    .iconAnchor(MarkerAnchor.Bottom)
                    .decal(true);
            tomtomMap.addMarker(markerBuilder);
        }
    }

    private void initTomTomServices() {
        Map<ApiKeyType, String> mapKeys = new HashMap<>();
        mapKeys.put(ApiKeyType.MAPS_API_KEY, "Your MAP API KEY");

        MapProperties mapProperties = new MapProperties.Builder()
                .keys(mapKeys)
                .build();

        Log.d(TAG, "Creating map fragment");
        MapFragment mapFragment = MapFragment.newInstance(mapProperties);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.map_fragment, mapFragment)
                .commit();

        Log.d(TAG, "Request map from map fragment");
        mapFragment.getAsyncMap(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull TomtomMap map) {
                Log.d(TAG, "Map retrieved");
                tomtomMap = map;
                tomtomMap.setMyLocationEnabled(true);;
                tomtomMap.centerOn(37, -121, 8);
            }
        });





    }
}
