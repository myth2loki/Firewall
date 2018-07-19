package com.timedancing.easyfirewall.component;

import android.app.Dialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.timedancing.easyfirewall.R;

public class InputDialog extends Dialog implements View.OnClickListener {
    private String title;
    private OnOKClickListener mListener;

    public InputDialog(@NonNull Context context, String title) {
        super(context);
        this.title = title;
        initView();
    }

    public void setListener(OnOKClickListener listener) {
        mListener = listener;
    }

    private void initView() {
        setContentView(R.layout.layout_input_dialog);
        findViewById(R.id.cancel).setOnClickListener(this);
        findViewById(R.id.ok).setOnClickListener(this);
        TextView titleTv = (TextView) findViewById(R.id.title);
        titleTv.setText(title);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cancel:
                dismiss();
                break;
            case R.id.ok:
                saveData();
                dismiss();
                break;
        }
    }

    private void saveData() {
        EditText edit = (EditText) findViewById(R.id.input);
        String content = edit.getEditableText().toString();
        if (mListener != null) {
            mListener.onClick(content);
        }
    }

    public interface OnOKClickListener {
        void onClick(String text);
    }
}
