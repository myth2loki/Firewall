package com.timedancing.easyfirewall.activity;

import android.os.Bundle;

import com.timedancing.easyfirewall.R;
import com.timedancing.easyfirewall.fragment.UnlockFragment;

public class UnlockActivity extends BaseActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle(R.string.parent_login);
		setContentView(R.layout.activity_lock);
		getSupportFragmentManager().beginTransaction()
				.add(R.id.main_root, new UnlockFragment())
				.commit();
	}

}
