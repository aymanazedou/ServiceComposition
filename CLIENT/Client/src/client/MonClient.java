package client;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Properties;

public class MonClient {

	static int ComEnPort;
	static String compositionEngineHostname;
	static String CompositionWsdlDirectory;
	static int delai;
	static int NumClient;
	static String message="";
	static int NumberOfParameters;

	public static void main(String[] args) throws IOException {


		//LOAD PROPERTIES
		Properties prop = new Properties();
		InputStream input = MonClient.class.getClassLoader().getResourceAsStream("Properties/conf.properties");
		prop.load(input);


		//GET VALUES FROM THE PROPERTY FILE
		NumClient = Integer.parseInt(prop.getProperty("nombreClients"));
		ComEnPort = Integer.parseInt(prop.getProperty("compositionEnginePort"));
		compositionEngineHostname = prop.getProperty("compositionEngineHostname");
		delai = Integer.parseInt(prop.getProperty("delai"));
		NumberOfParameters = Integer.parseInt(prop.getProperty("NumberOfParameters"));
		CompositionWsdlDirectory = prop.getProperty("CompositionWsdlDirectory");

		System.out.println("Number of Clients created : "+NumClient);
		System.out.println("Composition Engine Port Number : " + ComEnPort);
		System.out.println("Composition Engine Hostname : "+ compositionEngineHostname);
		System.out.println("Time that the client will sleep before terminating the thread : "+delai);
		System.out.println("Number of parameters : "+NumberOfParameters);
		System.out.println("--------------------------------------------------------------------------");


		//CREATE A TABLE OF CLIENTS
		Client[] tab=new Client[NumClient];

		//INITIATE CLIENTS, EACH CLIENT HAS AN ID AND A RANDOM MESSAGE TO SEND.
		int i;
		for(i=0;i<tab.length;i++) {
			tab[i]=new Client(compositionEngineHostname,ComEnPort,delai,i,CompositionWsdlDirectory,NumberOfParameters);
		}

		//START CLIENTS SEQUENTIALLY
		for(i=0;i<tab.length;i++) {
			tab[i].start();
		}				
	}
}    

//==============================================================CLIENT THREAD============================================================================

class Client extends Thread {


	int compositionEnginePort;
	String compositionEngineHostname;
	int delai;
	int nbParams;
	BufferedReader in;
	PrintWriter out;
	FileOutputStream fos;
	BufferedOutputStream bos;
	String message;
	Socket SocClient=null;
	int ID;
	int bytesRead;
	String CompositionWsdlDirectory;

	public Client(String compositionEngineHostname, int compositionEnginePort,int delai, int ID,String CompositionWsdlDirectory,int nbParams) {
		this.ID=ID;
		this.compositionEnginePort=compositionEnginePort;
		this.compositionEngineHostname=compositionEngineHostname;
		this.delai=delai;
		this.CompositionWsdlDirectory=CompositionWsdlDirectory;
		this.nbParams=nbParams;
	}

	@Override
	public void run() {
		while(true) {
			//CREATE THE SOCKET TO COMMUNICATE WITH THE COMPOSITION ENGINE.
			try {
				SocClient = new Socket(compositionEngineHostname, compositionEnginePort);	
			}catch (Exception e) {
				System.out.println("ID : "+(ID+1)+" Can't create the Socket!!");
				return;
			}
			//INITIATE THE INPUT AND OUTPUT STREAMS TO COMMUNICATION WITH THE COMPOSITION ENGINE USING MESSAGES.
			try {
				out = new PrintWriter(SocClient.getOutputStream());
				in = new BufferedReader(new InputStreamReader(SocClient.getInputStream()));
			} catch (IOException e) {
				e.printStackTrace();
			}
			//SEND THE MESSAGE TO COMPOSITION ENGINE TO GET THE COMPOSITION OF SERVICES.
			int n1=(int)(Math.random()*nbParams);
			int n2=(int)(Math.random()*nbParams);
			message = "exist "+"P"+n1+" P"+n2 +" "+ (ID+1);
			out.println(message);
			out.flush();
			System.out.println("ID : "+(ID+1)+" ||*** Commande : "+ message);

			//READ THE RESPONSE AND GET WSDL FILES FROM THE COMPOSITION ENGINE
			try {
				//RESPONSE IS IN THE FORM [service1,NetworkTime1,ExecutionTime1 < service2,NetworkTime2,ExecutionTime2 < service3,NetworkTime3,ExecutionTime3]
				String Response = in.readLine();
				System.out.println("ID : "+(ID+1)+" || Response : "+Response);


				//Download WSDL files sent by the Composition Engine when the composition exists.
				if(!Response.equals("NoParameter") && !Response.equals("NoService") && !Response.equals("NoComposition")) {

					//Split the message in order to create a directory of the composition, Directory name in the form [PxPy].
					String[] splitMessage = message.split(" ");
					String DirectoryName = splitMessage[1]+splitMessage[2];

					//Create the folder in the directory where we will store all composition folders.
					File newFolder = new File(CompositionWsdlDirectory+"/"+(ID+1)+DirectoryName);
					boolean created =  newFolder.mkdir();
					if(created) {
						System.out.println("ID : "+(ID+1)+" || Folder was created !");
					}
					else {
						System.out.println("ID : "+(ID+1)+" || Unable to create folder");
					}

					//start retrieving all WSDL files corresponding to the services.
					String[] splitResp = Response.split("<");
					InputStream is;
					for (int i = (splitResp.length-1); i >=0; i--) {
						String[] splitPara = splitResp[i].split(",");

						//SEND A MESSAGE TO THE COMPOSTION ENGINE TO DOWNDLOAD THE WSDL FILE OF THE SERVICES MENTIONED IN THE COMPOSITION RESPONSE.
						out.println("SendFile "+ splitPara[0]);
						out.flush();
						
						//GET THE MESSAGE FROM THE COMPOSITION ENGINE ABOUT THE SIZE OF THE FILE
						long FileSize=Long.parseLong(in.readLine());
						long AlreadyRead=0;
						
						//READ THE STREAM AND WRITE ON THE FILE.
						try {
							is = SocClient.getInputStream();
							
							//CREATING THE BUFFER WHERE THE STREAM READ WILL BE STOCKED (BUFFER OF SIZE 350 BYTES)
							byte [] mybytearray  = new byte [350];
							//CREATING THE FILE WHERE THE STREAMED DATA WILL BE WRITTEN.
							fos = new FileOutputStream(CompositionWsdlDirectory+"/"+(ID+1)+DirectoryName+"/"+splitPara[0]+".wsdl");
							bos = new BufferedOutputStream(fos);
							//START READING THE STREAM AND WRITING IN THE FILE.
							while (AlreadyRead<FileSize) {
								bytesRead=is.read(mybytearray,0,mybytearray.length);
								bos.write(mybytearray, 0 , bytesRead);
								bos.flush();
								System.out.println("ID : "+(ID+1)+" || bytes downloaded for the service : " + splitPara[0] + " (" + bytesRead + " bytes )");
								AlreadyRead+=bytesRead;
							}
							fos.close();
							bos.close();
							System.out.println("ID : "+(ID+1)+" || WSDL file of " + splitPara[0] + " downloaded");
						} catch (IOException e) {
							e.printStackTrace();
						}
					}

					//START REQUESTING THE SERVICES.
					System.out.println("ID : "+(ID+1)+" || Start requesting services...");
					for (int i = (splitResp.length-1); i >= 0 ; i--) {
						String[] splitPara = splitResp[i].split(",");
						System.out.println("ID : "+(ID+1)+" || --> Sending request to the service : "+splitPara[0]);
						System.out.println("ID : "+(ID+1)+" || Network delay (round trip) to reach the service "+splitPara[0]+" is equal to "+ (2*Integer.parseInt(splitPara[1])+"ms"));
						try {
							//TIME REQUIRED TO REACH THE SERVICE AND COME BACK.
							sleep((2*Integer.parseInt(splitPara[1])));
						}catch (Exception e) {
							System.out.println("ID : "+(ID+1)+" Can't Sleep while establishing the round trip time");
						}
						System.out.println("ID : "+(ID+1)+" || Execution time of the service "+splitPara[0]+" is equal to "+Integer.parseInt(splitPara[2])+"ms");
						try {
							//TIME OF EXECUTION OF THE SERVICES REQUESTED.
							sleep(Integer.parseInt(splitPara[2]));
						}catch (Exception e) {
							System.out.println("ID : "+(ID+1)+"Can't Sleep while stablishing the exution time");
						}
					}
				}
			}catch (Exception e) {
			}

			
			//WHEN FINISHED, WAIT SOME TIME, THEN SEND ANOTHER REQUEST OF COMPOSITION TO COMPOSITION ENGINE
			try {
				sleep(delai);
			}catch (Exception e) {
				System.out.println("Can't Sleep!!");
			}
			try {
				SocClient.close();	
			}catch (Exception e) {
				System.out.println("Socket can't be closed");
			}
		}
	}
}