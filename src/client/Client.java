package client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

import org.json.JSONObject;

public class Client {
	static boolean isPending = false;
	
	public static void main(String[] args) {
		final String IP = "127.0.0.1";
		final int PORT = 9001;
		
		
		try ( 
			Socket socket = new Socket(IP, PORT);
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
			BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			Scanner scan = new Scanner(System.in)){
			
			System.out.println("서버에 연결되었습니다.");
			
			
		
		//	ReceiveThread rt = new ReceiveThread(br);
		//	rt.start();
			
			String id = sendId(scan, pw);
			Thread.sleep(300);
			
			while(true) {
				System.out.println("메뉴를 선택하세요:");
				System.out.println("1: [커피 주문]");
				System.out.println("2: [전체 채팅 보내기]");
				System.out.println("3: [1:1채팅]");
				System.out.println("4: [종료]");
				String choice = scan.nextLine();
				
				switch (choice) {
                case "1":
                    sendOrder(scan, pw, id);
                    break;
                case "2":
                    sendAllChat(scan, pw, id);
                    break;
                case "3":
                    sendOneChat(scan, pw, id); // 1:1 채팅 메소드 호출
                    break;
                case "4":
                    System.out.println("클라이언트를 종료합니다.");
                    return; // 메인 메소드 종료로 프로그램 종료
                default:
                    System.out.println("잘못된 입력입니다.");
				}
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static String sendId(Scanner sc, PrintWriter pw) {
		System.out.println("사이렌오더에 사용할 ID를 입력하세요. >>");
		String id = sc.nextLine();
		
		JSONObject idObj = new JSONObject();
		idObj.put("cmd", "ID");
		idObj.put("id", id);
		
		String packet = idObj.toString();
		pw.println(packet);
		pw.flush();
		
		return id;
	}
	public static void sendOrder(Scanner sc, PrintWriter pw, String id) {
		System.out.println("주문할 커피번호와 수량을 입력하세요");
		String orderInfo = sc.nextLine();
		String[]parts = orderInfo.split(" ");
		if(parts.length==2) {
			JSONObject orderObj = new JSONObject();
			orderObj.put("cmd", "ORDER");
			orderObj.put("id", id);
			orderObj.put("coffeeId",Integer.parseInt(parts[0]));
			orderObj.put("quantity",Integer.parseInt(parts[1]));
			
			pw.println(orderObj.toString());
			pw.flush();
		}else {
			System.out.println("잘못된 입력입니다.");
		}
	}



	
	
	public static void sendAllChat (Scanner sc, PrintWriter pw, String id) {
		boolean isRun = true;
		while(isRun) {
			System.out.println("전송 메세지 (quit는 종료)>>");
			String msg = sc.nextLine();
			if(msg.equals("quit")) {
				isRun = false;
				break;
			}
			JSONObject packetObj = new JSONObject();
			packetObj.put("cmd", "ALLCHAT");
			packetObj.put("id", id);
			packetObj.put("msg", msg);
			
			String packet = packetObj.toString();
			
			pw.println(packet);
			pw.flush();
		
		}
	}
	public static void sendOneChat(Scanner sc, PrintWriter pw, String id) {
		System.out.println("대화할 상대방의 ID를 입력하세요:");
		String yourId = sc.nextLine();
		System.out.println("보낼 메시지를 입력하세요:");
		String message = sc.nextLine();
		
		JSONObject chatObj = new JSONObject();
		chatObj.put("cmd", "ONECHAT");
		chatObj.put("yourId", yourId);
		chatObj.put("id", id);
		chatObj.put("msg",message);
		
		pw.println(chatObj.toString());
		pw.flush();
		
	}
	
	
	
class ReceiveThread extends Thread{
	private BufferedReader br = null;
	
	public ReceiveThread(BufferedReader br) {
		this.br =br;
	}
	@Override
	public void run() {
		try {
			while(true) {
				String packet = br.readLine();
				if(packet==null)
					break;
				
				JSONObject packetObj = new JSONObject(packet);
				processPacket(packetObj);
				Client.isPending =false;
			}
			
		} catch(Exception e) {
			System.out.println(e.getMessage());
		}
	}
	public void processPacket(JSONObject packetObj) {
		String cmd = packetObj.getString("cmd");
		if(cmd.equals("ID")) {
			String ack = packetObj.getString("ack");
			if(ack.equals("ok"))
				System.out.println("[서버 응답] ID 등록 성공");
			else if(ack.equals("fail"))
				System.out.println("[서버 응답] ID 등록 실패");
			else
				System.out.printf("[서버 응답] ID 등록 %s\n", ack);
		}
	
	else if(cmd.equals("ALLCHAT")) {
		String ack = packetObj.getString("ack");
		if(ack.equals("ok"))
			System.out.println("[서버 응답] 채팅 전송 성공");
		else if(ack.equals("fail"))
			System.out.println("[서버 응답] 채팅 전송 실패");
		else
			System.out.printf("[서버 응답] 채팅 전송 %s\n", ack);
		}
		//서버의 1:1 채팅에 대한 응답
	else if(cmd.equals("ONECHAT")) {
		String fromId = packetObj.getString("id");
		String msg = packetObj.getString("msg");
		System.out.printf("[1:1채팅]%s:%s\n",fromId,msg);
		
	}
	else if(cmd.equals("BROADCHAT")) {
		String id = packetObj.getString("id");
		String msg = packetObj.getString("msg");
		System.out.printf("채팅 메세지 [%s] %s\n", id,msg);
	}
	else if(cmd.equals("UNICHAT")) {
		String fromId = packetObj.getString("id");
		String msg = packetObj.getString("msg");
		System.out.printf("[1:1채팅]%s:%s\n",fromId,msg);
		
	}
	}
}
}



