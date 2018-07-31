package com.timedancing.easyfirewall.fragment;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;

import com.timedancing.easyfirewall.BuildConfig;
import com.timedancing.easyfirewall.R;
import com.timedancing.easyfirewall.constant.AppGlobal;
import com.timedancing.easyfirewall.core.logger.Logger;
import com.timedancing.easyfirewall.filter.TimeDurationFilter;
import com.timedancing.easyfirewall.filter.TimeRangeFilter;
import com.timedancing.easyfirewall.util.SharedPrefUtil;

import java.util.Calendar;
import java.util.Date;

public class TimeSettingFragment extends BaseSettingFragment implements View.OnClickListener,
        CompoundButton.OnCheckedChangeListener, TextWatcher {
    private static final String TAG = "TimeSettingFragment";
    private static final boolean DEBUG = BuildConfig.DEBUG;

    public static final String START_TIME = "ts_start_time";
    public static final String END_TIME = "ts_end_time";
    public static final String TYPE = "ts_type";
    public static final String DURATION = "ts_duration";

    public static final int TIME_RANGE = 0b01;
    public static final int TIME_DURATION = 0b10;

    private int mType;
    private Calendar mStartCalendar;
    private Calendar mEndCalendar;
    private Button mTimeRangeDateStartButton;
    private Button mTimeRangeTimeStartButton;
    private Button mTimeRangeDateEndButton;
    private Button mTimeRangeTimeEndButton;
    private int mDuration;

    @Override
    public String getTitle() {
        return "时间规则";
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setting_time, container, false);
        mTimeRangeDateStartButton = (Button) view.findViewById(R.id.time_range_date_start_button);
        mTimeRangeDateStartButton.setText(getDateString(mStartCalendar));
        mTimeRangeDateStartButton.setOnClickListener(this);
        mTimeRangeTimeStartButton = (Button) view.findViewById(R.id.time_range_time_start_button);
        mTimeRangeTimeStartButton.setText(getTimeString(mStartCalendar));
        mTimeRangeTimeStartButton.setOnClickListener(this);

        mTimeRangeDateEndButton = (Button) view.findViewById(R.id.time_range_date_end_button);
        mTimeRangeDateEndButton.setText(getDateString(mEndCalendar));
        mTimeRangeDateEndButton.setOnClickListener(this);
        mTimeRangeTimeEndButton = (Button) view.findViewById(R.id.time_range_time_end_button);
        mTimeRangeTimeEndButton.setText(getTimeString(mEndCalendar));
        mTimeRangeTimeEndButton.setOnClickListener(this);

        EditText timeDuration = (EditText) view.findViewById(R.id.time_duration_edit_text);
        timeDuration.addTextChangedListener(this);

        view.findViewById(R.id.time_save_button).setOnClickListener(this);
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mType = SharedPrefUtil.getInt(context, AppGlobal.GLOBAL_PREF_NAME, TYPE, 0);
        long startTime = SharedPrefUtil.getLong(context, AppGlobal.GLOBAL_PREF_NAME, START_TIME, -1);
        long endTime = SharedPrefUtil.getLong(context, AppGlobal.GLOBAL_PREF_NAME, END_TIME, -1);
        mStartCalendar = Calendar.getInstance();
        if (startTime > -1) {
            mStartCalendar.setTime(new Date(startTime));
        }
        mEndCalendar = Calendar.getInstance();
        if (endTime > -1) {
            mEndCalendar.setTime(new Date(endTime));
        }
        mDuration = SharedPrefUtil.getInt(context, AppGlobal.GLOBAL_PREF_NAME, DURATION, 0);
        if (DEBUG) {
            Log.d(TAG, "onAttach: type = " + mType);
            Log.d(TAG, "onAttach: startTime = " + startTime);
            Log.d(TAG, "onAttach: endTime = " + endTime);
            Log.d(TAG, "onAttach: mDuration = " + mDuration);
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        CheckBox cb = (CheckBox) getView().findViewById(R.id.time_range_check_box);
        cb.setChecked((mType & 0b01) == TIME_RANGE);
        cb.setOnCheckedChangeListener(this);
        cb = (CheckBox) getView().findViewById(R.id.time_duration_check_box);
        cb.setChecked((mType & 0b10) == TIME_DURATION);
        cb.setOnCheckedChangeListener(this);
        ((EditText) getView().findViewById(R.id.time_duration_edit_text)).setText(mDuration == 0 ? "" : mDuration + "");
        if (DEBUG) {
            Log.d(TAG, "onActivityCreated: mType & 0b01 = " + (mType & 0b01));
            Log.d(TAG, "onActivityCreated: mType & 0b10 = " + (mType & 0b10));
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        if (s.length() == 0) {
            return;
        }
        String text = s.toString();
        try {
            mDuration = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "请输入分钟数", Toast.LENGTH_SHORT).show();
            if (DEBUG) {
                Log.e(TAG, "afterTextChanged: time duration error", e);
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.time_range_date_start_button:
                showDatePickerDialog(true);
                break;
            case R.id.time_range_time_start_button:
                showTimePickerDialog(true);
                break;
            case R.id.time_range_date_end_button:
                showDatePickerDialog(false);
                break;
            case R.id.time_range_time_end_button:
                showTimePickerDialog(false);
                break;
            case R.id.time_save_button:
                SharedPrefUtil.saveInt(getContext(), AppGlobal.GLOBAL_PREF_NAME, TYPE, mType);
                if ((mType & TIME_RANGE) == TIME_RANGE) {
                    SharedPrefUtil.saveLong(getContext(), AppGlobal.GLOBAL_PREF_NAME, START_TIME, mStartCalendar.getTimeInMillis());
                    SharedPrefUtil.saveLong(getContext(), AppGlobal.GLOBAL_PREF_NAME, END_TIME, mEndCalendar.getTimeInMillis());
                    TimeRangeFilter.reload();
                    Logger.getInstance(getContext())
                            .insert(getString(R.string.set_time_range_x_to_x,
                                    getDateString(mStartCalendar) + " " + getTimeString(mStartCalendar),
                                    getDateString(mEndCalendar) + " " + getTimeString(mEndCalendar)));
                }
                if ((mType & TIME_DURATION) == TIME_DURATION && mDuration > 0) {
                    SharedPrefUtil.saveInt(getContext(), AppGlobal.GLOBAL_PREF_NAME, DURATION, mDuration);
                    TimeDurationFilter.reload();
                    Logger.getInstance(getContext())
                            .insert(getString(R.string.set_time_duration_x, mDuration + ""));
                }
                Toast.makeText(getContext(), R.string.succeeded_to_save_setting, Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.time_range_check_box:
                if (mType == 0) {
                    mType = TIME_RANGE;
                } else {
                    if (isChecked) {
                        mType |= TIME_RANGE;
                    } else {
                        mType &= 0b10;
                    }
                }
                if (DEBUG) {
                    Log.d(TAG, "onCheckedChanged: time range type = " + mType);
                }
                break;
            case R.id.time_duration_check_box:
                if (mType == 0) {
                    mType = TIME_DURATION;
                } else {
                    if (isChecked) {
                        mType |= TIME_DURATION;
                    } else {
                        mType &= 0b01;
                    }
                }
                if (DEBUG) {
                    Log.d(TAG, "onCheckedChanged: time duration type = " + mType);
                }
                break;
        }
    }

    public void showDatePickerDialog(final boolean isStart) {
        final Calendar calendar = isStart ? mStartCalendar : mEndCalendar;
        new DatePickerDialog(getContext(), R.style.AlertDialog, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        String dateTemplate = String.format("%4d-%2d-%2d", year, monthOfYear + 1, dayOfMonth);
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, monthOfYear);
                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        if (isStart) {
                            mTimeRangeDateStartButton.setText(dateTemplate);
                        } else {
                            mTimeRangeDateEndButton.setText(dateTemplate);
                        }
                    }
                }
                ,calendar.get(Calendar.YEAR)
                ,calendar.get(Calendar.MONTH)
                ,calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    public void showTimePickerDialog(final boolean isStart) {
        final Calendar calendar = isStart ? mStartCalendar : mEndCalendar;
        new TimePickerDialog(getContext(), R.style.AlertDialog, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                String timeTemplate = String.format("%2d:%2d", hourOfDay, minute);
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
                if (isStart) {
                    mTimeRangeTimeStartButton.setText(timeTemplate);
                } else {
                    mTimeRangeTimeEndButton.setText(timeTemplate);
                }
            }
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true)
        .show();
    }

    public static String getDateString(Calendar calendar) {
        int year = calendar.get(Calendar.YEAR);
        int monthOfYear = calendar.get(Calendar.MONTH);
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
        return String.format("%4d-%2d-%2d", year, monthOfYear + 1, dayOfMonth);
    }

    public static String getTimeString(Calendar calendar) {
        int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        return String.format("%2d:%2d", hourOfDay, minute);
    }
}
