**Important Notice**
Remote control by broadcast intents has to be enabled under "Settings" > "Behaviour" before Syncthing listens to broadcast intents sent by third-party automation apps.

Syncthing can be controlled externally by sending Broadcast-Intents. Applications like **Tasker**, **Llama** or **Automate** now can _start_ or _stop_ Syncthing on behalf of the user.
Use cases would be to run Syncthing only in special conditions - like at home and charging, or once every night, ...

The following intent actions are available:
* Let Syncthing Follow Run Conditions: com.fireworld.syncthing.action.FOLLOW
`adb shell am broadcast -a com.fireworld.syncthing.action.FOLLOW -p com.fireworld.syncthing`

* Force Start Syncthing: com.fireworld.syncthing.action.START
`adb shell am broadcast -a com.fireworld.syncthing.action.START -p com.fireworld.syncthing`

* Force Stop Syncthing: com.fireworld.syncthing.action.STOP
`adb shell am broadcast -a com.fireworld.syncthing.action.STOP -p com.fireworld.syncthing`

The intents should be set to 'broadcast' rather than starting an activity of service. Note that some apps, e.g. **Llama**, are sensitive to trailing spaces so be careful not to leave any when entering the action.

Tasker example action to start Syncthing:
* Action: Send Intent
```
Action: com.fireworld.syncthing.action.START
Type: None
Mime type: [ leave empty ]
Data: [ leave empty ]
Extra: [ leave empty ]
Package: com.fireworld.syncthing / for developers: com.fireworld.syncthing.debug
Class: [ leave empty ]
Target: Broadcast Receiver
Description: Start Syncthing
```

For the **Automate** app there is an example-flow available in the Automate-Community that demonstrates the start- and the stop-intent. Search for *Syncthing*.
