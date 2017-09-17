package com.avontell.resq_responder;

import android.content.Context;
import android.content.DialogInterface;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.NumberFormat;

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
        updateDisasterInfo(DisasterStatus.CLEAR, "Loading...", "Loading your alert information.");
        updateDisasterInfo(DisasterStatus.IN_PROGRESS, "Loading...", "Loading your alert information.");
        updateTeam("Alpha Blue Dogs");
        updateResqueue();
        new UpdateStatusTask().execute();

        return rootView;
    }

    class UpdateStatusTask extends AsyncTask<Void, Void, Void> {

        JSONObject result;

        @Override
        protected Void doInBackground(Void ... voids) {

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            result = ResQApi.getStatus();
            Log.e("RESULT", result.toString());
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            DisasterStatus stat = DisasterStatus.IN_PROGRESS;
            try {
                switch (result.getString("status")) {
                    case "In progress":
                        stat = DisasterStatus.IN_PROGRESS;
                        break;
                    case "Clear":
                        stat = DisasterStatus.CLEAR;
                        break;
                    case "Approaching":
                        stat = DisasterStatus.APPROACHING;
                        break;
                    case "Aftermath":
                        stat = DisasterStatus.AFTERMATH;
                        break;
                }
                updateDisasterInfo(stat, result.getString("title"), result.getString("description"));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            new UpdateStatusTask().execute();

        }
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

            int[] colorList = new int[]{R.color.gradOne,R.color.gradTwo,R.color.gradThree,R.color.gradFour,R.color.gradFive};

            NumberFormat formatter = new DecimalFormat("#0.0");
            resqueueView.removeAllViews();
            for (int i = 0; i <= 4; i++) {
                try {

                    final JSONObject person = result.getJSONObject(i);

                    // Inflate and add the layout
                    LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    final View personView = inflater.inflate(R.layout.people_item, null);
                    personView.setBackgroundResource(colorList[i]);
                    TextView personName = personView.findViewById(R.id.resq_name);
                    personName.setText(person.getString("name"));
                    TextView distanceView = personView.findViewById(R.id.person_distance);
                    Location myLocation = ((MainActivity) getActivity()).getLocation();
                    double lat = person.getJSONArray("location").getJSONObject(0).getDouble("lat");
                    double lon = person.getJSONArray("location").getJSONObject(0).getDouble("long");
                    if (myLocation != null) {
                        double approxDist = Math.sqrt(Math.pow(myLocation.getLatitude() - lat, 2) + Math.pow(myLocation.getLongitude() - lon, 2));
                        distanceView.setText("" + formatter.format(approxDist) + "mi");
                    } else {
                        distanceView.setText("1.2 mi");
                    }

                    // Set the onClick behavior
                    personView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                            try {

                                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

                                builder.setTitle(person.getString("name"));
                                builder.setMessage("Medical condition status: " + person.getInt("medicalConditions") + "\n"
                                        + "Allergy status: " + person.getInt("allergies") + "\n"
                                        + "Medication status: " + person.getInt("medications") + "\n"
                                        + "Height: " + person.getInt("height") + " inches\n"
                                        + "Weight: " + person.getInt("weight") + " lbs.\n"
                                        + "Age: " + person.getInt("age") + "\n"
                                        + "Number of kids: " + person.getInt("kids") + "\n"
                                        + "Number of animals: " + person.getInt("animals") + "\n"
                                        + "Has spouse?: " + (person.getBoolean("spouse") ? "Yes" : "No") + "\n"
                                        + "Has vehicle: " + (person.getBoolean("hasTransportation") ? "Yes" : "No"));

                                String positiveText = "ResQ Maps";
                                builder.setPositiveButton(positiveText,
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.cancel();
                                            }
                                        });

                                String negativeText = getString(android.R.string.ok);
                                builder.setNegativeButton(negativeText,
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.cancel();
                                            }
                                        });

                                AlertDialog dialog = builder.create();
                                dialog.show();

                            } catch (Exception e) {

                            }

                        }
                    });

                    resqueueView.addView(personView);

                } catch (Exception e) {

                }
            }

        }

    }

}
