package com.gc.restutils.rest;

import scala.Option;
import android.os.AsyncTask;

public abstract class WebConnectoreBase extends
        AsyncTask<String, String, Option<String>> {

    @Override
    protected Option<String> doInBackground(String... arg0) {

        return doInBackground(arg0[0]);

    }

    protected abstract Option<String> doInBackground(String operation);

}
