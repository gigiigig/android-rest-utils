package com.gc.restutils

import android.os.AsyncTask
import scala.xml._
import scala.io._
import scala.io.Source
import android.util._
import android.app.Activity
import java.util.Date
import android.app.ProgressDialog
import android.os.Handler
import java.text.SimpleDateFormat
import scala.actors.Actor
import scala.actors.Actor._

abstract class WebConnector(activity: Activity,
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

  val BASE_URL = "http://services.begenius.it/hotel/xml/"

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

}

trait PostDownload {

  def execute(obj: Object)

}
