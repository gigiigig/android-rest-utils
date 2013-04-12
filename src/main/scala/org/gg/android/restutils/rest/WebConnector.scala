package org.gg.android.restutils.rest
import java.io.FileNotFoundException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import org.gg.android.restutils.util.Utils;

import scala.actors.Actor
import scala.collection.JavaConverters._
import android.app.Activity
import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.os.Handler
import android.preference.PreferenceManager
import android.util._
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView.ScaleType
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout

/*
 * Class for manage background donwload processes ,
 * Manage download background with download operation
 * noticed as ProgressDialog,
 * image injected in some GroupView
 * or image show over the activity content
 * 
 * This class is not to be used directly , 
 * but for be extended.
 * 
 * Every subclass must call executeRequest method 
 * for start managed background download,
 * 
 */
abstract class WebConnector(
  val activity: Context,
  private var successPostDownload: PostDownload = null,
  private var downloadErrorPostDownload: PostDownload = null,
  private var requestErrorPostDownload: PostDownload = null,
  tempCache: Boolean = false, permanentCache: Boolean = false,
  protected val loaderShower: LoaderShower,
  dataDownloadErrorMessage: String,
  dataRequestErrorMessage: String) extends WebConnectoreBase {

  def this(activity: Context,
           tempCache: Boolean,
           permanentCache: Boolean,
           loaderShower: LoaderShower,
           dataDownloadErrorMessage: String,
           dataRequestErrorMessage: String) = {
    this(activity, null, null, null, tempCache, permanentCache, loaderShower, dataDownloadErrorMessage, dataRequestErrorMessage)
  }

  /**
   * This is the simpler constructor with only
   * mandatory fields for make class working
   *
   */
  def this(activity: Context,
           tempCache: Boolean,
           permanentCache: Boolean,
           loaderShower: LoaderShower) = {
    this(activity, tempCache, permanentCache, loaderShower, "Download error", "Request error")
  }

  def this(activity: Context, loaderShower: LoaderShower, dataDownloadErrorMessage: String,
           dataRequestErrorMessage: String) = {
    this(activity, false, false, loaderShower, dataDownloadErrorMessage, dataRequestErrorMessage)
  }

  def this(activity: Context, loaderShower: LoaderShower) = {
    this(activity, false, false, loaderShower)
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

  /**
   * On downloadSuccess and onDownloadError prameters,
   * here are postDwonload class,
   * it means that are excuted after standard
   * functions wich manage the dialog's behavior,
   * for alter dialogs behavior,
   * it must be reimplement methods
   *
   * onDownloadSuccess(String)
   *
   * and
   *
   * onDownloadError
   *
   */
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

    val returned =
      try {
        doGet(url)
      } catch {
        case e =>
          Log.e(TAG, "doInBackground [" + e + "]")
          None
      }

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
      case None =>
        Log.d(TAG, "onPostExecute [returned none run onDownloadError]")
        onDownloadError
      case Some(content) => {
        Log.d(TAG, "doInBackground returned [content]")
        onDownloadSuccess(content)
      }
    }
  }

  def onDownloadSuccess(content: String) = {

    WebConnector ! Dismiss(activity.asInstanceOf[Activity], loaderShower)
    successPostDownload.execute(content)

  }

  def onDownloadError = {

    Log.w(TAG, "onDownloadError [download error]");

    WebConnector ! Message(activity.asInstanceOf[Activity], dataDownloadErrorMessage, loaderShower)
    new Handler().postDelayed(new Runnable() {
      override def run() = {
        WebConnector ! Dismiss(activity.asInstanceOf[Activity], loaderShower)
        if (downloadErrorPostDownload != null)
          downloadErrorPostDownload.execute(null)
      }
    }, 3000)

  }

  def onRequestError(error: String) = {

    Log.w(TAG, "onDownloadError [request error]");
    WebConnector ! Message(activity.asInstanceOf[Activity], dataRequestErrorMessage + " : " + error, loaderShower)
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
          Log.d(TAG, "act dialog for activity[" + activity + "] is[" + dialog + "]");

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

          val dialog = dialogs.get(activity)

          dialog match {
            case None =>
              Log.w(TAG, "act called dismiss from actvity without dilog registered[" + activity + "]");

            case Some(dialog) =>

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
          }

        case Message(activity, text, loaderShower) =>

          val dialog = dialogs.get(activity)

          dialog match {

            case None =>
              Log.w(TAG, "act called dismiss from actvity without dilog registered[" + activity + "]");

            case Some(dialog) =>

              runOnUi(activity, { () =>
                Log.d(TAG, "act [change message to progress dialog]")
                dialog._1 setMessage text
              })

          }

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

    scpref foreach { item: Tuple2[String,_] =>
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

  val TAG = classOf[DialogLoader].getName()

  val dialog = createDialog(activity.asInstanceOf[Activity])

  override def show = {
    dialog.show()
  }

  override def hide() = {
    dialog.dismiss()
  }

  override def setMessage(message: String) = {
    dialog.setMessage(message)
  }

  def createDialog(newActivity: Activity): ProgressDialog = {

    Log.d(TAG, "apply [set new activity and create new progressdialog]")

    val messageDialog = new ProgressDialog(newActivity)
    messageDialog.setIndeterminate(true)
    messageDialog.setMessage(message)

    messageDialog

  }

}

class ImageLoader(activity: Activity, view: ViewGroup, bitmapId: Int) extends LoaderShower {

  val TAG = classOf[ImageLoader].getName()

  //retrive image from id
  val image = ImageLoader.createImage(activity, bitmapId)

  val layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
    ViewGroup.LayoutParams.FILL_PARENT,
    Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL)

  val layout = new LinearLayout(activity)
  layout.addView(image, layoutParams)

  override def show() = {

    //set image on viewGroup as content
    //view.setVisibility(View.GONE)

    // shuld be the first image
    view.addView(layout, 0)

  }

  override def hide() = {
    //val index = view.indexOfChild(image)
    //Log.d(TAG, "hide index of image[" + index + "]")
    view.removeView(layout)
  }

  override def setMessage(message: String) = {
    //this is used maybe to show message only when necessary
  }

}

/**
 * Create an image loader located on an arbitrary
 * image position of the screen
 */
class LayoutedImageLoader(activity: Activity, bitmapId: Int, marginTop: Int) extends LoaderShower {

  def this(activity: Activity, bitmapId: Int) = {
    this(activity, bitmapId, 25)
  }

  var added = false

  //retrive image from id
  val image = ImageLoader.createImage(activity, bitmapId)

  val layout = new LinearLayout(activity)
  val params = new LinearLayout.LayoutParams(
    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
    Gravity.TOP | Gravity.CENTER_HORIZONTAL)

  params.setMargins(0, Utils.dipToPx(activity, marginTop).toInt, 0, 0)

  layout.addView(image, params)

  override def show() = {

    if (!added) {
      //add this content in fullscreen
      //to activity , image should be centered
      activity.addContentView(layout, new ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT))

      added = true

    } else {
      layout.setVisibility(View.VISIBLE)
    }
  }

  override def hide() = {
    layout.setVisibility(View.GONE)
  }

  override def setMessage(message: String) = {
    //this is used maybe to show message only when necessary
  }

}

object ImageLoader {

  def createImage(activity: Activity, bitmapId: Int): ImageView = {
    //retrive image from id
    val image = new ImageView(activity)
    image.setImageResource(bitmapId)
    val params = new ViewGroup.LayoutParams(80, 80)
    image.setLayoutParams(params)
    image.setScaleType(ScaleType.CENTER)
    image.setAnimation(animation)

    image
  }

  lazy val animation = {

    val ROTATE_FROM = 0.0f; // from what position you want to rotate
    // it
    val ROTATE_TO = 360.0f; // how many times you want it to
    // rotate in one 'animation' (in
    // this example you want to fully
    // rotate -360 degrees- it 10
    // times)
    //
    val r = new RotateAnimation(ROTATE_FROM, ROTATE_TO,
      Animation.RELATIVE_TO_SELF, 0.5f,
      Animation.RELATIVE_TO_SELF, 0.5f);
    r.setDuration(1000); // here you determine how fast you want the
    // image to rotate
    r.setRepeatCount(Animation.INFINITE); // how many times you want to
    // repeat the animation
    r.setInterpolator(new LinearInterpolator()); // the curve of the
    // animation; use
    // LinearInterpolator
    // to keep a consistent
    // speed all the way

    r

  }

}


