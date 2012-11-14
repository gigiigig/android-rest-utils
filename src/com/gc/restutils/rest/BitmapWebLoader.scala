package com.gc.restutils.rest

import android.content.Context
import android.view.animation.RotateAnimation
import android.widget.ImageView
import android.util.Log
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.widget.ImageView.ScaleType
import com.gc.restutils.R
import android.graphics.Bitmap
import org.apache.http.HttpResponse
import org.apache.http.params.HttpConnectionParams
import org.apache.http.HttpEntity
import org.apache.http.client.methods.HttpGet
import android.graphics.drawable.BitmapDrawable
import org.apache.http.entity.BufferedHttpEntity
import org.apache.http.params.BasicHttpParams
import java.net.MalformedURLException
import java.io.InputStream
import java.io.ByteArrayOutputStream
import org.apache.http.client.HttpClient
import java.io.ByteArrayInputStream
import java.net.URL
import org.apache.http.impl.client.DefaultHttpClient

class BitmapWebLoader(context: Context, view: ImageView = null) extends WebConnector(context) {

  import WebConnector._

  override val TAG = classOf[BitmapWebLoader].toString()

  protected var image: Bitmap = null
  protected val imagePostDownload: ImagePostDownload = new ImagePostDownload(view)
  protected val onlySave = view == null

  override def onPreExecute() = {
    //not call super to avoid to show dialog
    //called only for onlySave , used for batch download
    if (onlySave) {
      super.onPreExecute()
      WebConnector ! Message("download images")
    }
    imagePostDownload.prepareView()
  }

  override def onDownloadSuccess(content: String): Unit = {
    imagePostDownload.execute(image)
  }

  override def onPostExecute(content: Option[String]) = {
    //dismiss is called evrytime , but probably is not nedded
    //in the most cases
    WebConnector ! Dismiss()
    content match {

      case None =>
      case Some(content) => {
        //remove if dialog is not called
        onDownloadSuccess(content)
      }

    }

  }

  override def doInBackground(url: String) = {
    image = downloadImage(url)
    if (!onlySave) Some("") else None
  }

  def loadImageFromWeb(url: String): Unit = {
    loadImageFromWeb(url, this.imagePostDownload)
  }

  def loadImageFromWeb(url: String, newPostDowload: ImagePostDownload): Unit = {

    //if (newPostDowload != null) this.imagePostDownload = newPostDowload
    Log.d(TAG, "loadImageFromWeb serach image with url [" + url + "]")

    // Log.d(TAG,
    // "loadImageFromWeb [image not in current cache try with permanent cache]")

    var bs = getImageFromPermanentCache(url, context)

    if (bs != null) {
      Log.d(TAG, "loadImageFromWeb [founded image in permanent cache, crate bitmap]")
      var bitmap: Bitmap = null

      try {
        bitmap = new BitmapDrawable(new ByteArrayInputStream(bs))
          .getBitmap()
        Log.d(TAG, "loadImageFromWeb created bitmap[" + bitmap
          + "]")

      } catch {
        case e => Log.e(TAG, "loadImageFromWeb bitmap creation error[" + e + "]")
      }

      if (bitmap == null)
        deleteImageFromCache(url, context)
      else
        newPostDowload.execute(bitmap)

    } else {
      Log.d(TAG,
        "loadImageFromWeb [image not in permanent cache, start download]")
      execute(url)
    }

  }

  protected def downloadImage(url: String): Bitmap = {

    try {
      Log.d(TAG, "downloadImage ciao url[" + url + "]");

      // InputStream is = (InputStream) new
      // URL(url+"?mime=bmp").getContent();
      // BitmapDrawable d = (BitmapDrawable)
      // Drawable.createFromStream(
      // is, "test.jpg");

      // if needed add domain
      val WS_DOMAIN = ""
      val origUrl = url
      val newUrl = WS_DOMAIN + url

      val httpRequest = new HttpGet(new URL(url).toURI())
      val httpParameters = new BasicHttpParams()

      // Set the timeout in milliseconds until a connection is
      // established.
      val timeoutConnection = 15000
      HttpConnectionParams.setConnectionTimeout(httpParameters,
        timeoutConnection);
      // Set the default socket timeout (SO_TIMEOUT)
      // in milliseconds which is the timeout for waiting for data.
      val timeoutSocket = 15000
      HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);

      val httpclient = new DefaultHttpClient(httpParameters)
      val response = httpclient.execute(httpRequest).asInstanceOf[HttpResponse]

      val entity = response.getEntity()
      val bufHttpEntity = new BufferedHttpEntity(entity)

      val instream = bufHttpEntity.getContent()

      val bos = inputStreamToByteArray(instream)
      saveCache(origUrl, bos.toByteArray(), context)

      if (onlySave) {
        return null;
      } else {
        return new BitmapDrawable(new ByteArrayInputStream(
          bos.toByteArray())).getBitmap();
      }

    } catch {
      case e =>
        Log.w(TAG, "downloadImage [" + e + "]")
        null
    }

  }

  protected def inputStreamToByteArray(instream: InputStream): ByteArrayOutputStream = {
    val bos = new ByteArrayOutputStream()
    var next = instream.read()
    while (next > -1) {
      bos.write(next);
      next = instream.read()
    }
    bos.flush();
    bos
  }

  protected def saveCache(url: String, image: Array[Byte], context: Context) = {
    putImageToPermanentCache(url, image, context)
  }

  def putImageToPermanentCache(url: String, image: Array[Byte], context: Context) = {
    try {
      Log.d(TAG, "putImageToPermanentCache [" + url + "]");
      writeObjectToDisk(image, url.replace("/", "_"), context);
    } catch {
      case e => Log.e(TAG, "doInBackground [" + e + "]");
    }
  }

  protected def getImageFromPermanentCache(url: String, context: Context): Array[Byte] = {

    Log.d(TAG, "getImageFromPermanentCache url[" + url + "]");

    val objectFromDisk = getObjectFromDisk(url.replace("/", "_"),
      context);
    if (objectFromDisk != null)
      return objectFromDisk.asInstanceOf[Array[Byte]]
    else
      return null;

  }

  protected def deleteImageFromCache(url: String, context: Context) = {
    deleteObjectFromDisk(url.replace("/", "_"), context)
  }

}

class ImagePostDownload(view: ImageView) extends PostDownload {

  val TAG = classOf[ImagePostDownload].toString

  def prepareView() {

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
    try {
      // la view nel frattempo postrebbe non esistere più se l'utente
      // ha cambiato pagina
      view.setImageResource(R.drawable.loader);
      view.setScaleType(ScaleType.CENTER);
      view.startAnimation(r);
    } catch {
      case e: NullPointerException => Log.w(TAG, "doInBackground [" + e + "]")
    }
  }

  override def execute(result: Object) = {

    try {

      view.clearAnimation();
      view.setScaleType(ScaleType.CENTER_CROP);

      if (result != null) {
        // la view nel frattempo postrebbe non esistere più se
        // l'utente
        // ha cambiato pagina
        Log.d(TAG, "ImagePostDownload execute[set bitmap]");
        view.setImageBitmap(result.asInstanceOf[Bitmap]);

      } else {
        view.setImageResource(R.drawable.not_image);
      }

      view.invalidate();

    } catch {
      case e: NullPointerException =>
        Log.w(TAG, "ImagePostDownload execute[" + e + "]")
        view.setImageResource(R.drawable.not_image);
    }

  }

  def getView(): ImageView = {
    return view;
  }

}