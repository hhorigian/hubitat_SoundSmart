# hubitat_SoundSmart 


Esse é o driver para utilizar os produtos de Streaming da SoundSmart com Hubitat: SA20, SE10, SS50, WA60.
No caso, esse é o driver, para utilizar qualquer um deles. 


# Instalação do Player 
1. Instalar o Driver: Hubitat_TRATO_SoundSmart-Player_Driver.groovy em: Developer Tools, Drivers Code, + New Driver.
2. Instalar o Driver: Hubitat_TRATO_SoundSmart-Group_Driver.groovy em: Developer Tools, Drivers Code, + New Driver.
3. Chegou a hora de instalar o Device(Player). Ir em Devices  -> Add Device -> Add Virtual Device. Device Type = "SoundSmart-Player" / Device Name = O nome que você quer colocar para identificar esse SoundSmart; ex: "SoundSmart Varanda" / Select Room = Sala onde vai ser colocado dentro do setup do Hubitat.
<img src="https://images2.imgbox.com/21/b5/Vq8GrOdS_o.jpg" alt="image host"/>
5. Entrar em   -> "Devices", procurar o "SoundSmart Varanda", colocar o endereço IP no campo "SoundSmart IP Addres" e salvar. 
<img src="https://images2.imgbox.com/cf/a0/yaG5MWmw_o.jpg" alt="image host"/>

<br>
Repetir esse procedimento para todos os SoundSmart player que tenha no projeeto nesse Hub Hubitat.
<br>
# Instalação do Modo Multi-Room  
1. Instalar o APP: Hubitat_TRATO_SoundSmart-Manager_App.groovy em: Developer Tools, Apps Code, + New App.
2. Instalar o APP: Hubitat_TRATO_SoundSmart-Manager Instance_App em: Developer Tools, Apps Code, + New App.
3. Instalar o APP SoundSmart-Manager na seção APPS, indo em Apps - > Add User app -> e procurar o SoundSmart Manager 
<img src="https://images2.imgbox.com/b1/4c/KCLLluZ6_o.jpg" alt="image host"/></a>
4. Entrar em   -> "Devices", procurar o "SoundSmart Manager" e entrar em ele, para assim criar um novo grupo multi-room.
5. Uma vez dentro do SoundSmart Manager, pesquisar os devices na rede, e seleccionar qual vai ser o "Master", e quais serão os "Slaves" do grupo. Criar um nome para o novo grupo criado para que possa ser identificado. Serão criado novos dispositivos virtuais para acessar e controlar o grupo. 

<br>
<b>Instalação no Dashboard </b> 


1- Adicionar o SoundSmart (device) no dashboard.  
2- Adicionar um TILE do tipo "Music Player" no dashboard.  
3- Para adicionar os inputs de HDMI, Optico, Wifi, USB:  

a) Cada input é um botão do SoundSmart (Device). Adicionando um tile novo, mas no caso do tipo BUTTON, e o numero de comando desejado + PUSH. Sendo: <br>
		Botão 1 : inputwifi  
		Botão 2 : inputoptica  
		Botão 3 : inputbluetooth  
        Botão 4 : inputaux   
        Botão 5 : inputusb  
        Botão 6 : inputhdmi    
<br>
<img src="https://images2.imgbox.com/98/9c/Xeb70KvD_o.png" alt="image host"/></a>        
<br>
a) Para adicionar os STATUS de "Playing/Stoped" e o "INPUT" atual seleccionado. 
 Precisa adicionar um atributo do SoundSmart (Device). Adicionando um tile novo, mas no caso do tipo ATTRIBUTE. Adicionar um Tile para o Status, e outro Tile para o Input.
<br>
<img src="https://images2.imgbox.com/73/c7/20O5bZAO_o.png" alt="image host"/></a>


