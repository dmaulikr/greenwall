package com.bulsy.greenwall;

import android.animation.AnimatorSet;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends ActionBarActivity {
    static final String LOG_ID = "Greenie";
    Screen entryScreen;
    PlayScreen playScreen;
    Screen currentScreen;
    FullScreenView mainView;
    Typeface gamefont;
    public SoundPool soundpool = null;
    Map<Sound, Integer> soundMap = null;


    /**
     * Initialize the activity.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);

            super.onCreate(savedInstanceState);

            // create screens
            entryScreen = new EntryScreen(this);
            playScreen = new PlayScreen(this);

            mainView = new FullScreenView(this);
            //mainView.setRenderer(new SimpleRenderer());
            setContentView(mainView);

//        gamefont = Typeface.createFromAsset(getAssets(), "smartiecaps.ttf");
            gamefont = Typeface.createFromAsset(getAssets(), "comics.ttf");

            setVolumeControlStream(AudioManager.STREAM_MUSIC);
            soundpool = new SoundPool(15, AudioManager.STREAM_MUSIC, 0);
            soundMap = new HashMap();
            AssetFileDescriptor descriptor = getAssets().openFd("combo2.mp3");
            soundMap.put(Sound.COMBO, soundpool.load(descriptor, 1));
            descriptor = getAssets().openFd("splat.mp3");
            soundMap.put(Sound.SPLAT, soundpool.load(descriptor, 1));
            descriptor = getAssets().openFd("wetsplat.mp3");
            soundMap.put(Sound.WETSPLAT, soundpool.load(descriptor, 1));
            descriptor = getAssets().openFd("wetsplat2.mp3");
            soundMap.put(Sound.KSPLAT, soundpool.load(descriptor, 1));
            descriptor = getAssets().openFd("whoosh.mp3");
            soundMap.put(Sound.THROW, soundpool.load(descriptor, 1));
        } catch (Exception e) {
            // panic, crash, fine -- but let me know what happened.
            Log.d(LOG_ID, "onCreate", e);
        }
    }

    public void playSound(Sound s, float vol, float rate) {
        soundpool.play(soundMap.get(s), vol, vol, 0, 0, rate);
    }
    public void playSound(Sound s) {
        playSound(s, 0.9f, 1);
    }

    /**
     * Handle resuming of the game,
     */
    @Override
    protected void onResume() {
        super.onResume();
        mainView.resume();
    }

    /**
     * Handle pausing of the game.
     */
    @Override
    protected void onPause() {
        super.onPause();
        mainView.pause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public Typeface getGameFont() {
        return gamefont;
    }

    // screen transitions

    /**
     * Start a new game.
     */
    public void startGame() {
        this.playScreen.initGame();
        currentScreen = this.playScreen;
    }

    /**
     * Leave game and return to title screen.
     */
    public void leaveGame() {
        currentScreen = this.entryScreen;
    }

    /**
     * completely exit the game.
     */
    public void exit() {
        finish();
        System.exit(0);
    }

    /**
     * This inner class handles the main render loop, and delegates drawing and event handling to
     * the individual screens.
     */
    private class FullScreenView extends SurfaceView implements Runnable, View.OnTouchListener {
        private volatile boolean isRendering = false;
        Thread renderThread = null;
        SurfaceHolder holder;

        public FullScreenView(Context context) {
            super(context);
            holder = getHolder();
            currentScreen = entryScreen;
            setOnTouchListener(this);
        }

        public void resume() {
            isRendering = true;
            renderThread = new Thread(this);
            renderThread.start();
        }

        @Override
        public void run() {

            try {
                while(isRendering){
                    while(!holder.getSurface().isValid()) {
                        try {
                            Thread.sleep(5);
                        } catch (Exception e) { /* we don't care */  }
                    }

                    // update screen's context
                    currentScreen.update(this);

                    // draw screen
                    Canvas c = holder.lockCanvas();
                    currentScreen.draw(c, this);
                    holder.unlockCanvasAndPost(c);
                }
            } catch (Exception e) {
                // arguably overzealous to grab all exceptions here...but i want to know.
                Log.d(LOG_ID, "View", e);
                e.printStackTrace();
            }

        }

        public void pause() {
            isRendering = false;
            while(true) {
                try {
                    renderThread.join();
                    return;
                } catch (InterruptedException e) {
                    // retry
                }
            }
        }

        public boolean onTouch(View v, MotionEvent event) {
            try {
                return currentScreen.onTouch(event);
            }
            catch (Exception e) {
                // arguably overzealous to grab all exceptions here...but i want to know.
                Log.d(LOG_ID, "onTouch", e);
            }
            return false;
        }
    }
}
