package org.gg.android.restutils.activity

import com.google.android.maps.MapActivity

class BaseMapActivity extends MapActivity with BaseActivity {
    override def isRouteDisplayed() = false
}