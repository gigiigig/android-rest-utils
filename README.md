android-rest-utils
==================

Some utils to consume REST web services and asyncronous images load

This is an android library which contains some useful utils to
consume rest services, it is implemented using AsynkTask and 
the Scala actor model, that means project is written in Scala,
and use sbt as build tool, with [Jberkel androi plugin](https://github.com/jberkel/android-plugin)

If you don't want install sbt to build project, just download compiled jar from [classes.min.jar](https://www.dropbox.com/s/602m9g6emcr0y6s/classes.min.jar)
and include in lib folder of your Android project.

The class to use for call REST services is `WebConnector`, and to start request use `executeRequest` method.

To create an ImageView which load an image from a remote url , use class `BitmapWebLoader` and call the method `loadImageFromWeb`

`WebConnector` class have different constructors to fit different situations, best way to use is subclass and create
a simpler constrctor, this is an example:

    public class BaseWebloader extends WebConnector {

        public BaseWebloader(Context activity) {
            super(activity, true, false, getDialogLoader(activity), activity
                    .getString(R.string.data_download_error), activity
                    .getString(R.string.data_download_error));
        }
    
        public BaseWebloader(Context activity, ViewGroup view) {
            super(activity, true, false, getLayoutedImageLoader(activity));
        }
    
        protected static DialogLoader getDialogLoader(Context activity) {
            return new DialogLoader(activity,
                    activity.getString(R.string.data_download));
        }
    
        protected static ImageLoader getImageLoader(Context activity, ViewGroup view) {
            return new ImageLoader((Activity) activity, view, R.drawable.loader);
        }
    
        protected static LayoutedImageLoader getLayoutedImageLoader(Context activity) {
            return new LayoutedImageLoader((Activity) activity, R.drawable.loader,
                    85);
        }
    
        public BaseWebloader(Context activity, boolean tempCache,
                boolean permanentCache) {
            super(activity, tempCache, permanentCache, getDialogLoader(activity));
        }
    }

`BitmapWebLoader` is easier to use 
    
    String imageUrl = "YOUR IMAGE URL";
    ImageView image = activity.findViewById(R.id.image_view_id);

    new BitmapWebLoader(activity, image, R.drawable.loader,
            R.drawable.not_image).loadImageFromWeb(imageUrl);
