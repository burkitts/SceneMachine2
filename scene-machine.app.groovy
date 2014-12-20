/**
 *  App Name:   Scene Machine
 *
 *  Author: 	Todd Wackford
 *				twack@wackware.net
 *  Date: 		2013-06-14
 *  Version: 	1.0
 *  
 *  Updated:    2013-07-25
 *  
 *  BF #1		Fixed bug where string null was being returned for non-dimmers and
 *              was trying to assign to variable.
 *  
 *  
 *  This app lets the user select from a list of switches or dimmers and record 
 *  their currents states as a Scene. It is suggested that you name the app   
 *  during install something like "Scene - Romantic Dinner" or   
 *  "Scene - Movie Time". Switches can be added, removed or set at new levels   
 *  by editing and updating the app from the smartphone interface.
 *
 *  Usage Note: GE-Jasco dimmers with ST is real buggy right now. Sometimes the levels
 *              get correctly, sometimes not.
 *              On/Off is OK.
 *              Other dimmers should be OK.
 *
 *	Modified by Andrew Cockburn:
 *	- Add Virtual Button Support
 *	- Add support for Hue Bulbs
 *
 *	With this version, for scenes in general and Hue bulbs in particular, i have found best results by setting up the scene using the Phone App
 *  The leaving it for 5 minutes or so before recording. Probably a polling issue.
 *  
 * Use License: Non-Profit Open Software License version 3.0 (NPOSL-3.0)
 *              http://opensource.org/licenses/NPOSL-3.0
 */
 
definition(
    name: "Scene Machine 2",
    namespace: "aimc",
    author: "Todd Wackford, additions by Andrew Cockburn",
    description: "Record a static scene and optionally activate with a switch (virtual or real). Supports lights, dimmers and Hue bulbs.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	section("Select Switch to monitor"){
		input "theSwitch", "capability.switch", required: false
	}
    section("Select switches ...") {
		input "switches", "capability.switch", multiple: true
	}
    
    //Uncomment this section below to test/change on the IDE. Smartphone does 
	//not need it.
	
    //section("Record New Scene?") {  	
	//	input "record", "enum", title: "New Scene...", multiple: false, 
	//         required: true, metadata:[values:['No','Yes']]
	//}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	subscribe(app, appTouch) 
	if (theSwitch)
    {
    	subscribe(theSwitch, "switch.On", onHandler)
    }
    getDeviceSettings()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	if (theSwitch)
    {
		subscribe(theSwitch, "switch.On", onHandler)
    }
    subscribe(app, appTouch)
    
   //if(record == "Yes") //uncomment this line to test/change stuff on the IDE
      getDeviceSettings()
}

def appTouch(evt) {
	log.debug "appTouch: $evt"
	setScene()
}

def onHandler(evt) {
	log.debug "Switch: $evt"
	setScene()
}

private setScene() {

	def i = 0
    def switchName  = ""
    def switchType  = ""
    def switchState = ""
    def dimmerValue = ""
    def saturationValue = ""
    def hueValue = ""
    
	for(myData in state.lastSwitchData) {

    	switchName  = myData.switchName
    	switchType  = myData.switchType
    	switchState = myData.switchState

		if(myData.dimmerValue != "null")					//
   			dimmerValue = myData.dimmerValue.toInteger() 	//BF #1
        else												//
			dimmerValue = 0									//

		if(myData.saturationValue != "null")
   			saturationValue = myData.saturationValue.toInteger() 	
        else												
        	saturationValue = 0

		if(myData.hueValue != "null")
    		hueValue = myData.hueValue.toInteger()
        else
			hueValue = 0

        log.info "switchName: $switchName"
        log.info "switchType: $switchType"
        log.info "switchState: $switchState"
        log.info "dimmerValue: $dimmerValue"
        log.info "saturationValue: $saturationValue"
        log.info "hueValue: $hueValue"

		if (switchState == "on")
        	switches[i].on()
            
        if ((switchState == "on") && (switches[i].latestValue("level") != null))
            switches[i].setLevel(dimmerValue)
         
        if ((switchState == "on") && (switches[i].latestValue("saturation") != null))
            switches[i].setSaturation(saturationValue)
         
        if ((switchState == "on") && (switches[i].latestValue("hue") != null))
            switches[i].setHue(hueValue)
         
        if(switchState == "off")
        	switches[i].off()
          
        i++
        log.info "Device setting is Done-------------------"
    }

	if (theSwitch)
    {
    	def seconds = 1
        def now = new Date()
		def runTime = new Date(now.getTime() + (seconds * 1000))
        log.info "Scheduling switch off"
		runOnce(runTime, switchOff)
    }

}

def switchOff()
{
	log.info "Switching off"
	theSwitch.off()
}

private getDeviceSettings() {

    def cnt = 0
    for(myCounter in switches) {
    	switches[cnt].refresh() //this was a try to get dimmer values (bug)
       	// switches[cnt].poll() // Doesn't seem to work for GE Link devices, maybe all Zigbee?
    	cnt++
    }
    
    state.lastSwitchData = [cnt]
    
	def i = 0
    def switchName  = ""
    def switchType  = ""
    def switchState = ""
    def dimmerValue = ""
    def saturationValue = ""
    def hueValue = ""

	for(mySwitch in switches) {
        switchName  = mySwitch.device.toString()
        switchType  = mySwitch.name.toString()
        switchState = mySwitch.latestValue("switch").toString()
		//dimmerValue below returns null if it is not a dimmer
        dimmerValue = mySwitch.latestValue("level").toString()   
		//saturationValue, hueValue below returns null if it is not a hue bulb
        saturationValue = mySwitch.latestValue("saturation").toString()   
        hueValue = mySwitch.latestValue("hue").toString()   
        
        state.lastSwitchData[i] = [switchName: switchName,
        						   switchType: switchType,
                                   switchState: switchState,
                                   dimmerValue: dimmerValue,
                                   saturationValue: saturationValue,
                                   hueValue: hueValue]

        log.debug "SwitchData: ${state.lastSwitchData[i]}"
        i++   
	}  
}

