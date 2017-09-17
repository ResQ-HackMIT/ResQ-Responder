package com.avontell.resq_responder;

import android.hardware.Camera;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * The AR view of the responder's application
 * @author Aaron Vontell
 */
public class ARFragment extends Fragment {

    public ARFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.ar_fragment, container, false);

        FrameLayout arViewPane = (FrameLayout) rootView.findViewById(R.id.ar_view_pane);

        ArDisplayView arDisplay = new ArDisplayView(getActivity(), getActivity());
        arViewPane.addView(arDisplay);
        Camera cam = arDisplay.getCamera();
        OverlayView arContent = new OverlayView(getContext(), cam);
        arViewPane.addView(arContent);

        return rootView;
    }
}
