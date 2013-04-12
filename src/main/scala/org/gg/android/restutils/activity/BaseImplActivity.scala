package org.gg.android.restutils.activity

import android.os.Bundle
import android.app.Activity
import android.view.Window

class BaseImplActivity extends BaseActivity {

  protected val activity: Activity = this
  
  override def onCreate(savedInstanceState: Bundle) = {
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    super.onCreate(savedInstanceState)
  }

}