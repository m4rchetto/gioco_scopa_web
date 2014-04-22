package gioco_scopa_web.presenter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;

import org.apache.catalina.websocket.MessageInbound;
import org.apache.catalina.websocket.WsOutbound;

import gioco_scopa_web.model.*;
import gioco_scopa_web.model.Procedure_di_gioco.Stato_nodo;

import org.json.*;

/**
 * Need tomcat-koyote.jar on class path, otherwise has compile error "the hierarchy of the type ... is inconsistent"
 *
 */
public class Web_socket extends MessageInbound{

	private String name;
	private WsOutbound myoutbound;
	private Procedure_di_gioco gioco;
	private JSONObject json_obj_buffer=new JSONObject();
	
	public Web_socket(HttpServletRequest httpServletRequest) {}

	@Override
	public void onOpen(WsOutbound outbound) {
	//setting only a client per time
	this.myoutbound = outbound;
	try{
		synchronized(Singleton_gioco.getInstance()){
			while (Singleton_gioco.getInstance().get_just_once()==true){
				Singleton_gioco.getInstance().wait();
			}
			gioco=Procedure_di_gioco.getInstance(); 
		}
	 }
	 catch (InterruptedException e){
		 e.printStackTrace();
	 }
	 //System.out.println("bEGIN client");
	 /*		
		//1)

		
		//2)
		gioco.mescola_carte();
		
		//3)
		HashMap<Integer,Carta> onTableCards=gioco.pesca_carte_tavolo();
		
		for (int i=0;i<10;i++){
			Carta c=onTableCards.get(i+1);
			if (c!=null)
			System.out.print(c.get_seme().toString() + " " + c.get_valore().toString() + "\n");
		}
		
		//4)
		ArrayList<Carta> computer=new ArrayList<Carta>();
		ArrayList<Carta> player=new ArrayList<Carta>();
		// true indicates that cards are distributed from computer to player
		// and so the first to begin is the player
		gioco.distribuisci_carte(true,computer,player);
		
		for (int i=0;i<computer.size();i++){
			Carta c=computer.get(i);
			System.out.print(c.get_seme().toString() + " " + c.get_valore().toString() + "\n");
		}
		for (int i=0;i<player.size();i++){
			Carta c=player.get(i);
			System.out.print(c.get_seme().toString() + " " + c.get_valore().toString() + "\n");
		}
		
		//5) alfa-beta pruning
		//prepara nodo iniziale
		Stato_nodo stato=new Stato_nodo(onTableCards,computer,player,null,0,0,0,0);
		Carta cartaComputer=gioco.alfa_beta_search(stato);
		
		cartaComputer=cartaComputer;
		
		
		//Procedure_di_gioco.Stato_nodo s= new Procedure_di_gioco.Stato_nodo();
		
		
		//for (int i=0;i<gioco.mazzo.size();i++){
		//	Carta c=gioco.mazzo.get(i);
		//	System.out.print(c.get_seme().toString() + " " + c.get_valore().toString() + "\n");
		//}
	 */
 }
 
	@Override
	public void onClose(int status) {
	//System.out.println("Close client");
	synchronized(Singleton_gioco.getInstance()){
		Singleton_gioco.getInstance().set_just_once(false);
	 	Singleton_gioco.getInstance().notify();
	}
	}
 
	@Override
	protected void onBinaryMessage(ByteBuffer arg0) throws IOException {}

	@Override
	protected void onTextMessage(CharBuffer inChar) throws IOException {

	JSONObject json_obj=new JSONObject();	
	String message=inChar.toString();
	
	int type_message=0;
	int m1=message.indexOf("1newpartita"); //1
	int m2=message.indexOf("2mossa_computer"); //2
	int m3=message.indexOf("3aggiorna_mossa_computer"); //3
	int m4=message.indexOf("4mossa_player"); //4
	int m5=message.indexOf("5newset"); //5
	if (m1!=-1){
		type_message=1;
	}else{
		if (m2!=-1){
			type_message=2;
		}else{
			if (m3!=-1){
				type_message=3;
			}else{
				if (m4!=-1){
					type_message=4;
				}else{
					if (m5!=-1){
						type_message=5;
					}
				}
			}			
		}
	}
	
	switch (type_message){
		case 1:
			if (gioco.new_match_is_available()){
				gioco.set_new_match_available(false);
				gioco.mescola_carte();
				gioco.pesca_carte_tavolo_gioco();
				gioco.distribuisci_carte_mano_gioco(true);
				try{
				
				JSONArray json_obj_carte_tavolo=new JSONArray();
				for (int i=0;i<10;i++){
					Carta carta=gioco.get_carte_tavolo_gioco().get(i+1);
					if (carta!=null){
						String carta_json=String.valueOf(carta.get_valore());
						carta_json=carta_json.concat("_" + gioco.get_string_seme(carta.get_seme()));
						json_obj_carte_tavolo.put(carta_json);						
					}
				}
				
				JSONArray json_obj_carte_player=new JSONArray();
				for (int i=0;i<gioco.get_carte_player_mano_partita().size();i++){
					Carta carta=gioco.get_carte_player_mano_partita().get(i);
					if (carta!=null){
						String carta_json=String.valueOf(carta.get_valore());
						carta_json=carta_json.concat("_" + gioco.get_string_seme(carta.get_seme()));
						json_obj_carte_player.put(carta_json);	
					}
				}
				
				json_obj.put("type","1newpartita");
				json_obj.put("tavolo",json_obj_carte_tavolo);
				json_obj.put("player",json_obj_carte_player);
				
				CharBuffer outbuffer = CharBuffer.wrap(json_obj.toString());
				this.myoutbound.writeTextMessage(outbuffer);

				}
				catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else{
				return;
			}
			break;
		case 2:
			
			gioco.attendi(5000);
			
			Procedure_di_gioco.Stato_nodo stato=gioco.next_state();
			
			Carta carta_IA_computer=gioco.alfa_beta_search(stato);
			
			String cartajson=String.valueOf(carta_IA_computer.get_valore());
			cartajson=cartajson.concat("_" + gioco.get_string_seme(carta_IA_computer.get_seme()));
			
			gioco.aggiorna_stato_gioco(Procedure_di_gioco.Stato_nodo.Turno.MAX,carta_IA_computer);
			
			JSONArray json_obj_carte_tavolo=new JSONArray();
			for (int i=0;i<10;i++){
				Carta carta=gioco.get_carte_tavolo_gioco().get(i+1);
				if (carta!=null){
					String carta_json=String.valueOf(carta.get_valore());
					carta_json=carta_json.concat("_" + gioco.get_string_seme(carta.get_seme()));
					json_obj_carte_tavolo.put(carta_json);						
				}
			}
			
			try{
			json_obj.put("type","2mossa_computer");
			json_obj.put("carta_computer",cartajson);

			json_obj_buffer.put("type","3aggiorna_mossa_computer");
			json_obj_buffer.put("tavolo",json_obj_carte_tavolo);
			json_obj_buffer.put("carte_computer", gioco.get_num_carte_computer_partita());
			json_obj_buffer.put("carte_player", gioco.get_num_carte_player_partita());
			json_obj_buffer.put("denari_computer", gioco.get_num_denari_computer_partita());
			json_obj_buffer.put("denari_player", gioco.get_num_denari_player_partita());
			json_obj_buffer.put("primiera_computer", gioco.get_num_7_computer_partita());
			json_obj_buffer.put("primiera_player", gioco.get_num_7_player_partita());
			json_obj_buffer.put("sette_bello_computer", gioco.get_sette_bello_computer_partita());
			json_obj_buffer.put("sette_bello_player", gioco.get_sette_bello_player_partita());
			json_obj_buffer.put("re_bello_computer", gioco.get_re_bello_computer_partita());
			json_obj_buffer.put("re_bello_player", gioco.get_re_bello_player_partita());
			
			CharBuffer outbuffer = CharBuffer.wrap(json_obj.toString());
			this.myoutbound.writeTextMessage(outbuffer);
			
			}
			catch (JSONException e) {
				e.printStackTrace();
			}
			break;
		case 3:
			
			gioco.attendi(3500);
			
			CharBuffer outbuffer = CharBuffer.wrap(json_obj_buffer.toString());
			this.myoutbound.writeTextMessage(outbuffer);
			json_obj_buffer=new JSONObject();
			break;
		case 4:
			break;
		case 5:
			break;
	}

  /*
	this.myoutbound.flush();
  */
	}

}
