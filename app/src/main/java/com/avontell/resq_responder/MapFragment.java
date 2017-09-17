package com.avontell.resq_responder;

import android.content.Context;
import android.content.DialogInterface;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * A map for the responders
 * @author Aaron Vontell
 */
public class MapFragment extends Fragment implements OnMapReadyCallback {

    private SupportMapFragment fragment;
    private GoogleMap map;

    public MapFragment() {

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        FragmentManager fm = getChildFragmentManager();
        fragment = (SupportMapFragment) fm.findFragmentById(R.id.map_container);
        if (fragment == null) {
            fragment = SupportMapFragment.newInstance();
            fm.beginTransaction().replace(R.id.map_container, fragment).commit();
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        fragment.getMapAsync(this);
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.map_fragment, container, false);

        return rootView;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setMyLocationEnabled(true);
        new UpdateResqueueTask().execute();
    }

    class UpdateResqueueTask extends AsyncTask<Void, Void, Void> {

        JSONArray result = new JSONArray();

        @Override
        protected Void doInBackground(Void... voids) {
            result = ResQApi.getTriage();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            LatLng toMove = null;

            for (int i = 0; i <= result.length(); i++) {
                try {

                    JSONObject person = result.getJSONObject(i);
                    JSONObject location = person.getJSONArray("location").getJSONObject(0);
                    double lat = location.getDouble("lat");
                    double lon = location.getDouble("long");
                    LatLng loc = new LatLng(lat, lon);
                    map.addMarker(new MarkerOptions().position(loc)
                            .title(person.getString("name")));

                    if (toMove == null) {
                        toMove = loc;
                    }

                } catch (Exception e) {

                }
            }

            map.moveCamera(CameraUpdateFactory.newLatLngZoom(toMove, 13));

        }

    }

}
