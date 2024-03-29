package com.amaris.amarisprueba.threads;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.amaris.amarisprueba.models.Word;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class ReaderTextTxt implements Runnable {

    private static final String TAG = ReaderTextTxt.class.getSimpleName();

    public static final int KEY_NOTIFY_WORDS_HANDLER = 1000;
    public static final int KEY_NOTIFY_INDEXES_HANDLER = 1001;
    public static final int KEY_NOTIFY_DURATION_HANDLER = 1002;

    private static final int NUMBER_OF_REPEATED_WORDS = 1500;
    public static final String KEY_DATA = "key.data";
    public static final String KEY_INDEXES = "key.indexes";
    public static final String KEY_DURATION = "key.duration";

    private int repeatedWordCounter = 0;
    private long startTime;

    private InputStream inputStream;
    private List<Word> words;
    private HashMap<String, Integer> indexes;
    private Handler handler;

    public ReaderTextTxt(Handler handler, InputStream inputStream) {
        this.handler = handler;
        this.inputStream = inputStream;

        indexes = new HashMap<>();
        words = new ArrayList<>();
    }

    private ReaderTextTxt() {
        throw new IllegalStateException("Must be call from parameters constructor");
    }

    @Override
    public void run() {
        try {
            startTime = System.nanoTime();
            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String line;

                while ((line = bufferedReader.readLine()) != null) {
                    // \\s+ means any number of whitespaces between tokens
                    if (!TextUtils.isEmpty(line)) {
                        line = line.replaceAll("[^A-Za-z0-9 ]", "");
                        Iterable<String> wordsIte = Splitter.on(" ").split(line);
                        List<String> words = Lists.newArrayList(wordsIte.iterator());
                        processLine(words);

                        if (this.words.size() > words.size()) {
                            int start = this.words.size() - words.size();
                            int end = this.words.size();

                            List<Word> list = this.words.subList(start, end);
                            notifyNewWords(new ArrayList<>(list));

                        } else {
                            notifyNewWords(new ArrayList<>(this.words));
                        }

                        if (repeatedWordCounter >= NUMBER_OF_REPEATED_WORDS) {
                            notifyIndexes(indexes);
                            repeatedWordCounter = 0;
                        }
                    }
                }

                inputStream.close();
            }
        } catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }

        long endTime = System.nanoTime();
        long duration = endTime - startTime;
        notifyIndexes(indexes);
        notifyTime(duration);

    }

    private void notifyTime(long duration) {
        Bundle bundle = new Bundle();
        bundle.putLong(KEY_DURATION, duration);

        Message message = Message.obtain();
        message.setData(bundle);
        message.what = KEY_NOTIFY_DURATION_HANDLER;

        handler.sendMessage(message);
    }

    private void notifyIndexes(HashMap<String, Integer> indexes) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(KEY_INDEXES, indexes);

        Message message = Message.obtain();
        message.setData(bundle);
        message.what = KEY_NOTIFY_INDEXES_HANDLER;

        handler.sendMessage(message);
    }

    private void processLine(List<String> words) {
        for (String word : words) {

            Word wordO = new Word();
            wordO.setWord(word);
            this.words.add(wordO);
            indexWord(word);
        }
    }

    private void indexWord(String word) {
        word = capitalize(word);
        if (indexes.containsKey(word)) {
            Integer counter = indexes.get(word);
            counter++;
            indexes.put(word, counter);

            repeatedWordCounter++;

        } else {
            indexes.put(word, 1);
        }
    }

    private void notifyNewWords(ArrayList<Word> words) {
        Message message = Message.obtain();
        message.what = KEY_NOTIFY_WORDS_HANDLER;
        Bundle bunde = new Bundle();
        bunde.putSerializable(KEY_DATA, words);
        message.setData(bunde);

        handler.sendMessage(message);
    }

    private String capitalize(final String line) {
        return Character.toUpperCase(line.charAt(0)) + line.substring(1);
    }
}
