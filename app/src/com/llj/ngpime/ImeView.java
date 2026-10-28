package com.llj.ngpime;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Gamepad-first pinyin keyboard. No touch handling: D-pad moves a focus ring. */
final class ImeView extends View {
    private static final int MODE_ZH = 0;
    private static final int MODE_EN = 1;
    private static final int MODE_SYM = 2;

    private static final int T_LETTER = 0;
    private static final int T_DEL = 1;
    private static final int T_SPACE = 2;
    private static final int T_ENTER = 3;
    private static final int T_MODE = 4;
    private static final int T_SYM = 5;
    private static final int T_CLEAR = 6;
    private static final int T_PREV = 7;
    private static final int T_NEXT = 8;
    private static final int T_CAND = 9;
    private static final int T_CHAR = 10;

    private static final int COLS = 10;
    private static final int ROWS = 4;
    private static final int PAGE = 8;

    private static final class Key {
        final String label;
        final int type;
        final String value;
        Key(String label, int type, String value) {
            this.label = label;
            this.type = type;
            this.value = value;
        }
    }

    private final NgpImeService service;
    private final Dictionary dict;
    private final Paint bgPaint = new Paint();
    private final Paint keyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint keyFuncPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint keyFocusPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint smallPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint accentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint candBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint candSelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private final Key[][] letters = new Key[ROWS][COLS];
    private final Key[][] symbols = new Key[ROWS][COLS];

    private int widthPx;
    private int heightPx;
    private int statusH;
    private int candH;
    private int keyW;
    private int keyH;
    private int gridTop;

    private int mode = MODE_ZH;
    private int row = 0;
    private int col = 0;
    private boolean candFocus = false;
    private String buffer = "";
    private List<Dictionary.Word> candidates = Collections.emptyList();
    private int candIndex = 0;
    private EditorInfo editorInfo;

    ImeView(NgpImeService service, Dictionary dict) {
        super(service);
        this.service = service;
        this.dict = dict;
        setFocusable(true);
        setFocusableInTouchMode(true);
        setBackgroundColor(0xFF0D1015);

        bgPaint.setColor(0xFF0D1015);
        keyPaint.setColor(0xFF1B222D);
        keyFuncPaint.setColor(0xFF2A3444);
        keyFocusPaint.setStyle(Paint.Style.STROKE);
        keyFocusPaint.setStrokeWidth(dp(2f));
        keyFocusPaint.setColor(0xFF6EA8FF);
        textPaint.setColor(0xFFE8EDF5);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        smallPaint.setColor(0xFF9AA4B2);
        smallPaint.setTextAlign(Paint.Align.LEFT);
        accentPaint.setColor(0xFF6EA8FF);
        accentPaint.setTextAlign(Paint.Align.LEFT);
        accentPaint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        candBgPaint.setColor(0xFF141A23);
        candSelPaint.setColor(0xFF244A7A);

        buildLetters();
        buildSymbols();
        dict.loadAsync(new Runnable() {
            @Override public void run() {
                if (dict.isReady()) {
                    updateCandidates();
                    invalidate();
                }
            }
        });
    }

    private void buildLetters() {
        String[] r0 = {"Q","W","E","R","T","Y","U","I","O","P"};
        String[] r1 = {"A","S","D","F","G","H","J","K","L"};
        String[] r2 = {"Z","X","C","V","B","N","M"};
        for (int i = 0; i < r0.length; i++) letters[0][i] = new Key(r0[i], T_LETTER, r0[i].toLowerCase());
        for (int i = 0; i < r1.length; i++) letters[1][i] = new Key(r1[i], T_LETTER, r1[i].toLowerCase());
        letters[1][9] = new Key("⌫", T_DEL, null);
        for (int i = 0; i < r2.length; i++) letters[2][i] = new Key(r2[i], T_LETTER, r2[i].toLowerCase());
        letters[2][7] = new Key("中/EN", T_MODE, null);
        letters[2][8] = new Key("符号", T_SYM, null);
        letters[2][9] = new Key("空格", T_SPACE, null);
        letters[3][0] = new Key("回车", T_ENTER, null);
        letters[3][1] = new Key("清空", T_CLEAR, null);
        letters[3][2] = new Key("◀", T_PREV, null);
        letters[3][3] = new Key("▶", T_NEXT, null);
        letters[3][4] = new Key("候选", T_CAND, null);
    }

    private void buildSymbols() {
        String[] r0 = {"1","2","3","4","5","6","7","8","9","0"};
        String[] r1 = {"@","#","$","%","&","*","-","+","(",")"};
        String[] r2 = {"!","?",":",";","'","\"","/","\\","_","="};
        for (int i = 0; i < r0.length; i++) symbols[0][i] = new Key(r0[i], T_CHAR, r0[i]);
        for (int i = 0; i < r1.length; i++) symbols[1][i] = new Key(r1[i], T_CHAR, r1[i]);
        for (int i = 0; i < r2.length; i++) symbols[2][i] = new Key(r2[i], T_CHAR, r2[i]);
        symbols[3][0] = new Key("中/英", T_MODE, null);
        symbols[3][1] = new Key("空格", T_SPACE, null);
        symbols[3][2] = new Key("回车", T_ENTER, null);
        symbols[3][3] = new Key("清空", T_CLEAR, null);
        symbols[3][4] = new Key("⌫", T_DEL, null);
    }

    private Key[][] grid() { return mode == MODE_SYM ? symbols : letters; }

    private String label(Key k) {
        if (k.type == T_MODE) {
            if (mode == MODE_ZH) return "中/EN";
            if (mode == MODE_EN) return "EN/中";
            return "中/英";
        }
        return k.label;
    }

    private float dp(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                getResources().getDisplayMetrics());
    }

    private float sp(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v,
                getResources().getDisplayMetrics());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        if (w <= 0) w = getResources().getDisplayMetrics().widthPixels;
        int h = (int) (getResources().getDisplayMetrics().heightPixels * 0.56f);
        int min = (int) dp(340f);
        int max = (int) dp(520f);
        if (h < min) h = min;
        if (h > max) h = max;
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        widthPx = w;
        heightPx = h;
        statusH = (int) dp(30f);
        candH = (int) dp(58f);
        keyW = w / COLS;
        keyH = (h - statusH - candH) / ROWS;
        gridTop = statusH + candH;
        ensureFocus();
    }

    private void ensureFocus() {
        Key[][] g = grid();
        if (g[row][col] == null) {
            for (int r = 0; r < ROWS; r++) {
                for (int c = 0; c < COLS; c++) {
                    if (g[r][c] != null) { row = r; col = c; return; }
                }
            }
            row = 0;
            col = 0;
        }
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        c.drawRect(0, 0, widthPx, heightPx, bgPaint);

        // status line
        String modeText = mode == MODE_ZH ? "中" : mode == MODE_EN ? "EN" : "符";
        accentPaint.setTextSize(sp(16));
        c.drawText(modeText, dp(12), statusH * 0.68f, accentPaint);
        String shown = buffer;
        if (mode == MODE_SYM) shown = "符号";
        if (shown == null) shown = "";
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setTextSize(sp(17));
        textPaint.setColor(0xFFE8EDF5);
        float bx = dp(48);
        c.drawText(shown, bx, statusH * 0.68f, textPaint);
        String state;
        if (!dict.isReady()) state = dict.isLoading() ? "词库加载中…" : "词库未加载";
        else if (!candidates.isEmpty()) state = (candIndex / PAGE + 1) + "/" + ((candidates.size() + PAGE - 1) / PAGE);
        else if (!buffer.isEmpty()) state = "无候选";
        else state = "A输入 B删 X空格 Y候选";
        smallPaint.setTextSize(sp(11));
        c.drawText(state, widthPx - smallPaint.measureText(state) - dp(10), statusH * 0.68f, smallPaint);

        // candidates
        c.drawRect(0, statusH, widthPx, statusH + candH, candBgPaint);
        if (candidates.isEmpty()) {
            smallPaint.setTextSize(sp(14));
            String hint = buffer.isEmpty() ? (mode == MODE_ZH ? "拼音候选" : "英文 / 数字") : "无候选，Enter 直接上屏";
            c.drawText(hint, dp(14), statusH + candH * 0.62f, smallPaint);
        } else {
            int start = (candIndex / PAGE) * PAGE;
            int cw = widthPx / PAGE;
            for (int i = 0; i < PAGE; i++) {
                int idx = start + i;
                if (idx >= candidates.size()) break;
                float l = i * cw;
                if (candFocus && idx == candIndex) {
                    c.drawRect(l + dp(2), statusH + dp(3), l + cw - dp(2), statusH + candH - dp(3), candSelPaint);
                }
                String word = candidates.get(idx).word;
                textPaint.setTextSize(sp(20));
                textPaint.setColor(0xFFF2F5FA);
                float tw = textPaint.measureText(word);
                if (tw > cw - dp(8)) {
                    textPaint.setTextSize(sp(15));
                }
                drawCenter(c, word, l + cw / 2f, statusH + candH / 2f, textPaint);
            }
        }

        // key grid
        Key[][] g = grid();
        for (int r = 0; r < ROWS; r++) {
            for (int cc = 0; cc < COLS; cc++) {
                Key k = g[r][cc];
                if (k == null) continue;
                float l = cc * keyW + dp(3);
                float t = gridTop + r * keyH + dp(3);
                float rr = (cc + 1) * keyW - dp(3);
                float bb = gridTop + (r + 1) * keyH - dp(3);
                rect.set(l, t, rr, bb);
                boolean focused = !candFocus && r == row && cc == col;
                c.drawRoundRect(rect, dp(8), dp(8), isFunction(k) ? keyFuncPaint : keyPaint);
                if (focused) c.drawRoundRect(rect, dp(8), dp(8), keyFocusPaint);
                textPaint.setTextSize(isFunction(k) ? sp(13) : sp(22));
                textPaint.setColor(0xFFE8EDF5);
                String lab = label(k);
                Paint.FontMetrics kfm = textPaint.getFontMetrics();
                float ty = (t + bb) / 2f - (kfm.ascent + kfm.descent) / 2f;
                drawCenter(c, lab, (l + rr) / 2f, ty, textPaint);
            }
        }
    }

    private boolean isFunction(Key k) {
        return k.type != T_LETTER && k.type != T_CHAR;
    }

    private void drawCenter(Canvas c, String s, float x, float y, Paint p) {
        Paint.Align old = p.getTextAlign();
        p.setTextAlign(Paint.Align.CENTER);
        c.drawText(s, x, y, p);
        p.setTextAlign(old);
    }

    // ---------------- input ----------------

    private InputConnection ic() { return service.getCurrentInputConnection(); }

    void onStartInput(EditorInfo info) {
        editorInfo = info;
        buffer = "";
        candidates = Collections.emptyList();
        candIndex = 0;
        candFocus = false;
        int type = info == null ? InputType.TYPE_CLASS_TEXT : info.inputType;
        int cls = type & InputType.TYPE_MASK_CLASS;
        int var = type & InputType.TYPE_MASK_VARIATION;
        if (cls == InputType.TYPE_CLASS_NUMBER) {
            mode = MODE_SYM;
        } else if (var == InputType.TYPE_TEXT_VARIATION_PASSWORD
                || var == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                || var == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
                || var == InputType.TYPE_NUMBER_VARIATION_PASSWORD) {
            mode = MODE_EN;
        } else {
            mode = MODE_ZH;
        }
        row = 0;
        col = 0;
        ensureFocus();
        requestFocus();
        invalidate();
    }

    void reset() {
        buffer = "";
        candidates = Collections.emptyList();
        candIndex = 0;
        candFocus = false;
        invalidate();
    }

    private void updateCandidates() {
        if (mode != MODE_ZH || buffer.isEmpty()) {
            candidates = Collections.emptyList();
            candIndex = 0;
            candFocus = false;
            return;
        }
        candidates = dict.query(buffer);
        if (candIndex >= candidates.size()) candIndex = 0;
        if (candidates.isEmpty()) candFocus = false;
        invalidate();
    }

    private void setComposing() {
        InputConnection ic = ic();
        if (ic == null) return;
        if (buffer.isEmpty()) ic.finishComposingText();
        else ic.setComposingText(buffer, 1);
    }

    private void letter(String s) {
        if (mode == MODE_EN) {
            InputConnection ic = ic();
            if (ic != null) ic.commitText(s, 1);
            return;
        }
        if (mode != MODE_ZH) return;
        if (buffer.length() >= 32) return;
        buffer += s;
        setComposing();
        updateCandidates();
        invalidate();
    }

    private void commitWord(String w) {
        InputConnection ic = ic();
        if (ic == null) return;
        if (w != null && !w.isEmpty()) ic.commitText(w, 1);
        ic.finishComposingText();
        buffer = "";
        candidates = Collections.emptyList();
        candIndex = 0;
        candFocus = false;
        invalidate();
    }

    private void commitRaw() {
        if (!buffer.isEmpty()) commitWord(buffer);
    }

    private void commitSelectedOrFirst() {
        if (candidates.isEmpty()) {
            if (!buffer.isEmpty()) commitRaw();
            return;
        }
        int idx = candFocus ? candIndex : 0;
        if (idx < 0) idx = 0;
        if (idx >= candidates.size()) idx = 0;
        commitWord(candidates.get(idx).word);
    }

    private void delete() {
        if (mode == MODE_ZH && !buffer.isEmpty()) {
            buffer = buffer.substring(0, buffer.length() - 1);
            setComposing();
            updateCandidates();
            invalidate();
            return;
        }
        InputConnection ic = ic();
        if (ic != null) ic.deleteSurroundingText(1, 0);
    }

    private void space() {
        if (mode == MODE_ZH && !buffer.isEmpty()) {
            commitSelectedOrFirst();
            return;
        }
        InputConnection ic = ic();
        if (ic != null) ic.commitText(" ", 1);
    }

    private void enter() {
        if (mode == MODE_ZH && !buffer.isEmpty()) {
            commitSelectedOrFirst();
        }
        InputConnection ic = ic();
        if (ic == null) return;
        int action = editorInfo == null ? EditorInfo.IME_ACTION_NONE
                : (editorInfo.imeOptions & EditorInfo.IME_MASK_ACTION);
        if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            ic.performEditorAction(action);
        } else {
            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
        }
    }

    private void clear() {
        buffer = "";
        candidates = Collections.emptyList();
        candIndex = 0;
        candFocus = false;
        InputConnection ic = ic();
        if (ic != null) ic.finishComposingText();
        invalidate();
    }

    private void cycleMode() {
        mode = (mode + 1) % 3;
        clear();
        row = 0;
        col = 0;
        ensureFocus();
        invalidate();
    }

    private void toggleMode() {
        if (mode == MODE_SYM) mode = MODE_ZH;
        else mode = (mode == MODE_ZH) ? MODE_EN : MODE_ZH;
        clear();
        row = 0;
        col = 0;
        ensureFocus();
        invalidate();
    }

    private void toggleSymbols() {
        mode = (mode == MODE_SYM) ? MODE_ZH : MODE_SYM;
        clear();
        row = 0;
        col = 0;
        ensureFocus();
        invalidate();
    }

    private void toggleCandidateFocus() {
        if (candidates.isEmpty()) return;
        candFocus = !candFocus;
        if (candFocus) candIndex = (candIndex / PAGE) * PAGE;
        invalidate();
    }

    private void page(int dir) {
        if (candidates.isEmpty()) return;
        int idx = candIndex + dir * PAGE;
        if (idx < 0) idx = 0;
        if (idx >= candidates.size()) idx = ((candidates.size() - 1) / PAGE) * PAGE;
        candIndex = idx;
        candFocus = true;
        invalidate();
    }

    private void moveCandidate(int dc) {
        if (candidates.isEmpty()) return;
        int idx = candIndex + dc;
        if (idx < 0) idx = 0;
        if (idx >= candidates.size()) idx = candidates.size() - 1;
        candIndex = idx;
        candFocus = true;
        invalidate();
    }

    private void activate() {
        if (candFocus) {
            commitSelectedOrFirst();
            return;
        }
        Key k = grid()[row][col];
        if (k == null) return;
        switch (k.type) {
            case T_LETTER: letter(k.value); break;
            case T_CHAR:
                InputConnection ic = ic();
                if (ic != null) ic.commitText(k.value, 1);
                break;
            case T_DEL: delete(); break;
            case T_SPACE: space(); break;
            case T_ENTER: enter(); break;
            case T_MODE: toggleMode(); break;
            case T_SYM: toggleSymbols(); break;
            case T_CLEAR: clear(); break;
            case T_PREV: page(-1); break;
            case T_NEXT: page(1); break;
            case T_CAND: toggleCandidateFocus(); break;
        }
    }

    private void moveFocus(int dr, int dc) {
        if (candFocus) {
            if (dc != 0) { moveCandidate(dc); return; }
            if (dr > 0) { candFocus = false; ensureFocus(); invalidate(); }
            return;
        }
        Key[][] g = grid();
        if (dr < 0 && row == 0 && !candidates.isEmpty()) {
            candFocus = true;
            candIndex = (candIndex / PAGE) * PAGE;
            invalidate();
            return;
        }
        if (dc != 0) {
            for (int i = 0; i < COLS; i++) {
                int nc = (col + dc + COLS) % COLS;
                if (g[row][nc] != null) { col = nc; break; }
            }
        } else if (dr != 0) {
            for (int i = 0; i < ROWS; i++) {
                int nr = (row + dr + ROWS) % ROWS;
                if (g[nr][col] != null) { row = nr; break; }
            }
        }
        invalidate();
    }

    // ---------------- key routing from InputMethodService ----------------

    boolean handleKeyDown(int keyCode, KeyEvent event) {
        int repeat = event.getRepeatCount();
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP: moveFocus(-1, 0); return true;
            case KeyEvent.KEYCODE_DPAD_DOWN: moveFocus(1, 0); return true;
            case KeyEvent.KEYCODE_DPAD_LEFT: moveFocus(0, -1); return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT: moveFocus(0, 1); return true;
            case KeyEvent.KEYCODE_BUTTON_A:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                if (repeat == 0) activate();
                return true;
            case KeyEvent.KEYCODE_BUTTON_B:
                delete();
                return true;
            case KeyEvent.KEYCODE_BUTTON_X:
                space();
                return true;
            case KeyEvent.KEYCODE_BUTTON_Y:
                toggleCandidateFocus();
                return true;
            case KeyEvent.KEYCODE_BUTTON_L1:
                page(-1);
                return true;
            case KeyEvent.KEYCODE_BUTTON_R1:
                page(1);
                return true;
            case KeyEvent.KEYCODE_BUTTON_START:
                enter();
                return true;
            case KeyEvent.KEYCODE_BUTTON_SELECT:
                cycleMode();
                return true;
            case KeyEvent.KEYCODE_DEL:
                delete();
                return true;
            case KeyEvent.KEYCODE_SPACE:
                space();
                return true;
            case KeyEvent.KEYCODE_ENTER:
                enter();
                return true;
            default:
                break;
        }
        if (repeat == 0 && keyCode >= KeyEvent.KEYCODE_A && keyCode <= KeyEvent.KEYCODE_Z) {
            letter(String.valueOf((char) ('a' + (keyCode - KeyEvent.KEYCODE_A))));
            return true;
        }
        if (repeat == 0 && keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
            InputConnection ic = ic();
            if (ic != null) ic.commitText(String.valueOf((char) ('0' + (keyCode - KeyEvent.KEYCODE_0))), 1);
            return true;
        }
        return false;
    }
}
