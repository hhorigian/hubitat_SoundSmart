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


NOTE: this structure was copied from @jwetzel492's Combined Presence parent app
https://github.com/joelwetzel/Hubitat-Combined-Presence

NOTE: this structure was copied from @tomw

*/

definition(
    name: "SoundSmart - Manager",
    namespace: "TRATO",
    author: "VH",
    description: "",
    category: "Convenience",
	iconUrl: "",
    iconX2Url: "",
    iconX3Url: "")


preferences {
     page name: "mainPage", title: "", install: true, uninstall: true
}

def installed() {
    log.info "Installed with settings: ${settings}"
    initialize()
}

def updated() {
    log.info "Updated with settings: ${settings}"
    initialize()
}

def initialize() {
    log.info "There are ${childApps.size()} child apps installed."
    childApps.each { child ->
    	log.info "Child app: ${child.label}"
    }
}

def installCheck() {         
	state.appInstalled = app.getInstallationState()
	
	if (state.appInstalled != 'COMPLETE') {
		section{paragraph "Click no  'Done' para instalar o '${app.label}' APP principal"}
  	}
  	else {
    	log.info "APP Principal Instalado OK"
  	}
}

def mainPage()
{
    dynamicPage(name: "mainPage")
    {
        installCheck()
		
		if (state.appInstalled == 'COMPLETE')
        {
            section("<b>Instâncias de SoundSmart Manager :</b>")
            {
                app(name: "anyOpenApp", appName: "SoundSmart - Manager Instance", namespace: "TRATO", title: "<b>Adicionar um novo Grupo de SoundSmart</b>", multiple: true)
			}
		}
	}
}


