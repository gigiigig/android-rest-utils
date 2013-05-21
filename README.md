android-rest-utils
==================

Some utils to consume REST web services and asyncronous images load

This is an android library which contains some useful utils to
consume rest services, it is implemented using AsynkTask and 
the Scala actor model, that means project is written in Scala,
and use sbt as build tool, with [Jberkel androi plugin](https://github.com/jberkel/android-plugin)

If you don't want install sbt to build project, just download compiled jar from [classes.min.jar](https://www.dropbox.com/s/602m9g6emcr0y6s/classes.min.jar)
and include in lib folder of your Android project.

The class to user for call REST services is `WebConnector`, and to start request use `executeRequest` method.

To create an ImageView which load an image from a remote url , use class `BitmapWebLoader` and call the method `loadImageFromWeb`
    
