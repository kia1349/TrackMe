package ie.nuigalway.trackme.fragment;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;

import ie.nuigalway.trackme.R;
import ie.nuigalway.trackme.helper.CloudDBHandler;
import ie.nuigalway.trackme.helper.GPSHelper;
import ie.nuigalway.trackme.helper.LocalDBHandler;
import ie.nuigalway.trackme.helper.MessageHandler;
import ie.nuigalway.trackme.helper.SessionManager;
import ie.nuigalway.trackme.services.FallDetectionService;
import ie.nuigalway.trackme.services.GPSService;

public class HomeFragment extends Fragment implements OnMapReadyCallback, View.OnClickListener{

    private static final int fl = 1;
    private static final int ss = 2;
    private static final int fd = 3;

    private static final String TAG = HomeFragment.class.getSimpleName();

    private OnFragmentInteractionListener mListener;
    private static final String IDENTIFIER = "Location";
    //private static final String KEY = "locData";

    private static final String LAT = "latData";
    private static final String LNG = "lngData";
    private static final String CDT = "timeData";
    private LatLng currentLocation;
    private GPSHelper gh;
    private CloudDBHandler cdb;
    private LocalDBHandler ldb;
    private MessageHandler mh;
    private GoogleMap map;
    private int aflCheck, smsCheck;
    private Button trackMeButton, trackUserButton, smsBtn;
    private SessionManager sm;
   // private ProgressDialog pd;
    private Context ctx;



    public HomeFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
    }
    @Override
    public void onResume() {

        super.onResume();
        getActivity().registerReceiver(rec, new IntentFilter(IDENTIFIER));

    }
    @Override
    public void onPause() {

        super.onPause();
        getActivity().unregisterReceiver(rec);

    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_home, null, false);

        ctx = getContext();

        sm = new SessionManager(ctx);
        ldb = new LocalDBHandler(ctx);
        cdb = new CloudDBHandler(ctx);
        mh = new MessageHandler(ctx);
        trackMeButton = (Button) v.findViewById(R.id.trackme_button);
        trackMeButton.setOnClickListener(this);

        trackUserButton = (Button) v.findViewById(R.id.trackuser_button);
        trackUserButton.setOnClickListener(this);

        smsBtn = (Button) v.findViewById(R.id.send_sms);
        smsBtn.setOnClickListener(this);

        aflCheck = ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION);

        Log.i(TAG, "Permission already given to access location using device");

        if (aflCheck != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Requesting permission to access location");

            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    fl);
        }

        SupportMapFragment smf = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        smf.getMapAsync(this);

        return v;
    }

    @Override
    public void onAttach(Context context) {
        ctx = context;
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        if(aflCheck!=-1){
            try{
                getMap();
            }catch(IOException e){

                e.printStackTrace();
            }
        }
    }

    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults)  {

        switch (requestCode) {

            case fl: {

                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    aflCheck = ContextCompat.checkSelfPermission(getActivity(),
                            Manifest.permission.ACCESS_FINE_LOCATION);
                    Log.i(TAG, "Checking if location permission given. Status: "+aflCheck);

                    try{
                        getMap();
                    }catch(IOException e){

                        e.printStackTrace();
                    }
                }
                else{
                    Log.i(TAG, "Location permission denied by user"+aflCheck);
                }
                }
            case ss: {

                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    smsCheck = ContextCompat.checkSelfPermission(getActivity(),
                            Manifest.permission.SEND_SMS);
                    Log.i(TAG, "Checking if SMS permission given. Status: "+smsCheck);

                    //mh.sendMessage();
                }
                else{
                    Log.i(TAG, "SMS permission denied by user"+smsCheck);
                }
                
            }
        }
    }

    private void getMap() throws IOException{

        String address;
        gh = new GPSHelper(ctx);
        if(!gh.checkInternetServiceAvailable()) {

            new AlertDialog.Builder(getActivity()).
                    setTitle("No Connection").
                    setMessage("Enable Internet Connection For Better Accuracy.\n"+
                            "Otherwise Addresses Can't Be Displayed").
                    setNeutralButton("Close", null).show();
        }

        currentLocation = gh.getCurrentStaticLocation();
        map.setPadding(10, 10, 10, 10);
        address = gh.getAddressString(currentLocation);

        Log.d(TAG, "User Location is :" + address );
        map.addMarker(new MarkerOptions().position(currentLocation).title(address));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.trackme_button:

                if(!sm.isGPSServiceRunning()) {
                    Log.d(TAG,"Should be false: " +String.valueOf(sm.isGPSServiceRunning()));
                    Log.i(TAG,"Button Click :"+sm.isGPSServiceRunning());
                    Toast.makeText(ctx, "Starting GPS Tracking Service", Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Starting Service: " + GPSService.class.getSimpleName());
                    Intent intent = new Intent(getActivity(), GPSService.class);
                    getActivity().startService(intent);
                }else if(sm.isGPSServiceRunning()){
                    Log.d(TAG,"Should be true: " +String.valueOf(sm.isGPSServiceRunning()));
                    Toast.makeText(ctx, "Tracking Already Started", Toast.LENGTH_LONG).show();
                }
                break;


            case R.id.trackuser_button:

                final EditText txtUrl = new EditText(ctx);
                txtUrl.setHint("Enter Username Here");
                new AlertDialog.Builder(ctx)
                        .setTitle("Track User")
                        .setMessage("Enter Username Of User You Would Like To Track")
                        .setView(txtUrl)
                        .setPositiveButton("Track", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                String email = txtUrl.getText().toString();
                                if(email.isEmpty()){

                                    Toast.makeText(ctx, "Please Enter Username To Track", Toast.LENGTH_LONG).show();
                                }else {

                                    Bundle bundle = new Bundle();
                                    bundle.putSerializable("email", email);
                                    TrackUserFragment fragment = new TrackUserFragment();
                                    fragment.setArguments(bundle);

                                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                                    ft.replace(R.id.flContent, fragment );
                                    ft.commit();
                                }
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        })
                        .show();
                break;

            case R.id.send_sms:

                Intent intent = new Intent(getActivity(), FallDetectionService.class);
                getActivity().startService(intent);

                Log.d(TAG, "Starting Service: " + FallDetectionService.class.getSimpleName());

                break;


        }
    }

    private BroadcastReceiver rec = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {

            map.clear();

            Double lat = intent.getDoubleExtra(LAT,0.0);
            Double lon = intent.getDoubleExtra(LNG,0.0);
            String cdt = intent.getExtras().get(CDT).toString();
            Log.d(TAG, "onReceive Location Data: "+ldb.getUserLocation().toString());

            LatLng currPos = new LatLng(lat,lon);
            try {
                map.addMarker(new MarkerOptions().position(currPos).title(gh.getShortAddressString(currPos)+" @ "+cdt
                        ));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };
}




