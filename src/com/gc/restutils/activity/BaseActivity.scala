package com.gc.restutils.activity

import android.app.Activity
import android.content.Intent
import android.content.Context

trait BaseActivity extends Activity {

  /**
   * Go to passed activity
   *
   * @param className
   */
  def toActivity(className: Class[Activity]): Boolean = {
    toActivity(className, false)
  }

  /**
   * Go to passed activity and finish current if boolean finish is true
   *
   * @param className
   * @param finish
   */
  def toActivity(className: Class[Activity], finish: Boolean): Boolean = {
    toActivity(className, finish, null)
  }

  /**
   * Go to passed activity and finish current if boolean finish is true
   *
   * @param className
   * @param has
   *            map of extras
   */
  def toActivity(className: Class[Activity], extras: Map[String, Serializable]): Boolean = {
    toActivity(className, false, extras)
  }

  def toActivity(className: Class[Activity], toFinish: Boolean, extras: Map[String, Serializable]): Boolean = {
    val intent = new Intent(this, className);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

    if (extras != null) {
      val keySet = extras.keySet
      for (string <- keySet) {
        intent.putExtra(string, extras.get(string));
      }
    }

    if (toFinish)
      finish();

    startActivity(intent);
    return false;
  }

}