package blogspot.destinationengineering.MyMaps;


public class MyMapsActivity extends MapActivity implements LocationListener {
    /** Called when the activity is first created. */
	MapView map;
	MyLocationOverlay compass;
    MapController controller;
    Drawable d;
	// Debugging
    private static final String TAG = "BluetoothChat";
    private static final boolean D = true;
    private static final String ROBOTNAME = "linvor";
    
    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Array adapter for the conversation thread
    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothChatService mChatService = null;
    private static String ambNo,copNo;
    
    LocationManager lm;
    String towers;
    Location location;
    int lat,longi;
    static int lati=0,longit=0;
    List<Overlay> overlayList;
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        map = (MapView) findViewById(R.id.mv);
        map.setBuiltInZoomControls(true);
        map.setTraffic(true);
        d = getResources().getDrawable(R.drawable.point);
        
        SharedPreferences getPref = PreferenceManager.getDefaultSharedPreferences(this);
        ambNo = getPref.getString("amb", "108");
        copNo = getPref.getString("cop", "100");
        compass = new MyLocationOverlay(MyMapsActivity.this, map);
        
        if(D) Log.e(TAG, "+++ ON CREATE +++");

       /* ***LocationManager mlocManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);*/
		lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		Criteria c = new Criteria();
		towers = lm.getBestProvider(c, false);
		
/* ***LocationListener mlocListener = new MyLocationListener();
        mlocManager.requestLocationUpdates( LocationManager.GPS_PROVIDER, 0, 0, mlocListener);*/
    
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
}
	
	 @Override
	    public void onStart() {
	        super.onStart();
	        if(D) Log.e(TAG, "++ ON START ++");
	        // If BT is not on, request that it be enabled.
	        // setupChat() will then be called during onActivityResult
	        if (!mBluetoothAdapter.isEnabled()) {
	        	if(D) Log.e(TAG, "++ ENTERING BLOCK 1 ++");
		           Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
	            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
	        	if(D) Log.e(TAG, "++ EXITING ENTERING BLOCK 1 ++");
		          // Otherwise, setup the chat session
	        } else {
	        	if(D) Log.e(TAG, "++ BLOCK 2 ++");
		         if (mChatService == null) setupChat();
	            SharedPreferences getPref = PreferenceManager.getDefaultSharedPreferences(this);
	            ambNo = getPref.getString("amb", "108");
	            copNo = getPref.getString("cop", "100");
	         
	        }
	    }

	    @Override
	    public synchronized void onResume() {
	        try{
	        	compass.enableCompass();
	        }catch(Exception e){
	        	
	        }
	    	super.onResume();
	        if(D) Log.e(TAG, "+ ON RESUME +");
	       try{ overlayList = map.getOverlays();
	       overlayList.add(compass);
	       location = lm.getLastKnownLocation(towers);
	       if(location!= null){
	    	   lat = (int)(location.getLatitude() *1E6);
	    		longi = (int)(location.getLongitude() *1E6);	   
	    		GeoPoint point = new GeoPoint(lat,longi);
	            controller = map.getController();
	            controller.animateTo(point);
	             controller.setZoom(18);
	             OverlayItem overlayitem = new OverlayItem(point, "blah", "blah");
	             CustomPinpoint custom = new CustomPinpoint(d,MyMapsActivity.this); 
	             custom.insertPinpoint(overlayitem);
	             overlayList.add(custom);
	       }else{
	    	   Toast.makeText( getApplicationContext(), "Location detection failed!", Toast.LENGTH_SHORT).show();
	       }
		 }catch(Exception e){
	            	//do nothing
	            }
	       lm.requestLocationUpdates(towers, 500, 1, this);
	        // Performing this check in onResume() covers the case in which BT was
	        // not enabled during onStart(), so we were paused to enable it...
	        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
	        if (mChatService != null) {
	            // Only if the state is STATE_NONE, do we know that we haven't started already
	            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
	              // Start the Bluetooth chat services
	              mChatService.start();
	            }
	        }
	    }

	    private void setupChat() {
	        Log.d(TAG, "setupChat()");

	     

	        // Initialize the BluetoothChatService to perform bluetooth connections
	        mChatService = new BluetoothChatService(this, mHandler);

	        // Initialize the buffer for outgoing messages
	        mOutStringBuffer = new StringBuffer("");
	    }

	    @Override
	    public synchronized void onPause() {
	    try{
	    	compass.disableCompass();
	    }catch(Exception e){
	    	
	    }
	    	super.onPause();
	    	lm.removeUpdates(this);
	    	if(D) Log.e(TAG, "- ON PAUSE -");
	    }

	    @Override
	    public void onStop() {
	    	super.onStop();
	    	try{
	        	if (mChatService != null) mChatService.stop();
	        }catch(Exception e){
	        	// do nothing
	        }
	    	
	    	if(D) Log.e(TAG, "-- ON STOP --");
	    }

	    @Override
	    public void onDestroy() {
	    	super.onDestroy();
	        try{
	        	if (mChatService != null) mChatService.stop();
	        	mBluetoothAdapter.disable();
	        }catch(Exception e){
	        	// do nothing
	        }
	        finish();
	        if(D) Log.e(TAG, "--- ON DESTROY ---");
	    }

	    
	    
	    private void ensureDiscoverable() {
	        if(D) Log.d(TAG, "ensure discoverable");
	        if (mBluetoothAdapter.getScanMode() !=
	            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
	            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
	            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
	            startActivity(discoverableIntent);
	        }
	    }

	    /**
	     * Sends a message.
	     * @param message  A string of text to send.
	     */
	    private void sendMessage(String message) {
	        // Check that we're actually connected before trying anything
	        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
	            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
	            return;
	        }

	        // Check that there's actually something to send
	        if (message.length() > 0) {
	            // Get the message bytes and tell the BluetoothChatService to write
	            byte[] send = message.getBytes();
	            mChatService.write(send);

	            // Reset out string buffer to zero and clear the edit text field
	            mOutStringBuffer.setLength(0);
	        }
	    }

	    

	    // The Handler that gets information back from the BluetoothChatService
	    private final Handler mHandler = new Handler() {
	        @Override
	        public void handleMessage(Message msg) {
	            switch (msg.what) {
	            case MESSAGE_STATE_CHANGE:
	                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
	                switch (msg.arg1) {
	                case BluetoothChatService.STATE_CONNECTED:
	                    break;
	                case BluetoothChatService.STATE_CONNECTING:
	                    break;
	                case BluetoothChatService.STATE_LISTEN:
	                case BluetoothChatService.STATE_NONE:
	                    break;
	                }
	                break;
	            case MESSAGE_WRITE:
	                byte[] writeBuf = (byte[]) msg.obj;
	                // construct a string from the buffer
	                String writeMessage = new String(writeBuf);
	                break;
	            case MESSAGE_READ:
	                byte[] readBuf = (byte[]) msg.obj;
	                // construct a string from the valid bytes in the buffer
	                String readMessage = new String(readBuf, 0, msg.arg1);
	                Toast.makeText(getApplicationContext(), "Sending messages to emergency services!", Toast.LENGTH_LONG).show();
	                {
	                	String phoneNumber1 = ambNo;
	            	    String phoneNumber2 = copNo;
	            	    Geocoder geocoder = new Geocoder(getBaseContext(),Locale.getDefault());
	            	    try{
	            	    	GeoPoint pointx = new GeoPoint(lati,longit);
	            	    List<Address> address = geocoder.getFromLocation(pointx.getLatitudeE6()/(1E6),pointx.getLongitudeE6()/(1E6), 1);	
	            	    String add = "";
	            	    if(address.size() > 0){
	            	    	for(int i = 0; i < address.get(0).getMaxAddressLineIndex() ; i++){
	            				add += address.get(0).getAddressLine(i) + "\n";}}
	            				Calendar cal = Calendar.getInstance();
	            			    int Sec = cal.get(Calendar.SECOND);
	            			    int Min = cal.get(Calendar.MINUTE);
	            			    int Hr = cal.get(Calendar.HOUR_OF_DAY);
	            			    String message;
	            			    if(Hr > 12){
	            			    	Hr-=12;
	            			    message = "ACCIDENT AT :\n" + lati/(1E6) + " LATITUDE \n" + longit/(1E6) + " LONGITUDE \n\n" + "TIME: " + Hr + ":" + Min + ":" + Sec + " PM"; 
	            			    }else{
	            			    message = "ACCIDENT AT :\n" + lati/(1E6) + " LATITUDE \n" + longit/(1E6) + " LONGITUDE \n\n" + "TIME: " + Hr + ":" + Min + ":" + Sec + " AM"; 
	            				}
	            			    
	            			    message += "\n\nADDRESS:\n" + add;
	            			    SmsManager smsManager = SmsManager.getDefault();
	            			    smsManager.sendTextMessage(phoneNumber1, null, message, null, null);
	            			    smsManager.sendTextMessage(phoneNumber2, null, message, null, null);
	            		}catch(Exception e){
	            		
	            	    	Calendar cal = Calendar.getInstance();
	            		    int Sec = cal.get(Calendar.SECOND);
	            		    int Min = cal.get(Calendar.MINUTE);
	            		    int Hr = cal.get(Calendar.HOUR_OF_DAY);
	            		    String message;
	            		    if(Hr > 12){
	            		    	Hr-=12;
	            		    message = "ACCIDENT AT :\n" + lati/(1E6) + " LATITUDE \n" + longit/(1E6) + " LONGITUDE \n\n" + "TIME: " + Hr + ":" + Min + ":" + Sec + " PM"; 
	            		    }else{
	            		    message = "ACCIDENT AT :\n" + lati/(1E6) + " LATITUDE \n" + longit/(1E6) + " LONGITUDE \n\n" + "TIME: " + Hr + ":" + Min + ":" + Sec + " AM"; 
	            			}
	            		    SmsManager smsManager = SmsManager.getDefault();
	            		    smsManager.sendTextMessage(phoneNumber1, null, message, null, null);
	            		    smsManager.sendTextMessage(phoneNumber2, null, message, null, null);
	            			}
	            	    
	            	  
	                }
	                break;
	            case MESSAGE_DEVICE_NAME:
	                // save the connected device's name
	                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
	                Toast.makeText(getApplicationContext(), "Connected to "
	                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
	                break;
	            case MESSAGE_TOAST:
	                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
	                               Toast.LENGTH_SHORT).show();
	                Toast.makeText(getApplicationContext(), "Sending messages to emergency services!", Toast.LENGTH_LONG).show();
	                {
	                	String phoneNumber1 = ambNo;
	            	    String phoneNumber2 = copNo;
	            	    Geocoder geocoder = new Geocoder(getBaseContext(),Locale.getDefault());
	            	    try{
	            	    	GeoPoint pointx = new GeoPoint(lati,longit);
	            	    List<Address> address = geocoder.getFromLocation(pointx.getLatitudeE6()/(1E6),pointx.getLongitudeE6()/(1E6), 1);	
	            	    String add = "";
	            	    if(address.size() > 0){
	            	    	for(int i = 0; i < address.get(0).getMaxAddressLineIndex() ; i++){
	            				add += address.get(0).getAddressLine(i) + "\n";}}
	            				Calendar cal = Calendar.getInstance();
	            			    int Sec = cal.get(Calendar.SECOND);
	            			    int Min = cal.get(Calendar.MINUTE);
	            			    int Hr = cal.get(Calendar.HOUR_OF_DAY);
	            			    String message;
	            			    if(Hr > 12){
	            			    	Hr-=12;
	            			    message = "ACCIDENT AT :\n" + lati/(1E6) + " LATITUDE \n" + longit/(1E6) + " LONGITUDE \n\n" + "TIME: " + Hr + ":" + Min + ":" + Sec + " PM"; 
	            			    }else{
	            			    message = "ACCIDENT AT :\n" + lati/(1E6) + " LATITUDE \n" + longit/(1E6) + " LONGITUDE \n\n" + "TIME: " + Hr + ":" + Min + ":" + Sec + " AM"; 
	            				}
	            			    
	            			    message += "\n\nADDRESS:\n" + add;
	            			    SmsManager smsManager = SmsManager.getDefault();
	            			    smsManager.sendTextMessage(phoneNumber1, null, message, null, null);
	            			    smsManager.sendTextMessage(phoneNumber2, null, message, null, null);
	            		}catch(Exception e){
	            		
	            	    	Calendar cal = Calendar.getInstance();
	            		    int Sec = cal.get(Calendar.SECOND);
	            		    int Min = cal.get(Calendar.MINUTE);
	            		    int Hr = cal.get(Calendar.HOUR_OF_DAY);
	            		    String message;
	            		    if(Hr > 12){
	            		    	Hr-=12;
	            		    message = "ACCIDENT AT :\n" + lati/(1E6) + " LATITUDE \n" + longit/(1E6) + " LONGITUDE \n\n" + "TIME: " + Hr + ":" + Min + ":" + Sec + " PM"; 
	            		    }else{
	            		    message = "ACCIDENT AT :\n" + lati/(1E6) + " LATITUDE \n" + longit/(1E6) + " LONGITUDE \n\n" + "TIME: " + Hr + ":" + Min + ":" + Sec + " AM"; 
	            			}
	            		    SmsManager smsManager = SmsManager.getDefault();
	            		    smsManager.sendTextMessage(phoneNumber1, null, message, null, null);
	            		    smsManager.sendTextMessage(phoneNumber2, null, message, null, null);
	            			}
	            	    
	            	  
	                }
	                break;
	            }
	        }
	    };

	    public void onActivityResult(int requestCode, int resultCode, Intent data) {
	        if(D) Log.d(TAG, "onActivityResult " + resultCode);
	        switch (requestCode) {
	        case REQUEST_CONNECT_DEVICE:
	            // When DeviceListActivity returns with a device to connect
	            if (resultCode == Activity.RESULT_OK) {
	                // Get the device MAC address
	                String address = data.getExtras()
	                                     .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
	                // Get the BLuetoothDevice object
	                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
	                // Attempt to connect to the device
	                mChatService.connect(device);
	            }
	            break;
	        case REQUEST_ENABLE_BT:
	            // When the request to enable Bluetooth returns
	            if (resultCode == Activity.RESULT_OK) {
	                // Bluetooth is now enabled, so set up a chat session
	                setupChat();
	                Toast.makeText(this, "Currently preferred service numbers \n\n" + "AMBULANCE : " + ambNo + "\n\nPOLICE : " + copNo , Toast.LENGTH_LONG).show();
	            
	            } else {
	                // User did not enable Bluetooth or an error occured
	                Log.d(TAG, "BT not enabled");
	                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
	                finish();
	            }
	        }
	    }

	    @Override
	    public boolean onCreateOptionsMenu(Menu menu) {
	        MenuInflater inflater = getMenuInflater();
	        inflater.inflate(R.menu.option_menu, menu);
	        return true;
	    }

	    @Override
	    public boolean onOptionsItemSelected(MenuItem item) {
	        switch (item.getItemId()) {
	        case R.id.scan:
	            // Launch the DeviceListActivity to see devices and do scan
	        	try
	        	{
	        	mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	        	Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
	        	Iterator<BluetoothDevice> it = pairedDevices.iterator();
	        	while (it.hasNext())
	        	{BluetoothDevice bd = it.next();
	        	if (bd.getName().equalsIgnoreCase(ROBOTNAME)) {
	        		mChatService.connect(bd);
	        		return false;
	        		}
	        		}
	        		}
	        		catch (Exception e)
	        		{
	        		Log.e(TAG,"Failed in findRobot() " + e.getMessage());
	        		}
	        		return true;
	        case R.id.discoverable:
	            // Ensure this device is discoverable by others
	            ensureDiscoverable();
	            return true;
	        case R.id.nos:
	        	Intent OpenApp = new Intent("blogspot.destinationengineering.MyMaps.NUMBERS");
	        	startActivity(OpenApp);
	        	return true;
	        }
	        return false;
	    }


	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}

	public void onLocationChanged(Location l) {
		// TODO Auto-generated method stub
		lat = (int) (l.getLatitude() *1E6);
		lati = lat;
		longi = (int) (l.getLongitude() *1E6);
		longit = longi;
		GeoPoint point = new GeoPoint(lat,longi);
        controller = map.getController();
        controller.animateTo(point);
         controller.setZoom(18);
         OverlayItem overlayitem = new OverlayItem(point, "blah", "blah");
         CustomPinpoint custom = new CustomPinpoint(d,MyMapsActivity.this); 
         custom.insertPinpoint(overlayitem);
         overlayList.add(custom);

	}

	public void onProviderDisabled(String arg0) {
		// TODO Auto-generated method stub
		
	}

	public void onProviderEnabled(String arg0) {
		// TODO Auto-generated method stub
		
	}

	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		// TODO Auto-generated method stub
		
	}
	public void sendSMS() {

		GeoPoint point = new GeoPoint(lat,longi);
	    String phoneNumber1 = ambNo;
	    String phoneNumber2 = copNo;
	    
	    Toast.makeText(this, "Sending messages to Emergency services.", Toast.LENGTH_LONG).show();

		
	    Geocoder geocoder = new Geocoder(getBaseContext(),Locale.getDefault());
	    try{
	    List<Address> address = geocoder.getFromLocation(point.getLatitudeE6()/(1E6),point.getLongitudeE6()/(1E6), 1);	
	    String add = "";
	    if(address.size() > 0){
	    	for(int i = 0; i < address.get(0).getMaxAddressLineIndex() ; i++){
				add += address.get(0).getAddressLine(i) + "\n";
				Calendar cal = Calendar.getInstance();
			    int Sec = cal.get(Calendar.SECOND);
			    int Min = cal.get(Calendar.MINUTE);
			    int Hr = cal.get(Calendar.HOUR_OF_DAY);
			    String message;
			    if(Hr > 12){
			    	Hr-=12;
			    message = "ACCIDENT AT :\n" + lat/(1E6) + " LATITUDE \n" + longi/(1E6) + " LONGITUDE \n\n" + "TIME: " + Hr + ":" + Min + ":" + Sec + " PM"; 
			    }else{
			    message = "ACCIDENT AT :\n" + lat/(1E6) + " LATITUDE \n" + longi/(1E6) + " LONGITUDE \n\n" + "TIME: " + Hr + ":" + Min + ":" + Sec + " AM"; 
				}
			    
			    message += "\n\nADDRESS:\n" + add;
			    SmsManager smsManager = SmsManager.getDefault();
			    smsManager.sendTextMessage(phoneNumber1, null, message, null, null);
			    smsManager.sendTextMessage(phoneNumber2, null, message, null, null);
				}
	    }
	    }catch(Exception e){
		
	    	Calendar cal = Calendar.getInstance();
		    int Sec = cal.get(Calendar.SECOND);
		    int Min = cal.get(Calendar.MINUTE);
		    int Hr = cal.get(Calendar.HOUR_OF_DAY);
		    String message;
		    if(Hr > 12){
		    	Hr-=12;
		    message = "ACCIDENT AT :\n" + lat/(1E6) + " LATITUDE \n" + longi/(1E6) + " LONGITUDE \n\n" + "TIME: " + Hr + ":" + Min + ":" + Sec + " PM"; 
		    }else{
		    message = "ACCIDENT AT :\n" + lat/(1E6) + " LATITUDE \n" + longi/(1E6) + " LONGITUDE \n\n" + "TIME: " + Hr + ":" + Min + ":" + Sec + " AM"; 
			}
		    SmsManager smsManager = SmsManager.getDefault();
		    smsManager.sendTextMessage(phoneNumber1, null, message, null, null);
		    smsManager.sendTextMessage(phoneNumber2, null, message, null, null);
			}
	    
	    }
	
	
	
}