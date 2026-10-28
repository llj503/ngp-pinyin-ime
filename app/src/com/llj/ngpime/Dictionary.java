package com.llj.ngpime;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;

/** Offline pinyin dictionary: dict.tsv.gz, one "pinyin<TAB>word<TAB>freq" per line. */
final class Dictionary {
    private static final String TAG = "NgpImeDict";
    private static final int MAX_RESULTS = 80;
    private static final int MAX_SCAN = 4000;
    static final class Word {
        final String pinyin;
        final String word;
        final int freq;
        Word(String pinyin, String word, int freq) {
            this.pinyin = pinyin;
            this.word = word;
            this.freq = freq;
        }
    }

    private final Context context;
    private final TreeMap<String, List<Word>> map = new TreeMap<>();
    private volatile boolean loading;
    private volatile boolean ready;

    Dictionary(Context context) {
        this.context = context.getApplicationContext();
    }

    boolean isReady() { return ready; }
    boolean isLoading() { return loading; }
    int size() { return map.size(); }

    void loadAsync(final Runnable done) {
        if (loading || ready) return;
        loading = true;
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    load();
                    ready = true;
                    Log.i(TAG, "loaded " + map.size() + " keys");
                } catch (Throwable t) {
                    Log.e(TAG, "load failed", t);
                } finally {
                    loading = false;
                    if (done != null) {
                        new Handler(Looper.getMainLooper()).post(done);
                    }
                }
            }
        }, "dict-loader").start();
    }

    private void load() throws IOException {
        GZIPInputStream gz = new GZIPInputStream(context.getAssets().open("dict.tsv.gz"));
        BufferedReader br = new BufferedReader(new InputStreamReader(gz, StandardCharsets.UTF_8));
        String line;
        try {
            while ((line = br.readLine()) != null) {
                String[] p = line.split("\t");
                if (p.length < 3 || p[0].isEmpty() || p[1].isEmpty()) continue;
                int f;
                try { f = Integer.parseInt(p[2]); } catch (NumberFormatException e) { f = 1; }
                List<Word> list = map.get(p[0]);
                if (list == null) {
                    list = new ArrayList<>(2);
                    map.put(p[0], list);
                }
                list.add(new Word(p[0], p[1], f));
            }
        } finally {
            br.close();
        }
    }

    /** Exact pinyin first, then prefix matches; frequency descending. */
    List<Word> query(String input) {
        List<Word> out = new ArrayList<>();
        if (!ready || input == null || input.isEmpty()) return out;
        final String key = input.toLowerCase();
        HashSet<String> seen = new HashSet<>();
        List<Word> exact = map.get(key);
        if (exact != null) {
            for (Word w : exact) {
                if (seen.add(w.word)) out.add(w);
            }
        }
        NavigableMap<String, List<Word>> sub = map.subMap(key, true, key + "\uffff", false);
        int scanned = 0;
        outer:
        for (List<Word> list : sub.values()) {
            for (Word w : list) {
                if (seen.add(w.word)) out.add(w);
                if (++scanned >= MAX_SCAN) break outer;
            }
        }
        Collections.sort(out, new java.util.Comparator<Word>() {
            @Override public int compare(Word a, Word b) {
                boolean ae = a.pinyin.equals(key);
                boolean be = b.pinyin.equals(key);
                if (ae != be) return ae ? -1 : 1;
                if (a.freq != b.freq) return a.freq > b.freq ? -1 : 1;
                return a.word.compareTo(b.word);
            }
        });
        if (out.size() > MAX_RESULTS) {
            return new ArrayList<>(out.subList(0, MAX_RESULTS));
        }
        return out;
    }
}
