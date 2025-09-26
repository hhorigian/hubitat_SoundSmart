# hubitat_SoundSmart 


Esse é o driver para utilizar os produtos de Streaming da SoundSmart com Hubitat: SA20, SE10, SS50, WA60.
No caso, esse é o driver, para utilizar qualquer um deles. 

Esses drivers estão disponiveis no Hubitat Package Manager (HPM)

# Instalação do Player (Manual)
1. Instalar o Driver: Hubitat_TRATO_SoundSmart-Player_Driver.groovy em: Developer Tools, Drivers Code, + New Driver.
2. Instalar o Driver: Hubitat_TRATO_SoundSmart-Group_Driver.groovy em: Developer Tools, Drivers Code, + New Driver.
3. Chegou a hora de instalar o Device(Player). Ir em Devices  -> Add Device -> Add Virtual Device. Device Type = "SoundSmart-Player" / Device Name = O nome que você quer colocar para identificar esse SoundSmart; ex: "SoundSmart Varanda" / Select Room = Sala onde vai ser colocado dentro do setup do Hubitat.
<img src="https://images2.imgbox.com/21/b5/Vq8GrOdS_o.jpg" alt="image host"/>
5. Entrar em   -> "Devices", procurar o "SoundSmart Varanda", colocar o endereço IP no campo "SoundSmart IP Addres" e salvar. 
<img src="https://images2.imgbox.com/cf/a0/yaG5MWmw_o.jpg" alt="image host"/>

<br>
Repetir esse procedimento para todos os SoundSmart player que tenha no projeeto nesse Hub Hubitat.
<br>
<br>
**Instalação do Modo Multi-Room  (Manual)**

1. Instalar o APP: Hubitat_TRATO_SoundSmart-Manager_App.groovy em: Developer Tools, Apps Code, + New App.  
3. Instalar o APP: Hubitat_TRATO_SoundSmart-Manager Instance_App em: Developer Tools, Apps Code, + New App. 
4. Instalar o APP SoundSmart-Manager na seção APPS, indo em Apps - > Add User app -> e procurar o SoundSmart Manager  
<img src="https://images2.imgbox.com/b1/4c/KCLLluZ6_o.jpg" alt="image host"/></a>

6. Entrar em   -> "APP", procurar o "SoundSmart Manager" e entrar em ele, para assim criar um novo grupo multi-room.
   
8. Uma vez dentro do SoundSmart Manager, pesquisar os devices na rede, e seleccionar qual vai ser o "Master", e quais serão os "Slaves" do grupo. Criar um nome para o novo grupo criado para que possa ser identificado. Será criado um novo DEVICE, com o nome do grupo e childs dentro dele.
9. Esse novo device, tem o master control das musicas, pause, volume.
10. O grupo só fica ativo, quando enviado um comando PUSH = 1. Nesse momento o grupo se forma. Para separar o grupo, é só enviar um PUSH = 0. Isso pode ser feito via dashboard fazendo um push, ou se preferir uma regra para ligar/desligar o grupo usando um virtual switch. 
   

<br>
<b>Instalação no Dashboard </b> 

1- Adicionar o SoundSmart (device) no dashboard.  
2- Adicionar um TILE do tipo "Music Player" no dashboard.   

</br>
<b>Para adicionar os inputs de HDMI, Optico, Wifi, USB, et:  </b> <br> 
a) Cada input é um botão do SoundSmart (Device). Adicionando um tile novo, mas no caso do tipo BUTTON, e o nome da função desejada + PUSH.(a partir da versão 2.1.4 do driver).  Sendo as funções disponíveis respeitando minusculas e maiusculas:


	inputwifi   
	inputoptica  
	inputbluetooth  
        inputaux   
        inputusb  
        inputhdmi    
<br>
<img src="https://images2.imgbox.com/98/9c/Xeb70KvD_o.png" alt="image host"/></a>        
<br><br>

<b>Para adicionar os STATUS de "Playing/Stoped" e o "INPUT" atual seleccionado. </b><br>
 Precisa adicionar um atributo do SoundSmart (Device). Adicionando um tile novo, mas no caso do tipo ATTRIBUTE. Adicionar um Tile para o Status, e outro Tile para o Input.
<br><br>

<img src="https://images2.imgbox.com/73/c7/20O5bZAO_o.png" alt="image host"/></a>

<br><br>


<b>Para adicionar um PRESET JÁ SALVO  anteriormente no WIIM no SoundSmart. </b> 
<br> 
 Cada PRESET é um botão do SoundSmart (Device). Adicionando um tile novo, mas no caso do tipo BUTTON, e o nome da função desejada + PUSH(a partir da versão 2.1.4 do driver) . Sendo:
<br>
Presets: 

        preset1 
        preset2  
        preset3  
        preset4  
        preset5 
        preset6  
        preset7  
        preset8  
        preset9  
        preset10  
<br><br>
<b>Para Silenciar os Prompts do SoundSmart quando eles voltam de uma queda de Energia</b> <br> 
Usar o setup do Device, o comando PUSH. Com as seguintes opções:

       promptDisable 
       promptEnable 

<b>Para Mudar o tipo de reprodução: Shuffle  </b> <br> 
Comando PUSH. Com as seguintes opções:

       repeatall 
       repeatsingle
       shufflerepeat 
       shufflenorepeat
       

<b>É possível a partir da versão 2.1.4 do driver, enviar qualquer comando usando o botão + PUSH + nome da função. Aqui as funções disponíveis:  </b> <br> 

       play 
       pause
       stop 
       nextTrack
       previousTrack
       volumeDown
       volumeUp
       mute
       unmute
       resume
       

