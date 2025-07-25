**Important Notice**
Remote control by broadcast intents has to be enabled under "Settings" > "Behaviour" before Syncthing listens to broadcast intents sent by third-party automation apps.

Syncthing can be controlled externally by sending Broadcast-Intents. Applications like **Tasker**, **Llama** or **Automate** now can _start_ or _stop_ Syncthing on behalf of the user.
Use cases would be to run Syncthing only in special conditions - like at home and charging, or once every night, ...

The following intent actions are available:
* Let Syncthing Follow Run Conditions: com.github.catfriend1.syncthingandroid.action.FOLLOW
`adb shell am broadcast -a com.github.catfriend1.syncthingandroid.action.FOLLOW -p com.github.catfriend1.syncthingandroid`

* Force Start Syncthing: com.github.catfriend1.syncthingandroid.action.START
`adb shell am broadcast -a com.github.catfriend1.syncthingandroid.action.START -p com.github.catfriend1.syncthingandroid`

* Force Stop Syncthing: com.github.catfriend1.syncthingandroid.action.STOP
`adb shell am broadcast -a com.github.catfriend1.syncthingandroid.action.STOP -p com.github.catfriend1.syncthingandroid`

The intents should be set to 'broadcast' rather than starting an activity of service. Note that some apps, e.g. **Llama**, are sensitive to trailing spaces so be careful not to leave any when entering the action.

Tasker example action to start Syncthing:
* Action: Send Intent
```
Action: com.github.catfriend1.syncthingandroid.action.START
Type: None
Mime type: [ leave empty ]
Data: [ leave empty ]
Extra: [ leave empty ]
Package: com.github.catfriend1.syncthingandroid / for developers: com.github.catfriend1.syncthingandroid.debug
Class: [ leave empty ]
Target: Broadcast Receiver
Description: Start Syncthing
```

For the **Automate** app there is an example-flow available in the Automate-Community that demonstrates the start- and the stop-intent. Search for *Syncthing*.
