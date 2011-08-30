// Copyright 2011 Uriel Avalos and Frank and Robot Productions

// This software uses libraries from FFmpeg licensed under the LGLv2.1.

// This software uses GLWallpaperService licensed under the Apache v2.

// This file is part of FFvideo Live Wallpaper.

// FFvideo Live Wallpaper is free software: you can redistribute it
// and/or modify it under the terms of the GNU General Public License as
// published by the Free Software Foundation, either version 3 of the
// License, or (at your option) any later version.

// FFvideo Live Wallpaper is distributed in the hope that it will be
// useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with FFvideo Live Wallpaper.  If not, see <http://www.gnu.org/licenses/>.

package ffvideolivewallpaper.frankandrobot.com;

import android.util.Log;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.view.ViewGroup;
import android.util.TypedValue;
import android.graphics.Typeface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class PrefsSlider extends Preference
    implements OnSeekBarChangeListener {

    TextView fpsBox;
    int fps;
    SharedPreferences.Editor editor;

    // This is the constructor called by the inflater
    public PrefsSlider(Context context, AttributeSet attrs) {
        super(context, attrs);
	//setWidgetLayoutResource(R.layout.preference_slider);        
    }

    @Override
	protected View onCreateView(ViewGroup parent) {
	//setup layout
	RelativeLayout layout = new RelativeLayout(getContext());
	layout.setPadding(15,10,15,10);
	//setup title
	RelativeLayout.LayoutParams titleLayout = new RelativeLayout
	    .LayoutParams( RelativeLayout.LayoutParams.FILL_PARENT,
			   RelativeLayout.LayoutParams.WRAP_CONTENT );
	titleLayout.addRule(RelativeLayout.ALIGN_PARENT_TOP);
	TextView titleView = new TextView(getContext());
	titleView.setText(getTitle());
	titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP,21);
	titleView.setTextColor(0xffffffff);
	titleView.setLayoutParams(titleLayout);
	titleView.setId(5405); //random id
	//seekbar
	RelativeLayout.LayoutParams barLayout = new RelativeLayout
	    .LayoutParams( RelativeLayout.LayoutParams.FILL_PARENT,
			   RelativeLayout.LayoutParams.WRAP_CONTENT );
	barLayout.addRule(RelativeLayout.BELOW,titleView.getId());
	SeekBar bar = new SeekBar(getContext());
	bar.setMax(30);
	bar.setProgress(fps);
	bar.setLayoutParams(barLayout);
	bar.setId(7778); //another random id
	bar.setOnSeekBarChangeListener(this);//set up scrolling detection
	//fps box
	RelativeLayout.LayoutParams fpsBoxLayout = new RelativeLayout
	    .LayoutParams( RelativeLayout.LayoutParams.WRAP_CONTENT,
			   RelativeLayout.LayoutParams.WRAP_CONTENT );
	fpsBoxLayout.addRule(RelativeLayout.BELOW,bar.getId());
	fpsBoxLayout.addRule(RelativeLayout.CENTER_HORIZONTAL);
	fpsBox = new TextView(getContext());
	fpsBox.setTextSize(TypedValue.COMPLEX_UNIT_SP,18);
	fpsBox.setText(bar.getProgress()+" fps");
	fpsBox.setLayoutParams(fpsBoxLayout);
	//add views to layout
	layout.addView(titleView);
	layout.addView(bar);
	layout.addView(fpsBox);
	//layout.setId(android.R.id.preference_slider);
	//setup shared preferences
	return layout;
    }
		
    @Override
	public void onProgressChanged(SeekBar bar, int progress, boolean user) {
        // Give the client a chance to ignore this change if they deem
        // it invalid (not really sure when this is the case but part
        // of official template
        if (!callChangeListener(progress)) {
            // They don't want the value to be set
            return;
        }
	//update fps
	fps=progress;
        // Save to persistent storage (this method will make sure this
        // preference should be persistent, along with other useful checks)
        persistInt(fps);
	//update view
	fpsBox.setText(progress+" fps");        
	fpsBox.post(new Runnable() {
		public void run() {
		    fpsBox.invalidate();
		}
	    });
	//save preference
	//Log.d("PrefsSlider","Saving fps...");
	//Log.d("PrefsSlider",String.valueOf(fps));
	editor = getEditor();
	editor.putInt(getKey(),fps);
	editor.commit();
    }

    @Override
	public void onStartTrackingTouch(SeekBar bar) {}

    @Override
	public void onStopTrackingTouch(SeekBar bar) {}

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        // This preference type's value type is Integer, so we read the default
        // value from the attributes as an Integer.
        return a.getInteger(index, 0);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        if (restoreValue) {
            // Restore state
	    fps = getPersistedInt(fps);
        } else {
            // Set state
	    fps = (Integer) defaultValue;
	    persistInt(fps);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        /*
         * Suppose a client uses this preference type without persisting. We
         * must save the instance state so it is able to, for example, survive
         * orientation changes.
         */
        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            // No need to save instance state since it's persistent
            return superState;
        }
        // Save the instance state
        final SavedState myState = new SavedState(superState);
        myState.fps = fps;
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }
        // Restore the instance state
        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        fps = myState.fps;
        notifyChanged();
    }
    
    /**
     * SavedState, a subclass of {@link BaseSavedState}, will store the state
     * of MyPreference, a subclass of Preference.
     * <p>
     * It is important to always call through to super methods.
     */
    private static class SavedState extends BaseSavedState {
        int fps;
        
        public SavedState(Parcel source) {
            super(source);
	    // Restore the click counter
            fps = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
	    // Save the click counter
            dest.writeInt(fps);
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
