package com.llj.ngpime;

import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;

/** Android IME shell for the gamepad-first pinyin keyboard. */
public final class NgpImeService extends InputMethodService {
    private Dictionary dict;
    private ImeView view;

    @Override
    public void onCreate() {
        super.onCreate();
        dict = new Dictionary(this);
        dict.loadAsync(new Runnable() {
            @Override public void run() {
                if (view != null) view.invalidate();
            }
        });
    }

    @Override
    public View onCreateInputView() {
        view = new ImeView(this, dict);
        return view;
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        if (view != null) view.onStartInput(attribute);
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        if (view != null) {
            view.onStartInput(info);
            view.requestFocus();
        }
    }

    @Override
    public void onFinishInput() {
        if (view != null) view.reset();
        super.onFinishInput();
    }

    @Override
    public void onWindowShown() {
        super.onWindowShown();
        if (view != null) {
            view.requestFocus();
            view.invalidate();
        }
    }

    @Override
    public boolean onEvaluateFullscreenMode() {
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (view != null && view.handleKeyDown(keyCode, event)) return true;
        return super.onKeyDown(keyCode, event);
    }
}
