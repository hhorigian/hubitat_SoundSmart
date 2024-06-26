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

1.0.0 - @tomw - Initial release
2.0.1 - @hhorigian - versão BR. Versão SoundSmart. 

NOTE: this structure was copied from @tomw


*/

metadata
{
    definition(name: "SoundSmart - Group", namespace: "TRATO", author: "VH", importUrl: "")
    {
        capability "AudioVolume"
        capability "Configuration"
        capability "HoldableButton"
        capability "Initialize"
        capability "MusicPlayer"
        capability "PushableButton"
        capability "Refresh"
        capability "SpeechSynthesis"
        
        command "bindSlave", ["Slave IP"]
        command "bindGroup", ["Slave IPs"]
        command "unbindGroup"        
        command "unbindSlave", ["Slave IP"]

        command "setSlaveVolume", ["Slave IP", "Level"]
        command "setSlaveMute", ["Slave IP"]
        command "setSlaveUnmute", ["Slave IP"]
        
        command "push", ["button"]
        command "hold", ["button"]
        
        attribute "commStatus", "string"
        attribute "currentSlaves", "string"
    }
}

preferences
{
    section
    {
        input name: "masterIP", type: "text", title: "IP address of group master", required: true
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

def initialize()
{
    logDebug("SoundSmart group initialize()")
    
    sendEvent(name: "commStatus", value: "unknown")
    sendEvent(name: "currentSlaves", value: "unknown")
    sendEvent(name:"numberOfButtons", value: 1)
    
    refresh()
}

def updated()
{
    logDebug("SoundSmart group updated()")
    
    configure()
}

def configure()
{
    logDebug("SoundSmart group configure()")

    unschedule()
    
    state.clear()
    clearSlaveMap()
    
    initialize()
    
    if(!existingChildren())
    {
        createChildren()
    }
    
    if(getSlaveMap()?.isEmpty() && !getSlaveList()?.isEmpty())
    {
        catchUpSlaveMap()
    }
}

def refresh()
{
    logDebug("SoundSmart group refresh()")
    
    unschedule()
    
    updateSlaveList()
    if(checkCommStatus())
    {
        processSlaveMapForIP()
    }
    
    refreshFromMasterDevice()
    
    // refresh once per minute
    runIn(60, refresh)
}

def uninstalled()
{
    logDebug("SoundSmart group uninstalled()")
    
    deleteChildren()
}

def createChildren()
{
    logDebug("SoundSmart group createChildren()")
    
    // create master player device
    createChildPlayer(masterIP.toString())
    createChildPlayer("slave")
}

def createChildPlayer(ip)
{
    deleteChildDevice(getPlayerDni(ip))
    
    logDebug("getPlayerDni(ip) = ${getPlayerDni(ip)}")
    
    child = addChildDevice("SoundSmart - Player", getPlayerDni(ip), [label:"${getPlayerName(ip)}", isComponent:true, name:"${getPlayerName(ip)}"])
    if(child)
    {
        if(ip != "slave")
        {
            // this is the master device
            child.updateSetting("IP_address", ip.toString())
            child.updateDataValue("groupRole", "master")
            child.configure()
        }
        else
        {
            // this is a slave device
            child.updateDataValue("groupRole", "slave")
        }
    }
    
    return child
}

def deleteChildren()
{
    for(child in getChildDevices())
    {
        deleteChildDevice(child.deviceNetworkId)
    }
}

def existingChildren()
{
    // look for data tags in existing devices
    // to make decision on whether creation is necessary
    
    def masterFound = false
    def slaveFound = false
    
    for(child in getChildDevices())
    {
        def grRole = child.getDataValue("groupRole")
        if(grRole)
        {
            switch(grRole)
            {
                case "master":
                    if(getPlayerDni(masterIP) == child.getDeviceNetworkId())
                    {
                        // make sure this child is a version of the master we're checking for
                        masterFound = true
                    }
                
                    break
                
                case "slave":
                    slaveFound = true
                    break
            }
        }
    }
    
    return (masterFound && slaveFound)
}

def bindSlave(slaveIP)
{
    bindSlave(reconcileIP(slaveIP, "external"), true)
}

def bindSlave(slaveIP, waitForConnect)
{
    logDebug("SoundSmart group bindSlave(${slaveIP})")
    
    def child = getPlayerDevice("slave")
    if(child)
    {
        child.updateIP(slaveIP)
        child.setVoicePromptsState()
        
        grpDetails = child.getGroupingDetails()
        
        if(null == grpDetails)
        {
            logDebug("failed adding slave ${slaveIP}")
            return
        }
        
        def resp
        
        def masterDetails = getMasterGroupingDetails()
        
        switch(grpDetails.strategy)
        {
            case "router":
                logDebug("eth slave")
                resp = httpGetExec(slaveIP, "ConnectMasterAp:JoinGroupMaster:eth${masterIP}:wifi0.0.0.0")
                break
            
            case "direct":
                logDebug("wls slave")
                resp = httpGetExec(slaveIP, "ConnectMasterAp:ssid=${asciiToHex(masterDetails.ssid)}:ch=${masterDetails.chan}:auth=OPEN:encry=NONE:pwd=:chext=0")
                break
        }
        
        if(null == resp)
        {
            logDebug("failed adding slave ${slaveIP}")
            return
        }
        
        addToSlaveMap(grpDetails)
        if(waitForConnect)
        {
            waitForSlaveByUUID(grpDetails.upnp_uuid.toString(), true)
        }
        
        child.updateIP(null)
    }
    
    refresh()
}

def bindGroup(slaveIPs)
{
    logDebug("SoundSmart group bindGroup(${slaveIPs})")
    
    def slaveIPsList = slaveIPs.replaceAll(' ', '').split(',')
    
    for(slave in slaveIPsList)
    {
        bindSlave(slave, false)
    }
    
    for(slave in slaveIPsList)
    {
        // parallelize wait as much as possible
        waitForSlaveByUUID(lookupUuidFromSlaveIp(slave), true)
    }
    
    refresh()
}

def unbindGroup()
{
    logDebug("SoundSmart group unGroup()")
    
    // get list of current slaves
    updateSlaveList()
    slBefore = getSlaveList()
    
    if(httpGetExec(masterIP, "multiroom:Ungroup"))
    {
        for(slave in slBefore)
        {
            waitForSlaveByUUID(slave.uuid, false)
        }
    }
    
    refresh()
}

def unbindSlave(slaveIP)
{
    unbindSlave(reconcileIP(slaveIP, "group"), false)
}

def unbindSlave(slaveIP, n_waitForDisconnect)
{
    logDebug("SoundSmart group unbindSlave(${slaveIP})")
    
    groupIP = lookupGroupIpFromSlaveIp(slaveIP)
    uuId = lookupUuidFromSlaveIp(slaveIP)
    
    if(groupIP)
    {
        if(httpGetExec(masterIP, "multiroom:SlaveKickout:${groupIP}"))
        {
            if(!n_waitForDisconnect && (null != uuId))
            {
                waitForSlaveByUUID(uuId, n_waitForDisconnect)
            }
            
            refresh()
        }
    }
}

def push(button)
{
    sendEvent(name:"pushed", value:button, isStateChange:true)
    
    if(!searchSlaveListForIP(reconcileIP(button, "group")))
    {
        // slave not currently in list, so bind it
        if(reconcileIP(button, "external"))
        {
            bindSlave(button)
        }
    }
    else
    {
        // slave is in list, so unbind it
        if(reconcileIP(button, "group"))
        {
            unbindSlave(button)
        }
    }
        
    
    return
}

def hold(button)
{
    sendEvent(name:"held", value:button, isStateChange:true)
    
    if(reconcileIP(button, "group"))
    {
        unbindSlave(button)
    }
    
    return
}

def reconcileIP(term, ipType)
{
    if(term.isInteger())
    {
        logDebug("number")
        
        // use for app events or TBD
        return
    }
    
    if(term.split(/\./).size() == 4)
    {
        logDebug("IP Address")
        
        switch(ipType)
        {
            case "group":
                return lookupGroupIpFromSlaveIp(term)
            default:
                // assume this is just a passthrough
                return term
        }
    }
    
    logDebug("other string")
    
    // handle as device name
    switch(ipType)
    {
        case "group":
            return lookupIpFromSlaveName(term, "group")
        default:
            if("0.0.0.0" != lookupIpFromSlaveName(term, "eth"))
            {
                // try to find ethernet first
                return lookupIpFromSlaveName(term, "eth")
            }
            else if(lookupIpFromSlaveName(term, "apcli"))
            {
                // else, try to find wireless
                return lookupIpFromSlaveName(term, "apcli")
            }
    }
    
    // didn't get a match
    return
}

def play()
{
    child = getPlayerDevice(masterIP)
    if(child)
    {
        child.play()
        refresh()
    }   
}

def pause()
{
    child = getPlayerDevice(masterIP)
    if(child)
    {
        child.pause()
        refresh()
    }
}

def stop()
{
    child = getPlayerDevice(masterIP)
    if(child)
    {
        child.stop()
        refresh()
    }
}

def mute()
{
    child = getPlayerDevice(masterIP)
    if(child)
    {
        child.mute()
        refresh()
    }
}

def unmute()
{
    child = getPlayerDevice(masterIP)
    if(child)
    {
        child.unmute()
        refresh()
    }
}

def setLevel(volumelevel)
{
    child = getPlayerDevice(masterIP)
    if(child)
    {
        child.setLevel(volumelevel)
        refresh()
    }
}

def setVolume(volumelevel)
{
    child = getPlayerDevice(masterIP)
    if(child)
    {
        child.setVolume(volumelevel)
        refresh()
    }
}

def volumeDown()
{
    child = getPlayerDevice(masterIP)
    if(child)
    {
        child.volumeDown()
        refresh()
    }
}

def volumeUp()
{
    child = getPlayerDevice(masterIP)
    if(child)
    {
        child.volumeUp()
        refresh()
    }
}

def nextTrack()
{
    child = getPlayerDevice(masterIP)
    if(child)
    {
        child.nextTrack()
        refresh()
    }
}

def previousTrack()
{
    child = getPlayerDevice(masterIP)
    if(child)
    {
        child.previousTrack()
        refresh()
    }
}

def playTrack(trackuri)
{
    child = getPlayerDevice(masterIP)
    if(child)
    {
        child.playTrack(trackuri)
        refresh()
    }
}

def restoreTrack(trackuri)
{
    child = getPlayerDevice(masterIP)
    if(child)
    {
        child.restoreTrack(trackuri)
        refresh()
    }
}

def resumeTrack(trackuri)
{
    child = getPlayerDevice(masterIP)
    if(child)
    {
        child.resumeTrack(trackuri)
        refresh()
    }
}

def setTrack(trackuri)
{
    child = getPlayerDevice(masterIP)
    if(child)
    {
        child.setTrack(trackuri)
        refresh()
    }
}

def playText(text)
{
    child = getPlayerDevice(masterIP)
    if(child)
    {
        child.playText(text)
        refresh()
    }
}

def speak(text)
{
    playText(text)
}

def setSlaveVolume(slaveIP, level)
{
    logDebug("SoundSmart group setSlaveVolume(${level})")
    
    groupIP = reconcileIP(slaveIP, "group")
    
    if(groupIP)
    {
        httpGetExec(masterIP, "multiroom:SlaveVolume:${groupIP}:${level}")
        refresh()
    }
}

def setSlaveMute(slaveIP)
{
    logDebug("SoundSmart group setSlaveMute()")
    
    groupIP = reconcileIP(slaveIP, "group")
    
    if(groupIP)
    {
        httpGetExec(masterIP, "multiroom:SlaveMute:${groupIP}:1")
        refresh()
    }
}

def setSlaveUnmute(slaveIP)
{
    logDebug("SoundSmart group setSlaveMute()")
    
    groupIP = reconcileIP(slaveIP, "group")
    
    if(groupIP)
    {
        httpGetExec(masterIP, "multiroom:SlaveMute:${groupIP}:0")
        refresh()
    }
}

def getBaseURI(devIP)
{
    return "http://" + devIP + "/httpapi.asp?command="
}

def updateSlaveList()
{
    def resp_json
    
    resp_json = parseJson(httpGetExec(masterIP, "multiroom:getSlaveList"))
    if(resp_json)
    {
        setSlaveList(resp_json.slave_list)
        
        def slAttr = ""
        
        for(slave in resp_json.slave_list)
        {
            slAttr += "${slave.name}<br>"
        }
            
        sendEvent(name: "currentSlaves", value: (slAttr.toString() ? slAttr.toString() : "(none)"))
        sendEvent(name: "commStatus", value: "good")
    }
    else
    {
        logDebug("SoundSmart group refresh() failed")
        sendEvent(name: "commStatus", value: "error")
    }
}    

def setSlaveList(slaveList)
{
    state.slave_list = slaveList
}

def getSlaveList()
{
    return state.slave_list
}

def searchSlaveListForUUID(slaveUUID)
{
    // locate a slave by UUID
    logDebug("searchSlaveListForUUID(${slaveUUID})")
    
    sl = getSlaveList()
    
    for(slave in sl)
    {
        if(slave.uuid == slaveUUID)
        {
            // found it
            return true
        }
    }
    
    return false
}

def searchSlaveListForIP(slaveIP)
{
    // locate a slave by UUID
    logDebug("searchSlaveListForIP(${slaveIP})")
    
    sl = getSlaveList()
    
    for(slave in sl)
    {
        if(slave.ip == slaveIP)
        {
            // found it
            return true
        }
    }
    
    return false
}

def waitForSlaveByUUID(slaveUUID, present_npresent)
{
    if(null == slaveUUID)
    {
        logDebug("empty UUID")
    }
    
    for(i = 0; ; i++)
    {
        updateSlaveList()
        
        if(present_npresent == searchSlaveListForUUID(slaveUUID))
        {
            break
        }
        
        if(i >= 15)
        {
            // 15s timeout
            logDebug("timed out waiting for slave")
            break
        }
        
        pauseExecution(1000)
    }
}

def addToSlaveMap(groupingDetails)
{
    if(null == getSlaveMap())
    {
        clearSlaveMap()
    }
    
    state.slaveMap[groupingDetails.upnp_uuid.toString()] = groupingDetails
}

def clearSlaveMap()
{
    state.slaveMap = [:]
}

def getSlaveMap()
{
    return state.slaveMap
}

def setSlaveMap(sm)
{
    state.slaveMap = sm
}

def lookupGroupIpFromSlaveIp(slaveIP)
{
    // locate user-visible IP in any relevant fields and return internal group IP
    
    logDebug("lookupGroupIpFromSlaveIp(${slaveIP})")
             
    def sm = getSlaveMap()
    def smKeys = sm.keySet()
    
    for(slave in smKeys)
    {
        smSub = sm[slave]
        
        if((slaveIP == smSub.eth2) || (slaveIP == smSub.apcli0) || (slaveIP == smSub.groupIP))
        {
            logDebug("matched slaveIP = ${slaveIP}")
            
            // if any of those match, this is likely us
            return smSub.groupIP
        }
    }
    
    // if we didn't find it, give back the input as a last-ditch
    return slaveIP
}

def lookupIpFromSlaveName(name, ipType)
{
    // locate slave by name and return requested IP
    // supported ip types: group, eth, apcli
    
    logDebug("lookupIpFromSlaveName(${name}, ${ipType})")
             
    def sm = getSlaveMap()
    def smKeys = sm.keySet()
    
    for(slave in smKeys)
    {
        smSub = sm[slave]
        
        if(name == smSub.name)
        {
            logDebug("matched name = ${name}")
            
            switch(ipType)
            {
                case "eth":
                    return smSub.eth2
                
                case "apcli":
                    return smSub.apcli0
                
                case "group":
                default:
                    return smSub.groupIP
            }
        }
    }
    
    // we didn't find it
    return
}

def lookupUuidFromSlaveIp(slaveIP)
{
    // locate user-visible IP in any relevant fields and return UUID
    
    logDebug("lookupUuidFromSlaveIp(${slaveIP})")
             
    def sm = getSlaveMap()
    def smKeys = sm.keySet()
    
    for(slave in smKeys)
    {
        smSub = sm[slave]
        
        if((slaveIP == smSub.eth2) || (slaveIP == smSub.apcli0) || (slaveIP == smSub.groupIP))
        {
            logDebug("matched slaveIP = ${slaveIP}")
            
            // if any of those match, this is likely us
            return smSub.upnp_uuid
        }
    }
    
    // we didn't find it
    return
}

def processSlaveMapForIP()
{
    // update internal group IP
    if(null == getSlaveList())
    {
        return
    }
    
    // match internal IP with pre-group IP
    
    // what the master sees now
    def sl = getSlaveList()
    
    // what we saw when grouping or catching up
    def sm = getSlaveMap()
    
    for(slave in sl)
    {
        if(sm?.containsKey(slave.uuid.toString()))
        {
            // found it, so update internal IP
            sm[slave.uuid.toString()].groupIP = slave.ip.toString()
        }
    }
    
    setSlaveMap(sm)
}

def catchUpSlaveMap()
{
    // if we inherited a group (no slave map from time of grouping)    
    sl = getSlaveList()
    
    // add the info we have
    for(slave in sl)
    {
        gdTemp = [:]
        
        gdTemp['upnp_uuid'] = slave.uuid
        gdTemp['groupIP'] = slave.ip
        gdTemp['name'] = slave.name
        
        addToSlaveMap(gdTemp)
    }
}

def getPlayerDni(ip)
{
    return device.getName() + "-" + ip
}

def getPlayerName(ip)
{
    return getPlayerDni(ip)
}

def getPlayerDevice(ip)
{
    return getChildDevice(getPlayerDni(ip))
}
    
def getMasterGroupingDetails()
{
    child = getPlayerDevice(masterIP)
    if(child)
    {
        return child.getGroupingDetails()
    }
}

def refreshFromMasterDevice()
{
    child = getPlayerDevice(masterIP)
    if(null == child)
    {
        return
    }
    
    // process attributes from master into group
    
    child.refresh()
    
    tmpVol = child.currentValue("volume")
    if(tmpVol)
    {
        sendEvent(name: "volume", value: tmpVol.toInteger())
    }
    
    tmpLevel = child.currentValue("level")
    if(tmpLevel)
    {
        sendEvent(name: "level", value: tmpLevel.toInteger())
    }
    
    tmpMute = child.currentValue("mute")
    if(tmpMute)
    {
        sendEvent(name: "mute", value: tmpMute)
    }
    
    tmpStatus = child.currentValue("status")
    if(tmpStatus)
    {
        sendEvent(name: "status", value: tmpStatus)
    }
    
    tmpTrData = child.currentValue("trackData")    
    if(tmpTrData)
    {
        sendEvent(name: "trackData", value: tmpTrData)
    }
    
    tmpTrDesc = child.currentValue("trackDescription")    
    if(tmpTrDesc)
    {
        sendEvent(name: "trackDescription", value: tmpTrDesc)
    }
}

def setupGroupFromApp(masterIP)
{
    logDebug("setupGroupFromApp() masterIP = ${masterIP}")
    
    device.updateSetting("masterIP", masterIP.toString())
}
    

def bindGroupFromApp(slaveIPs)
{
    logDebug("bindGroupFromApp() slaveIPs = ${slaveIPs}")
    
    bindGroup(slaveIPs)
}

def unbindGroupFromApp()
{
    logDebug("unbindGroupFromApp()")
    
    unbindGroup()
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

def asciiToHex(asciiValue)
{
    return new String(hubitat.helper.HexUtils.byteArrayToHexString(asciiValue.getBytes()))
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

def httpGetExec(devIP, suffix)
{
    logDebug("${device.getLabel()} httpGetExec(${suffix})")
    
    try
    {
        getString = getBaseURI(devIP) + suffix
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
