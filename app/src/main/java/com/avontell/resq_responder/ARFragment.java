package com.avontell.resq_responder;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * The AR view of the responder's application
 * @author Aaron Vontell
 */
public class ARFragment extends Fragment {

    private OverlayView overlay;
    private CardView arCard;
    private TextView arName;
    private TextView arCond;
    private TextView arKids;
    private TextView arPets;

    public ARFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.ar_fragment, container, false);

        FrameLayout arViewPane = (FrameLayout) rootView.findViewById(R.id.ar_view_pane);
        arCard = rootView.findViewById(R.id.ar_card);
        arCard.setVisibility(View.GONE);

        ArDisplayView arDisplay = new ArDisplayView(getActivity(), getActivity());
        arViewPane.addView(arDisplay);
        Camera cam = arDisplay.getCamera();
        overlay = new OverlayView(getContext(), cam, this);
        arViewPane.addView(overlay);

        arName = rootView.findViewById(R.id.ar_card_name);
        arCond = rootView.findViewById(R.id.ar_card_dis);
        arKids = rootView.findViewById(R.id.ar_card_kid);
        arPets = rootView.findViewById(R.id.ar_card_pet);

        new UpdateResqueueTask().execute();

        return rootView;
    }

    private boolean animating = false;
    private boolean currentlyShowing = false;

    public void acceptARUpdate(String provider, boolean visible) {

        Log.e("BOOLZ", "" + visible + " " + currentlyShowing);

        if (visible && !currentlyShowing) {
            arCard.setVisibility(View.VISIBLE);
            arCard.setTranslationY(arCard.getHeight() * 1.3f);
            arCard.animate()
                    //.alphaBy(1.0f)
                    .translationY(0)
                    .setDuration(700)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                        }
                    });
            currentlyShowing = true;
        }

        if (!visible && currentlyShowing) {
            arCard.setTranslationY(0);
            arCard.animate()
                    //.alphaBy(-1.0f)
                    .translationY(arCard.getHeight() * 1.3f)
                    .setDuration(700)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                        }
                    });
            currentlyShowing = false;
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
            overlay.providePeople(result);

            try {
                JSONObject person = result.getJSONObject(0);
                arName.setText(person.getString("name"));
                arCond.setText("Asthma");
                arKids.setText(person.getInt("kids") + " kids");
                arPets.setText(person.getInt("animals") + " pets");
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }

}
