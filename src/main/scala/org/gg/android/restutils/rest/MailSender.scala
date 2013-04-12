package org.gg.android.restutils.rest
import android.content.Context
import org.gg.android.restutils.util.MailUtils;

import scala.collection.JavaConversions
import android.util.Log

class MailSender(activity: Context, sender: String,
                 receivers: Array[String], subject: String,
                 message: String, loader: LoaderShower) extends WebConnector(activity, loader) {

  override val TAG = classOf[MailSender].toString()
  
  def this(activity: Context, sender: String,
           receivers: Array[String], subject: String,
           message: String, dialogMessage: String) = {
    this(activity, sender, receivers, subject, message, new DialogLoader(activity, dialogMessage))

  }

  override def doInBackground(url: String) = {

    val mailUtils = new MailUtils("", "", "mail.apphotels.com")

    mailUtils.setSubject(subject)
    mailUtils.setBody(message)
    mailUtils.setFrom(sender)
    mailUtils.setTo(receivers)
    
    try{
      mailUtils.send()
      Option("ok")
    } catch {
      case e => 
        Log.e(TAG, "doInBackground [" + e + "]")
        None
    }   

  }

}