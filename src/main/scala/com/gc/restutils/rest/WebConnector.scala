package com.gc.restutils.rest
import java.io.FileNotFoundException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import scala.actors.Actor
import scala.collection.JavaConverters._
import com.gc.restutils.R
import android.app.Activity
import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.os.Handler
import android.preference.PreferenceManager
import android.util._
import android.view.ViewGroup

abstract class WebConnector(activity: Context,
                            private var successPostDownload: PostDownload = null,
                            private var downloadErrorPostDownload: PostDownload = null,
                            private var requestErrorPostDownload: PostDownload = null,
                            tempCache: Boolean = false, permanentCache: Boolean = false,
                            loaderShower: LoaderShower) extends WebConnectoreBase {

  def this(activity: Context, tempCache: Boolean, permanentCache: Boolean, loaderShower: LoaderShower) = {
    this(activity, null, null, null, tempCache, permanentCache, loaderShower)
  }

  def this(activity: Context, loaderShower: LoaderShower) = {
    this(activity, false, false, loaderShower: LoaderShower)
  }

  import WebConnector._

  //WebConnector(activity.asInstanceOf[Activity])

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

    var toReturn: String = null

    Log.d(TAG, "doInBackground use temp cache[" + tempCache + "]")
    Log.d(TAG, "doInBackground use permanent cache[" + permanentCache + "]")

    if (tempCache) {
      toReturn = getFromTempCache(url)
      Log.d(TAG, "doInBackground from temp cache[" + toReturn + "]");
    }

    if (toReturn == null && permanentCache) {
      toReturn = getFromPermanentCache(url, activity)
      Log.d(TAG, "doInBackground from permenent cache[" + toReturn + "]");
    }

    if (toReturn == null) {
      execute(url)
    } else
      successPostDownload.execute(toReturn)

  }

  override def doInBackground(url: String): Option[String] = {

    val returned = doGet(url)

    returned.foreach { content =>
      if (tempCache) putToTempCache(url, content)
      if (permanentCache) putToPermanentCache(url, content, activity)
    }

    returned

  }

  override def onPreExecute() = {
    WebConnector ! Show(activity.asInstanceOf[Activity], loaderShower)
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

    WebConnector ! Dismiss(activity.asInstanceOf[Activity], loaderShower)
    successPostDownload.execute(content)

  }

  def onDownloadError = {

    Log.e(TAG, "onDownloadError [download error]");

    WebConnector ! Message(activity.asInstanceOf[Activity], activity.getString(R.string.data_download_error), loaderShower)
    new Handler().postDelayed(new Runnable() {
      override def run() = {
        WebConnector ! Dismiss(activity.asInstanceOf[Activity], loaderShower)
      }
    }, 3000)

    if (downloadErrorPostDownload != null)
      downloadErrorPostDownload.execute(null)

  }

  def onRequestError(error: String) = {

    Log.e(TAG, "onDownloadError [request error]");
    WebConnector ! Message(activity.asInstanceOf[Activity], activity.getString(R.string.data_request_error) + " : " + error, loaderShower)
    new Handler().postDelayed(new Runnable() {
      override def run() = {
        WebConnector ! Dismiss(activity.asInstanceOf[Activity], loaderShower)

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

  def main(args: Array[String]) {
    println(dialogs)
  }

  val TAG = classOf[WebConnector].toString()

  case class Show(activity: Activity, loaderShower: LoaderShower)
  case class Dismiss(activity: Activity, loaderShower: LoaderShower)
  case class Message(activity: Activity, text: String, loaderShower: LoaderShower)

  var dialogs = Map[Activity, (LoaderShower, Int)]()

  //start the actor
  start

  override def act() = {

    while (true) {

      receive {

        case Show(activity, loaderShower) =>

          val dialog = dialogs.get(activity)
          dialog match {

            case None =>
              createAndShowMessageDialog(activity, loaderShower)

            case Some(dialog) =>
              dialog._2 match {
                case 0 =>
                  runOnUi(activity, { () =>
                    dialog._1.show()
                    increaseDialog(activity)
                  })
                case _ =>
                  increaseDialog(activity)
              }
          }

        case Dismiss(activity, loaderShower) =>

          val dialog = dialogs(activity)
          dialog._2 match {
            case 1 =>
              runOnUi(activity, { () =>
                dialog._1.hide()
                decreaseDialog(activity)
              })
            case x =>
              if (x > 1)
                decreaseDialog(activity)

          }

        case Message(activity, text, loaderShower) =>
          runOnUi(activity, { () =>
            Log.d(TAG, "act [change message to progress dialog]")
            dialogs(activity)._1 setMessage text
          })

      }

      def runOnUi(activity: Activity, f: () => Unit) = {
        activity.runOnUiThread(new Runnable() {
          def run() {
            f()
          }
        })
      }

    }
  }

  def createAndShowMessageDialog(activity: Activity, loaderShower: LoaderShower) = {
    val dialogCount = dialogs.get(activity)
    dialogCount match {
      case None =>
        activity.runOnUiThread(new Runnable() {
          override def run() {
            val newDialog = (loaderShower, 1)
            dialogs += (activity -> newDialog)
            newDialog._1.show()
            dialogs(activity)
          }
        });
      case Some(elem) => elem
    }
  }

  def increaseDialog(activity: Activity) = {
    val dialogCount = dialogs(activity)
    val newElem = (dialogCount._1, dialogCount._2 + 1)
    dialogs += (activity -> newElem)
  }

  def decreaseDialog(activity: Activity) = {
    val dialogCount = dialogs(activity)
    val newElem = (dialogCount._1, dialogCount._2 - 1)
    dialogs += (activity -> newElem)
  }

  def createDialog(newActivity: Activity): ProgressDialog = {

    Log.d(TAG, "apply [set new activity and create new progressdialog]")

    val messageDialog = new ProgressDialog(newActivity)
    messageDialog.setIndeterminate(true)
    messageDialog.setMessage(newActivity.getString(R.string.data_download))

    messageDialog

  }

  val tempCache = scala.collection.mutable.Map[String, String]() withDefaultValue (null)

  def formatDate(date: Date) = {
    new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
  }

  def parseDate(date: String) = {
    new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)
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
  def deleteFromCache(name: String, activity: Activity) = {

    // delete from permanent chache
    val preferences = PreferenceManager
      .getDefaultSharedPreferences(activity)

    val edit = preferences.edit()
    edit.remove(name);

    edit.commit();

    // delete from temp cache
    tempCache.remove(name);

  }

  def clearTempCache() = {
    tempCache.clear()
  }

  def clearPermanentCache(activity: Activity) = {

    val preferences = PreferenceManager
      .getDefaultSharedPreferences(activity)

    val scpref = preferences.getAll() asScala

    scpref foreach { item =>
      preferences.edit().remove(item._1)
    }

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

trait LoaderShower {
  def show()
  def hide()
  def setMessage(message: String)
}

class DialogLoader(activity: Context, message: String) extends LoaderShower {

  val dialog = WebConnector.createDialog(activity.asInstanceOf[Activity])

  override def show = {
    dialog.show()
  }

  override def hide() = {
    dialog.dismiss()
  }
  
  override def setMessage(message: String) = {
    dialog.setMessage(message)
  }


}

class ImageLoader(activity: Activity, view: ViewGroup, bitmap: Int) extends LoaderShower {

  override def show() = {

  }

  override def hide() = {

  }

}



