package com.oceantri.mh3;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import java.util.ArrayList;

/** Selects supported cheats before the native game runtime starts. */
final class CheatSelection {
    private static final String PREFS = "mh3_startup_cheats";
    private static final String[] NAMES = {"No damage", "No action stamina drain", "Infinite health", "Infinite stamina"};
    private static final String[] LABELS = {"Sin daño", "Sin gasto de resistencia", "Vida infinita", "Resistencia infinita"};

    private CheatSelection() {}

    static String[] choose(final Activity activity) {
        final SharedPreferences prefs = activity.getSharedPreferences(PREFS, Activity.MODE_PRIVATE);
        final boolean[] checked = new boolean[NAMES.length];
        for (int i = 0; i < checked.length; i++) checked[i] = prefs.getBoolean(NAMES[i], false);
        final Object lock = new Object();
        final String[][] selected = {new String[0]};
        final boolean[] done = {false};
        activity.runOnUiThread(new Runnable() {
            @Override public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle("Trucos para esta partida");
                builder.setMessage("Se aplican al iniciar. Los trucos desactivan el juego en línea.");
                builder.setMultiChoiceItems(LABELS, checked, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override public void onClick(DialogInterface d, int which, boolean value) { checked[which] = value; }
                });
                builder.setPositiveButton("Iniciar", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int which) {
                        ArrayList<String> values = new ArrayList<>();
                        SharedPreferences.Editor editor = prefs.edit();
                        for (int i = 0; i < checked.length; i++) {
                            editor.putBoolean(NAMES[i], checked[i]);
                            if (checked[i]) values.add(NAMES[i]);
                        }
                        editor.apply();
                        synchronized (lock) { selected[0] = values.toArray(new String[0]); done[0] = true; lock.notifyAll(); }
                    }
                });
                builder.setNegativeButton("Sin trucos", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int which) {
                        prefs.edit().clear().apply();
                        synchronized (lock) { selected[0] = new String[0]; done[0] = true; lock.notifyAll(); }
                    }
                });
                builder.setCancelable(false);
                builder.show();
            }
        });
        synchronized (lock) {
            while (!done[0]) {
                try { lock.wait(); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); return new String[0]; }
            }
        }
        return selected[0];
    }
}
