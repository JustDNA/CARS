package blogspot.destinationengineering.MyMaps;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class numbers extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.prefs);
	}

}
