package hr.ipicek.jamb.network.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;


// callback interface za chat poruke, ovo se implementira u clientu i registrira u chat service
public interface ChatListener extends Remote {

// callback metoda koja se poziva kad stigne nova chat poruka
void onMessageReceived(ChatMessage message) throws RemoteException;
}