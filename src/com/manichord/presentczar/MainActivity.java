package com.manichord.presentczar;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Presentation;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.media.MediaRouter;
import android.media.MediaRouter.RouteInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.TextView;

public class MainActivity extends Activity
{
    private final String TAG = "PresentationWithMediaRouterActivity";

    private static final boolean ENABLE_JAVASCRIPT = true;
    private static final boolean ENABLE_DOM_STORAGE = true;
    private static final boolean ENABLE_APPCACHE = true;
    private static final boolean ENABLE_FORMSAVE = false;
    private static final boolean ENABLE_PASSWORDSAVE = false;

    private MediaRouter mMediaRouter;
    private DemoPresentation mPresentation;
    private TextView mInfoTextView;
    private static WebView webcontent;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        // Be sure to call the super class.
        super.onCreate(savedInstanceState);

        // Get the media router service.
        mMediaRouter = (MediaRouter) getSystemService(Context.MEDIA_ROUTER_SERVICE);

        // See assets/res/any/layout/presentation_with_media_router_activity.xml
        // for this view layout definition, which is being set here as the
        // content of our screen.
        setContentView(R.layout.main);

        // Get a text view where we will show information about what's
        // happening.
        mInfoTextView = (TextView) findViewById(R.id.info_text);
    }

    @Override
    protected void onResume() {
        // Be sure to call the super class.
        super.onResume();

        // Listen for changes to media routes.
        mMediaRouter.addCallback(MediaRouter.ROUTE_TYPE_LIVE_VIDEO,
                mMediaRouterCallback);

        // Update the presentation based on the currently selected route.
        updatePresentation();
    }

    @Override
    protected void onPause() {
        // Be sure to call the super class.
        super.onPause();

        // Stop listening for changes to media routes.
        mMediaRouter.removeCallback(mMediaRouterCallback);

        updateContents();
    }

    @Override
    protected void onStop() {
        // Be sure to call the super class.
        super.onStop();

        // Dismiss the presentation when the activity is not visible.
        if (mPresentation != null) {
            Log.i(TAG,
                    "Dismissing presentation because the activity is no longer visible.");
            mPresentation.dismiss();
            mPresentation = null;
        }
    }

    private void updatePresentation() {
        // Get the current route and its presentation display.
        MediaRouter.RouteInfo route = mMediaRouter.getSelectedRoute(
                MediaRouter.ROUTE_TYPE_LIVE_VIDEO);
        Display presentationDisplay = route != null ? route
                .getPresentationDisplay() : null;

        // Dismiss the current presentation if the display has changed.
        if (mPresentation != null
                && mPresentation.getDisplay() != presentationDisplay) {
            Log.i(TAG,
                    "Dismissing presentation because the current route no longer "
                            + "has a presentation display.");
            mPresentation.dismiss();
            mPresentation = null;
        }

        // Show a new presentation if needed.
        if (mPresentation == null && presentationDisplay != null) {
            Log.i(TAG, "Showing presentation on display: "
                    + presentationDisplay);
            mPresentation = new DemoPresentation(this, presentationDisplay);
            mPresentation.setOnDismissListener(mOnDismissListener);
            try {
                mPresentation.show();
            } catch (WindowManager.InvalidDisplayException ex) {
                Log.w(TAG,
                        "Couldn't show presentation!  Display was removed in "
                                + "the meantime.", ex);
                mPresentation = null;
            }
        }

        // Update the contents playing in this activity.
        updateContents();
    }

    private void updateContents() {
        // Show either the content in the main activity or the content in the
        // presentation
        // along with some descriptive text about what is happening.
        if (mPresentation != null) {
            mInfoTextView
                    .setText("presentation_with_media_router_now_playing_remotely");
            setupWebview();

        } else {
            mInfoTextView
                    .setText("presentation_with_media_router_now_playing_locally");
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebview() {
        
        if (webcontent != null) {

            webcontent.getSettings().setJavaScriptEnabled(ENABLE_JAVASCRIPT);
            webcontent.getSettings().setDomStorageEnabled(ENABLE_DOM_STORAGE);
            webcontent.getSettings().setAppCacheEnabled(ENABLE_APPCACHE);
            webcontent.getSettings().setSaveFormData(ENABLE_FORMSAVE);
            webcontent.getSettings().setSavePassword(ENABLE_PASSWORDSAVE);

            webcontent.loadUrl("http://manichord.com");
        } else {
            Log.e(TAG, "NULL Webview");
        }
    }
    
    private final MediaRouter.SimpleCallback mMediaRouterCallback =
            new MediaRouter.SimpleCallback() {
                @Override
                public void onRouteSelected(MediaRouter router, int type,
                        RouteInfo info) {
                    Log.d(TAG, "onRouteSelected: type=" + type + ", info="
                            + info);
                    updatePresentation();
                }

                @Override
                public void onRouteUnselected(MediaRouter router, int type,
                        RouteInfo info) {
                    Log.d(TAG, "onRouteUnselected: type=" + type + ", info="
                            + info);
                    updatePresentation();
                }

                @Override
                public void onRoutePresentationDisplayChanged(
                        MediaRouter router, RouteInfo info) {
                    Log.d(TAG, "onRoutePresentationDisplayChanged: info="
                            + info);
                    updatePresentation();
                }
            };

    /**
     * Listens for when presentations are dismissed.
     */
    private final DialogInterface.OnDismissListener mOnDismissListener =
            new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    if (dialog == mPresentation) {
                        Log.i(TAG, "Presentation was dismissed.");
                        mPresentation = null;
                        updateContents();
                    }
                }
            };

    /**
     * The presentation to show on the secondary display.
     * <p>
     * Note that this display may have different metrics from the display on
     * which the main activity is showing so we must be careful to use the
     * presentation's own {@link Context} whenever we load resources.
     * </p>
     */
    private final static class DemoPresentation extends Presentation {

        public DemoPresentation(Context context, Display display) {
            super(context, display);
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            // Be sure to call the super class.
            super.onCreate(savedInstanceState);

            // Get the resources for the context of the presentation.
            // Notice that we are getting the resources from the context of the
            // presentation.
            Resources r = getContext().getResources();

            // Inflate the layout.
            setContentView(R.layout.presentation);
            webcontent = (WebView) findViewById(R.id.webContent);
        }
    }
}
