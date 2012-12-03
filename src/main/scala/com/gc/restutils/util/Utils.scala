package com.gc.restutils.util
import android.util.TypedValue
import android.content.Context
import android.content.res.Resources

object Utils {

  def dipToPx(context: Context, dip: Int): Float = {
    // Converts 14 dip into its equivalent px
    var r: Resources = context.getResources()
    val px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip,
      r.getDisplayMetrics());
    return px;
    // return dip;
  }

}