/**this program represent the registry that will be connected to a database and search for services that have the given input and return outputs
 * that will be communicated to the composition engine. The program verify in the data base if the parameter exist (if not, return NoParameter),
 * or if it exists and there is no service that take it as input (Returns NoService), or return the services and the corresponding output.
 */

package registry;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


//================================================================================================================
public class MonAnnuaire {
	static Connection connection=null;
	static String DbUserName;
	static String DbUserPassword;
	static String DbName;
	static String DbTableName;
	static String DbHostName;
	static String JDBC_Driver;
	
	
	public static void main(String[] args) throws SQLException, IOException {
		ServerSocket socketAnnuaire;
		Socket Socketdeannuaire;	
		
		//LOAD PROPERTIES
    	Properties prop = new Properties();
    	InputStream input = MonAnnuaire.class.getClassLoader().getResourceAsStream("Properties/conf.properties");
    	prop.load(input);

    	
    	//GET VALUES FROM THE PROPERTY FILE
    	int ListeningPort = Integer.parseInt(prop.getProperty("ListeningPort"));
    	DbUserName = prop.getProperty("DbUserName");
    	DbUserPassword = prop.getProperty("DbUserPassword");
    	DbName = prop.getProperty("DbName");
    	DbTableName = prop.getProperty("DbTableName");
    	DbHostName = prop.getProperty("DbHostName");
    	JDBC_Driver = prop.getProperty("JDBC_Driver");
    	
		
		
		
		
		//CONNECT TO THE DATABASE==============================================================================
		System.out.println("------------Mysql JDBC Connection-------------");
		try {
			Class.forName(JDBC_Driver);
		}
		catch (ClassNotFoundException e) {
			System.out.println("Driver not found!!");
			e.printStackTrace();
			return;
		}
		System.out.println("JDBC Driver OK!");
		try {
			MonAnnuaire.connection = DriverManager.getConnection("jdbc:mysql://"+DbHostName+"/"+DbName+"?"+ "user="+DbUserName+"&password="+DbUserPassword);
		}catch (Exception e) {
			return;
		}
		System.out.println("OK! Connected to DataBase ..");
		System.out.println("============================");
		
		//Each time a Son-thread will treat the request of a client of the Composition Engine===============
		socketAnnuaire = new ServerSocket(ListeningPort);
		while (true) {
			Socketdeannuaire = socketAnnuaire.accept();
			(new MonFils(Socketdeannuaire)).start();
		}
	}
}
//===========================================================================================================








//================================================================================================================
class MonFils extends Thread{
	String msg;
	Socket soc;
	BufferedReader in;
	PrintWriter out;
	List<String> links = new ArrayList<>();
	FileInputStream fis;
	BufferedInputStream bis;
	OutputStream os;

	public MonFils(Socket soc) {
		this.soc=soc;
		try {
			out = new PrintWriter(soc.getOutputStream());
			in = new BufferedReader(new InputStreamReader(soc.getInputStream()));
			fis = null;
		    bis = null;
		    os = null;
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	@Override
	public void run() {
		try {
		while(true) {
			//to verify that a parameter exist in the data base, it should exist either as an input or output of a service.
			boolean ParamExist = false;
			//verify if at least one service exists with the requested input parameter.
			boolean ServiceExist = false;
			
			msg = "";
			msg = in.readLine();
			
			//get the information needed from the message==========================================================
			String[] splitMsg = msg.split(" ");
			//Create Statement
			java.sql.Statement statement = MonAnnuaire.connection.createStatement();
			//send reply=====================command : EXIST=======================================================
			if (splitMsg[0].equals("exist")){
				links.clear();
				ResultSet resultSetIN = null;
				ResultSet resultSetVerifyParameter = null;
				String ServiceName = null;
				String OutPara = "";
				String ResponseExist = "";
				int Net_Time;
				int Exec_Time;
				
				resultSetIN = statement.executeQuery("SELECT OUTP,servicename,linkWSDL,Net_Time,Exec_Time FROM "+MonAnnuaire.DbName+"."+MonAnnuaire.DbTableName+" WHERE INP ='"+splitMsg[1]+"'");
				int i=0;
				
				//Construct the message
				while (resultSetIN.next()) {
					ParamExist=true;
					ServiceExist=true;
					OutPara = resultSetIN.getString("OUTP");
					ServiceName = resultSetIN.getString("servicename");
					Net_Time = resultSetIN.getInt("Net_Time");
					Exec_Time = resultSetIN.getInt("Exec_Time");
					if (i==0){
						ResponseExist =ResponseExist+OutPara+","+ServiceName+","+Net_Time+","+Exec_Time;
					}
					else {
						ResponseExist =ResponseExist+" "+OutPara+","+ServiceName+","+Net_Time+","+Exec_Time;
					}
					i++;
					
				}
				
				resultSetVerifyParameter = statement.executeQuery("SELECT INP FROM "+MonAnnuaire.DbName+"."+MonAnnuaire.DbTableName+" WHERE OUTP ='"+splitMsg[1]+"'");
				
				if (resultSetVerifyParameter.next()) {
					ParamExist=true;
				}
				if (!ParamExist) {
					ResponseExist="NoParameter";
				}
				if (!ServiceExist && ParamExist) {
					ResponseExist="NoService";
				}
				
				//send the Response of EXIST message
				System.out.println("Command : " + splitMsg[0] + " || Parameter : "+ splitMsg[1]+" || Response : "+ResponseExist);
				out.println(ResponseExist);
				out.flush();
			}
			
			// send WSDL file
			else if (splitMsg[0].equals("SendFile")){
				ResultSet resultSetWSDL = null;
				resultSetWSDL = statement.executeQuery("SELECT linkWSDL FROM "+MonAnnuaire.DbName+"."+MonAnnuaire.DbTableName+" WHERE servicename ='"+splitMsg[1]+"'");
					try {
						
						//SEND THE FILE'S SIZE TO THE COMPOSITION ENGINE.
						resultSetWSDL.next();
						File File = new File (resultSetWSDL.getString("linkWSDL"));
						int length = (int)File.length();
						out.println(length);
						out.flush();
						
						//SENDING THE FILE
						os = soc.getOutputStream();
						
						//CREATING THE BUFFER WHERE THE DATA READ FROM THE FILE WILL BE STOCKED.
						byte [] mybytearray  = new byte [300];
						fis = new FileInputStream(File);
						bis = new BufferedInputStream(fis);
						
						//START READING FROM THE FILE AND SENDING TO THE COMPOSITION ENGINE.
						int bytesRead;
						while((bytesRead = bis.read(mybytearray,0,mybytearray.length))>0) {
							os.write(mybytearray,0,bytesRead);
							os.flush();
							System.out.println("Data of the service "+ splitMsg[1]+" sent (" + mybytearray.length + " bytes) ...");
							if (bis.available()<=0) {
								break;
							}
						}
						
						System.out.println("Service : "+ splitMsg[1]+" SENT");

					}catch(IOException e) {
						e.printStackTrace();
					}
					fis.close();
					bis.close();
				}
		}
		}catch (IOException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}