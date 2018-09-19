package com.protect.kid.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.protect.kid.BuildConfig;
import com.protect.kid.core.blackwhite.BlackContent;
import com.protect.kid.core.blackwhite.BlackIP;
import com.protect.kid.db.DAOFactory;
import com.protect.kid.db.GeneralDAO;
import com.protect.kid.filter.PushBlackContentFilter;
import com.protect.kid.filter.PushBlackIpFilter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import cn.jpush.android.api.JPushInterface;

public class JPushReceiver extends BroadcastReceiver {
    private static final boolean DEBUG = BuildConfig.DEBUG;
    private static final String TAG = "JPush";

    private static final String IP_AND_DOMAINS = "ipAndDomains";
    private static final String CONTENTS = "contents";

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        if (DEBUG) {
            Log.d(TAG, "onReceive: onReceive - " + intent.getAction() + ", extras: " + printBundle(bundle));
        }

        if (bundle == null) {
            if (DEBUG) {
                Log.w(TAG, "onReceive: no bundle found");
            }
            return;
        }

        if (JPushInterface.ACTION_REGISTRATION_ID.equals(intent.getAction())) {
            String regId = bundle.getString(JPushInterface.EXTRA_REGISTRATION_ID);
            if (DEBUG) {
                Log.d(TAG, "onReceive: 接收Registration Id : " + regId);
            }
            //send the Registration Id to your server...

        } else if (JPushInterface.ACTION_MESSAGE_RECEIVED.equals(intent.getAction())) {
            if (DEBUG) {
                Log.d(TAG, "onReceive: 接收到推送下来的自定义消息: " + bundle.getString(JPushInterface.EXTRA_MESSAGE));
            }
            processCustomMessage(context, bundle);

        } else if (JPushInterface.ACTION_NOTIFICATION_RECEIVED.equals(intent.getAction())) {
            if (DEBUG) {
                Log.d(TAG, "onReceive: 接收到推送下来的通知");
            }
            int notifactionId = bundle.getInt(JPushInterface.EXTRA_NOTIFICATION_ID);
            if (DEBUG) {
                Log.d(TAG, "onReceive: 接收到推送下来的通知的ID: " + notifactionId);
            }

        } else if (JPushInterface.ACTION_NOTIFICATION_OPENED.equals(intent.getAction())) {
            if (DEBUG) {
                Log.d(TAG, "onReceive: 用户点击打开了通知");
            }
            startActivity(context, bundle);


        } else if (JPushInterface.ACTION_RICHPUSH_CALLBACK.equals(intent.getAction())) {
            if (DEBUG) {
                Log.d(TAG, "onReceive: 用户收到到RICH PUSH CALLBACK: " + bundle.getString(JPushInterface.EXTRA_EXTRA));
            }
            //在这里根据 JPushInterface.EXTRA_EXTRA 的内容处理代码，比如打开新的Activity， 打开一个网页等..

        } else if(JPushInterface.ACTION_CONNECTION_CHANGE.equals(intent.getAction())) {
            boolean connected = intent.getBooleanExtra(JPushInterface.EXTRA_CONNECTION_CHANGE, false);
            if (DEBUG) {
                Log.w(TAG, "onReceive: " + intent.getAction() + " connected state change to " + connected);
            }
        } else {
            if (DEBUG) {
                Log.d(TAG, "onReceive:  Unhandled intent - " + intent.getAction());
            }
        }
    }

    /**
     * 打开推送信息对应的窗口
     * @param context
     * @param bundle
     */
    private void startActivity(Context context, Bundle bundle) {

    }

    /**
     * 打印所有的 intent extra 数据
     */
    private static String printBundle(Bundle bundle) {
        StringBuilder sb = new StringBuilder();
        for (String key : bundle.keySet()) {
            if (key.equals(JPushInterface.EXTRA_NOTIFICATION_ID)) {
                sb.append("\nkey:" + key + ", value:" + bundle.getInt(key));
            }else if(key.equals(JPushInterface.EXTRA_CONNECTION_CHANGE)){
                sb.append("\nkey:" + key + ", value:" + bundle.getBoolean(key));
            } else if (key.equals(JPushInterface.EXTRA_EXTRA)) {
                if (TextUtils.isEmpty(bundle.getString(JPushInterface.EXTRA_EXTRA))) {
                    Log.i(TAG, "This message has no Extra data");
                    continue;
                }

                try {
                    JSONObject json = new JSONObject(bundle.getString(JPushInterface.EXTRA_EXTRA));
                    Iterator<String> it =  json.keys();

                    while (it.hasNext()) {
                        String myKey = it.next();
                        sb.append("\nkey:" + key + ", value: [" +
                                myKey + " - " +json.optString(myKey) + "]");
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Get message extra JSON error!");
                }

            } else {
                sb.append("\nkey:" + key + ", value:" + bundle.getString(key));
            }
        }
        return sb.toString();
    }

    /**
     * send msg to MainActivity
     * @param context
     * @param bundle
     */
    private void processCustomMessage(Context context, Bundle bundle) {
        String message = bundle.getString(JPushInterface.EXTRA_MESSAGE);
//        String extras = bundle.getString(JPushInterface.EXTRA_EXTRA);
        if (TextUtils.isEmpty(message)) {
            Log.w(TAG, "processCustomMessage: message is null, ignore");
        }
        try {
            JSONObject jsonObject = new JSONObject(message);
            if (jsonObject.has(IP_AND_DOMAINS)) {
                JSONArray array = jsonObject.getJSONArray(IP_AND_DOMAINS);
                List<BlackIP> ipAndDomainList = new ArrayList<>();
                for (int i = 0; i < array.length(); i++) {
                    BlackIP ip = new BlackIP();
                    ip.ip = array.getString(i);
                    ipAndDomainList.add(ip);
                }
                GeneralDAO<BlackIP> dao = DAOFactory.getPushDAO(context, BlackIP.class);
                dao.create(ipAndDomainList);
                if (ipAndDomainList.size() > 0) {
                    PushBlackIpFilter.reload();
                }
                if (DEBUG) {
                    Log.d(TAG, "processCustomMessage: save ip domain to db: " + ipAndDomainList);
                }
            }

            if (jsonObject.has("contents")) {
                JSONArray array = jsonObject.getJSONArray(CONTENTS);
                List<BlackContent> blackContentList = new ArrayList<>();
                for (int i = 0; i < array.length(); i++) {
                    BlackContent content = new BlackContent();
                    content.content = array.getString(i);
                    blackContentList.add(content);
                }
                GeneralDAO<BlackContent> dao = DAOFactory.getPushDAO(context, BlackContent.class);
                dao.create(blackContentList);
                if (blackContentList.size() > 0) {
                    PushBlackContentFilter.reload();
                }
                if (DEBUG) {
                    Log.d(TAG, "processCustomMessage: save content to db: " + blackContentList);
                }
            }
        } catch (JSONException e) {
            if (DEBUG) {
                Log.e(TAG, "processCustomMessage: parse message failed: " + message, e);
            }
        }
    }
}
