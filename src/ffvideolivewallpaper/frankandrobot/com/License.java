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

import android.app.Activity;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;
import android.content.Intent;
import android.view.View;

public class License extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.license);
        //TextView t = (TextView) findViewById(R.id.about_box);
        //t.setMovementMethod(LinkMovementMethod.getInstance());
    }
}