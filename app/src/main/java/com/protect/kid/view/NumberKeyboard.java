package com.protect.kid.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.TextView;

import com.protect.kid.R;

public class NumberKeyboard extends FrameLayout implements View.OnClickListener {

//	private int mWidth;
	private int mTextSize;

	private OnKeyboardInputListener mKeyboardInputListener;

	public NumberKeyboard(Context context, AttributeSet attrs) {
		super(context, attrs);

		DisplayMetrics dm = context.getResources().getDisplayMetrics();
		mTextSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12.0f, dm);
//		init(context);
	}

	public NumberKeyboard(Context context) {
		this(context, null);
	}

	private void init(Context context) {
		GridLayout gridLayout = new GridLayout(context);
		addView(gridLayout, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams
				.MATCH_PARENT));
		gridLayout.setColumnCount(3);
		gridLayout.setRowCount(4);

		GridLayout.Spec row;
		GridLayout.Spec col;
		for (int i = 1; i < 10; i++) {
			row = GridLayout.spec((i - 1) / 3);
			col = GridLayout.spec((i - 1) % 3);
			createButton(String.valueOf(i), i, gridLayout, row, col);
		}
		row = GridLayout.spec(3);
		col = GridLayout.spec(0);
		createButton("x", 10, gridLayout, row, col);
		row = GridLayout.spec(3);
		col = GridLayout.spec(1);
		createButton("0", 11, gridLayout, row, col);
		row = GridLayout.spec(3);
		col = GridLayout.spec(2);
		createButton("▶", 12, gridLayout, row, col);
	}

	private void createButton(String text, int index, GridLayout gridLayout, GridLayout.Spec row, GridLayout.Spec col) {
		int screenWidth = gridLayout.getContext().getResources().getDisplayMetrics().widthPixels;
		GridLayout.LayoutParams lp = new GridLayout.LayoutParams(row, col);
		lp.width = screenWidth / 3;
		lp.height = lp.width / 2;
		TextView textView = new TextView(gridLayout.getContext());
		textView.setText(text);
		textView.setGravity(Gravity.CENTER);
		textView.setClickable(true);
		textView.setTextColor(Color.WHITE);
		textView.setTextSize(mTextSize);
//		textView.setTypeface(Typeface.DEFAULT_BOLD);
		textView.setBackgroundResource(R.drawable.ripple_rect_round);
//			textView.setTag(i != 10 ? i : 0);
		textView.setTag(text);
		textView.setOnClickListener(this);
		gridLayout.addView(textView, lp);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//		int width = MeasureSpec.getSize(widthMeasureSpec);
//		int height = MeasureSpec.getSize(heightMeasureSpec);
//		mWidth = Math.min(width, height);
//		widthMeasureSpec = MeasureSpec.makeMeasureSpec(mWidth, MeasureSpec.EXACTLY);
//		heightMeasureSpec = MeasureSpec.makeMeasureSpec(mWidth, MeasureSpec.EXACTLY);
		removeAllViews();
		init(getContext());
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	@Override
	public void onClick(View v) {
		String number = v.getTag() + "";
		if (mKeyboardInputListener != null) {
			mKeyboardInputListener.onKeyboardInput(number);
		}
	}

	public void setKeyboardInputListener(OnKeyboardInputListener keyboardInputListener) {
		mKeyboardInputListener = keyboardInputListener;
	}

	public interface OnKeyboardInputListener {
		void onKeyboardInput(String number);
	}
}
