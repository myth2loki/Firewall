package com.timedancing.easyfirewall.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.timedancing.easyfirewall.BuildConfig;
import com.timedancing.easyfirewall.R;
import com.timedancing.easyfirewall.animation.SupportAnimator;
import com.timedancing.easyfirewall.animation.ViewAnimationUtils;
import com.timedancing.easyfirewall.cache.AppCache;
import com.timedancing.easyfirewall.cache.AppConfig;
import com.timedancing.easyfirewall.core.util.VpnServiceHelper;
import com.timedancing.easyfirewall.event.HostUpdateEvent;
import com.timedancing.easyfirewall.event.VPNEvent;
import com.timedancing.easyfirewall.network.HostHelper;
import com.timedancing.easyfirewall.util.DebugLog;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import de.greenrobot.event.EventBus;

public class MainActivity extends BaseActivity {
	private static final String TAG = "MainActivity";

	private View mImgStart;
	private View mImgEnd;
	private TextView mTvRun;
	private View mRippleView;
	private View mTipsView;
	private View mMaskView;
	private View mSettingView;

	private ProgressDialog mProgressDialog = null;
	private ProgressDialog mUpdateProgressDialog = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle(getString(R.string.home_page));
		setContentView(R.layout.activity_main);

		mImgEnd = findViewById(R.id.img_end);
		mImgStart = findViewById(R.id.img_start);
		mRippleView = findViewById(R.id.rippleView);
		mTvRun = (TextView) findViewById(R.id.btn_run);
		mMaskView = findViewById(R.id.blackMask);
		mTipsView = findViewById(R.id.tipsLayout);
		mSettingView = findViewById(R.id.img_setting);
		mTvRun.setSelected(VpnServiceHelper.vpnRunningStatus());
		mRippleView.setSelected(mTvRun.isSelected());
		mTvRun.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (AppConfig.isNeedShowTips(MainActivity.this)) {
					AppConfig.setIsNeedShowTips(MainActivity.this, false);
					mMaskView.setVisibility(View.GONE);
					mTipsView.setVisibility(View.GONE);
				}


				boolean nextStatus = !mTvRun.isSelected();
				if (!nextStatus) { //如果是关闭操作，立即更新界面
					changeButtonStatus(false);
				}
				startAnimation(nextStatus);
				if (VpnServiceHelper.vpnRunningStatus() != nextStatus) {
					VpnServiceHelper.changeVpnRunningStatus(MainActivity.this, nextStatus);
				}

			}
		});

		mMaskView.setVisibility(AppConfig.isNeedShowTips(this) ? View.VISIBLE : View.GONE);
		mTipsView.setVisibility(AppConfig.isNeedShowTips(this) ? View.VISIBLE : View.GONE);

		mSettingView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this, SettingActivity.class);
				startActivity(intent);
			}
		});

		mProgressDialog = new ProgressDialog(MainActivity.this);
		mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		mProgressDialog.setMessage(getString(R.string.preparing));

		mUpdateProgressDialog = new ProgressDialog(MainActivity.this);
		mUpdateProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		mUpdateProgressDialog.setMessage(getString(R.string.updating_config));

		AppCache.syncBlockCountWithLeanCloud(this);

		if (!VpnServiceHelper.vpnRunningStatus()) {
			HostHelper.updateHost(this);
		}

		//TEST
		sendUdp();
	}

	private void sendUdp() {
		new Thread() {
			public void run() {
				DatagramSocket datagramSocket = null;
				InetAddress dstAddress = null;
				try {
					dstAddress = InetAddress.getByName("baidu.com");
					datagramSocket = new DatagramSocket(0);
					datagramSocket.connect(dstAddress, 90);
				} catch (SocketException e) {
					e.printStackTrace();
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
				while (true) {
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					byte[] buff = "test".getBytes();
					int length = buff.length;
					DatagramPacket packet = null;
					packet = new DatagramPacket(buff, length, dstAddress, 90);
					try {
						datagramSocket.send(packet);
						if (BuildConfig.DEBUG) {
							Log.d(TAG, "run: send test udp, packet = " + packet.getLength());
						}
					} catch (SocketException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();
	}

	@Override
	protected void onStop() {
		super.onStop();
		EventBus.getDefault().unregister(this);
	}

	@Override
	protected void onDestroy() {
		if (mProgressDialog.isShowing()) {
			mProgressDialog.dismiss();
		}
		mProgressDialog = null;
		super.onDestroy();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == VpnServiceHelper.START_VPN_SERVICE_REQUEST_CODE) {
			if (resultCode == RESULT_OK) {
				VpnServiceHelper.startVpnService(this);
			} else {
				changeButtonStatus(false);
				DebugLog.e("canceled");
			}
			return;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (VpnServiceHelper.vpnRunningStatus()) {
			mImgStart.setVisibility(View.GONE);
			mTvRun.setText(R.string.shutdown);
		} else {
			mImgStart.setVisibility(View.VISIBLE);
			mTvRun.setText(R.string.start);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		EventBus.getDefault().registerSticky(this);
	}

	private void startAnimation(boolean isRunning) {
		SupportAnimator animator = null;
		int width = mImgStart.getWidth() / 2;
		int height = mImgStart.getHeight() / 2;
		double longRadius = Math.sqrt((width * width) + (height * height));
		if (isRunning) {
			animator = ViewAnimationUtils.createCircularReveal(mImgStart, width, height, (float) longRadius, 0);
			animator.addListener(new SupportAnimator.SimpleAnimationListener() {
				@Override
				public void onAnimationStart() {
					mImgStart.setVisibility(View.VISIBLE);
				}

				@Override
				public void onAnimationEnd() {
					mImgStart.setVisibility(View.GONE);
				}
			});
		} else {
			animator = ViewAnimationUtils.createCircularReveal(mImgStart, width, height, 0, (float) longRadius);
			animator.addListener(new SupportAnimator.SimpleAnimationListener() {
				@Override
				public void onAnimationEnd() {
					mImgStart.setVisibility(View.VISIBLE);
				}

				@Override
				public void onAnimationStart() {
					mImgStart.setVisibility(View.VISIBLE);
				}
			});
		}

		animator.setDuration(350);
		animator.start();
	}

	@SuppressWarnings("unused")
	public void onEventMainThread(VPNEvent event) {
		boolean selected = event.isEstablished();

		switch (event.getStatus()) {
			case STARTING:
				mProgressDialog.show();
				break;
			default:
				changeButtonStatus(selected);
				mTvRun.postDelayed(new Runnable() {
					@Override
					public void run() {
						if (mProgressDialog.isShowing()) {
							mProgressDialog.dismiss();
						}
					}
				}, 500);
				break;
		}

	}

	@SuppressWarnings("unused")
	public void onEventMainThread(HostUpdateEvent event) {
		switch (event.getStatus()) {
			case Updating:
				mUpdateProgressDialog.show();
				break;
			case UpdateFinished:
				mUpdateProgressDialog.dismiss();
				break;
		}
	}

	private void changeButtonStatus(boolean isRunning) {
		mTvRun.setSelected(isRunning);
		mTvRun.setText(mTvRun.isSelected() ? R.string.shutdown : R.string.start);
		mRippleView.setSelected(mTvRun.isSelected());
	}
}
