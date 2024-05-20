/*

Copyright 2024 - VH

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

-------------------------------------------

Change history:

1.0.0 - @tomw - Initial release. Created and developed by tomw. 
2.0.1 - @hhorigian - versão SoundSmart. Changed name to SoundSmart name for compatibility, added some customizationa and fixes for Brazil. 
2.0.2 - added input buttons
2.0.3 - 05/09/2024. Added attribute for input mode
2.0.4 - 05/09/2024. Added table for cover arts


NOTE: this structure was copied from @tomw

*/

metadata
{
    definition(name: "SoundSmart - Player", namespace: "TRATO", author: "VH", importUrl: "")
    {
        capability "AudioVolume"
        capability "Configuration"
        capability "Initialize"
        capability "MusicPlayer"
        capability "Refresh"
        capability "SpeechSynthesis"
        capability "Switch"
        capability "PushableButton"

        
        //TODO: capability "AudioNotification"
        
        command "executeCommand", ["command"]
        command "inputhdmi" 
        command "inputwifi"
        command "inputoptical"
        command "inputbluetooth"
        command "inputaux"
        command "inputusb"

        attribute "commStatus", "string"
        attribute "input", "string"

    }
}

preferences
{
    section
    {
        input "IP_address", "text", title: "IP address of SoundSmart", required: true
        input "api_key_audio", "text",  title: "Audio CD Covers API Key ", required: false
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
    }
}

def logDebug(msg) 
{
    if (logEnable)
    {
        log.debug(msg)
    }
}

def installed()
{
    logDebug("SoundSmart player installed()")
    
    initialize()
}

def initialize()
{
    logDebug("SoundSmart player initialize()")
    
    sendEvent(name: "commStatus", value: "unknown")
    sendEvent(name: "input", value: "--")
    
    refresh()
    
    // set voice prompts behavior based on debugging state
    setVoicePromptsState()
}

def updated()
{
    logDebug("SoundSmart player updated()")
    
    configure()
}

def configure()
{
    logDebug("SoundSmart player configure()")
    
    unschedule()
    state.clear()
    initialize()
}

def refresh()
{
    refresh(false)
}

def refreshTracks()
{
    refreshTracks(false)
}


def refresh(useCachedValues)
{
    logDebug("SoundSmart player refresh()")
    
    if(null == IP_address)
    {
        return
    }
    
    // unschedule refresh, just in case someone did it directly
    unschedule()
    
    updateStatusEx(useCachedValues)
    updatePlayerStatus(useCachedValues)
    
    updateUriAndDesc(true)
    
    // refresh once per minute
    runIn(5, refresh)
}




def uninstalled()
{
    logDebug("SoundSmart player uninstalled()")
    
    unschedule()
}


//Case para los botones de push en el dashboard. 
def push(pushed) {
	logDebug("push: button = ${pushed}")
	if (pushed == null) {
		logWarn("push: pushed is null.  Input ignored")
		return
	}
	pushed = pushed.toInteger()
	switch(pushed) {
		case 1 : inputwifi(); break
		case 2 : inputoptical(); break
		case 3 : inputbluetooth(); break
        case 4 : inputaux(); break
        case 5 : inputusb(); break
        case 6 : inputhdmi(); break        
		default:
			logDebug("push: Botão inválido.")
			break
	}
}



def play()
{
    logDebug("SoundSmart player play()")
    resume()    
}
    
def resume()
{
    logDebug("SoundSmart player resume()")
    
    if(httpGetExec("setPlayerCmd:resume"))
    {
        refresh()
    }
}

def pause()
{
    logDebug("SoundSmart player pause()")
    
    if(httpGetExec("setPlayerCmd:pause"))
    {
        refresh()
    }
}

def stop()
{
    logDebug("SoundSmart player stop()")
    
    if(httpGetExec("setPlayerCmd:stop"))
    {
        refresh()
    }
}

def mute()
{
    logDebug("SoundSmart player mute()")
    
    if(httpGetExec("setPlayerCmd:mute:1"))
    {
        refresh()
    }
}

def unmute()
{
    logDebug("SoundSmart player unmute()")
    
    if(httpGetExec("setPlayerCmd:mute:0"))
    {
        refresh()
    }
}

def setLevel(volumelevel)
{
    logDebug("SoundSmart player setLevel()")
    
    if(!checkCommStatus())
    {
        refresh()
        return
    }
    
    // bound to [0..100]
    def intVolumeLevel = (volumelevel >= 0) ? ((volumelevel <=100) ? volumelevel : 100) : 0
    
    if(httpGetExec("setPlayerCmd:vol:" + intVolumeLevel))
    {
        refresh()
    }
    
    // mute if new volumelevel is 0
    if(volumelevel <= 0)
    {
        mute()
    }
    else
    {
        unmute()
    }
}

def setVolume(volumelevel)
{
    logDebug("SoundSmart player setVolume()")
    
    setLevel(volumelevel)
}

def volumeDown()
{
    logDebug("SoundSmart player volumeDown()")
    
    setLevel(getCurrentVolumeLevel() - 10)
}

def volumeUp()
{
    logDebug("SoundSmart player volumeUp()")
    
    setLevel(getCurrentVolumeLevel() + 10)
}

def nextTrack()
{
    logDebug("SoundSmart player nextTrack()")
    
    if(httpGetExec("setPlayerCmd:next"))
    {
        refresh()
    }
}

def previousTrack()
{
    logDebug("SoundSmart player previousTrack()")
    
    if(httpGetExec("setPlayerCmd:prev"))
    {
        refresh()
    }
}

def playTrack(trackuri)
{
    logDebug("SoundSmart player playTrack(${trackuri})")
    
    if(httpGetExec("setPlayerCmd:play:${trackuri}"))
    {
        refresh()
    }
}

def restoreTrack(trackuri)
{
    logDebug("SoundSmart player restoreTrack(${trackuri})")
    
    playTrack(trackuri)
}

def resumeTrack(trackuri)
{
    logDebug("SoundSmart player resumeTrack(${trackuri})")
    
    playTrack(trackuri)
}

def setTrack(trackuri)
{
    logDebug("SoundSmart player setTrack(${trackuri})")
    
    // this one is weird.  mute, play track, restore pause or stop, and finally restore mute/unmute
    
    refresh()
    
    if(!checkCommStatus())
    {
        refresh()
        return
    }
    
    def muteStatus = getPlayerStatus().mute
    def playStatus = getPlayerStatus().status.toString()
    
    mute()
    playTrack(trackuri)
    
    switch(playStatus)
    {
        case "paused":
            pause()
            break
        case "stopped":
            stop()
            break
    }
    
    if (!muteStatus.toInteger())
    {
        unmute()
    }
    
    refresh()
}

def playText(text)
{
    logDebug("SoundSmart player playText(${text})")
    
    playTrack(textToSpeech(text, "Joanna").uri)
}

def speak(text)
{
    logDebug("SoundSmart player speak(${text})")
    
    playText(text)
}

def executeCommand(suffix)
{
    logDebug("SoundSmart player executeCommand(${suffix})")
    
    return httpGetExec(suffix)
}


def inputwifi()
{
    logDebug("SoundSmart player change to WiFi")    
    executeCommand("setPlayerCmd:switchmode:wifi")
    
}

def inputoptical()
{
    logDebug("SoundSmart player change to Optical")    
    executeCommand("setPlayerCmd:switchmode:optical")
    
}

def inputhdmi()
{
    logDebug("SoundSmart player change to HDMI")    
    executeCommand("setPlayerCmd:switchmode:HDMI")
    
}

def inputbluetooth()
{
    logDebug("SoundSmart player change to Bluetooth")    
    executeCommand("setPlayerCmd:switchmode:bluetooth")
    
}


def inputaux()
{
    logDebug("SoundSmart player change to Aux")    
    executeCommand("setPlayerCmd:switchmode:line-in")
    
}

def inputusb()
{
    logDebug("SoundSmart player change to USB")    
    executeCommand("setPlayerCmd:switchmode:udisk")
    
}

def getBaseURI()
{
    return "http://" + IP_address + "/httpapi.asp?command="
}

def updateStatusEx(useCachedValues)
{
    def resp_json
    
    if(useCachedValues)
    {
        resp_json = getStatusEx()
    }
    else
    {
        resp_json = parseJson(httpGetExec("getStatusEx"))
        if(resp_json)
        {
            setStatusEx(resp_json)
            sendEvent(name: "commStatus", value: "good")
        }
        else
        {
            sendEvent(name: "commStatus", value: "error")
            return
        }
    }
}

def setStatusEx(statusEx)
{
    logDebug("statusEx = ${statusEx}")
    
    state.StatusEx = statusEx
}

def getStatusEx()
{
    return state.StatusEx
}

def updatePlayerStatus(useCachedValues)
{
    def resp_json
    
    if(useCachedValues)
    {
        resp_json = getPlayerStatus()
    }
    else
    {
        resp_json = parseJson(httpGetExec("getPlayerStatus"))
        if(resp_json)
        {
            setPlayerStatus(resp_json)
            sendEvent(name: "commStatus", value: "good")
        }
        else
        {
            sendEvent(name: "commStatus", value: "error")
            return
        }
    }
    
    // update attributes
    sendEvent(name: "level", value: resp_json.vol.toInteger())
    sendEvent(name: "mute", value: (resp_json.mute.toInteger() ? "muted" : "unmuted"))
    
    def tempStatus = ""
    switch(resp_json.status.toString())
    {
        case "stop":
            tempStatus = "stopped"
            break
        case "play":
            tempStatus = "playing"
            break
        case "load":
            tempStatus = "loading"
            break
        case "pause":
            tempStatus = "paused"
            break
    }        
    sendEvent(name: "status", value: tempStatus)
    
    //carga o input que está tocando
    def tempInput = ""
    switch(resp_json.mode.toString())
    {
        case "31":
            tempInput = "Spotify"
            break
        case "40":
            tempInput = "Line In"
            break
        case "41":
            tempInput = "Bluetooth"
            break
        case "43":
            tempInput = "Óptico"
            break
        case "1":
            tempInput = "Airplay"
            break 
        case "0":
            tempInput = "Sem Input"
            break
        case "11":
            tempInput = "USB"
            break  
        case "49":
            tempInput = "HDMI"
            break          
    }        
    sendEvent(name: "input", value: tempInput)
}

def setPlayerStatus(playerStatus)
{
    logDebug("playerStatus = ${playerStatus}")
    
    state.PlayerStatus = playerStatus
}

def getPlayerStatus()
{
    return state.PlayerStatus
}

def getCurrentVolumeLevel()
{
    return getPlayerStatus().vol.toInteger()
}

def setVoicePromptsState()
{
    if(logEnable)
    {
        executeCommand("PromptEnable")
    }
    else
    {
        executeCommand("PromptDisable")
    }
}

def checkCommStatus()
{
    switch(device.currentValue("commStatus"))
    {
        case "good":
            logDebug("checkCommStatus() success")
            return true
        
        case "error":
        case "unknown":
        default:
            logDebug("checkCommStatus() failed")
            return false
    }
}

def updateUriAndDesc(useCachedValues)
{
    if(!useCachedValues)
    {
        refresh()
    }
    
    if(!checkCommStatus())
    {
        return
    }
    
    //def tmpTrackData = "uri:${hexToAscii(getPlayerStatus().Title)}"
    def tmpTrackDesc_back = "${hexToAscii(getPlayerStatus().Title)} | ${hexToAscii(getPlayerStatus().Artist)} from ${hexToAscii(getPlayerStatus().Album)}"
    def tmpTrackDesc = "${hexToAscii(getPlayerStatus().Title)} | ${hexToAscii(getPlayerStatus().Artist)} "  
    
    
    def tmpTitle = hexToAscii(getPlayerStatus().Title)
    def tmpArtist = hexToAscii(getPlayerStatus().Artist)
    
    //se a fonte é spotify e está tocando mando pegar o album  da música.
    if ((getPlayerStatus().mode == "31") && (getPlayerStatus().status == "play"))
    
    {
        tmpTrackData = "SPOTIFY"
        def tempapi_key_audio = "f72ca3d6b5086f9991adbfb3c183912b"
        //  settings.api_key_audio
        def getcoverURI = "http://ws.audioscrobbler.com/2.0/?method=track.getInfo&api_key=" + api_key_audio + "&artist=" + tmpArtist + "&track=" + tmpTitle + "&autocorrect=1&format=json"    
    
        
        def coverfile2
        coverfile2 = httpPOSTExec(getcoverURI)
        def tmpTrackDesc_temp = "<td> ${hexToAscii(getPlayerStatus().Title)}<br>${hexToAscii(getPlayerStatus().Artist)}</td></tr></table>"  
        
        
        def imgfile = "<table style='border-collapse: collapse;margin-left: auto; margin-right: auto;border='0'><tr><td><img src=" + state.AlbumCover + "></td>"
        tmpTrackDesc =  imgfile + tmpTrackDesc_temp
        sendEvent(name: "trackData", value: tmpTrackData)
        sendEvent(name: "trackDescription", value: tmpTrackDesc)
        
    } else
    {
        tmpTrackData = "N/A"
    
    }
    
    
}


def updateIP(ip)
{
    if(null == ip)
    {
        device.clearSetting("IP_address")
        return
    }
    
    device.updateSetting("IP_address", ip.toString())
}


def getGroupingDetails()
{
    updateStatusEx(false)
    def statusEx = getStatusEx()
    
    if(null == statusEx)
    {
        logDebug("failed getGroupingDetails().  check connection and IP")
        return
    }
    
    // grouping details:
    // upnp_uuid: UUID from player
    // ssid: SSID from player
    // eth2: ethernet IP (0.0.0.0 if not connected)
    // apcli0: wireless IP
    // strategy: grouping strategy
    // groupIP: device IP once grouped (TBD)
    // name: player name
    // chan: WifiChannel 
    
    def grpStrat = ("0.0.0.0" == statusEx?.eth2) ? "direct" : "router"
    
    return([upnp_uuid: statusEx.upnp_uuid, ssid: statusEx.ssid, eth2: statusEx.eth2, apcli0: statusEx.apcli0, strategy: grpStrat,  groupIP: "", name: statusEx.DeviceName, chan: statusEx.WifiChannel])
}

def hexToAscii(hexStr)
{
    return new String(hubitat.helper.HexUtils.hexStringToByteArray(hexStr))
}

def parseJson(resp)
{
    def jsonSlurper = new groovy.json.JsonSlurper()
    
    try
    {
        resp_json = jsonSlurper.parseText(resp.toString())        
        return resp_json
    }
    catch (Exception e)
    {
        log.warn "parse failed: ${e.message}"
        return null
    }
}
     

def httpGetExec(suffix)
{
    logDebug("httpGetExec(${suffix})")
    
    try
    {
        getString = getBaseURI() + suffix
        httpGet(getString.replaceAll(' ', '%20'))
        { resp ->
            if (resp.data)
            {
                logDebug("resp.data = ${resp.data}")
                return resp.data
            }
        }
    }
    catch (Exception e)
    {
        logDebug("httpGetExec() failed: ${e.message}")
    }
}


def httpPOSTExec(URI)
{
    
    try
    {
        getString = URI
        segundo = ""
        httpPostJson(getString.replaceAll(' ', '%20'),segundo,  )
        { resp ->
            if (resp.data)
            {
                       
                        def resp_json
                        def coverfile
                        resp_json = resp.data
                        coverfile = resp_json.track.album.image[1]."#text"
                        //log.info "CoverAlbum Filename " + coverfile 
                        state.AlbumCover = coverfile
                                  
            }
        }
    }
                            

    catch (Exception e)
    {
        logDebug("httpPostExec() failed: ${e.message}")
    }
    
}
