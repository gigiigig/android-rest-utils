package com.gc.restutils.activity

import android.app.Activity
import android.content.Intent
import android.content.Context
import android.content.res.Resources.NotFoundException
import android.util.Log
import android.view.KeyEvent
import android.text.Spanned
import android.text.Html

trait BaseActivity extends Activity {

  val TAG = classOf[BaseActivity].toString()

  /**
   * Go to passed activity
   *
   * @param className
   */
  def toActivity(className: Class[_ <: Activity]): Boolean = {
    toActivity(className, false)
  }

  /**
   * Go to passed activity and finish current if boolean finish is true
   *
   * @param className
   * @param finish
   */
  def toActivity(className: Class[_ <: Activity], finish: Boolean): Boolean = {
    toActivity(className, finish, null)
  }

  /**
   * Go to passed activity and finish current if boolean finish is true
   *
   * @param className
   * @param has
   *            map of extras
   */
  def toActivity(className: Class[_ <: Activity], extras: Map[String, Serializable]): Boolean = {
    toActivity(className, false, extras)
  }

  def toActivity(className: Class[_ <: Activity], toFinish: Boolean, extras: Map[String, Serializable]): Boolean = {
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

  /**
   * Return value of string in resurces that have this id
   *
   * @param name
   * @return
   */
  def getString(name: String): String = {

    val identifier = getResources().getIdentifier(name, "string",
      getApplicationInfo().packageName);
    try {
      return getResources().getString(identifier);
    } catch {
      case e: NotFoundException => Log.e(TAG, "getString [" + e.getMessage() + "]"); name
    }

  }

  /**
   * Add back function to device back button
   *
   * @param keyCode
   * @param event
   * @return
   */
  override def onKeyDown(keyCode: Int, event: KeyEvent): Boolean = {
    if (keyCode == KeyEvent.KEYCODE_BACK) {
      toParent()
    } else {
      super.onKeyDown(keyCode, event);
    }
  }

  /**
   * Define to parent behavior , useful for back button
   */
  def toParent(): Boolean = {
    finish()
    false
  }

}

object BaseActivity {

  /**
   * Return formatted Spannet text from html string and replace <strong> tag
   * with <b> tag
   *
   * @param text
   * @return
   */
  def fromHtml(text: String): Spanned = {
    Html.fromHtml(text.replace("<strong>", "<b>").replace(
      "</strong>", "</b>"))
  }
  
}

