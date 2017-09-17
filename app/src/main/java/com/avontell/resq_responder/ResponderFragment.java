package com.avontell.resq_responder;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * The home page of the responder's application
 * @author Aaron Vontell
 */
public class ResponderFragment extends Fragment {

    private CardView quickView;
    private TextView quickTitle;
    private TextView quickExtra;
    private ImageView quickIcon;
    private TextView teamView;
    private LinearLayout resqueueView;

    public ResponderFragment() {

    }

    /**
     * Update the quick information regarding this disaster
     * @param status
     * @param quickInfo
     * @param extraInfo
     */
    public void updateDisasterInfo(DisasterStatus status, String quickInfo, String extraInfo) {

        quickExtra.setVisibility(View.VISIBLE);

        switch (status) {
            case APPROACHING:
                quickView.setCardBackgroundColor(getActivity().getColor(R.color.notifWarn));
                quickTitle.setText(quickInfo);
                quickExtra.setText(extraInfo);
                quickIcon.setImageDrawable(getActivity().getDrawable(R.drawable.caution_icon));
                break;
            case CLEAR:
                quickView.setCardBackgroundColor(getActivity().getColor(R.color.notifGood));
                quickTitle.setText("No Nearby Disasters");
                quickExtra.setVisibility(View.GONE);
                quickIcon.setImageDrawable(getActivity().getDrawable(R.drawable.sunny_icon));
                break;
            case IN_PROGRESS:
                quickView.setCardBackgroundColor(getActivity().getColor(R.color.notifWarn));
                quickTitle.setText(quickInfo);
                quickExtra.setText(extraInfo);
                quickIcon.setImageDrawable(getActivity().getDrawable(R.drawable.track_icon));
                break;
            case AFTERMATH:
                quickView.setCardBackgroundColor(getActivity().getColor(R.color.notifComing));
                quickTitle.setText(quickInfo);
                quickExtra.setText(extraInfo);
                quickIcon.setImageDrawable(getActivity().getDrawable(R.drawable.paint_icon));
                break;
        }

    }

    public void updateTeam(String team) {
        if (team == null) {
            teamView.setText("Unassigned");
        } else {
            teamView.setText(team);
        }
    }

    public void updateResqueue() {
        new UpdateResqueueTask().execute();
    }

    public void addShelter() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.responder_fragment, container, false);

        // Attach all views
        quickView = rootView.findViewById(R.id.quick_view);
        quickTitle = rootView.findViewById(R.id.quick_title);
        quickIcon = rootView.findViewById(R.id.quick_icon);
        quickExtra = rootView.findViewById(R.id.quick_info);
        teamView = rootView.findViewById(R.id.team_view);
        resqueueView = rootView.findViewById(R.id.resqueue);

        //updateDisasterInfo(DisasterStatus.CLEAR, null, null);
        updateDisasterInfo(DisasterStatus.AFTERMATH, "Hurricane Irma", "Category 5 hurricane heading toward Florida.");
        updateTeam("Alpha Blue Dogs");
        updateResqueue();

        return rootView;
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

            resqueueView.removeAllViews();
            for (int i = 0; i < result.length(); i++) {
                try {

                    JSONObject person = result.getJSONObject(i);

                    // Inflate and add the layout
                    LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    View personView = inflater.inflate(R.layout.people_item, null);
                    TextView personName = personView.findViewById(R.id.resq_name);
                    personName.setText(person.getString("name"));
                    resqueueView.addView(personView);

                } catch (Exception e) {

                }
            }

        }

    }

}
