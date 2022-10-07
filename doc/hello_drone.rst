Ground SDK Hello Drone Tutorial
===============================

*Ground SDK Hello Drone Tutorial* is a step-by-step guide that helps you
develop an Android application using Ground SDK Android |version|.
This application is able to connect to an ANAFI drone and a Skycontroller remote control,
display the battery charge level and video stream, and take off or land the drone.

At the end of this tutorial, you will be able to:

- Setup your developement environement
- Setup your project to use Ground SDK Android |version|
- Connect to a drone
- Display drone connection state
- Display drone battery charge level
- Make take off and land the drone
- Display the video live stream
- Connect to a remote control
- Display remote control connection state
- Display remote control battery charge level

The full project is available `here`_.

.. Note:: This tutorial is based on Ground SDK Android version |version|.

.. _here : https://github.com/Parrot-Developers/groundsdk-android-samples/tree/master/HelloDrone

Prerequisites
^^^^^^^^^^^^^

Before starting this tutorial, you have to:

 - `Download Andoid Studio`_
 - `Install Andoid Studio`_
 - `Create a new Kotlin project (Empty Activity)`_

.. _Download Andoid Studio: https://developer.android.com/studio
.. _Install Andoid Studio: https://developer.android.com/studio/install
.. _Create a new Kotlin project (Empty Activity): https://developer.android.com/studio/projects/create-project?hl=en

Setup project
^^^^^^^^^^^^^

First you need to configure your project to use Ground SDK Android.

For this purpose, open the application `app/build.gradle` file, and add
the Ground SDK Android dependencies:

.. literalinclude:: ../HelloDrone/app/build.gradle
   :lines: 34-48
   :emphasize-lines: 12-14

This allows to downlaod and link Ground SDK AAR to the project.

To make your project compatible with Ground SDK you need to increase the minimun Android SDK version
supported by your project.

In the same file:

.. literalinclude:: ../HelloDrone/app/build.gradle
   :lines: 6-17
   :emphasize-lines: 6-7

Your project setup is ready, let's start coding!

Get GroundSdk session
^^^^^^^^^^^^^^^^^^^^^

In order to use GroundSdk in your application, you first have to
`obtain a GroundSdk session`_ at the activity creation. So open your activity
file, and add:

.. literalinclude:: ../HelloDrone/app/src/main/java/com/parrot/hellodrone/MainActivity.kt
   :language: kotlin
   :lines: 60-63, 102-106, 120-124, 447
   :emphasize-lines: 3-4, 10-13

This `GroundSdk session`_ keeps and manages all GroundSdk references, according
to the Android Activity lifecycle.

.. _obtain a GroundSdk session: https://developer.parrot.com/docs/refdoc-android/groundsdk%207.0.0%20-a-p-i/com.parrot.drone.groundsdk/-managed-ground-sdk/obtain-session.html
.. _GroundSdk session: https://developer.parrot.com/docs/refdoc-android/groundsdk%207.0.0%20-a-p-i/com.parrot.drone.groundsdk/-managed-ground-sdk/index.html

Drone connection
^^^^^^^^^^^^^^^^

To connect to a drone, you should use the `AutoConnection`_ facility.

At the Activity start, `get the facility`_ and `start it`_.

.. literalinclude:: ../HelloDrone/app/src/main/java/com/parrot/hellodrone/MainActivity.kt
   :language: kotlin
   :lines: 126-137, 172-174
   :emphasize-lines: 4-14

Auto connection will automatically select and connect the device.

You need to monitor the `drone`_ change to stop using the old one and start
using the new one.

.. literalinclude:: ../HelloDrone/app/src/main/java/com/parrot/hellodrone/MainActivity.kt
   :language: kotlin
   :lines: 65-67, 80, 126-143, 147-153, 171-174, 187-191, 203-208, 227
   :emphasize-lines: 2-3, 18-30, 35-39, 41-45

.. _AutoConnection: https://developer.parrot.com/docs/refdoc-android/groundsdk%207.0.0%20-a-p-i/com.parrot.drone.groundsdk.facility/-auto-connection/index.html
.. _get the facility: https://developer.parrot.com/docs/refdoc-android/groundsdk%207.0.0%20-a-p-i/com.parrot.drone.groundsdk/-ground-sdk/get-facility.html
.. _start it: https://developer.parrot.com/docs/refdoc-android/groundsdk%207.0.0%20-a-p-i/com.parrot.drone.groundsdk.facility/-auto-connection/start.html
.. _drone: https://developer.parrot.com/docs/refdoc-android/groundsdk%207.0.0%20-a-p-i/com.parrot.drone.groundsdk.facility/-auto-connection/get-drone.html

Drone monitoring
^^^^^^^^^^^^^^^^

Now you will monitor and display the drone connection state and its battery
state.

Drone user interface
--------------------

To display drone information, replace the default TextView by your own
TextViews in your layout.

Your `res/layout/activity_main.xml` file should look like
this:

.. literalinclude:: ../HelloDrone/app/src/main/res/layout/activity_main.xml
   :language: xml
   :lines: 1-45, 75, 94
   :emphasize-lines: 9-46

Then get and initialize this new text views in your activity.

.. literalinclude:: ../HelloDrone/app/src/main/java/com/parrot/hellodrone/MainActivity.kt
   :language: kotlin
   :lines: 89, 92-95, 102-107, 109-110, 115-117, 119-124
   :emphasize-lines: 1-5, 11-16

Drone state monitoring
----------------------

In order to display the drone connection state, set an observer on the
`drone state`_, and get its `ConnectionState`_.

When you have finished with it and you want to stop monitoring it,
`close`_ the drone state reference.

.. literalinclude:: ../HelloDrone/app/src/main/java/com/parrot/hellodrone/MainActivity.kt
   :language: kotlin
   :lines: 65-69, 80, 188-193, 203-212, 227-228, 279-292
   :emphasize-lines: 4-5, 11-12, 19-22, 25-

.. _drone state: https://developer.parrot.com/docs/refdoc-android/groundsdk%207.0.0%20-a-p-i/com.parrot.drone.groundsdk.device/-drone/get-state.html
.. _ConnectionState: https://developer.parrot.com/docs/refdoc-android/groundsdk%207.0.0%20-a-p-i/com.parrot.drone.groundsdk.device/-device-state/get-connection-state.html
.. _close: https://developer.parrot.com/docs/refdoc-android/groundsdk%207.0.0%20-a-p-i/com.parrot.drone.groundsdk/-ref/close.html

Drone battery monitoring
------------------------

In order to display the drone battery charge level, monitor the drone
`battery info instrument`_, using `getInstrument`_, then get its
`charge`_ level.

.. literalinclude:: ../HelloDrone/app/src/main/java/com/parrot/hellodrone/MainActivity.kt
   :language: kotlin
   :lines: 65-71, 80, 188-196, 203-215, 227-228, 294-307
   :emphasize-lines: 6-7, 16-17, 29-30, 33-

.. _battery info instrument: https://developer.parrot.com/docs/refdoc-android/groundsdk%207.0.0%20-a-p-i/com.parrot.drone.groundsdk.device.instrument/-battery-info/index.html
.. _getInstrument: https://developer.parrot.com/docs/refdoc-android/groundsdk%207.0.0%20-a-p-i/com.parrot.drone.groundsdk.device.instrument/-instrument/-provider/get-instrument.html
.. _charge: https://developer.parrot.com/docs/refdoc-android/groundsdk%207.0.0%20-a-p-i/com.parrot.drone.groundsdk.device.instrument/-battery-info/charge.html

Reset drone user interface
--------------------------

When you stop monitoring a drone, you have to reset the drone user interface
to prevent garbage display.

.. literalinclude:: ../HelloDrone/app/src/main/java/com/parrot/hellodrone/MainActivity.kt
   :language: kotlin
   :lines: 126-155, 172-182, 186
   :emphasize-lines: 20-21, 35-

Take off / land button
^^^^^^^^^^^^^^^^^^^^^^

Now you will add a button to enable take off and landing.

Button layout
-------------

In your layout, add a button at the bottom of the screen.

Edit the `res/layout/activity_main.xml` file:

.. literalinclude:: ../HelloDrone/app/src/main/res/layout/activity_main.xml
   :language: xml
   :lines: 1-45, 75-76, 86-94
   :emphasize-lines: 48-54

Now bind the newly added button in your main activity, and attach a listener to
it.

.. literalinclude:: ../HelloDrone/app/src/main/java/com/parrot/hellodrone/MainActivity.kt
   :language: kotlin
   :lines: 89, 92-95, 100-107, 109-110, 113-117, 119-125, 368-372, 383
   :emphasize-lines: 6-7, 16-17, 28-

Manual piloting monitor
-----------------------

In order to pilot the drone, you have to use the
`Manual Copter Piloting Interface`_.

Monitor it using `getPilotingItf`_,
and update the button view according to the availability of these actions
(`canTakeOff`_ / `canLand`_).

.. literalinclude:: ../HelloDrone/app/src/main/java/com/parrot/hellodrone/MainActivity.kt
   :language: kotlin
   :lines: 65-73, 80, 176-183, 186-199, 203-218, 227-228, 309-366
   :emphasize-lines: 8-9, 18, 31-32, 47-48, 51-67, 69-

.. _Manual Copter Piloting Interface: https://developer.parrot.com/docs/refdoc-android/groundsdk%207.0.0%20-a-p-i/com.parrot.drone.groundsdk.device.pilotingitf/-manual-copter-piloting-itf/index.html
.. _getPilotingItf: https://developer.parrot.com/docs/refdoc-android/groundsdk%207.0.0%20-a-p-i/com.parrot.drone.groundsdk.device.pilotingitf/-piloting-itf/-provider/get-piloting-itf.html
.. _canTakeOff: https://developer.parrot.com/docs/refdoc-android/groundsdk%207.0.0%20-a-p-i/com.parrot.drone.groundsdk.device.pilotingitf/-manual-copter-piloting-itf/can-take-off.html
.. _canLand: https://developer.parrot.com/docs/refdoc-android/groundsdk%207.0.0%20-a-p-i/com.parrot.drone.groundsdk.device.pilotingitf/-manual-copter-piloting-itf/can-land.html

Take off / landing requests
---------------------------

Now you need to `take off`_ or `land`_ the drone when the button is clicked,
according to their availabilities.

.. literalinclude:: ../HelloDrone/app/src/main/java/com/parrot/hellodrone/MainActivity.kt
   :language: kotlin
   :lines: 72-73, 80, 368-383
   :emphasize-lines: 8-18

.. _take off: https://developer.parrot.com/docs/refdoc-android/groundsdk%207.0.0%20-a-p-i/com.parrot.drone.groundsdk.device.pilotingitf/-manual-copter-piloting-itf/take-off.html
.. _land: https://developer.parrot.com/docs/refdoc-android/groundsdk%207.0.0%20-a-p-i/com.parrot.drone.groundsdk.device.pilotingitf/-manual-copter-piloting-itf/land.html

Video stream
^^^^^^^^^^^^

The next step will allow you to add a live stream video view.

Video layout
------------

In your `res/layout/activity_main.xml` layout, add a `GsdkStreamView`_

.. literalinclude:: ../HelloDrone/app/src/main/res/layout/activity_main.xml
   :language: xml
   :lines: 1-45, 75-94
   :emphasize-lines: 48-55

Then get it in your activity.

.. literalinclude:: ../HelloDrone/app/src/main/java/com/parrot/hellodrone/MainActivity.kt
   :language: kotlin
   :lines: 89-95, 102-110, 115-117, 119-124
   :emphasize-lines: 2-3, 14

.. _GsdkStreamView: https://developer.parrot.com/docs/refdoc-android/groundsdk%207.0.0%20-a-p-i/com.parrot.drone.groundsdk.stream/-gsdk-stream-view/index.html

Video display
-------------

In order to display the live video stream in the GsdkStreamView, you need to:

- Monitor the `stream server peripheral`_
- `Monitor its live stream`_
- `Start to play`_ the stream
- `Attach this stream to your GsdkStreamView`_
- Detach the stream from the GsdkStreamView when you want to stop rendering the
  stream

.. literalinclude:: ../HelloDrone/app/src/main/java/com/parrot/hellodrone/MainActivity.kt
   :language: kotlin
   :lines: 65-80, 176-277
   :emphasize-lines: 10-15, 25-26, 42-43, 61-67, 70-

.. _stream server peripheral: https://developer.parrot.com/docs/refdoc-android/groundsdk%207.0.0%20-a-p-i/com.parrot.drone.groundsdk.device.peripheral/-stream-server/index.html
.. _Monitor its live stream: https://developer.parrot.com/docs/refdoc-android/groundsdk%207.0.0%20-a-p-i/com.parrot.drone.groundsdk.device.peripheral/-stream-server/live.html
.. _Start to play: https://developer.parrot.com/docs/refdoc-android/groundsdk%207.0.0%20-a-p-i/com.parrot.drone.groundsdk.device.peripheral.stream/-camera-live/play.html
.. _Attach this stream to your GsdkStreamView: https://developer.parrot.com/docs/refdoc-android/groundsdk%207.0.0%20-a-p-i/com.parrot.drone.groundsdk.stream/-gsdk-stream-view/set-stream.html

Remote control
^^^^^^^^^^^^^^

In this section you will see how to connect to a remote control, display its
connection state and battery charge level.

Remote control connection
-------------------------

You can use the `auto connection facility`_ as with the drone, and get the
`remote control`_ from it.

.. literalinclude:: ../HelloDrone/app/src/main/java/com/parrot/hellodrone/MainActivity.kt
   :language: kotlin
   :lines: 81-83, 88, 126-175, 385-388, 392-397, 403-408, 416
   :emphasize-lines: 1-3, 35-50, 55-71

.. _auto connection facility: https://developer.parrot.com/docs/refdoc-android/groundsdk%207.0.0%20-a-p-i/com.parrot.drone.groundsdk.facility/-auto-connection/index.html
.. _remote control: https://developer.parrot.com/docs/refdoc-android/groundsdk%207.0.0%20-a-p-i/com.parrot.drone.groundsdk.facility/-auto-connection/get-remote-control.html

Remote control user interface
-----------------------------

To display remote control information, add TextViews in the
`res/layout/activity_main.xml` layout.

.. literalinclude:: ../HelloDrone/app/src/main/res/layout/activity_main.xml
   :language: xml
   :emphasize-lines: 47-74

Then get, initialize and reset this new text views in your activity.

.. literalinclude:: ../HelloDrone/app/src/main/java/com/parrot/hellodrone/MainActivity.kt
   :language: kotlin
   :lines: 89-124, 385-392
   :emphasize-lines: 8-11, 23-24, 30, 41-43, 41-43

Remote control state and battery
--------------------------------

As with the drone, set an observer on the `remote control state`_ to display
its `connectionState`_.

Then monitor the `battery info instrument`_, using
`getInstrument`_ and display its `charge`_ level.

Finally, `close`_ the remote control references to stop monitoring them.

.. literalinclude:: ../HelloDrone/app/src/main/java/com/parrot/hellodrone/MainActivity.kt
   :language: kotlin
   :lines: 81-88, 394-446
   :emphasize-lines: 4-7, 13-17, 24-30, 33-61

.. _remote control state: https://developer.parrot.com/docs/refdoc-android/groundsdk%207.0.0%20-a-p-i/com.parrot.drone.groundsdk.device/-remote-control/get-state.html
.. _connectionState: https://developer.parrot.com/docs/refdoc-android/groundsdk%207.0.0%20-a-p-i/com.parrot.drone.groundsdk.device/-device-state/get-connection-state.html
.. _battery info instrument: https://developer.parrot.com/docs/refdoc-android/groundsdk%207.0.0%20-a-p-i/com.parrot.drone.groundsdk.device.instrument/-battery-info/index.html
.. _getInstrument: https://developer.parrot.com/docs/refdoc-android/groundsdk%207.0.0%20-a-p-i/com.parrot.drone.groundsdk.device.instrument/-instrument/-provider/get-instrument.html
.. _charge: https://developer.parrot.com/docs/refdoc-android/groundsdk%207.0.0%20-a-p-i/com.parrot.drone.groundsdk.device.instrument/-battery-info/charge.html
.. _close: https://developer.parrot.com/docs/refdoc-android/groundsdk%207.0.0%20-a-p-i/com.parrot.drone.groundsdk/-ref/close.html

Full project sources
^^^^^^^^^^^^^^^^^^^^

Thank you for having followed this tutorial. Hoping it was helpful to you.

You can find the full project on `github`_.

Please feel free to ask questions on the `Parrot forum for developers`_.

Wish you all the best with `Ground SDK`_!

.. _github : https://github.com/Parrot-Developers/groundsdk-android-samples/tree/master/HelloDrone
.. _Parrot forum for developers: https://forum.developer.parrot.com/
.. _Ground SDK: https://www.parrot.com/fr/drones/anafi-ai/technical-documentation/sdk
