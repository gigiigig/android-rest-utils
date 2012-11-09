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
import scala.actors.Actor._
import android.preference.PreferenceManager
import android.content.Context
import java.io.ObjectInputStream
import java.io.FileNotFoundException
import java.io.ObjectOutputStream
import com.gc.restutils.R

abstract class WebConnector(activity: Context,
                            private var successPostDownload: PostDownload = null,
                            private var downloadErrorPostDownload: PostDownload = null,
                            private var requestErrorPostDownload: PostDownload = null) extends WebConnectoreBase {

  import WebConnector._

  val BASE_URL = "http://services.begenius.it/hotel/xml/"

  val TAG = classOf[WebConnector].getName
  val TEST = false

  var operation: String = null
  var response: String = null

  case class Show
  case class Dismiss
  case class Message(text: String)

  val dialogManager = actor {

    var counter = 0
    val messageDialog = new ProgressDialog(activity)
    messageDialog.setIndeterminate(true)
    messageDialog.setMessage(activity.getString(R.string.data_download))

    loop {
      react {

        case Show() =>
          counter match {
            case 0 =>
              messageDialog.show()
              counter = counter + 1
            case _ =>
              counter = counter - 1
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

        case Message(text) => messageDialog setMessage text

      }
    }
  } 

  def executeRequest(content: String, operation: String, onDownloadSuccess: PostDownload, onDownloadError: PostDownload) = {

    this.operation = operation
    this.successPostDownload = onDownloadSuccess
    this.downloadErrorPostDownload = onDownloadError

    execute(content)

  }

  override def doInBackground(args: String): Option[String] = {

    var url = args(0)
    return doPost(args, operation)

  }

  override def onPreExecute() = {
    dialogManager ! Show()
  }

  override def onPostExecute(content: Option[String]) = {

    content match {

      case None => onDownloadError
      case Some(content) => {
        dialogManager ! Dismiss()
        onDownloadSuccess(content)
      }

    }

  }

  def onDownloadSuccess(content: String)

  def onDownloadError = {

    Log.e(TAG, "onDownloadError [download error]");

    dialogManager ! Message(activity.getString(R.string.data_download_error))
    new Handler().postDelayed(new Runnable() {
      override def run() = {
        dialogManager ! Dismiss()
      }
    }, 3000)

    if (downloadErrorPostDownload != null)
      downloadErrorPostDownload.execute(null)

  }
  def onRequestError(error: String) = {

    Log.e(TAG, "onDownloadError [request error]");
    dialogManager ! Message(activity.getString(R.string.data_request_error) + " : " + error)
    new Handler().postDelayed(new Runnable() {
      override def run() = {
        dialogManager ! Dismiss()

        if (requestErrorPostDownload != null)
          requestErrorPostDownload.execute(error)

      }
    }, 3000)

  }

}

object WebConnector {

  val TAG = classOf[WebConnector].toString()
  val BASE_URL = "http://services.begenius.it/hotel/xml/"

  val tempCache = scala.collection.mutable.Map[String, String]()

  def formatDate(date: Date) = {
    new SimpleDateFormat("yyyy-MM-dd").format(date)
  }

  def parseDate(date: String) = {
    new SimpleDateFormat("yyyy-MM-dd").parse(date)
  }

  def doPost(content: String, operation: String): Option[String] = {

    val client = new SimpleHttpClient()

    val response: Option[String] = client.post(
      url = BASE_URL + operation,
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