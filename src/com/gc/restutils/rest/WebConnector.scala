package com.gc.restutils.rest
import scala.xml._
import scala.io._
import android.util._
import android.app.Activity
import android.app.Activity._
import java.util.Date
import android.app.ProgressDialog
import android.os.Handler
import java.text.SimpleDateFormat
import scala.actors.Actor
import scala.actors.Actor._
import android.preference.PreferenceManager
import android.content.Context
import java.io.ObjectInputStream
import java.io.FileNotFoundException
import java.io.ObjectOutputStream
import com.gc.restutils.R
import android.os.Looper
import android.app.Activity

abstract class WebConnector(activity: Context,
                            private var successPostDownload: PostDownload = null,
                            private var downloadErrorPostDownload: PostDownload = null,
                            private var requestErrorPostDownload: PostDownload = null,
                            var tempCache: Boolean = false, var permanentCache: Boolean = false) extends WebConnectoreBase {

  def this(activity: Context, tempCache: Boolean, permanentCache: Boolean) = {
    this(activity, null, null, null, tempCache, permanentCache)
  }

  def this(activity: Context) = {
    this(activity, false, false)
  }

  import WebConnector._

  WebConnector(activity.asInstanceOf[Activity])

  val TAG = classOf[WebConnector].getName
  val TEST = false

  var response: String = null

  def executeRequest(url: String): Unit = {
    executeRequest(url, null)
  }

  def executeRequest(url: String, onDownloadSuccess: PostDownload): Unit = {
    executeRequest(url, onDownloadSuccess, null)
  }

  def executeRequest(url: String, onDownloadSuccess: PostDownload, onDownloadError: PostDownload): Unit = {

    if (onDownloadSuccess != null)
      this.successPostDownload = onDownloadSuccess
    if (onDownloadError != null)
      this.downloadErrorPostDownload = onDownloadError

    execute(url)

  }

  override def doInBackground(url: String): Option[String] = {

    var toReturn: String = null

    if (tempCache)
      toReturn = getFromTempCache(url)

    if (toReturn == null && permanentCache)
      toReturn = getFromPermanentCache(url, activity)

    if (toReturn == null) {
      val returned = doGet(url)

      returned.foreach { content =>
        if (tempCache) putToTempCache(url, content)
        if (permanentCache) putToPermanentCache(url, content, activity)
      }

      returned
    } else
      Some(toReturn)

  }

  override def onPreExecute() = {
    WebConnector ! Show()
  }

  override def onPostExecute(content: Option[String]) = {
    content match {
      case None => onDownloadError
      case Some(content) => {
        onDownloadSuccess(content)
      }
    }
  }

  def onDownloadSuccess(content: String) = {

    WebConnector ! Dismiss()
    successPostDownload.execute(content)

  }

  def onDownloadError = {

    Log.e(TAG, "onDownloadError [download error]");

    WebConnector ! Message(activity.getString(R.string.data_download_error))
    new Handler().postDelayed(new Runnable() {
      override def run() = {
        WebConnector ! Dismiss()
      }
    }, 3000)

    if (downloadErrorPostDownload != null)
      downloadErrorPostDownload.execute(null)

  }
  def onRequestError(error: String) = {

    Log.e(TAG, "onDownloadError [request error]");
    WebConnector ! Message(activity.getString(R.string.data_request_error) + " : " + error)
    new Handler().postDelayed(new Runnable() {
      override def run() = {
        WebConnector ! Dismiss()

        if (requestErrorPostDownload != null)
          requestErrorPostDownload.execute(error)

      }
    }, 3000)

  }

}

/**
 * This Object containes some utils methods,
 * but is also used from manage syncronized
 * operations between activities , like
 * download dialogs
 *
 */
object WebConnector extends Actor {

  val TAG = classOf[WebConnector].toString()
  var messageDialog: ProgressDialog = null
  var activity: Activity = null

  def apply(activity: Activity) = {

    //reset all dialog only when
    //is colled from new activity
    if (activity != this.activity) {

      Log.d(TAG, "apply [set new activity and create new progressdialog]")
      this.activity = activity

      messageDialog = new ProgressDialog(activity)
      messageDialog.setIndeterminate(true)
      messageDialog.setMessage(activity.getString(R.string.data_download))

      counter = 0

    }
  }

  var counter = 0

  case class Show
  case class Dismiss
  case class Message(text: String)

  //start the actor
  start

  override def act() = {
    while (true) {
      receive {
        case Show() =>
          counter match {
            case 0 =>
              Log.d(TAG, "act [called dialog show]")
              activity.runOnUiThread(new Runnable() {
                def run() {
                  Log.d(TAG, "act [show progress dialog]")
                  messageDialog.show()
                }
              })
              counter = counter + 1
            case _ =>
              counter = counter + 1
          }

        case Dismiss() =>
          counter match {
            case 1 =>
              messageDialog.dismiss()
              counter = counter - 1
            case x =>
              if (x > 1)
                counter = counter - 1

          }

        case Message(text) =>
          activity.runOnUiThread(new Runnable() {
            def run() {
              Log.d(TAG, "act [change message to progress dialog]")
              messageDialog setMessage text
            }
          })

      }

    }
  }

  val tempCache = scala.collection.mutable.Map[String, String]() withDefaultValue (null)

  def formatDate(date: Date) = {
    new SimpleDateFormat("yyyy-MM-dd").format(date)
  }

  def parseDate(date: String) = {
    new SimpleDateFormat("yyyy-MM-dd").parse(date)
  }

  def doPost(content: String, url: String): Option[String] = {

    val client = new SimpleHttpClient()

    val response: Option[String] = client.post(
      url = url,
      params = Map("xml" -> content),
      encode = "UTF-8")

    response

  }

  def doGet(url: String): Option[String] = {
    val client = new SimpleHttpClient()
    val response: Option[String] = client.get(url)
    response
  }

  /**
   * Get cached json from permanent cahche (disk)
   *
   * @param name
   * @param activity
   * @return
   */
  def getFromPermanentCache(name: String, activity: Context): String = {
    val preferences = PreferenceManager
      .getDefaultSharedPreferences(activity)
    preferences.getString(name, null)
  }

  /**
   * Put cached json from temp cahche (static variable)
   *
   * @param name
   * @param value
   * @param activity
   */
  def putToPermanentCache(name: String, value: String,
                          activity: Context) = {

    val preferences = PreferenceManager
      .getDefaultSharedPreferences(activity)

    val edit = preferences.edit()
    edit.putString(name, value);
    edit.commit();

  }

  /**
   * Get cached json from temp cahche (static variable)
   *
   * @param name
   * @param activity
   * @return
   */
  def getFromTempCache(name: String): String = {
    tempCache(name);
  }

  /**
   * Put cached json from permanent cahche (disk)
   *
   * @param name
   * @param value
   */
  def putToTempCache(name: String, value: String) = {
    tempCache += (name -> value)
  }

  /**
   * Delete from both permanent and temporary cache Json with given Url
   *
   * @param name
   *            URL of Json to delet from cache
   *
   */
  def deleteFromChache(name: String, activity: Activity) = {

    // delete from permanent chache
    val preferences = PreferenceManager
      .getDefaultSharedPreferences(activity)

    val edit = preferences.edit()
    edit.remove(name);

    edit.commit();

    // delete from temp cache
    tempCache.remove(name);

  }

  /**
   * Get generic Object from file stored in private app storage space
   *
   * @param fileName
   * @return
   */
  def getObjectFromDisk(fileName: String, activity: Context): Object = {

    var cache: Object = null
    Log.d(TAG, "getObjectFromDisk openfile[" + fileName + "]")

    try {
      val file = activity.openFileInput(fileName)
      cache = new ObjectInputStream(file).readObject()
    } catch {
      case e: FileNotFoundException => Log.d(TAG, "getObjectFromDisk [" + e + "]")
      case e                        => Log.e(TAG, "getObjectFromDisk [" + e + "]")
    }

    return cache;

  }

  /**
   * Put object to disk
   *
   * @param object
   * @param FILE_NAME
   * @param activity
   */
  def writeObjectToDisk(theObject: Object, FILE_NAME: String,
                        activity: Context) = {

    try {
      val file = activity.openFileOutput(FILE_NAME, Context.MODE_PRIVATE)
      new ObjectOutputStream(file).writeObject(theObject)

    } catch {
      case e: FileNotFoundException => Log.w(TAG, "getObjectFromDisk [" + e + "]")
      case e                        => Log.e(TAG, "getObjectFromDisk [" + e + "]")
    }

  }

  /**
   * Delete object from disk
   *
   * @param FILE_NAME
   * @param activity
   */
  def deleteObjectFromDisk(FILE_NAME: String, activity: Context) = {
    activity.deleteFile(FILE_NAME);
  }

}

trait PostDownload {
  def execute(obj: Object)
}