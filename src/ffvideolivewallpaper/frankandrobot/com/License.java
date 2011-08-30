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