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
2.0.5 - 05/30/2024. Added 10 Buttons for presets (buttons 20 to 29)
                    Added Prompt Disable/Enable (buttons 35 and button 36)
                    Added LastFM api default in Driver. 
2.0.6 - 06/06/2024  Added link to help in github
2.0.7 - 10/06/2024  Added input status 10 = Radio Online
2.0.8 -             Added Status Multiroom
2.0.9 - 15/06/2024  Added numberOfButtons + trackdescription attributes for Easy Dashboards compatibility.  
2.1.0 - 21/06/2024  Fixed Cover Disk Images. Changed lastfm API, for albumgetinfo function usage. 
2.1.1 - 29/07/2024  Fixed "" in case buttons.  
2.1.2 - 29/07/2024  Fixed case 3 "" in case buttons.  
2.1.3 - 16/08/2024  Added Shuffle Modes as buttons. 
2.1.4 - 29/08/2024  New feature: Possible to send names in buttons instead of numbers in dashboard; ex: Button number: preset1. Will execute preset 1. Read.me for more instructions. 
2.1.5 - 07/09/2024  Added Attributes LargeImage, URLImage
2.1.6 - 05/11/2025  Added  Buttons as Child Devices using Recreate Buttons. Buttons shown as Switch, easier for dashboard use. 
2.1.7 - 20/02/2026  Fixed Radio Online Cover and Name. 



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
        command "push_2"
        command "recreateChilds"
        command "removeChilds"
        
        attribute "commStatus", "string"
        attribute "trackDescription", "string"
        attribute "input", "string"
        attribute "volume", "integer"
        attribute "numberOfButtons", "integer"
		attribute "URLLargeCoverFile", "string"
		attribute "ImageLargeCover", "string"		
		attribute "ImageLargeCover", "string"		
        attribute "trackname", "string"


    }
}

    import groovy.transform.Field
    @Field static final String DRIVER = "by TRATO"
    @Field static final String USER_GUIDE = "https://github.com/hhorigian/hubitat_SoundSmart/blob/main/Player"


    String fmtHelpInfo(String str) {
    String prefLink = "<a href='${USER_GUIDE}' target='_blank'>${str}<br><div style='font-size: 70%;'>${DRIVER}</div></a>"
    return "<div style='font-size: 160%; font-style: bold; padding: 2px 0px; text-align: center;'>${prefLink}</div>"
    }



preferences
{
    section
    {
        input "IP_address", "text", title: "IP address of SoundSmart", required: true
        input "api_key_audio", "text",  title: "Audio CD Covers API Key ", required: false, defaultValue: "f72ca3d6b5086f9991adbfb3c183912b"
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
        //help guide
        input name: "UserGuide", type: "hidden", title: fmtHelpInfo("Manual do Driver") 
        input name: "createButtonsOnSave", type: "bool", title: "Criar/atualizar Child Switches para botões ao salvar", defaultValue: false
        

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
    sendEvent(name: "volume", value: 0)
    sendEvent(name: "trackDescription", value: "--")   
    sendEvent(name:"numberOfButtons", value:30)         
    
    refresh()
    
    // set voice prompts behavior based on debugging state
    setVoicePromptsState()
}

def updated()
{
    logDebug("SoundSmart player updated()")
    sendEvent(name: "volume", value: 0)
    sendEvent(name: "trackDescription", value: "--")
    sendEvent(name:"numberOfButtons", value:30)     
    configure()
    if (createButtonsOnSave) {
        createOrUpdateChildButtons(true)
    }
    
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
	log.debug("push: button = ${pushed}")
	if (pushed == null) {
		log.warn ("push: pushed is null.  Input ignored")
		return
	}
	//pushed = pushed.toInteger()
	switch(pushed) {
		case "1" : inputwifi(); break
		case "2" : inputoptical(); break
		case "3" : inputbluetooth(); break
        case "4" : inputaux(); break
        case "5" : inputusb(); break
        case "20" : preset1(); break
        case "21" : preset2(); break
        case "22" : preset3(); break
        case "23" : preset4(); break
        case "24" : preset5(); break
        case "25" : preset6(); break
        case "26" : preset7(); break
        case "27" : preset8(); break
        case "28" : preset9(); break
        case "29" : preset10(); break  
        case "35" : promptDisable(); break 
        case "36" : promptEnable(); break   
        case "37" : loopMode(0); break   
        case "38" : loopMode(1); break   
        case "39" : loopMode(2); break   
        case "40" : loopMode(3); break   		

		default:
			"${pushed}"()
            //logDebug("push: Botão inválido.")
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
    
    setLevel(getCurrentVolumeLevel() - 5)
}

def volumeUp()
{
    logDebug("SoundSmart player volumeUp()")
    
    setLevel(getCurrentVolumeLevel() + 5)
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


def repeatall()
{
    logDebug("SoundSmart player Repeat All")
    loopMode(0)
}

def repeatsingle()
{
    logDebug("SoundSmart player Repeat single track ")
    loopMode(1)
}

def shufflerepeat()
{
    logDebug("SoundSmart player Shuffle Repeat ")
    loopMode(2)
}

def shufflenorepeat()
{
    logDebug("SoundSmart player Shuffle No Repeat")
    loopMode(3)
}



def loopMode(loopmodevalue)
{
    logDebug("SoundSmart player change Loop (${loopmodevaluec})")
    
    if(httpGetExec("setPlayerCmd:loopmode:${loopmodevalue}"))
    {
        refresh()
    }
}

def promptDisable(){
     logDebug("SoundSmart player Prompt Disabled") 
     executeCommand("PromptDisable")     
}


def promptEnable (){
     logDebug("SoundSmart player Prompt Enabled") 
     executeCommand("PromptEnable ")     
}


def preset1(){
     logDebug("SoundSmart player preset 1 selected") 
     executeCommand("MCUKeyShortClick:1")     
}

def preset2(){
     logDebug("SoundSmart player preset 2 selected") 
     executeCommand("MCUKeyShortClick:2")     
}

def preset3(){
     logDebug("SoundSmart player preset 3 selected") 
     executeCommand("MCUKeyShortClick:3")     
}

def preset4(){
     logDebug("SoundSmart player preset 4 selected") 
     executeCommand("MCUKeyShortClick:4")     
}

def preset5(){
     logDebug("SoundSmart player preset 5 selected") 
     executeCommand("MCUKeyShortClick:5")     
}

def preset6(){
     logDebug("SoundSmart player preset 6 selected") 
     executeCommand("MCUKeyShortClick:6")     
}

def preset7(){
     logDebug("SoundSmart player preset 7 selected") 
     executeCommand("MCUKeyShortClick:7")     
}


def preset8(){
     logDebug("SoundSmart player preset 8 selected") 
     executeCommand("MCUKeyShortClick:8")     
}

def preset9(){
     logDebug("SoundSmart player preset 9 selected") 
     executeCommand("MCUKeyShortClick:19")     
}

def preset10(){
     logDebug("SoundSmart player preset 10 selected") 
     executeCommand("MCUKeyShortClick:10")     
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
    //logDebug("statusEx = ${statusEx}")
    
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
    sendEvent(name: "volume", value: resp_json.vol.toInteger())
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
        case "10":
            tempInput = "Radio Online"
            break
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
    //logDebug("playerStatus = ${playerStatus}")
    
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
    def tmpAlbum = hexToAscii(getPlayerStatus().Album)
    //log.info "tmpTitle = " + tmpTitle + " tmpArtist = " +  tmpArtist   + " tmpAlbum = " + tmpAlbum
    //log.info "tmpTrackDesc_back = " + tmpTrackDesc_back + " tmpTrackDesc = " +  tmpTrackDesc
    //log.info "getPlayerStatus().mode = " + getPlayerStatus().mode 
    
    
    //se a fonte é spotify e está tocando mando pegar o album  da música.
    if ((getPlayerStatus().mode == "31") && (getPlayerStatus().status == "play"))
    
    {
        tmpTrackData = "SPOTIFY"
        def tempapi_key_audio = "f72ca3d6b5086f9991adbfb3c183912b"
        //  settings.api_key_audio
        //def getcoverURI = "http://ws.audioscrobbler.com/2.0/?method=track.getInfo&api_key=" + api_key_audio + "&artist=" + tmpArtist + "&track=" + tmpTitle + "&autocorrect=1&format=json"    
        def getcoverURI = "http://ws.audioscrobbler.com/2.0/?method=album.getInfo&api_key=" + api_key_audio + "&artist=" + tmpArtist + "&album=" + tmpAlbum + "&autocorrect=1&format=json"    

        //log.debug "cover " + getcoverURI
        def coverfile2
        coverfile2 = httpPOSTExec(getcoverURI)
        coverfileLarge = httpPOSTExecLarge(getcoverURI)
		
        def tmpTrackDesc_temp = "<td> ${hexToAscii(getPlayerStatus().Title)}<br>${hexToAscii(getPlayerStatus().Artist)}</td></tr></table>"  
        def tmpTrackName = "<td> ${hexToAscii(getPlayerStatus().Title)}-${hexToAscii(getPlayerStatus().Artist)}</td></tr></table>"  
        
        
        def imgfile = "<table style='border-collapse: collapse;margin-left: auto; margin-right: auto;border='0'><tr><td><img src=" + state.SmallAlbumCover + "></td>"
        def imgfileLarge = "<img src=" + state.LargeAlbumCover + " style=width:365px;>"

		//def imgfile = "<img src=" + state.AlbumCover + ">"
        tmpTrackDesc =  imgfile + tmpTrackDesc_temp
		tmpURLLargeCover = state.LargeAlbumCover
		tmpLargeCoverImg = imgfileLarge
        sendEvent(name: "trackData", value: tmpTrackData)
        sendEvent(name: "trackDescription", value: tmpTrackDesc)
        sendEvent(name: "trackname", value: tmpTrackName)        
		sendEvent(name: "URLLargeCoverFile", value: tmpURLLargeCover)
		sendEvent(name: "ImageLargeCover", value: tmpLargeCoverImg)
        
    } else if ((getPlayerStatus().mode == "10") && (getPlayerStatus().status == "play")){   //RADIO ONLINE
        
        def tmpTrackName = "<td> ${hexToAscii(getPlayerStatus().Title)}</td></tr></table>"  
        sendEvent(name: "trackname", value: tmpTrackName)
        
    }else 
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
    //logDebug("httpGetExec(${suffix})")
    
    try
    {
        getString = getBaseURI() + suffix
        httpGet(getString.replaceAll(' ', '%20'))
        { resp ->
            if (resp.data)
            {
                //logDebug("resp.data = ${resp.data}")
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
                        coverfile = resp_json.album.image[1]."#text"
                        //coverfile = resp_json.track.album.image[1]."#text"
                        //log.info "CoverAlbum Filename " + coverfile 
                        state.SmallAlbumCover = coverfile
                                  
            }
        }
    }
                            

    catch (Exception e)
    {
        logDebug("httpPostExec() failed: ${e.message}")
    }
    
}

def httpPOSTExecLarge(URI)
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
                        coverfile = resp_json.album.image[4]."#text"
                        //coverfile = resp_json.track.album.image[1]."#text"
                        //log.info "CoverAlbum Filename " + coverfile 
                        state.LargeAlbumCover = coverfile
                                  
            }
        }
    }
                            

    catch (Exception e)
    {
        logDebug("httpPostExec() failed: ${e.message}")
    }
    
}

// --- CHILD SWITCHES: DEFINIÇÕES ---
@Field static final List<Map> SS_CHILD_BUTTON_DEFS = [
  // Inputs
  [label:"SS - Input WiFi",         handler:"inputwifi"],
  [label:"SS - Input Optical",      handler:"inputoptical"],
  [label:"SS - Input Bluetooth",    handler:"inputbluetooth"],
  [label:"SS - Input Aux",          handler:"inputaux"],
  [label:"SS - Input USB",          handler:"inputusb"],
  [label:"SS - Input HDMI",         handler:"inputhdmi"],

  // Volume e transporte
  [label:"SS - Volume Up",          handler:"volumeUp"],
  [label:"SS - Volume Down",        handler:"volumeDown"],
  [label:"SS - Play/Resume",        handler:"play"],
  [label:"SS - Pause",              handler:"pause"],
  [label:"SS - Stop",               handler:"stop"],
  [label:"SS - Next Track",         handler:"nextTrack"],
  [label:"SS - Previous Track",     handler:"previousTrack"],
  [label:"SS - Mute",               handler:"mute"],
  [label:"SS - Unmute",             handler:"unmute"],

  // Presets 1..10
  [label:"SS - Preset 1",           handler:"preset1"],
  [label:"SS - Preset 2",           handler:"preset2"],
  [label:"SS - Preset 3",           handler:"preset3"],
  [label:"SS - Preset 4",           handler:"preset4"],
  [label:"SS - Preset 5",           handler:"preset5"],
  [label:"SS - Preset 6",           handler:"preset6"],
  [label:"SS - Preset 7",           handler:"preset7"],
  [label:"SS - Preset 8",           handler:"preset8"],
  [label:"SS - Preset 9",           handler:"preset9"],
  [label:"SS - Preset 10",          handler:"preset10"],

  // Prompts
  [label:"SS - Prompt Disable",     handler:"promptDisable"],
  [label:"SS - Prompt Enable",      handler:"promptEnable"],

  // Loop/Shuffle
  [label:"SS - Repeat All",         handler:"repeatall"],
  [label:"SS - Repeat Single",      handler:"repeatsingle"],
  [label:"SS - Shuffle Repeat",     handler:"shufflerepeat"],
  [label:"SS - Shuffle No Repeat",  handler:"shufflenorepeat"]
]

// Botões no UI
def recreateChilds() { createOrUpdateChildButtons(true) }
def removeChilds()  { removeChildButtons() }

private void createOrUpdateChildButtons(Boolean removeExtras=false) {
  log.warn "Criando/atualizando Child Switches para botões do SoundSmart..."

  // >>> Sem filtro por métodos; cria todos os definidos
  List<Map> defs = SS_CHILD_BUTTON_DEFS
  log.warn "Childs planejados: ${defs.size()}"

  Set<String> keep = [] as Set
  defs.eachWithIndex { m, idx ->
    String label = m.label as String
    String handler = m.handler as String
    String dni = "${device.id}-SSBTN-${idx+1}"
    def child = getChildDevice(dni)

    if (!child) {
      try {
        log.warn ">> Criando child [${idx+1}] '${label}' (handler=${handler}) DNI=${dni}"
        child = addChildDevice(
          "hubitat",
          "Generic Component Switch",
          dni,
          [name: label, label: label, componentName: label, componentLabel: label, isComponent: true]
        )
        log.warn "   ✓ Child criado: ${child?.displayName ?: 'desconhecido'}"
      } catch (e) {
        log.error "   ✗ Falha ao criar child '${label}' (DNI=${dni}): ${e}", e
      }
    } else {
      try { if (child.label != label) { child.setLabel(label); log.warn "   ↺ Label atualizado para '${label}'" } } catch (ignored) { }
    }

    if (child) {
      try {
        child.updateDataValue("handler", handler)
        child.parse([[name:"switch", value:"off"]]) // estado inicial
      } catch (ignored) { }
      keep << dni
    }
  }

  if (removeExtras) {
    def extras = childDevices?.findAll { !(it.deviceNetworkId in keep) } ?: []
    extras.each {
      try { log.warn "Removendo child extra: ${it.displayName} (${it.deviceNetworkId})"; deleteChildDevice(it.deviceNetworkId) }
      catch (e) { log.error "Falha ao remover child '${it.displayName}': ${e}", e }
    }
  }

  log.warn "Concluído. Childs ativos: ${childDevices?.size() ?: 0}"
}

// Callbacks do 'Generic Component Switch'
def componentOn(cd)  { handleChildPress(cd) }
def componentOff(cd) { /* momentary: ignoramos o off manual; faremos auto-off */ }
// Chamado pelo "Generic Component Switch" quando o usuário dá Refresh no child
def componentRefresh(cd) {
  // Como nossos childs são momentary, garantimos que o estado visual fique "off"
  try {
    cd.parse([[name:"switch", value:"off"]])
  } catch (ignored) { }
  if (logEnable) log.debug "componentRefresh() aplicado em '${cd?.displayName}'"
}

private void handleChildPress(cd) {
  String handler = cd?.getDataValue("handler") ?: ""
  log.debug "Press (ON) em '${cd?.displayName}' -> handler='${handler}'"
  if (!handler) { log.warn "Child ${cd?.displayName} sem handler. Abortando."; return }

  // Se o método não existir, ainda assim o child funciona como momentary e fazemos log:
  def hasMethod = false
  try { hasMethod = (this.metaClass?.getMetaMethod(handler) != null) || !(this.respondsTo(handler).isEmpty()) } catch (ignored) { }
  if (!hasMethod) {
    log.warn "Handler '${handler}' não encontrado neste driver. Verifique o nome do método."
  } else {
    try { this."${handler}"() }
    catch (e) { log.error "Erro executando handler '${handler}': ${e}", e }
  }

  // auto-off em 1s
  runIn(1, "childOffSafe", [data:[dni: cd?.deviceNetworkId], overwrite: true])
}

def childOffSafe(data) {
  def child = data?.dni ? getChildDevice(data.dni as String) : null
  if (child) {
    try { child.parse([[name:"switch", value:"off"]]); log.debug "Auto-off enviado para '${child.displayName}'" } catch (ignored) { }
  }
}

private void removeChildButtons() {
  def toRemove = childDevices ?: []
  log.warn "Removendo ${toRemove.size()} child(s) do SoundSmart..."
  int removed = 0
  toRemove.each { cd ->
    try { deleteChildDevice(cd.deviceNetworkId); removed++ } catch (e) { log.error "Falha ao remover '${cd.displayName}': ${e}", e }
  }
  log.warn "Remoção concluída. Total removido: ${removed}"
}
