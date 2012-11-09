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

class BitmapWebLoader(context: Context) extends WebConnector(context) {
  override def onDownloadSuccess(content: String) : Unit = {

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

      if (result != null) {
        // la view nel frattempo postrebbe non esistere più se
        // l'utente
        // ha cambiato pagina
        Log.d(TAG, "ImagePostDownload execute[init set bitmap]");
        view.clearAnimation();
        view.setScaleType(ScaleType.CENTER_CROP);
        view.setImageBitmap(result.asInstanceOf[Bitmap]);
        view.invalidate();
        Log.d(TAG, "ImagePostDownload execute[end set bitmap]");

      } else {
        view.setImageResource(R.drawable.not_image);
      }

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