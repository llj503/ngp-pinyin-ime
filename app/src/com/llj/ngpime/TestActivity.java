package com.llj.ngpime;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Test screen: shows an EditText and forces our IME to open. */
public final class TestActivity extends Activity {
    private EditText edit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
                | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        getWindow().getDecorView().setBackgroundColor(0xFF0B0E13);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(20), dp(24), dp(12));
        root.setBackgroundColor(0xFF0B0E13);

        TextView title = new TextView(this);
        title.setText(R.string.test_title);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        title.setTextColor(0xFFE8EDF5);
        title.setGravity(Gravity.START);
        root.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView note = new TextView(this);
        note.setText(R.string.test_note);
        note.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        note.setTextColor(0xFF9AA4B2);
        note.setPadding(0, dp(8), 0, dp(12));
        root.addView(note, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        edit = new EditText(this);
        edit.setHint(R.string.test_hint);
        edit.setHintTextColor(0xFF5F6B7A);
        edit.setTextColor(0xFFF2F5FA);
        edit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        edit.setSingleLine(false);
        edit.setMinLines(3);
        edit.setGravity(Gravity.TOP | Gravity.START);
        edit.setPadding(dp(14), dp(12), dp(14), dp(12));
        edit.setBackgroundColor(0xFF141A23);
        edit.setRawInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        edit.setImeOptions(EditorInfo.IME_ACTION_NONE);
        root.addView(edit, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(120)));

        TextView hint = new TextView(this);
        hint.setText("D-pad 移动焦点 · A 输入/选字 · B 退格 · X 空格/首选 · Y 候选 · L1/R1 翻页 · Start 回车 · Select 中/英/符号");
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        hint.setTextColor(0xFF6EA8FF);
        hint.setPadding(0, dp(12), 0, 0);
        root.addView(hint, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(root);
        edit.postDelayed(new Runnable() {
            @Override public void run() {
                edit.requestFocus();
                edit.setSelection(edit.getText().length());
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(edit, InputMethodManager.SHOW_FORCED);
            }
        }, 500);
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                getResources().getDisplayMetrics());
    }
}
