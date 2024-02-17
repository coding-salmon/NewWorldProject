package Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

import org.json.JSONObject;

public class Server {
	public static void main(String[] args) {
		
		//사이렌오더 서버 포트 9001
		final int PORT = 9001;
		Hashtable<String, Socket> clientHt = new Hashtable<>();
		SalesDatabase salesDatabase = new SalesDatabase();
		CoffeeDatabase coffeeDatabase = new CoffeeDatabase();
		
		try {
			ServerSocket serverSocket = new ServerSocket(PORT);
			String mainThreadName = Thread.currentThread().getName();
			
		while(true) {
			System.out.printf("[서버-%s] Client의 접속을 기다립니다...\n", mainThreadName);
			Socket socket = serverSocket.accept();
			WorkerThread wt = new WorkerThread(socket, clientHt, salesDatabase, coffeeDatabase);
			wt.start();
			}
		} catch(Exception e) {
			System.out.println(e.getMessage());
		} finally {
			salesDatabase.close();
			coffeeDatabase.close();
		}
	}
}
class WorkerThread extends Thread{
	private Socket socket;
	private Hashtable<String, Socket> ht;
	private SalesDatabase salesDatabase;
	private CoffeeDatabase coffeeDatabase;
	
	public WorkerThread(Socket socket, Hashtable<String, Socket> ht,
			SalesDatabase salesDatabase, CoffeeDatabase coffeeDatabse) {
		this.socket =socket;
		this.ht = ht;
		this.salesDatabase = salesDatabase;
		this.coffeeDatabase = coffeeDatabse;
	}
	
	@Override
	public void run() {
		try {
			InetAddress inetAddr = socket.getInetAddress();
			System.out.printf("<서버-%s> %s로부터 접속했습니다.\n", getName(), inetAddr.getHostAddress());
			
			InputStream in = socket.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			OutputStream out = socket.getOutputStream();
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(out));
			
			
			while(true) {
				String line = br.readLine();
				if(line == null)
					break;
				JSONObject packetObj = new JSONObject(line);
				processPacket(packetObj);
			}
		} catch(Exception e) {
			System.out.printf("<서버-%s>%s\n", getName(), e.getMessage());
		}finally {
			try {
				socket.close();
			}catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void processPacket(JSONObject packetObj) throws IOException{
		JSONObject ackObj = new JSONObject();
		String cmd = packetObj.getString("cmd");
		
		switch (cmd) {
        case "ID":
            handleIdRequest(packetObj);
            break;
        case "ORDER":
            handleOrderRequest(packetObj);
            break;
        case "ALLCHAT":
            handleAllChatRequest(packetObj);
            break;
        case "ONECHAT":
            handleOneChatRequest(packetObj);
            break;
        default:
            System.out.println("알 수 없는 명령어: " + cmd);
            // 알 수 없는 명령어에 대한 처리, 예를 들어 클라이언트에게 알림을 보낼 수 있습니다.
            ackObj.put("cmd", "ERROR");
            ackObj.put("message", "알 수 없는 명령어입니다.");
            sendAck(ackObj.toString());
            break;
		}
}
		//id등록요청
		private void handleIdRequest(JSONObject packetObj) throws IOException{
			String id = packetObj.getString("id");
			//클라이언트 ID와 소켓을 해시테이블에 저장
			ht.put(id, this.socket);
			//성공 응답 전송
			JSONObject ackObj = new JSONObject();
			ackObj.put("cmd", "ID_ACK");
			ackObj.put("ack", "ok");
			ackObj.put("messge","ID등록 성공. 메뉴: 1.[커피주문] 2.[전체 채팅] 3. [1:1채팅] 4.[종료]");
			sendAck(ackObj.toString());//응답 전송 메소드 호출
		}
				
		//커피주문요청
		private void handleOrderRequest(JSONObject packetObj) throws IOException {
			//주문 정보 추출
			int coffeeId = packetObj.getInt("coffeeId");
			int quantity = packetObj.getInt("quantity");
			String id = packetObj.getString("id");
			
			//주문 처리
			double totalPrice =processOrder(coffeeId, quantity);
			String coffeeName = getCoffeeName(coffeeId);
			
			//주문 정보를 데이터 베이스에 저장
			salesDatabase.saveSales(id, coffeeName, quantity, totalPrice);
			
			//주문 성공 응답 전송
			JSONObject ackObj = new JSONObject();
			ackObj.put("cmd", "ORDER_ACK");
			ackObj.put("totalPrice", totalPrice);
			sendAck(ackObj.toString());
			
			
		}
		
		//접속자 전체한테 채팅 메시지 전송
		private void handleAllChatRequest(JSONObject packetObj) throws IOException{
			String id = packetObj.getString("id");
			String msg = packetObj.getString("msg");
			
			broadcastMessage(id, msg);
		}
			
			
		//특정 yourid 사용 클라이언트에 전송 패킷
		private void handleOneChatRequest(JSONObject packetObj) throws IOException{
			String id = packetObj.getString("id");
			String yourid = packetObj.getString("yourid");
			String msg = packetObj.getString("msg");
			unicastMessage(yourid,msg);
		}
		
	
	private void unicast(String packet, String yourid) throws IOException{
		Socket sock = (Socket) ht.get(yourid);
		
		OutputStream out = sock.getOutputStream();
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(out));
		pw.println(packet);
		pw.flush();
	}
	
	// 모든 클라이언트에게 메시지를 브로드캐스트하는 메소드
	private void broadcastMessage(String senderId, String message) throws IOException {
	    // 모든 클라이언트에게 메시지 전송 로직 구현
		Set<String> keys = ht.keySet();
		for(String key : keys) {
			Socket sock = ht.get(key);
			
			try {
				PrintWriter pw = new PrintWriter(new OutputStreamWriter(sock.getOutputStream()), true);
				JSONObject msgObj = new JSONObject();
				msgObj.put("cmd", "BROADCAST");
				msgObj.put("id", senderId);
				msgObj.put("msg", message);
				
				pw.println(msgObj.toString());
			} catch(IOException e) {
				System.out.println("메세지 전송 실패:"+e.getMessage());
			}
		}
	}

	// 지정된 클라이언트에게 메시지를 유니캐스트하는 메소드
	private void unicastMessage(String recipientId, String message) throws IOException {
	    // 지정된 클라이언트에게 메시지 전송 로직 구현
		Socket recipientSocket = ht.get(recipientId);
		if(recipientSocket !=null) {
			try {
				//수신자 소켓으로 출력 스트립 생성
				PrintWriter pw = new PrintWriter(new OutputStreamWriter(recipientSocket.getOutputStream()),true);
				JSONObject msgObj = new JSONObject();
				msgObj.put("cmd","UNICAST");
				msgObj.put("msg", message);
				
				pw.println(msgObj.toString());
			}catch(IOException e) {
				System.out.println("유니캐스트 메시지 전송 실패: "+e.getMessage());
			}
		}else {
			System.out.println("수신자 ID("+recipientId+")에 해당하는 클라이언트가 연결되어 있지 않습니다.");
		}
	}

	// 응답을 클라이언트에게 전송하는 메소드
	private void sendAck(String ack) throws IOException {
	    PrintWriter pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
	    pw.println(ack);
	    pw.flush();
	}
	
		//주문 처리 메소드
		private double processOrder(int coffeeId, int quantity) {
			String coffeeInfo = coffeeDatabase.getCoffeeInfo(coffeeId);
			JSONObject coffeeJson = new JSONObject(coffeeInfo);
			double coffeePrice = coffeeJson.getDouble("price");
			return coffeePrice *quantity;
		}
		
		//커피 정보 조회
		private String getCoffeeName(int coffeeId) {
			String coffeeInfo = coffeeDatabase.getCoffeeInfo(coffeeId);
			JSONObject coffeeJson = new JSONObject(coffeeInfo);
			return coffeeJson.getString("name");
		
	}
	
}












