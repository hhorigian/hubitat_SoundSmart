# hubitat_SoundSmart 


Esse é o driver para utilizar os produtos de Streaming da SoundSmart com Hubitat: SA20, SE10, SS50, WA60.
No caso, esse é o driver + app, para utilizar 


# Instalação do Player 
1. Instalar o Driver: Hubitat_TRATO_SoundSmart-Player_Driver.groovy em: Developer Tools, Drivers Code, + New Driver.
2. Instalar o Driver: Hubitat_TRATO_SoundSmart-Group_Driver.groovy em: Developer Tools, Drivers Code, + New Driver.
3. Chegou a hora de instalar o Device(Player). Ir em Devices  -> Add Device -> Add Virtual Device. Device Type = "SoundSmart-Player" / Device Name = O nome que você quer colocar para identificar esse SoundSmart; ex: "SoundSmart Varanda" / Select Room = Sala onde vai ser colocado dentro do setup do Hubitat.
<img src="https://images2.imgbox.com/21/b5/Vq8GrOdS_o.jpg" alt="image host"/>
5. Entrar em   -> "Devices", procurar o "SoundSmart Varanda", colocar o endereço IP no campo "SoundSmart IP Addres" e salvar. 
<img src="https://images2.imgbox.com/cf/a0/yaG5MWmw_o.jpg" alt="image host"/>

<br>
Repetir esse procedimento para todos os SoundSmart player que tenha no projeeto nesse Hub Hubitat.

# Instalação do Modo Multi-Room  
1. Instalar o APP: Hubitat_TRATO_SoundSmart-Manager_App.groovy em: Developer Tools, Apps Code, + New App.
2. Instalar o APP: Hubitat_TRATO_SoundSmart-Manager Instance_App em: Developer Tools, Apps Code, + New App.
3. Instalar o APP SoundSmart-Manager na seção APPS, indo em Apps - > Add User app -> e procurar o SoundSmart Manager 
<img src="https://images2.imgbox.com/b1/4c/KCLLluZ6_o.jpg" alt="image host"/></a>
4. Entrar em   -> "Devices", procurar o "SoundSmart Manager" e entrar em ele, para assim criar um novo grupo multi-room.
5. Uma vez dentro do SoundSmart Manager, pesquisar os devices na rede, e seleccionar qual vai ser o "Master", e quais serão os "Slaves" do grupo. Criar um nome para o novo grupo criado para que possa ser identificado. Serão criado novos dispositivos virtuais para acessar e controlar o grupo. 
