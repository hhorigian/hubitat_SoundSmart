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

1.0.0 - @tomw - Initial release.
2.0.0 - @hhorigian - Versão para BR. SoundSmart. 
2.1   - @hhorigian - fixed device driver name.

NOTE: this structure was copied from @tomw

*/

import groovy.transform.Field

definition(
    name: "SoundSmart - Manager Instance",
    parent: "TRATO:SoundSmart - Manager",
    namespace: "TRATO",
    author: "VH",
    description: "",
    category: "Convenience",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "")

preferences
{
    page(name: "mainPage1")
    page(name: "mainPage2")
    page(name: "mainPage2b")
}

def mainPage1()
{
    dynamicPage(name: "mainPage1", title: "", install: false, uninstall: true)
    {
        if(null == atomicState.devices)
        {
            atomicState.devices = [:]
        }
        
        section
        {
            href(page: "mainPage2", title: "<b>Comece o processo de descoberta</b>", description: "Descubra dispositivos SoundSmart em sua rede local.  Certifique-se de que todos os dispositivos estejam desagrupados.", params: [runDiscovery : true])
            input name: "discoveryTimeout", type: "number", title: "Tempo limite de descoberta (default 3 seg)", defaultValue: 3, range: "3-20", required: true, width: 2
        }
        section
        {
            href(page: "mainPage2b", title: "<b>Usar descoberta anterior</b>", description: "Reutilize dados de descoberta anteriores para editar o grupo.", params: [runDiscovery : false] )
        }
    }
}

def mainPage2(params)
{
    dynamicPage(name: "mainPage2", title: "", install: true, uninstall: true)
    {
        if(params.runDiscovery)
        {
            // clear devices list
            atomicState.devices = [:]
            // subscribe to receive upnp events
            subscribe(location, null, locationHandler, [filterEvents:false])
            // upnp request
            sendHubCommand(new hubitat.device.HubAction("lan discovery urn:schemas-upnp-org:device:MediaRenderer:1", hubitat.device.Protocol.LAN))        
            // limit wait to 3sec minimum
            pauseExecution(1000 * ((discoveryTimeout < 3) ? 3 : discoveryTimeout.toInteger()))
            // stop listening        
            unsubscribe(location)
        }
        
        section
        {
            paragraph "<b>Discovered devices:</b> ${atomicState.devices}"
        }
        
        section
        {
            href(page: "mainPage1", title: "<b>Retry discovery process</b>", description: "")
        }
        
        section
        {
            input name: "instanceName", type: "string", title: "Nome para o Grupo", required: true
            input name: "retainExistingDevices", type: "bool", title: "Manter os dispositivos existentes? (recomendado)", defaultValue: true, required: true
            input name: "masterIP", type: "enum", title: "Selecione o Master no grupo", options: atomicState.devices, required: true
            input name: "slaveIPs", type: "enum", title: "Selecione os slaves", options: atomicState.devices, required: false, multiple: true
            input name: "enableLogging", type: "bool", title: "Enable debug logging?", defaultValue: false, required: true
        }
    }
}

def mainPage2b(params)
{
    // necessary because two links to the same page with different params doesn't work
    mainPage2(params)
}

@Field String discoverySync = ""

def logDebug(msg)
{
    if(enableLogging)
    {
        log.debug "${msg}"
    }
}

def installed()
{
    app.updateLabel("SoundSmart - Manager - ${instanceName}")
    
    
    def child = getChildDevice(getGroupName())
    
    if(!(retainExistingDevices && child))
    {
        // create group device
        child = addChildDevice("TRATO", "SoundSmart - Group", getGroupName(), [isComponent: true, name: getGroupName(), label: getGroupName()])
        log.debug "child = " + child
    }
    
    if(child)
    {
        // set up group device
        child.setupGroupFromApp(getMasterIP())
        child.configure()
        // subscribe to button on group device
        subscribe(child, "pushed", pushedHandler)
        subscribe(child, "held", heldHandler)
    }
}

def uninstalled()
{
    // unbind this group before removing devices
    getChildDevice(getGroupName()).unbindGroupFromApp()
    
    deleteChildren()
}

def updated()
{
    unsubscribe()
    
    if(!retainExistingDevices)
    {
        deleteChildren()
    }
    
    installed()
}

def deleteChildren()
{
    for(child in getChildDevices())
    {
        deleteChildDevice(child.getDeviceNetworkId())
    }
}
    

def pushedHandler(evt)
{
    /*    
    logDebug("evt = ${evt}")
    logDebug("evt.name = ${evt.name}")
    logDebug("evt.value = ${evt.value}")
    
    logDebug("evt.getDevice().getDeviceNetworkId() = ${evt.getDevice().getDeviceNetworkId()}")
    logDebug("evt.getDevice().name = ${evt.getDevice().name}")
    */    
    
    if(evt.getDevice().name == getGroupName())
    {
        if(!evt.value.isInteger())
        {
            // non-numeric button
            return
        }
        
        switch(evt.value.toInteger())
        {
            case 0:
                // disable group
                getChildDevice(getGroupName()).unbindGroupFromApp()
                break
            case 1:
                disableAllGroups()
            
                // enable group
                getChildDevice(getGroupName()).bindGroupFromApp(getSlaveIPs())
                break
        }
    }
}

def heldHandler(evt)
{
    if(evt.getDevice().name == getGroupName())
    {
        if(!evt.value.isInteger())
        {
            // non-numeric button
            return
        }
        
        switch(evt.value.toInteger())
        {
            case 1:
                getChildDevice(getGroupName()).unbindGroupFromApp()
                break
        }
    }
}

def locationHandler(evt)
{
    //logDebug("evt = ${evt}")
    //logDebug("parseLanMessage(evt.description) = ${parseLanMessage(evt.description)}")
    //logDebug("parseLanMessage(evt.description).networkAddress = ${convertHexToIP(parseLanMessage(evt.description).networkAddress)}")
    //logDebug("XML = ${httpGetExec(convertHexToIP(parseLanMessage(evt.description).networkAddress), parseLanMessage(evt.description).ssdpPath)}")
    
    pauseExecution(new Random().nextInt(1000))
    
    def resp = httpGetExec(convertHexToIP(parseLanMessage(evt.description).networkAddress), convertHexToInt(parseLanMessage(evt.description).deviceAddress), parseLanMessage(evt.description).ssdpPath)
    
    if(

        resp?.device?.modelName?.toString()?.contains("SoundSystem") || 
        resp?.device?.modelName?.toString()?.contains("SoundSmart") || 
        resp?.device?.modelName?.toString()?.contains("FC_AUDIO") ) 
    {
        synchronized(discoverySync)
        {
            def tmpDevs = atomicState.devices
            tmpDevs.put(convertHexToIP(parseLanMessage(evt.description).networkAddress).toString(), "${resp.device.modelName.toString()} (${resp.device.friendlyName.toString()})")
            atomicState.devices = tmpDevs
        }
    }
}

private Integer convertHexToInt(hex)
{
    return Integer.parseInt(hex,16)
}

private String convertHexToIP(hex)
{
    return [convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

def getBaseName()
{
    return "${instanceName}"
}

def getGroupName()
{
    return getBaseName() + "-group"
}

def getMasterIP()
{
    return masterIP
}

def getSlaveIPs()
{
    // this needs to be a comma-separated string for
    // reasons imposed by the earlier prototype implementation
    // and as expected by the driver
    return slaveIPs.join(",")
}

def disableAllGroups()
{
    for(instance in app.getParent().getAllChildApps())
    {
        logDebug("instance.getChildDevices() = ${instance.getChildDevices()}")
        instance.getChildDevice(instance.getGroupName()).push("0")
    }
}

def getBaseURI(devIP, devPort)
{
    baseUri = "http://" + devIP
    if(null != devPort)
    {
        baseUri += (":" + devPort)
    }
    
    return baseUri
}

def httpGetExec(devIP, devPort, suffix)
{
    logDebug("httpGetExec(${devIP}, ${suffix})")
    
    try
    {
        def getString = getBaseURI(devIP, devPort) + suffix
        httpGet([uri: getString.replaceAll(' ', '%20'), contentType: "text/xml", requestContentType: "text/xml"])
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
        log.warn "httpGetExec() failed: ${e.message}"
    }
}
